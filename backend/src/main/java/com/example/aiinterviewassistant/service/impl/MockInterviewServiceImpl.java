package com.example.aiinterviewassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.example.aiinterviewassistant.service.ResumeService;
import com.example.aiinterviewassistant.service.UserAiPreferenceService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MockInterviewServiceImpl implements MockInterviewService {

    private static final int MAX_QUESTION_COUNT = 8;
    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String COMPLETED_STATUS = "COMPLETED";

    private final MockInterviewSessionMapper sessionMapper;
    private final MockInterviewTurnMapper turnMapper;
    private final ResumeService resumeService;
    private final UserAiPreferenceService userAiPreferenceService;
    private final AiService aiService;

    public MockInterviewServiceImpl(
            MockInterviewSessionMapper sessionMapper,
            MockInterviewTurnMapper turnMapper,
            ResumeService resumeService,
            UserAiPreferenceService userAiPreferenceService,
            AiService aiService) {
        this.sessionMapper = sessionMapper;
        this.turnMapper = turnMapper;
        this.resumeService = resumeService;
        this.userAiPreferenceService = userAiPreferenceService;
        this.aiService = aiService;
    }

    @Override
    public MockInterviewSessionResponse createSession(
            Long userId,
            Long resumeId,
            String targetPosition,
            String interviewRound) {
        requireUser(userId);
        ResumeDocument resume = resumeService.getOwnedResume(userId, resumeId);
        EffectiveAiModel aiModel = userAiPreferenceService.resolveEffectiveModel(userId);

        MockInterviewSession session = new MockInterviewSession();
        session.setUserId(userId);
        session.setResumeId(resume.getId());
        session.setTargetPosition(targetPosition.trim());
        session.setInterviewRound(interviewRound);
        session.setStatus(ACTIVE_STATUS);
        session.setQuestionCount(0);
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
    public MockInterviewTurnResponse answerTurn(Long userId, Long sessionId, Long turnId, String answer) {
        MockInterviewSession session = getActiveSession(userId, sessionId);
        MockInterviewTurn turn = getOwnedTurn(session, turnId);
        if (turn.getUserAnswer() != null) {
            throw new BusinessException(409, "This question has already been answered");
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
        if (turns.isEmpty() || turns.get(turns.size() - 1).getUserAnswer() == null) {
            throw new BusinessException(409, "Answer the current question before continuing");
        }
        if (session.getQuestionCount() >= MAX_QUESTION_COUNT) {
            throw new BusinessException(409, "This interview has reached the 8-question limit");
        }
        ResumeDocument resume = resumeService.getOwnedResume(userId, session.getResumeId());
        EffectiveAiModel aiModel = userAiPreferenceService.resolveAvailableModel(session.getAiModelId());
        return toTurnResponse(createQuestion(session, resume, aiModel, turns));
    }

    @Override
    public MockInterviewSessionResponse finishSession(Long userId, Long sessionId) {
        MockInterviewSession session = getActiveSession(userId, sessionId);
        List<MockInterviewTurn> turns = getTurns(sessionId);
        if (turns.stream().noneMatch(turn -> turn.getUserAnswer() != null)) {
            throw new BusinessException(409, "Answer at least one question before finishing");
        }
        EffectiveAiModel aiModel = userAiPreferenceService.resolveAvailableModel(session.getAiModelId());
        session.setSummary(aiService.generateMockInterviewSummary(
                aiModel,
                session.getTargetPosition(),
                session.getInterviewRound(),
                toTranscript(turns)));
        session.setStatus(COMPLETED_STATUS);
        session.setFinishedTime(LocalDateTime.now());
        sessionMapper.updateById(session);
        return toSessionResponse(session, turns);
    }

    private MockInterviewTurn createQuestion(
            MockInterviewSession session,
            ResumeDocument resume,
            EffectiveAiModel aiModel,
            List<MockInterviewTurn> previousTurns) {
        MockInterviewTurn turn = new MockInterviewTurn();
        turn.setSessionId(session.getId());
        turn.setSequenceNo(session.getQuestionCount() + 1);
        turn.setQuestion(aiService.generateMockInterviewQuestion(
                aiModel,
                resume.getExtractedContent(),
                session.getTargetPosition(),
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
            throw new BusinessException(404, "Mock interview session not found");
        }
        return session;
    }

    private MockInterviewSession getActiveSession(Long userId, Long sessionId) {
        MockInterviewSession session = getOwnedSession(userId, sessionId);
        if (!ACTIVE_STATUS.equals(session.getStatus())) {
            throw new BusinessException(409, "Mock interview is already completed");
        }
        return session;
    }

    private MockInterviewTurn getOwnedTurn(MockInterviewSession session, Long turnId) {
        MockInterviewTurn turn = turnMapper.selectOne(new LambdaQueryWrapper<MockInterviewTurn>()
                .eq(MockInterviewTurn::getId, turnId)
                .eq(MockInterviewTurn::getSessionId, session.getId()));
        if (turn == null) {
            throw new BusinessException(404, "Mock interview question not found");
        }
        return turn;
    }

    private List<MockInterviewTurn> getTurns(Long sessionId) {
        return turnMapper.selectList(new LambdaQueryWrapper<MockInterviewTurn>()
                .eq(MockInterviewTurn::getSessionId, sessionId)
                .orderByAsc(MockInterviewTurn::getSequenceNo));
    }

    private String toTranscript(List<MockInterviewTurn> turns) {
        StringBuilder builder = new StringBuilder();
        for (MockInterviewTurn turn : turns) {
            builder.append("Question ").append(turn.getSequenceNo()).append(": ").append(turn.getQuestion()).append('\n');
            if (turn.getUserAnswer() != null) {
                builder.append("Answer: ").append(turn.getUserAnswer()).append('\n');
                if (turn.getScore() != null) {
                    builder.append("Score: ").append(turn.getScore()).append("/10\n");
                }
            }
        }
        return builder.toString();
    }

    private MockInterviewSessionResponse toSessionResponse(
            MockInterviewSession session,
            List<MockInterviewTurn> turns) {
        return new MockInterviewSessionResponse(
                session.getId(), session.getTargetPosition(), session.getInterviewRound(), session.getStatus(),
                session.getQuestionCount(), session.getAiModelId(), session.getSummary(), session.getCreateTime(),
                session.getFinishedTime(), turns.stream().map(this::toTurnResponse).toList());
    }

    private MockInterviewTurnResponse toTurnResponse(MockInterviewTurn turn) {
        return new MockInterviewTurnResponse(
                turn.getId(), turn.getSequenceNo(), turn.getQuestion(), turn.getUserAnswer(), turn.getScore(),
                turn.getCorrectAnswer(), turn.getSuggestion(), turn.getCreateTime());
    }

    private void requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "Authentication is required");
        }
    }
}
