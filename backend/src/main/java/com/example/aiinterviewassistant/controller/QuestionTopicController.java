package com.example.aiinterviewassistant.controller;

import com.example.aiinterviewassistant.common.ApiResponse;
import com.example.aiinterviewassistant.service.TechnicalTopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "技术知识点", description = "按面试方向和语言查询通用技术知识点板块。")
@SecurityRequirement(name = "bearerAuth")
public class QuestionTopicController {

    private final TechnicalTopicService technicalTopicService;

    public QuestionTopicController(TechnicalTopicService technicalTopicService) {
        this.technicalTopicService = technicalTopicService;
    }

    @GetMapping("/api/question/topics")
    @Operation(summary = "查询技术知识点板块")
    public ApiResponse<List<String>> getTopics(
            @Parameter(description = "frontend 或 backend", required = true)
            @RequestParam String direction,
            @Parameter(description = "语言或技术栈", required = true)
            @RequestParam String language) {
        return ApiResponse.success(technicalTopicService.getTopics(direction, language));
    }
}
