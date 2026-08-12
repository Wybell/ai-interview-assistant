package com.example.aiinterviewassistant.service;

import com.example.aiinterviewassistant.dto.ResumeResponse;
import com.example.aiinterviewassistant.dto.ResumePreviewResponse;
import com.example.aiinterviewassistant.entity.ResumeDocument;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResumeService {

    ResumeResponse upload(Long userId, MultipartFile file);

    List<ResumeResponse> getResumes(Long userId);

    ResumePreviewResponse preview(Long userId, Long resumeId);

    ResumeDocument getOwnedResume(Long userId, Long resumeId);

    void delete(Long userId, Long resumeId);
}
