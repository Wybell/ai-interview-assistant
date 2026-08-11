package com.example.aiinterviewassistant.dto;

import java.time.LocalDateTime;

public record ResumeResponse(Long id, String fileName, String contentType, LocalDateTime createTime) {
}
