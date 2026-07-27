package com.example.aiinterviewassistant.service;

import com.example.aiinterviewassistant.dto.AiModelResponse;

import java.util.List;

public interface AiModelCatalogService {

    List<AiModelResponse> getAvailableModels(Long userId);
}
