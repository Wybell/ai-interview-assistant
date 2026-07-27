package com.example.aiinterviewassistant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.aiinterviewassistant.dto.MistakeResponse;
import com.example.aiinterviewassistant.dto.StudyProgressResponse;
import com.example.aiinterviewassistant.entity.AnswerRecord;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.example.aiinterviewassistant.mapper.AnswerRecordMapper;
import com.example.aiinterviewassistant.service.impl.StudyServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudyServiceImplTest {

    @Mock
    private AnswerRecordMapper answerRecordMapper;

    @InjectMocks
    private StudyServiceImpl studyService;

    @Captor
    private ArgumentCaptor<QueryWrapper<AnswerRecord>> queryWrapperCaptor;

    @Test
    void shouldQueryUserMistakes() {
        AnswerRecord answerRecord = new AnswerRecord();
        answerRecord.setId(1L);
        answerRecord.setTag("Java");
        answerRecord.setQuestion("What is JVM?");
        answerRecord.setUserAnswer("My answer");
        answerRecord.setScore(5);
        answerRecord.setCorrectAnswer("Standard answer");
        answerRecord.setSuggestion("Add details");
        List<AnswerRecord> answerRecords = List.of(answerRecord);
        when(answerRecordMapper.selectList(any())).thenReturn(answerRecords);

        List<MistakeResponse> actual = studyService.getMistakes(1L);

        assertThat(actual).containsExactly(new MistakeResponse(
                1L,
                "Java",
                "What is JVM?",
                "My answer",
                5,
                "Standard answer",
                "Add details",
                null
        ));

        verify(answerRecordMapper).selectList(queryWrapperCaptor.capture());

        String sql = queryWrapperCaptor.getValue().getSqlSegment();
        assertThat(sql).containsIgnoringCase("user_id");
        assertThat(sql).containsIgnoringCase("score");
        assertThat(sql).containsIgnoringCase("create_time");
    }

    @Test
    void shouldQueryUserProgress() {
        List<Map<String, Object>> expected = List.of(
                Map.of("tag", "Java", "totalCount", 3, "avgScore", 8.5)
        );
        when(answerRecordMapper.selectMaps(any())).thenReturn(expected);

        List<StudyProgressResponse> actual = studyService.getProgress(1L);

        assertThat(actual).containsExactly(new StudyProgressResponse("Java", 3L, 8.5));

        verify(answerRecordMapper).selectMaps(queryWrapperCaptor.capture());

        QueryWrapper<AnswerRecord> wrapper = queryWrapperCaptor.getValue();
        assertThat(wrapper.getSqlSegment()).containsIgnoringCase("user_id");
        assertThat(wrapper.getSqlSegment()).containsIgnoringCase("group by");
        assertThat(wrapper.getSqlSelect()).contains("tag");
    }

    @Test
    void shouldRejectUnauthenticatedMistakeQuery() {
        assertThatThrownBy(() -> studyService.getMistakes(null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请先登录");

        verifyNoInteractions(answerRecordMapper);
    }

    @Test
    void shouldRejectUnauthenticatedProgressQuery() {
        assertThatThrownBy(() -> studyService.getProgress(null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请先登录");

        verifyNoInteractions(answerRecordMapper);
    }
}
