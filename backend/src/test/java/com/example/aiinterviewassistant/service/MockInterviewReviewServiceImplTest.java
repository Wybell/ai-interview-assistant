package com.example.aiinterviewassistant.service;

import com.example.aiinterviewassistant.dto.AiMockInterviewReview;
import com.example.aiinterviewassistant.entity.MockInterviewSession;
import com.example.aiinterviewassistant.entity.MockInterviewTurn;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.example.aiinterviewassistant.mapper.MockInterviewReviewMapper;
import com.example.aiinterviewassistant.mapper.MockInterviewSessionMapper;
import com.example.aiinterviewassistant.mapper.MockInterviewTurnMapper;
import com.example.aiinterviewassistant.model.EffectiveAiModel;
import com.example.aiinterviewassistant.service.impl.MockInterviewReviewServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MockInterviewReviewServiceImplTest {

    @Mock private MockInterviewSessionMapper sessionMapper;
    @Mock private MockInterviewTurnMapper turnMapper;
    @Mock private MockInterviewReviewMapper reviewMapper;
    @Mock private UserAiPreferenceService userAiPreferenceService;
    @Mock private AiService aiService;

    @Test
    void shouldGenerateReviewForAnEndedInterviewWithAnswers() {
        when(sessionMapper.selectOne(any())).thenReturn(endedSession());
        when(turnMapper.selectList(any())).thenReturn(List.of(answeredTurn()));
        when(reviewMapper.selectOne(any())).thenReturn(null);
        when(userAiPreferenceService.resolveAvailableModel(1L)).thenReturn(new EffectiveAiModel(
                1L, "deepseek", "deepseek-chat", "DeepSeek", true));
        when(aiService.generateMockInterviewReview(any(), any(), any(), any(), any())).thenReturn(
                new AiMockInterviewReview("整体表现稳定", "表达清晰", "补足缓存一致性", "完成三次专项练习"));

        var response = service().generateReview(1L, 7L);

        assertThat(response.averageScore()).isEqualByComparingTo("8.00");
        assertThat(response.overallFeedback()).isEqualTo("整体表现稳定");
        verify(reviewMapper).insert(any());
    }

    @Test
    void shouldRejectReviewForAnActiveInterview() {
        MockInterviewSession session = endedSession();
        session.setStatus("ACTIVE");
        when(sessionMapper.selectOne(any())).thenReturn(session);

        assertThatThrownBy(() -> service().generateReview(1L, 7L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请先完成或提前结束本轮面试后再进行复盘");

        verify(aiService, never()).generateMockInterviewReview(any(), any(), any(), any(), any());
    }

    private MockInterviewReviewServiceImpl service() {
        return new MockInterviewReviewServiceImpl(
                sessionMapper, turnMapper, reviewMapper, userAiPreferenceService, aiService);
    }

    private MockInterviewSession endedSession() {
        MockInterviewSession session = new MockInterviewSession();
        session.setId(7L);
        session.setUserId(1L);
        session.setTargetPosition("Java backend intern");
        session.setInterviewRound("FIRST");
        session.setStatus("COMPLETED");
        session.setAiModelId(1L);
        session.setCreateTime(LocalDateTime.now());
        session.setFinishedTime(LocalDateTime.now());
        return session;
    }

    private MockInterviewTurn answeredTurn() {
        MockInterviewTurn turn = new MockInterviewTurn();
        turn.setId(11L);
        turn.setSequenceNo(1);
        turn.setTurnType("MAIN");
        turn.setQuestion("What is Redis?");
        turn.setUserAnswer("A cache database");
        turn.setScore(8);
        turn.setSuggestion("Explain persistence");
        turn.setCreateTime(LocalDateTime.now());
        return turn;
    }
}
