package com.example.aiinterviewassistant.controller;

import com.example.aiinterviewassistant.common.ApiResponse;
import com.example.aiinterviewassistant.dto.KnowledgeTopicResponse;
import com.example.aiinterviewassistant.service.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "知识库", description = "按方向和语言查询知识专题及解释")
@SecurityRequirement(name = "bearerAuth")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping("/api/knowledge/topics")
    @Operation(summary = "查询知识专题")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "专题列表"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登录")
    })
    public ApiResponse<List<KnowledgeTopicResponse>> getTopics(
            @Parameter(description = "frontend 或 backend", required = true)
            @RequestParam String direction,
            @Parameter(description = "语言或技术栈", required = true)
            @RequestParam String language) {
        return ApiResponse.success(knowledgeService.getTopics(direction, language));
    }

    @GetMapping("/api/knowledge/topics/{topicId}")
    @Operation(summary = "查询知识专题详情")
    public ApiResponse<KnowledgeTopicResponse> getTopic(@PathVariable Long topicId) {
        return ApiResponse.success(knowledgeService.getTopic(topicId));
    }
}
