package com.example.aiinterviewassistant.controller;

import com.example.aiinterviewassistant.common.ApiResponse;
import com.example.aiinterviewassistant.dto.AiScoreResult;
import com.example.aiinterviewassistant.dto.MistakeResponse;
import com.example.aiinterviewassistant.dto.QuestionRequest;
import com.example.aiinterviewassistant.dto.ScoreRequest;
import com.example.aiinterviewassistant.dto.StudyProgressResponse;
import com.example.aiinterviewassistant.service.InterviewService;
import com.example.aiinterviewassistant.service.StudyService;
import com.example.aiinterviewassistant.sse.InterviewScoreSseAdapter;
import com.example.aiinterviewassistant.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.validation.Valid;
import java.util.List;

@RestController
@Tag(name = "面试训练", description = "面试题生成、答案评分、错题本和学习进度接口。")
@SecurityRequirement(name = "bearerAuth")
public class AiController {

    private final UserContext userContext;
    private final InterviewService interviewService;
    private final StudyService studyService;
    private final InterviewScoreSseAdapter interviewScoreSseAdapter;

    public AiController(
            UserContext userContext,
            InterviewService interviewService,
            StudyService studyService,
            InterviewScoreSseAdapter interviewScoreSseAdapter) {
        this.userContext = userContext;
        this.interviewService = interviewService;
        this.studyService = studyService;
        this.interviewScoreSseAdapter = interviewScoreSseAdapter;
    }

    // Legacy static-page compatibility endpoint.
    @GetMapping("/api/question/ask")
    @Operation(
            summary = "生成面试题（旧版兼容）",
            description = "已废弃，仅为旧静态页面保留。新前端请使用 POST /api/question/ask 的 JSON 请求体。",
            deprecated = true
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "成功返回一段面试题文本",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiResponseString"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "未登录或登录状态已失效",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "上游 AI 服务调用或响应解析失败",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503",
                    description = "当前生效的 AI 模型暂不可用或未完成运行时配置",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ApiResponse<String> askQuestion(
            @Parameter(description = "面试知识点", example = "HashMap")
            @RequestParam(value = "tag", defaultValue = "HashMap") String tag,
            @Parameter(description = "true 时忽略当前用户和模型的题目缓存并重新生成", example = "false")
            @RequestParam(value = "refresh", defaultValue = "false") boolean refresh) {
        Long userId = userContext.getCurrentUserId();
        String question = interviewService.askQuestion(userId, "backend", "Java", tag, refresh);

        return ApiResponse.success(question);
    }

    @PostMapping("/api/question/ask")
    @Operation(
            summary = "生成面试题",
            description = "按当前登录用户生效的 AI 模型生成指定知识点的面试题。题目缓存按用户、模型和知识点隔离；refresh=true 会强制重新生成。"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "成功返回一段面试题文本",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiResponseString"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "tag 为空或超过 50 个字符",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "未登录或登录状态已失效",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "上游 AI 服务调用或响应解析失败",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503",
                    description = "当前生效的 AI 模型暂不可用或未完成运行时配置",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ApiResponse<String> askQuestion(@Valid @RequestBody QuestionRequest request) {
        Long userId = userContext.getCurrentUserId();
        String question = interviewService.askQuestion(
                userId,
                request.getDirection(),
                request.getLanguage(),
                request.getTag(),
                request.getKnowledgeTopicId(),
                request.isRefresh()
        );

        return ApiResponse.success(question);
    }

    // Legacy static-page compatibility endpoint.
    @GetMapping("/api/question/score")
    @Operation(
            summary = "评分面试答案（旧版兼容）",
            description = "已废弃，仅为旧静态页面保留。新前端请使用 POST /api/question/score 的 JSON 请求体。",
            deprecated = true
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "成功返回分数、参考答案和改进建议",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiResponseAiScoreResult"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "未登录或登录状态已失效",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "上游 AI 服务调用、响应解析或评分结果校验失败",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503",
                    description = "当前生效的 AI 模型暂不可用或未完成运行时配置",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ApiResponse<AiScoreResult> scoreAnswer(
            @Parameter(description = "待评分的面试题", required = true, example = "请说明 Java 8 中 HashMap 的 put 流程。")
            @RequestParam("question") String question,
            @Parameter(description = "用户回答", required = true, example = "先计算 hash 并定位桶；发生冲突时比较 key，必要时遍历链表或红黑树；超过阈值则扩容。")
            @RequestParam("answer") String userAnswer,
            @Parameter(description = "面试知识点", example = "HashMap")
            @RequestParam(value = "tag", required = false, defaultValue = "unknown") String tag) {
        Long userId = userContext.getCurrentUserId();
        AiScoreResult scoreResult = interviewService.scoreAnswer(
                userId,
                question,
                userAnswer,
                tag
        );

        return ApiResponse.success(scoreResult);
    }

    @PostMapping("/api/question/score")
    @Operation(
            summary = "评分面试答案",
            description = "使用当前登录用户生效的 AI 模型完成评分，并持久化保存本次答题记录和实际评分模型。"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "成功返回分数、参考答案和改进建议",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiResponseAiScoreResult"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "tag、question 或 answer 为空，或超过字段长度限制",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "未登录或登录状态已失效",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "上游 AI 服务调用、响应解析或评分结果校验失败",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503",
                    description = "当前生效的 AI 模型暂不可用或未完成运行时配置",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ApiResponse<AiScoreResult> scoreAnswer(@Valid @RequestBody ScoreRequest request) {
        Long userId = userContext.getCurrentUserId();
        AiScoreResult scoreResult = interviewService.scoreAnswer(
                userId,
                request.getQuestion(),
                request.getAnswer(),
                request.getTag()
        );

        return ApiResponse.success(scoreResult);
    }


    @PostMapping(value = "/api/question/score/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "娴佸紡璇勫垎",
            description = "JSON 请求体配合 Bearer Token 执行流式评分，完成时发送 event: done，失败时发送 event: error。"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "SSE 流式评分结果",
                    content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                            schema = @Schema(type = "string"))
            )
    })
    public SseEmitter streamScoreJson(@Valid @RequestBody ScoreRequest request) {
        Long userId = userContext.getCurrentUserId();
        return interviewScoreSseAdapter.streamScore(
                userId, request.getQuestion(), request.getAnswer(), request.getTag());
    }

    @GetMapping("/api/mistakes")
    @Operation(
            summary = "查询错题本",
            description = "返回当前登录用户所有评分低于 6 分的答题记录，按创建时间倒序排列。"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "成功返回错题记录列表",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiResponseMistakeList"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "未登录或登录状态已失效",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ApiResponse<List<MistakeResponse>> getMistakes() {
        Long userId = userContext.getCurrentUserId();
        List<MistakeResponse> mistakes = studyService.getMistakes(userId);

        return ApiResponse.success(mistakes);
    }

    @GetMapping("/api/progress")
    @Operation(
            summary = "查询学习进度",
            description = "按知识点汇总当前登录用户的答题次数和平均分。"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "成功返回按知识点汇总的学习进度",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiResponseStudyProgressList"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "未登录或登录状态已失效",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ApiResponse<List<StudyProgressResponse>> getProgress() {
        Long userId = userContext.getCurrentUserId();
        List<StudyProgressResponse> progress = studyService.getProgress(userId);

        return ApiResponse.success(progress);
    }
}
