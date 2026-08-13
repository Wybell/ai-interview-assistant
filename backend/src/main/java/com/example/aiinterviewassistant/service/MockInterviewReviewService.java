package com.example.aiinterviewassistant.service;

import com.example.aiinterviewassistant.dto.MockInterviewReviewResponse;
import com.example.aiinterviewassistant.dto.MockInterviewReviewSummaryResponse;

import java.util.List;

public interface MockInterviewReviewService {

    List<MockInterviewReviewSummaryResponse> getReviews(Long userId);

    MockInterviewReviewResponse getReview(Long userId, Long sessionId);

    MockInterviewReviewResponse generateReview(Long userId, Long sessionId);
}
