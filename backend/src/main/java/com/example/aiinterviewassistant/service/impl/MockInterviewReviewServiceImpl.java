package com.example.aiinterviewassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiinterviewassistant.dto.AiMockInterviewReview;
import com.example.aiinterviewassistant.dto.MockInterviewReviewResponse;
import com.example.aiinterviewassistant.dto.MockInterviewReviewSummaryResponse;
import com.example.aiinterviewassistant.dto.MockInterviewTurnResponse;
import com.example.aiinterviewassistant.entity.MockInterviewReview;
import com.example.aiinterviewassistant.entity.MockInterviewSession;
import com.example.aiinterviewassistant.entity.MockInterviewTurn;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.example.aiinterviewassistant.mapper.MockInterviewReviewMapper;
import com.example.aiinterviewassistant.mapper.MockInterviewSessionMapper;
import com.example.aiinterviewassistant.mapper.MockInterviewTurnMapper;
import com.example.aiinterviewassistant.model.EffectiveAiModel;
import com.example.aiinterviewassistant.service.AiService;
import com.example.aiinterviewassistant.service.MockInterviewReviewService;
import com.example.aiinterviewassistant.service.UserAiPreferenceService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MockInterviewReviewServiceImpl implements MockInterviewReviewService {

    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String MAIN_TURN = "MAIN";

    private final MockInterviewSessionMapper sessionMapper;
    private final MockInterviewTurnMapper turnMapper;
    private final MockInterviewReviewMapper reviewMapper;
    private final UserAiPreferenceService userAiPreferenceService;
    private final AiService aiService;

    public MockInterviewReviewServiceImpl(
            MockInterviewSessionMapper sessionMapper,
            MockInterviewTurnMapper turnMapper,
            MockInterviewReviewMapper reviewMapper,
            UserAiPreferenceService userAiPreferenceService,
            AiService aiService) {
        this.sessionMapper = sessionMapper;
        this.turnMapper = turnMapper;
        this.reviewMapper = reviewMapper;
        this.userAiPreferenceService = userAiPreferenceService;
        this.aiService = aiService;
    }

    @Override
    public List<MockInterviewReviewSummaryResponse> getReviews(Long userId) {
        requireUser(userId);
        return sessionMapper.selectList(new LambdaQueryWrapper<MockInterviewSession>()
                        .eq(MockInterviewSession::getUserId, userId)
                        .ne(MockInterviewSession::getStatus, ACTIVE_STATUS)
                        .orderByDesc(MockInterviewSession::getFinishedTime)
                        .orderByDesc(MockInterviewSession::getCreateTime))
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Override
    public MockInterviewReviewResponse getReview(Long userId, Long sessionId) {
        MockInterviewSession session = getOwnedEndedSession(userId, sessionId);
        MockInterviewReview review = findReview(sessionId);
        if (review == null) {
            throw new BusinessException(404, "本轮面试复盘尚未生成");
        }
        return toReviewResponse(session, review, getTurns(sessionId));
    }

    @Override
    public MockInterviewReviewResponse generateReview(Long userId, Long sessionId) {
        MockInterviewSession session = getOwnedEndedSession(userId, sessionId);
        List<MockInterviewTurn> turns = getTurns(sessionId);
        List<MockInterviewTurn> answeredTurns = turns.stream()
                .filter(turn -> turn.getUserAnswer() != null && !turn.getUserAnswer().isBlank())
                .toList();
        if (answeredTurns.isEmpty()) {
            throw new BusinessException(409, "至少回答一道题后才能生成面试复盘");
        }

        MockInterviewReview review = findReview(sessionId);
        if (review == null) {
            EffectiveAiModel aiModel = userAiPreferenceService.resolveAvailableModel(session.getAiModelId());
            AiMockInterviewReview aiReview = aiService.generateMockInterviewReview(
                    aiModel,
                    session.getTargetPosition(),
                    session.getTargetCompany(),
                    session.getInterviewRound(),
                    toTranscript(turns));
            review = new MockInterviewReview();
            review.setSessionId(sessionId);
            review.setAnsweredTurnCount(answeredTurns.size());
            review.setMainQuestionCount((int) turns.stream()
                    .filter(turn -> MAIN_TURN.equals(defaultTurnType(turn))).count());
            review.setFollowUpCount((int) turns.stream()
                    .filter(turn -> !MAIN_TURN.equals(defaultTurnType(turn))).count());
            review.setAverageScore(averageScore(answeredTurns));
            review.setOverallFeedback(aiReview.overallFeedback());
            review.setStrengths(aiReview.strengths());
            review.setImprovementAreas(aiReview.improvementAreas());
            review.setActionItems(aiReview.actionItems());
            review.setAiModelId(aiModel.id());
            review.setCreateTime(LocalDateTime.now());
            reviewMapper.insert(review);
        }
        return toReviewResponse(session, review, turns);
    }

    private MockInterviewReviewSummaryResponse toSummaryResponse(MockInterviewSession session) {
        MockInterviewReview review = findReview(session.getId());
        List<MockInterviewTurn> turns = getTurns(session.getId());
        int answeredTurnCount = (int) turns.stream()
                .filter(turn -> turn.getUserAnswer() != null && !turn.getUserAnswer().isBlank()).count();
        return new MockInterviewReviewSummaryResponse(
                session.getId(),
                session.getTargetPosition(),
                session.getTargetCompany(),
                session.getInterviewRound(),
                session.getStatus(),
                session.getResumeFileNameSnapshot(),
                answeredTurnCount,
                (int) turns.stream().filter(turn -> MAIN_TURN.equals(defaultTurnType(turn))).count(),
                (int) turns.stream().filter(turn -> !MAIN_TURN.equals(defaultTurnType(turn))).count(),
                review == null ? averageScore(turns) : review.getAverageScore(),
                review != null,
                answeredTurnCount > 0,
                session.getFinishedTime(),
                session.getCreateTime());
    }

    private MockInterviewReviewResponse toReviewResponse(
            MockInterviewSession session,
            MockInterviewReview review,
            List<MockInterviewTurn> turns) {
        return new MockInterviewReviewResponse(
                session.getId(), session.getTargetPosition(), session.getTargetCompany(), session.getInterviewRound(),
                session.getStatus(), session.getResumeFileNameSnapshot(), review.getAnsweredTurnCount(),
                review.getMainQuestionCount(), review.getFollowUpCount(), review.getAverageScore(),
                review.getOverallFeedback(), review.getStrengths(), review.getImprovementAreas(), review.getActionItems(),
                session.getFinishedTime(), review.getCreateTime(),
                turns.stream().map(this::toTurnResponse).toList());
    }

    private MockInterviewSession getOwnedEndedSession(Long userId, Long sessionId) {
        requireUser(userId);
        MockInterviewSession session = sessionMapper.selectOne(new LambdaQueryWrapper<MockInterviewSession>()
                .eq(MockInterviewSession::getId, sessionId)
                .eq(MockInterviewSession::getUserId, userId));
        if (session == null) {
            throw new BusinessException(404, "模拟面试不存在");
        }
        if (ACTIVE_STATUS.equals(session.getStatus())) {
            throw new BusinessException(409, "请先完成或提前结束本轮面试后再进行复盘");
        }
        return session;
    }

    private MockInterviewReview findReview(Long sessionId) {
        return reviewMapper.selectOne(new LambdaQueryWrapper<MockInterviewReview>()
                .eq(MockInterviewReview::getSessionId, sessionId));
    }

    private List<MockInterviewTurn> getTurns(Long sessionId) {
        return turnMapper.selectList(new LambdaQueryWrapper<MockInterviewTurn>()
                .eq(MockInterviewTurn::getSessionId, sessionId)
                .orderByAsc(MockInterviewTurn::getSequenceNo)
                .orderByAsc(MockInterviewTurn::getFollowUpNo)
                .orderByAsc(MockInterviewTurn::getId));
    }

    private BigDecimal averageScore(List<MockInterviewTurn> turns) {
        List<Integer> scores = turns.stream().map(MockInterviewTurn::getScore).filter(score -> score != null).toList();
        if (scores.isEmpty()) {
            return null;
        }
        return BigDecimal.valueOf(scores.stream().mapToInt(Integer::intValue).average().orElseThrow())
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String toTranscript(List<MockInterviewTurn> turns) {
        StringBuilder transcript = new StringBuilder();
        for (MockInterviewTurn turn : turns) {
            transcript.append(defaultTurnType(turn).equals(MAIN_TURN) ? "主问题" : "追问")
                    .append(' ').append(turn.getSequenceNo()).append("：").append(turn.getQuestion()).append('\n');
            if (turn.getUserAnswer() != null && !turn.getUserAnswer().isBlank()) {
                transcript.append("候选人回答：").append(turn.getUserAnswer()).append('\n')
                        .append("评分：").append(turn.getScore() == null ? "未评分" : turn.getScore() + "/10").append('\n')
                        .append("建议：").append(turn.getSuggestion() == null ? "无" : turn.getSuggestion()).append('\n');
            }
        }
        return transcript.toString();
    }

    private MockInterviewTurnResponse toTurnResponse(MockInterviewTurn turn) {
        return new MockInterviewTurnResponse(turn.getId(), turn.getSequenceNo(), defaultTurnType(turn),
                turn.getParentTurnId(), turn.getFollowUpNo(), turn.getQuestion(), turn.getUserAnswer(), turn.getScore(),
                turn.getCorrectAnswer(), turn.getSuggestion(), turn.getCreateTime());
    }

    private String defaultTurnType(MockInterviewTurn turn) {
        return turn.getTurnType() == null ? MAIN_TURN : turn.getTurnType();
    }

    private void requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "登录状态无效");
        }
    }
}
