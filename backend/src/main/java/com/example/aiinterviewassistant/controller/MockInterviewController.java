package com.example.aiinterviewassistant.controller;

import com.example.aiinterviewassistant.common.ApiResponse;
import com.example.aiinterviewassistant.dto.AnswerMockInterviewTurnRequest;
import com.example.aiinterviewassistant.dto.ActiveMockInterviewResponse;
import com.example.aiinterviewassistant.dto.CreateMockInterviewRequest;
import com.example.aiinterviewassistant.dto.MockInterviewSessionResponse;
import com.example.aiinterviewassistant.dto.MockInterviewReviewResponse;
import com.example.aiinterviewassistant.dto.MockInterviewReviewSummaryResponse;
import com.example.aiinterviewassistant.dto.MockInterviewTurnResponse;
import com.example.aiinterviewassistant.service.MockInterviewReviewService;
import com.example.aiinterviewassistant.service.MockInterviewService;
import com.example.aiinterviewassistant.utils.UserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/mock-interviews")
public class MockInterviewController {

    private final UserContext userContext;
    private final MockInterviewService mockInterviewService;
    private final MockInterviewReviewService mockInterviewReviewService;

    public MockInterviewController(
            UserContext userContext,
            MockInterviewService mockInterviewService,
            MockInterviewReviewService mockInterviewReviewService) {
        this.userContext = userContext;
        this.mockInterviewService = mockInterviewService;
        this.mockInterviewReviewService = mockInterviewReviewService;
    }

    @PostMapping
    public ApiResponse<MockInterviewSessionResponse> create(@Valid @RequestBody CreateMockInterviewRequest request) {
        return ApiResponse.success(mockInterviewService.createSession(
                userContext.getCurrentUserId(),
                request.getResumeId(),
                request.getTargetPosition(),
                request.getTargetCompany(),
                request.getInterviewRound()));
    }

    @GetMapping("/{sessionId}")
    public ApiResponse<MockInterviewSessionResponse> getSession(@PathVariable Long sessionId) {
        return ApiResponse.success(mockInterviewService.getSession(userContext.getCurrentUserId(), sessionId));
    }

    @GetMapping("/active")
    public ApiResponse<List<ActiveMockInterviewResponse>> getActiveSessions() {
        return ApiResponse.success(mockInterviewService.getActiveSessions(userContext.getCurrentUserId()));
    }

    @GetMapping("/reviews")
    public ApiResponse<List<MockInterviewReviewSummaryResponse>> getReviews() {
        return ApiResponse.success(mockInterviewReviewService.getReviews(userContext.getCurrentUserId()));
    }

    @GetMapping("/{sessionId}/review")
    public ApiResponse<MockInterviewReviewResponse> getReview(@PathVariable Long sessionId) {
        return ApiResponse.success(mockInterviewReviewService.getReview(userContext.getCurrentUserId(), sessionId));
    }

    @PostMapping("/{sessionId}/review")
    public ApiResponse<MockInterviewReviewResponse> generateReview(@PathVariable Long sessionId) {
        return ApiResponse.success(mockInterviewReviewService.generateReview(userContext.getCurrentUserId(), sessionId));
    }

    @PostMapping("/{sessionId}/turns/{turnId}/answer")
    public ApiResponse<MockInterviewTurnResponse> answer(
            @PathVariable Long sessionId,
            @PathVariable Long turnId,
            @Valid @RequestBody AnswerMockInterviewTurnRequest request) {
        return ApiResponse.success(mockInterviewService.answerTurn(
                userContext.getCurrentUserId(), sessionId, turnId, request.getAnswer()));
    }

    @PostMapping("/{sessionId}/questions")
    public ApiResponse<MockInterviewTurnResponse> nextQuestion(@PathVariable Long sessionId) {
        return ApiResponse.success(mockInterviewService.generateNextQuestion(userContext.getCurrentUserId(), sessionId));
    }

    @PostMapping("/{sessionId}/turns/{turnId}/follow-up")
    public ApiResponse<MockInterviewTurnResponse> followUp(
            @PathVariable Long sessionId,
            @PathVariable Long turnId) {
        return ApiResponse.success(mockInterviewService.generateFollowUpQuestion(
                userContext.getCurrentUserId(), sessionId, turnId));
    }

    @PostMapping("/{sessionId}/finish")
    public ApiResponse<MockInterviewSessionResponse> finish(@PathVariable Long sessionId) {
        return ApiResponse.success(mockInterviewService.finishSession(userContext.getCurrentUserId(), sessionId));
    }

    @PostMapping("/{sessionId}/end")
    public ApiResponse<MockInterviewSessionResponse> endEarly(@PathVariable Long sessionId) {
        return ApiResponse.success(mockInterviewService.endSessionEarly(userContext.getCurrentUserId(), sessionId));
    }
}
