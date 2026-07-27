package com.example.aiinterviewassistant.service;

import com.example.aiinterviewassistant.dto.MistakeResponse;
import com.example.aiinterviewassistant.dto.StudyProgressResponse;

import java.util.List;

public interface StudyService {

    List<MistakeResponse> getMistakes(Long userId);

    List<StudyProgressResponse> getProgress(Long userId);
}
