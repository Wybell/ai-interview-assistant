package com.example.aiinterviewassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.aiinterviewassistant.client.AiClientRegistry;
import com.example.aiinterviewassistant.dto.AiModelResponse;
import com.example.aiinterviewassistant.entity.AiModel;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.example.aiinterviewassistant.mapper.AiModelMapper;
import com.example.aiinterviewassistant.model.EffectiveAiModel;
import com.example.aiinterviewassistant.service.AiModelCatalogService;
import com.example.aiinterviewassistant.service.UserAiPreferenceService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class AiModelCatalogServiceImpl implements AiModelCatalogService {

    private final AiModelMapper aiModelMapper;
    private final UserAiPreferenceService userAiPreferenceService;

    private final AiClientRegistry aiClientRegistry;

    public AiModelCatalogServiceImpl(
            AiModelMapper aiModelMapper,
            UserAiPreferenceService userAiPreferenceService,
            AiClientRegistry aiClientRegistry) {
        this.aiModelMapper = aiModelMapper;
        this.userAiPreferenceService = userAiPreferenceService;
        this.aiClientRegistry = aiClientRegistry;
    }

    @Override
    public List<AiModelResponse> getAvailableModels(Long userId) {
        requireAuthenticatedUser(userId);

        EffectiveAiModel selectedModel = userAiPreferenceService.resolveEffectiveModel(userId);
        EffectiveAiModel defaultModel = userAiPreferenceService.resolveDefaultModel();

        QueryWrapper<AiModel> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("enabled", true)
                .orderByAsc("sort_order")
                .orderByAsc("id");

        return aiModelMapper.selectList(queryWrapper).stream()
                .filter(this::isRuntimeAvailable)
                .map(model -> new AiModelResponse(
                        model.getId(),
                        model.getProvider(),
                        model.getModelCode(),
                        model.getDisplayName(),
                        Objects.equals(model.getId(), defaultModel.id()),
                        Objects.equals(model.getId(), selectedModel.id())
                ))
                .toList();
    }

    private boolean isRuntimeAvailable(AiModel aiModel) {
        return aiClientRegistry.isModelAvailable(
                aiModel.getProvider(),
                aiModel.getModelCode()
        );
    }

    private void requireAuthenticatedUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
    }
}
