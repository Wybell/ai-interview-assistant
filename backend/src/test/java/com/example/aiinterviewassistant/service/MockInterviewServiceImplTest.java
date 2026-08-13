package com.example.aiinterviewassistant.service;

import com.example.aiinterviewassistant.entity.MockInterviewSession;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.example.aiinterviewassistant.mapper.MockInterviewSessionMapper;
import com.example.aiinterviewassistant.mapper.MockInterviewTurnMapper;
import com.example.aiinterviewassistant.service.impl.MockInterviewServiceImpl;
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
class MockInterviewServiceImplTest {

    @Mock
    private MockInterviewSessionMapper sessionMapper;

    @Mock
    private MockInterviewTurnMapper turnMapper;

    @Mock
    private ResumeService resumeService;

    @Mock
    private UserAiPreferenceService userAiPreferenceService;

    @Mock
    private AiService aiService;

    @Mock
    private MockInterviewReviewService mockInterviewReviewService;

    @Test
    void shouldEndOnlyTheCurrentUsersActiveSessionEarly() {
        MockInterviewSession session = activeSession();
        when(sessionMapper.selectOne(any())).thenReturn(session);
        when(turnMapper.selectList(any())).thenReturn(List.of());
        MockInterviewServiceImpl service = service();

        service.endSessionEarly(1L, 7L);

        assertThat(session.getStatus()).isEqualTo("ENDED_EARLY");
        assertThat(session.getFinishedTime()).isNotNull();
        verify(sessionMapper).updateById(session);
        verify(turnMapper).selectList(any());
    }

    @Test
    void shouldRejectEarlyEndForAnAlreadyCompletedSession() {
        MockInterviewSession session = activeSession();
        session.setStatus("COMPLETED");
        when(sessionMapper.selectOne(any())).thenReturn(session);
        MockInterviewServiceImpl service = service();

        assertThatThrownBy(() -> service.endSessionEarly(1L, 7L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("本轮模拟面试已经结束");

        verify(sessionMapper, never()).updateById(any());
    }

    private MockInterviewServiceImpl service() {
        return new MockInterviewServiceImpl(
                sessionMapper,
                turnMapper,
                resumeService,
                userAiPreferenceService,
                aiService,
                mockInterviewReviewService
        );
    }

    private MockInterviewSession activeSession() {
        MockInterviewSession session = new MockInterviewSession();
        session.setId(7L);
        session.setUserId(1L);
        session.setTargetPosition("Java backend intern");
        session.setInterviewRound("FIRST");
        session.setStatus("ACTIVE");
        session.setQuestionCount(1);
        session.setQuestionLimit(8);
        session.setAiModelId(1L);
        session.setCreateTime(LocalDateTime.now());
        return session;
    }
}
