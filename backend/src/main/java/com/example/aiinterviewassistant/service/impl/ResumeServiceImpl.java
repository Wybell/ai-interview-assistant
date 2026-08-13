package com.example.aiinterviewassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiinterviewassistant.config.ResumeStorageProperties;
import com.example.aiinterviewassistant.dto.ResumeResponse;
import com.example.aiinterviewassistant.dto.ResumePreviewResponse;
import com.example.aiinterviewassistant.entity.MockInterviewSession;
import com.example.aiinterviewassistant.entity.ResumeDocument;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.example.aiinterviewassistant.mapper.MockInterviewSessionMapper;
import com.example.aiinterviewassistant.mapper.ResumeDocumentMapper;
import com.example.aiinterviewassistant.service.ResumeService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ResumeServiceImpl implements ResumeService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private final ResumeDocumentMapper resumeDocumentMapper;
    private final MockInterviewSessionMapper mockInterviewSessionMapper;
    private final ResumeStorageProperties storageProperties;

    public ResumeServiceImpl(
            ResumeDocumentMapper resumeDocumentMapper,
            MockInterviewSessionMapper mockInterviewSessionMapper,
            ResumeStorageProperties storageProperties) {
        this.resumeDocumentMapper = resumeDocumentMapper;
        this.mockInterviewSessionMapper = mockInterviewSessionMapper;
        this.storageProperties = storageProperties;
    }

    @Override
    public ResumeResponse upload(Long userId, MultipartFile file) {
        requireUser(userId);
        validateFile(file);

        String originalFileName = Path.of(file.getOriginalFilename()).getFileName().toString();
        String extension = getExtension(originalFileName);
        String extractedContent = extractText(file, extension);
        if (extractedContent.isBlank()) {
            throw new BusinessException(400, "Resume contains no readable text");
        }

        Path targetPath = savePrivateFile(file, extension);
        ResumeDocument document = new ResumeDocument();
        document.setUserId(userId);
        document.setOriginalFileName(originalFileName);
        document.setContentType(normalizeContentType(file.getContentType()));
        document.setStoragePath(targetPath.toString());
        document.setExtractedContent(extractedContent);
        document.setCreateTime(LocalDateTime.now());
        try {
            resumeDocumentMapper.insert(document);
        } catch (RuntimeException exception) {
            deleteQuietly(targetPath);
            throw exception;
        }
        return toResponse(document);
    }

    @Override
    public List<ResumeResponse> getResumes(Long userId) {
        requireUser(userId);
        return resumeDocumentMapper.selectList(new LambdaQueryWrapper<ResumeDocument>()
                        .eq(ResumeDocument::getUserId, userId)
                        .orderByDesc(ResumeDocument::getCreateTime))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ResumePreviewResponse preview(Long userId, Long resumeId) {
        ResumeDocument document = getOwnedResume(userId, resumeId);
        return new ResumePreviewResponse(
                document.getId(),
                document.getOriginalFileName(),
                document.getContentType(),
                document.getExtractedContent()
        );
    }

    @Override
    public ResumeDocument getOwnedResume(Long userId, Long resumeId) {
        requireUser(userId);
        ResumeDocument document = resumeDocumentMapper.selectOne(new LambdaQueryWrapper<ResumeDocument>()
                .eq(ResumeDocument::getId, resumeId)
                .eq(ResumeDocument::getUserId, userId));
        if (document == null) {
            throw new BusinessException(404, "Resume not found");
        }
        return document;
    }

    @Override
    public void delete(Long userId, Long resumeId) {
        ResumeDocument document = getOwnedResume(userId, resumeId);
        Long sessionCount = mockInterviewSessionMapper.selectCount(new LambdaQueryWrapper<MockInterviewSession>()
                .eq(MockInterviewSession::getResumeId, resumeId));
        if (sessionCount != null && sessionCount > 0) {
            throw new BusinessException(409, "Resume is used by an interview record and cannot be deleted");
        }
        resumeDocumentMapper.deleteById(document.getId());
        deleteQuietly(Path.of(document.getStoragePath()));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "Resume file must not be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(400, "简历文件不能超过 10 MB");
        }
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank() || !isSupported(getExtension(originalFileName))) {
            throw new BusinessException(400, "Only PDF, DOCX, and TXT resumes are supported");
        }
    }

    private String extractText(MultipartFile file, String extension) {
        try (InputStream inputStream = file.getInputStream()) {
            return switch (extension) {
                case "pdf" -> extractPdf(inputStream);
                case "docx" -> extractDocx(inputStream);
                case "txt" -> new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
                default -> throw new BusinessException(400, "Unsupported resume file type");
            };
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BusinessException(400, "Resume file cannot be read");
        }
    }

    private String extractPdf(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            return new PDFTextStripper().getText(document).trim();
        }
    }

    private String extractDocx(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText().trim();
        }
    }

    private Path savePrivateFile(MultipartFile file, String extension) {
        try {
            Path root = Path.of(storageProperties.getStorageDirectory()).toAbsolutePath().normalize();
            Files.createDirectories(root);
            Path target = root.resolve(UUID.randomUUID() + "." + extension).normalize();
            if (!target.startsWith(root)) {
                throw new BusinessException(500, "Resume storage path is invalid");
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return target;
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BusinessException(500, "Resume file could not be stored");
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // The database record has already been removed; storage cleanup can be retried operationally.
        }
    }

    private boolean isSupported(String extension) {
        return "pdf".equals(extension) || "docx".equals(extension) || "txt".equals(extension);
    }

    private String getExtension(String fileName) {
        int separatorIndex = fileName.lastIndexOf('.');
        return separatorIndex < 0 ? "" : fileName.substring(separatorIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeContentType(String contentType) {
        return contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;
    }

    private ResumeResponse toResponse(ResumeDocument document) {
        return new ResumeResponse(document.getId(), document.getOriginalFileName(), document.getContentType(), document.getCreateTime());
    }

    private void requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "Authentication is required");
        }
    }
}
