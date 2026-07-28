package com.example.aiinterviewassistant.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiinterviewassistant.common.ApiResponse;
import com.example.aiinterviewassistant.entity.KnowledgeQuestion;
import com.example.aiinterviewassistant.entity.KnowledgeTopic;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.example.aiinterviewassistant.mapper.KnowledgeQuestionMapper;
import com.example.aiinterviewassistant.mapper.KnowledgeTopicMapper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin/knowledge")
public class AdminKnowledgeController {
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024;
    private final KnowledgeTopicMapper topicMapper;
    private final KnowledgeQuestionMapper questionMapper;

    public AdminKnowledgeController(KnowledgeTopicMapper topicMapper, KnowledgeQuestionMapper questionMapper) {
        this.topicMapper = topicMapper;
        this.questionMapper = questionMapper;
    }

    @GetMapping("/documents")
    public ApiResponse<List<KnowledgeTopic>> getDocuments(@RequestParam String direction,
                                                            @RequestParam String language,
                                                            Authentication authentication) {
        requireAdmin(authentication);
        return ApiResponse.success(topicMapper.selectList(new LambdaQueryWrapper<KnowledgeTopic>()
                .eq(KnowledgeTopic::getDirection, direction).eq(KnowledgeTopic::getLanguage, language)
                .orderByDesc(KnowledgeTopic::getId)));
    }

    @PostMapping(value = "/documents", consumes = "multipart/form-data")
    public ApiResponse<KnowledgeTopic> upload(@RequestParam String direction, @RequestParam String language,
                                               @RequestParam MultipartFile file, Authentication authentication) {
        requireAdmin(authentication);
        validateFile(file);
        String content = extractText(file);
        if (content.isBlank()) throw new BusinessException(400, "Word 文档没有可解析的正文内容");

        KnowledgeTopic document = new KnowledgeTopic();
        document.setDirection(direction);
        document.setLanguage(language);
        document.setCategory("上传资料");
        document.setTitle(file.getOriginalFilename());
        document.setSourceFileName(file.getOriginalFilename());
        document.setSummary(content.length() > 1000 ? content.substring(0, 1000) : content);
        document.setDocumentContent(content);
        document.setKeyPoints("[]");
        document.setPublished(1);
        document.setSortOrder(0);
        topicMapper.insert(document);
        return ApiResponse.success(document);
    }

    @DeleteMapping("/documents/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, Authentication authentication) {
        requireAdmin(authentication);
        if (topicMapper.selectById(id) == null) throw new BusinessException(404, "资料不存在");
        questionMapper.delete(new LambdaQueryWrapper<KnowledgeQuestion>().eq(KnowledgeQuestion::getTopicId, id));
        topicMapper.deleteById(id);
        return ApiResponse.success(null);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException(400, "请选择 Word 文档");
        if (file.getSize() > MAX_FILE_SIZE || !file.getOriginalFilename().toLowerCase().endsWith(".docx")) {
            throw new BusinessException(400, "仅支持 20MB 以内的 .docx 文件");
        }
    }

    private String extractText(MultipartFile file) {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream());
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText().trim();
        } catch (IOException exception) {
            throw new BusinessException(400, "Word 文档解析失败");
        }
    }

    private void requireAdmin(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(item -> "ROLE_ADMIN".equals(item.getAuthority()))) {
            throw new BusinessException(403, "只有管理员可以管理知识库");
        }
    }
}
