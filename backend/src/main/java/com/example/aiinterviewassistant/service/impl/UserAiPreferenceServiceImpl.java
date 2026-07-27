package com.example.aiinterviewassistant.service.impl;

import com.example.aiinterviewassistant.client.AiClientRegistry;
import com.example.aiinterviewassistant.dto.AiModelPreferenceResponse;
import com.example.aiinterviewassistant.entity.AiModel;
import com.example.aiinterviewassistant.entity.AiModelPolicy;
import com.example.aiinterviewassistant.entity.UserAiPreference;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.example.aiinterviewassistant.mapper.AiModelMapper;
import com.example.aiinterviewassistant.mapper.AiModelPolicyMapper;
import com.example.aiinterviewassistant.mapper.UserAiPreferenceMapper;
import com.example.aiinterviewassistant.model.EffectiveAiModel;
import com.example.aiinterviewassistant.service.UserAiPreferenceService;
import org.springframework.stereotype.Service;

@Service
public class UserAiPreferenceServiceImpl implements UserAiPreferenceService {

    private static final int DEFAULT_POLICY_ID = 1;

    private final AiModelMapper aiModelMapper;
    private final AiModelPolicyMapper aiModelPolicyMapper;
    private final UserAiPreferenceMapper userAiPreferenceMapper;

    private final AiClientRegistry aiClientRegistry;

    public UserAiPreferenceServiceImpl(
            AiModelMapper aiModelMapper,
            AiModelPolicyMapper aiModelPolicyMapper,
            UserAiPreferenceMapper userAiPreferenceMapper,
            AiClientRegistry aiClientRegistry) {
        this.aiModelMapper = aiModelMapper;
        this.aiModelPolicyMapper = aiModelPolicyMapper;
        this.userAiPreferenceMapper = userAiPreferenceMapper;
        this.aiClientRegistry = aiClientRegistry;
    }

    @Override
    public EffectiveAiModel resolveEffectiveModel(Long userId) {
        requireAuthenticatedUser(userId);

        UserAiPreference preference = userAiPreferenceMapper.selectById(userId);
        if (preference != null) {
            AiModel preferredModel = aiModelMapper.selectById(preference.getAiModelId());
            if (isSelectable(preferredModel)) {
                return toEffectiveModel(preferredModel, false);
            }
        }

        return resolveDefaultModel();
    }

    @Override
    public EffectiveAiModel resolveDefaultModel() {
        AiModelPolicy policy = aiModelPolicyMapper.selectById(DEFAULT_POLICY_ID);
        if (policy == null || policy.getDefaultAiModelId() == null) {
            throw new BusinessException(500, "系统默认 AI 模型配置缺失");
        }

        AiModel defaultModel = aiModelMapper.selectById(policy.getDefaultAiModelId());
        if (!isEnabled(defaultModel)) {
            throw new BusinessException(500, "系统默认 AI 模型不可用");
        }

        if (!isRuntimeAvailable(defaultModel)) {
            throw new BusinessException(503, "系统默认 AI 模型暂不可用");
        }

        return toEffectiveModel(defaultModel, true);
    }

    @Override
    public AiModelPreferenceResponse getEffectivePreference(Long userId) {
        return toResponse(resolveEffectiveModel(userId));
    }

    @Override
    public AiModelPreferenceResponse updatePreference(Long userId, Long modelId) {
        requireAuthenticatedUser(userId);

        if (modelId == null || modelId <= 0) {
            throw new BusinessException(400, "所选 AI 模型不可用");
        }

        AiModel selectedModel = aiModelMapper.selectById(modelId);
        if (!isEnabled(selectedModel)) {
            throw new BusinessException(400, "所选 AI 模型不可用");
        }

        if (!isRuntimeAvailable(selectedModel)) {
            throw new BusinessException(503, "所选 AI 模型暂不可用");
        }

        UserAiPreference preference = new UserAiPreference();
        preference.setUserId(userId);
        preference.setAiModelId(modelId);

        if (userAiPreferenceMapper.upsert(preference) <= 0) {
            throw new BusinessException(500, "保存 AI 模型偏好失败");
        }

        return toResponse(toEffectiveModel(selectedModel, false));
    }

    private boolean isEnabled(AiModel aiModel) {
        return aiModel != null && Boolean.TRUE.equals(aiModel.getEnabled());
    }

    private boolean isSelectable(AiModel aiModel) {
        return isEnabled(aiModel) && isRuntimeAvailable(aiModel);
    }

    private boolean isRuntimeAvailable(AiModel aiModel) {
        return aiModel != null && aiClientRegistry.isModelAvailable(
                aiModel.getProvider(),
                aiModel.getModelCode()
        );
    }

    private EffectiveAiModel toEffectiveModel(AiModel aiModel, boolean defaultSelection) {
        return new EffectiveAiModel(
                aiModel.getId(),
                aiModel.getProvider(),
                aiModel.getModelCode(),
                aiModel.getDisplayName(),
                defaultSelection
        );
    }

    private AiModelPreferenceResponse toResponse(EffectiveAiModel effectiveModel) {
        return new AiModelPreferenceResponse(
                effectiveModel.id(),
                effectiveModel.provider(),
                effectiveModel.modelCode(),
                effectiveModel.displayName(),
                effectiveModel.defaultSelection()
        );
    }

    private void requireAuthenticatedUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
    }
}
