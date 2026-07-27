package com.example.aiinterviewassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.aiinterviewassistant.dto.MistakeResponse;
import com.example.aiinterviewassistant.dto.StudyProgressResponse;
import com.example.aiinterviewassistant.entity.AnswerRecord;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.example.aiinterviewassistant.mapper.AnswerRecordMapper;
import com.example.aiinterviewassistant.service.StudyService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class StudyServiceImpl implements StudyService {

    private final AnswerRecordMapper answerRecordMapper;

    public StudyServiceImpl(AnswerRecordMapper answerRecordMapper) {
        this.answerRecordMapper = answerRecordMapper;
    }

    @Override
    public List<MistakeResponse> getMistakes(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }

        QueryWrapper<AnswerRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .lt("score", 6)
                .orderByDesc("create_time");

        return answerRecordMapper.selectList(wrapper).stream()
                .map(MistakeResponse::from)
                .toList();
    }

    @Override
    public List<StudyProgressResponse> getProgress(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }

        QueryWrapper<AnswerRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .select(
                        "tag",
                        "COUNT(*) as totalCount",
                        "AVG(score) as avgScore"
                )
                .groupBy("tag");

        return answerRecordMapper.selectMaps(wrapper).stream()
                .map(StudyProgressResponse::from)
                .toList();
    }
}
