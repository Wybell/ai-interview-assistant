package com.example.aiinterviewassistant.controller;

import com.example.aiinterviewassistant.common.ApiResponse;
import com.example.aiinterviewassistant.dto.ResumeResponse;
import com.example.aiinterviewassistant.service.ResumeService;
import com.example.aiinterviewassistant.utils.UserContext;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final UserContext userContext;
    private final ResumeService resumeService;

    public ResumeController(UserContext userContext, ResumeService resumeService) {
        this.userContext = userContext;
        this.resumeService = resumeService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ResumeResponse> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(resumeService.upload(userContext.getCurrentUserId(), file));
    }

    @GetMapping
    public ApiResponse<List<ResumeResponse>> getResumes() {
        return ApiResponse.success(resumeService.getResumes(userContext.getCurrentUserId()));
    }

    @DeleteMapping("/{resumeId}")
    public ApiResponse<Void> delete(@PathVariable Long resumeId) {
        resumeService.delete(userContext.getCurrentUserId(), resumeId);
        return ApiResponse.success(null);
    }
}
