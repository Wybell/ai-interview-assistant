package com.example.aiinterviewassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiinterviewassistant.dto.ActiveMockInterviewResponse;
import com.example.aiinterviewassistant.dto.AiScoreResult;
import com.example.aiinterviewassistant.dto.MockInterviewSessionResponse;
import com.example.aiinterviewassistant.dto.MockInterviewTurnResponse;
import com.example.aiinterviewassistant.entity.MockInterviewSession;
import com.example.aiinterviewassistant.entity.MockInterviewTurn;
import com.example.aiinterviewassistant.entity.ResumeDocument;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.example.aiinterviewassistant.mapper.MockInterviewSessionMapper;
import com.example.aiinterviewassistant.mapper.MockInterviewTurnMapper;
import com.example.aiinterviewassistant.model.EffectiveAiModel;
import com.example.aiinterviewassistant.service.AiService;
import com.example.aiinterviewassistant.service.MockInterviewService;
import com.example.aiinterviewassistant.service.MockInterviewReviewService;
import com.example.aiinterviewassistant.service.ResumeService;
import com.example.aiinterviewassistant.service.UserAiPreferenceService;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MockInterviewServiceImpl implements MockInterviewService {

    private static final Logger log = LoggerFactory.getLogger(MockInterviewServiceImpl.class);

    private static final int MAX_FOLLOW_UP_COUNT = 2;
    private static final String MAIN_TURN = "MAIN";
    private static final String FOLLOW_UP_TURN = "FOLLOW_UP";
    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String COMPLETED_STATUS = "COMPLETED";
    private static final String ENDED_EARLY_STATUS = "ENDED_EARLY";

    private final MockInterviewSessionMapper sessionMapper;
    private final MockInterviewTurnMapper turnMapper;
    private final ResumeService resumeService;
    private final UserAiPreferenceService userAiPreferenceService;
    private final AiService aiService;
    private final MockInterviewReviewService mockInterviewReviewService;

    public MockInterviewServiceImpl(
            MockInterviewSessionMapper sessionMapper,
            MockInterviewTurnMapper turnMapper,
            ResumeService resumeService,
            UserAiPreferenceService userAiPreferenceService,
            AiService aiService,
            MockInterviewReviewService mockInterviewReviewService) {
        this.sessionMapper = sessionMapper;
        this.turnMapper = turnMapper;
        this.resumeService = resumeService;
        this.userAiPreferenceService = userAiPreferenceService;
        this.aiService = aiService;
        this.mockInterviewReviewService = mockInterviewReviewService;
    }

    @Override
    public MockInterviewSessionResponse createSession(
            Long userId,
            Long resumeId,
            String targetPosition,
            String targetCompany,
            String interviewRound) {
        requireUser(userId);
        ResumeDocument resume = resumeService.getOwnedResume(userId, resumeId);
        EffectiveAiModel aiModel = userAiPreferenceService.resolveEffectiveModel(userId);

        MockInterviewSession session = new MockInterviewSession();
        session.setUserId(userId);
        session.setResumeId(resume.getId());
        session.setResumeFileNameSnapshot(resume.getOriginalFileName());
        session.setTargetPosition(targetPosition.trim());
        session.setTargetCompany(normalizeOptionalText(targetCompany));
        session.setInterviewRound(interviewRound);
        session.setStatus(ACTIVE_STATUS);
        session.setQuestionCount(0);
        session.setQuestionLimit(questionLimitFor(interviewRound));
        session.setAiModelId(aiModel.id());
        session.setCreateTime(LocalDateTime.now());
        sessionMapper.insert(session);

        createQuestion(session, resume, aiModel, List.of());
        return toSessionResponse(session, getTurns(session.getId()));
    }

    @Override
    public MockInterviewSessionResponse getSession(Long userId, Long sessionId) {
        MockInterviewSession session = getOwnedSession(userId, sessionId);
        return toSessionResponse(session, getTurns(sessionId));
    }

    @Override
    public List<ActiveMockInterviewResponse> getActiveSessions(Long userId) {
        requireUser(userId);
        return sessionMapper.selectList(new LambdaQueryWrapper<MockInterviewSession>()
                        .eq(MockInterviewSession::getUserId, userId)
                        .eq(MockInterviewSession::getStatus, ACTIVE_STATUS)
                        .orderByDesc(MockInterviewSession::getCreateTime))
                .stream()
                .map(session -> new ActiveMockInterviewResponse(
                        session.getId(),
                        session.getResumeId(),
                        session.getResumeFileNameSnapshot(),
                        session.getTargetPosition(),
                        session.getInterviewRound(),
                        session.getQuestionCount(),
                        session.getQuestionLimit() == null
                                ? questionLimitFor(session.getInterviewRound()) : session.getQuestionLimit(),
                        session.getCreateTime()))
                .toList();
    }

    @Override
    public MockInterviewTurnResponse answerTurn(Long userId, Long sessionId, Long turnId, String answer) {
        MockInterviewSession session = getActiveSession(userId, sessionId);
        MockInterviewTurn turn = getOwnedTurn(session, turnId);
        MockInterviewTurn currentTurn = latestTurn(getTurns(sessionId));
        if (currentTurn == null || !turnId.equals(currentTurn.getId())) {
            throw new BusinessException(409, "请先回答当前问题，不能跳过问题作答");
        }
        if (turn.getUserAnswer() != null) {
            throw new BusinessException(409, "这道题已经回答过了");
        }
        EffectiveAiModel aiModel = userAiPreferenceService.resolveAvailableModel(session.getAiModelId());
        AiScoreResult scoreResult = aiService.scoreAnswer(aiModel, turn.getQuestion(), answer.trim());
        turn.setUserAnswer(answer.trim());
        turn.setScore(scoreResult.getScore());
        turn.setCorrectAnswer(scoreResult.getCorrectAnswer());
        turn.setSuggestion(scoreResult.getSuggestion());
        turnMapper.updateById(turn);
        return toTurnResponse(turn);
    }

    @Override
    public MockInterviewTurnResponse generateNextQuestion(Long userId, Long sessionId) {
        MockInterviewSession session = getActiveSession(userId, sessionId);
        List<MockInterviewTurn> turns = getTurns(sessionId);
        MockInterviewTurn currentTurn = latestTurn(turns);
        if (currentTurn == null || currentTurn.getUserAnswer() == null) {
            throw new BusinessException(409, "请先完成当前问题的回答");
        }
        int questionLimit = session.getQuestionLimit() == null
                ? questionLimitFor(session.getInterviewRound()) : session.getQuestionLimit();
        if (session.getQuestionCount() >= questionLimit) {
            throw new BusinessException(409, "本轮主问题已完成，你可以继续追问当前问题，或结束本轮面试");
        }
        ResumeDocument resume = resumeService.getOwnedResume(userId, session.getResumeId());
        EffectiveAiModel aiModel = userAiPreferenceService.resolveAvailableModel(session.getAiModelId());
        return toTurnResponse(createQuestion(session, resume, aiModel, turns));
    }

    @Override
    public MockInterviewTurnResponse generateFollowUpQuestion(Long userId, Long sessionId, Long turnId) {
        MockInterviewSession session = getActiveSession(userId, sessionId);
        MockInterviewTurn parentTurn = getOwnedTurn(session, turnId);
        if (!MAIN_TURN.equals(defaultTurnType(parentTurn))) {
            throw new BusinessException(409, "追问只能针对主问题，不能继续追问追问");
        }
        List<MockInterviewTurn> turns = getTurns(sessionId);
        MockInterviewTurn currentTurn = latestTurn(turns);
        if (currentTurn == null || currentTurn.getUserAnswer() == null) {
            throw new BusinessException(409, "请先完成当前问题的回答");
        }
        if (!isCurrentQuestion(parentTurn, currentTurn)) {
            throw new BusinessException(409, "请先处理当前问题，不能跳过当前问题追问");
        }
        int followUpCount = (int) turns.stream()
                .filter(turn -> turnId.equals(turn.getParentTurnId()))
                .count();
        if (followUpCount >= MAX_FOLLOW_UP_COUNT) {
            throw new BusinessException(409, "当前问题最多追问两次");
        }

        ResumeDocument resume = resumeService.getOwnedResume(userId, session.getResumeId());
        EffectiveAiModel aiModel = userAiPreferenceService.resolveAvailableModel(session.getAiModelId());
        MockInterviewTurn contextTurn = currentTurn;
        MockInterviewTurn followUp = new MockInterviewTurn();
        followUp.setSessionId(session.getId());
        followUp.setSequenceNo(parentTurn.getSequenceNo());
        followUp.setTurnType(FOLLOW_UP_TURN);
        followUp.setParentTurnId(parentTurn.getId());
        followUp.setFollowUpNo(followUpCount + 1);
        followUp.setQuestion(aiService.generateMockInterviewFollowUpQuestion(
                aiModel,
                resume.getExtractedContent(),
                session.getTargetPosition(),
                session.getTargetCompany(),
                session.getInterviewRound(),
                parentTurn.getQuestion(),
                contextTurn.getUserAnswer(),
                contextTurn.getScore(),
                contextTurn.getSuggestion(),
                turns.stream()
                        .filter(turn -> turnId.equals(turn.getParentTurnId()))
                        .map(MockInterviewTurn::getQuestion)
                        .toList()));
        followUp.setFocusTag(session.getTargetPosition());
        followUp.setCreateTime(LocalDateTime.now());
        turnMapper.insert(followUp);
        return toTurnResponse(followUp);
    }

    @Override
    public MockInterviewSessionResponse finishSession(Long userId, Long sessionId) {
        MockInterviewSession session = getActiveSession(userId, sessionId);
        List<MockInterviewTurn> turns = getTurns(sessionId);
        int questionLimit = session.getQuestionLimit() == null
                ? questionLimitFor(session.getInterviewRound()) : session.getQuestionLimit();
        MockInterviewTurn currentTurn = latestTurn(turns);
        if (session.getQuestionCount() < questionLimit || currentTurn == null || currentTurn.getUserAnswer() == null) {
            throw new BusinessException(409, "请完成本轮全部主问题后再生成面试总结；如需中途离开，请选择提前结束面试");
        }
        EffectiveAiModel aiModel = userAiPreferenceService.resolveAvailableModel(session.getAiModelId());
        session.setSummary(aiService.generateMockInterviewSummary(
                aiModel,
                session.getTargetPosition(),
                session.getTargetCompany(),
                session.getInterviewRound(),
                toTranscript(turns)));
        session.setStatus(COMPLETED_STATUS);
        session.setFinishedTime(LocalDateTime.now());
        sessionMapper.updateById(session);
        try {
            mockInterviewReviewService.generateReview(userId, sessionId);
        } catch (RuntimeException exception) {
            log.warn("mock_interview_review_generation_failed sessionId={} exception={}", sessionId,
                    exception.getClass().getSimpleName());
        }
        return toSessionResponse(session, turns);
    }

    @Override
    public MockInterviewSessionResponse endSessionEarly(Long userId, Long sessionId) {
        MockInterviewSession session = getActiveSession(userId, sessionId);
        session.setStatus(ENDED_EARLY_STATUS);
        session.setFinishedTime(LocalDateTime.now());
        sessionMapper.updateById(session);
        return toSessionResponse(session, getTurns(sessionId));
    }

    private MockInterviewTurn createQuestion(
            MockInterviewSession session,
            ResumeDocument resume,
            EffectiveAiModel aiModel,
            List<MockInterviewTurn> previousTurns) {
        MockInterviewTurn turn = new MockInterviewTurn();
        turn.setSessionId(session.getId());
        turn.setSequenceNo(session.getQuestionCount() + 1);
        turn.setTurnType(MAIN_TURN);
        turn.setQuestion(aiService.generateMockInterviewQuestion(
                aiModel,
                resume.getExtractedContent(),
                session.getTargetPosition(),
                session.getTargetCompany(),
                session.getInterviewRound(),
                toTranscript(previousTurns)));
        turn.setFocusTag(session.getTargetPosition());
        turn.setCreateTime(LocalDateTime.now());
        turnMapper.insert(turn);
        session.setQuestionCount(turn.getSequenceNo());
        sessionMapper.updateById(session);
        return turn;
    }

    private MockInterviewSession getOwnedSession(Long userId, Long sessionId) {
        requireUser(userId);
        MockInterviewSession session = sessionMapper.selectOne(new LambdaQueryWrapper<MockInterviewSession>()
                .eq(MockInterviewSession::getId, sessionId)
                .eq(MockInterviewSession::getUserId, userId));
        if (session == null) {
            throw new BusinessException(404, "模拟面试不存在");
        }
        return session;
    }

    private MockInterviewSession getActiveSession(Long userId, Long sessionId) {
        MockInterviewSession session = getOwnedSession(userId, sessionId);
        if (!ACTIVE_STATUS.equals(session.getStatus())) {
            throw new BusinessException(409, "本轮模拟面试已经结束");
        }
        return session;
    }

    private MockInterviewTurn getOwnedTurn(MockInterviewSession session, Long turnId) {
        MockInterviewTurn turn = turnMapper.selectOne(new LambdaQueryWrapper<MockInterviewTurn>()
                .eq(MockInterviewTurn::getId, turnId)
                .eq(MockInterviewTurn::getSessionId, session.getId()));
        if (turn == null) {
            throw new BusinessException(404, "模拟面试问题不存在");
        }
        return turn;
    }

    private List<MockInterviewTurn> getTurns(Long sessionId) {
        return turnMapper.selectList(new LambdaQueryWrapper<MockInterviewTurn>()
                .eq(MockInterviewTurn::getSessionId, sessionId)
                .orderByAsc(MockInterviewTurn::getSequenceNo)
                .orderByAsc(MockInterviewTurn::getCreateTime)
                .orderByAsc(MockInterviewTurn::getId));
    }

    private String toTranscript(List<MockInterviewTurn> turns) {
        StringBuilder builder = new StringBuilder();
        for (MockInterviewTurn turn : turns) {
            builder.append(MAIN_TURN.equals(defaultTurnType(turn)) ? "主问题 " : "追问 ")
                    .append(turn.getSequenceNo());
            if (turn.getFollowUpNo() != null) {
                builder.append("（第").append(turn.getFollowUpNo()).append("次）");
            }
            builder.append("：").append(turn.getQuestion()).append('\n');
            if (turn.getUserAnswer() != null) {
                builder.append("回答：").append(turn.getUserAnswer()).append('\n');
                if (turn.getScore() != null) {
                    builder.append("评分：").append(turn.getScore()).append("/10\n");
                }
            }
        }
        return builder.toString();
    }

    private MockInterviewSessionResponse toSessionResponse(
            MockInterviewSession session,
            List<MockInterviewTurn> turns) {
        return new MockInterviewSessionResponse(
                session.getId(), session.getTargetPosition(), session.getTargetCompany(), session.getInterviewRound(), session.getStatus(),
                session.getQuestionCount(), session.getQuestionLimit() == null
                        ? questionLimitFor(session.getInterviewRound()) : session.getQuestionLimit(),
                session.getAiModelId(), session.getSummary(), session.getCreateTime(),
                session.getFinishedTime(), turns.stream().map(this::toTurnResponse).toList());
    }

    private MockInterviewTurnResponse toTurnResponse(MockInterviewTurn turn) {
        return new MockInterviewTurnResponse(
                turn.getId(), turn.getSequenceNo(), defaultTurnType(turn), turn.getParentTurnId(), turn.getFollowUpNo(),
                turn.getQuestion(), turn.getUserAnswer(), turn.getScore(),
                turn.getCorrectAnswer(), turn.getSuggestion(), turn.getCreateTime());
    }

    private void requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private int questionLimitFor(String interviewRound) {
        return switch (interviewRound) {
            case "FIRST" -> 8;
            case "SECOND" -> 12;
            case "THIRD" -> 10;
            case "HR" -> 4;
            default -> throw new BusinessException(400, "面试轮次无效");
        };
    }

    private MockInterviewTurn latestTurn(List<MockInterviewTurn> turns) {
        return turns.isEmpty() ? null : turns.get(turns.size() - 1);
    }

    private boolean isCurrentQuestion(MockInterviewTurn parentTurn, MockInterviewTurn currentTurn) {
        return parentTurn.getId().equals(currentTurn.getId())
                || parentTurn.getId().equals(currentTurn.getParentTurnId());
    }

    private String defaultTurnType(MockInterviewTurn turn) {
        return turn.getTurnType() == null ? MAIN_TURN : turn.getTurnType();
    }
}
