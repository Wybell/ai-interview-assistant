package com.example.aiinterviewassistant.controller;

import com.example.aiinterviewassistant.utils.UserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;

import com.example.aiinterviewassistant.dto.AiScoreResult;
import com.example.aiinterviewassistant.dto.MistakeResponse;
import com.example.aiinterviewassistant.dto.QuestionRequest;
import com.example.aiinterviewassistant.dto.ScoreRequest;
import com.example.aiinterviewassistant.dto.StudyProgressResponse;
import com.example.aiinterviewassistant.common.ApiResponse;
import com.example.aiinterviewassistant.sse.InterviewScoreSseAdapter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.validation.Valid;

import com.example.aiinterviewassistant.service.InterviewService;
import com.example.aiinterviewassistant.service.StudyService;
import java.util.List;

@RestController
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
    public ApiResponse<String> askQuestion(
            @RequestParam(value = "tag", defaultValue = "HashMap") String tag,
            @RequestParam(value = "refresh", defaultValue = "false") boolean refresh) {

        Long userId = userContext.getCurrentUserId();
        String question = interviewService.askQuestion(userId, tag, refresh);

        return ApiResponse.success(question);
    }

    @PostMapping("/api/question/ask")
    public ApiResponse<String> askQuestion(@Valid @RequestBody QuestionRequest request) {
        Long userId = userContext.getCurrentUserId();
        String question = interviewService.askQuestion(
                userId,
                request.getTag(),
                request.isRefresh()
        );

        return ApiResponse.success(question);
    }

    // Legacy static-page compatibility endpoint.
    @GetMapping("/api/question/score")
    public ApiResponse<AiScoreResult> scoreAnswer(
            @RequestParam("question") String question,
            @RequestParam("answer") String userAnswer,
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

    @GetMapping(value = "/api/question/score/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamScore(@RequestParam String question,
                                  @RequestParam String answer,
                                  @RequestParam(defaultValue = "unknown") String tag) {
        Long userId = userContext.getCurrentUserId();
        return interviewScoreSseAdapter.streamScore(userId, question, answer, tag);
    }

    // 错题本：得分低于 6 分的记录
    @GetMapping("/api/mistakes")
    public ApiResponse<List<MistakeResponse>> getMistakes() {
        Long userId = userContext.getCurrentUserId();
        List<MistakeResponse> mistakes = studyService.getMistakes(userId);

        return ApiResponse.success(mistakes);
    }

    // 学习进度：按知识点统计练习次数和平均分
    @GetMapping("/api/progress")
    public ApiResponse<List<StudyProgressResponse>> getProgress() {
        Long userId = userContext.getCurrentUserId();
        List<StudyProgressResponse> progress = studyService.getProgress(userId);

        return ApiResponse.success(progress);
    }
}
