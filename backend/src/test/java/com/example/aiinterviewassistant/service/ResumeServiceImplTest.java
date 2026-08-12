package com.example.aiinterviewassistant.service;

import com.example.aiinterviewassistant.config.ResumeStorageProperties;
import com.example.aiinterviewassistant.dto.ResumePreviewResponse;
import com.example.aiinterviewassistant.entity.ResumeDocument;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.example.aiinterviewassistant.mapper.MockInterviewSessionMapper;
import com.example.aiinterviewassistant.mapper.ResumeDocumentMapper;
import com.example.aiinterviewassistant.service.impl.ResumeServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeServiceImplTest {

    @Mock
    private ResumeDocumentMapper resumeDocumentMapper;

    @Mock
    private MockInterviewSessionMapper mockInterviewSessionMapper;

    @Test
    void shouldPreviewOnlyTheCurrentUsersResume() {
        ResumeDocument document = new ResumeDocument();
        document.setId(7L);
        document.setUserId(1L);
        document.setOriginalFileName("resume.pdf");
        document.setContentType("application/pdf");
        document.setExtractedContent("Java 后端项目经历");
        when(resumeDocumentMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(document);
        ResumeServiceImpl service = new ResumeServiceImpl(
                resumeDocumentMapper,
                mockInterviewSessionMapper,
                new ResumeStorageProperties()
        );

        ResumePreviewResponse preview = service.preview(1L, 7L);

        assertThat(preview).isEqualTo(new ResumePreviewResponse(
                7L,
                "resume.pdf",
                "application/pdf",
                "Java 后端项目经历"
        ));
        verify(resumeDocumentMapper).selectOne(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectPreviewWhenResumeIsNotOwnedByCurrentUser() {
        when(resumeDocumentMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(null);
        ResumeServiceImpl service = new ResumeServiceImpl(
                resumeDocumentMapper,
                mockInterviewSessionMapper,
                new ResumeStorageProperties()
        );

        assertThatThrownBy(() -> service.preview(1L, 7L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Resume not found");
    }
}
