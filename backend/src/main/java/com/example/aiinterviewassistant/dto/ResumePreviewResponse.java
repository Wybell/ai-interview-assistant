package com.example.aiinterviewassistant.dto;

public record ResumePreviewResponse(
        Long id,
        String fileName,
        String contentType,
        String content) {
}
