package com.example.aiinterviewassistant.service;

import com.example.aiinterviewassistant.dto.AiModelPreferenceResponse;
import com.example.aiinterviewassistant.client.AiClientRegistry;
import com.example.aiinterviewassistant.entity.AiModel;
import com.example.aiinterviewassistant.entity.AiModelPolicy;
import com.example.aiinterviewassistant.entity.UserAiPreference;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.example.aiinterviewassistant.mapper.AiModelMapper;
import com.example.aiinterviewassistant.mapper.AiModelPolicyMapper;
import com.example.aiinterviewassistant.mapper.UserAiPreferenceMapper;
import com.example.aiinterviewassistant.model.EffectiveAiModel;
import com.example.aiinterviewassistant.service.impl.UserAiPreferenceServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAiPreferenceServiceImplTest {

    @Mock
    private AiModelMapper aiModelMapper;

    @Mock
    private AiModelPolicyMapper aiModelPolicyMapper;

    @Mock
    private UserAiPreferenceMapper userAiPreferenceMapper;

    @Mock
    private AiClientRegistry aiClientRegistry;

    @InjectMocks
    private UserAiPreferenceServiceImpl userAiPreferenceService;

    @Test
    void shouldResolveEnabledUserPreferenceBeforePolicyDefault() {
        when(userAiPreferenceMapper.selectById(7L)).thenReturn(preference(7L, 2L));
        when(aiModelMapper.selectById(2L)).thenReturn(change2ProModel(true));
        when(aiClientRegistry.isModelAvailable("change2proapi", "gpt-5.6-luna")).thenReturn(true);

        EffectiveAiModel actual = userAiPreferenceService.resolveEffectiveModel(7L);

        assertThat(actual).isEqualTo(new EffectiveAiModel(
                2L,
                "change2proapi",
                "gpt-5.6-luna",
                "GPT-5.6 Luna",
                false
        ));
        verifyNoInteractions(aiModelPolicyMapper);
    }

    @Test
    void shouldFallbackToPolicyDefaultWhenUserHasNoPreference() {
        when(userAiPreferenceMapper.selectById(7L)).thenReturn(null);
        when(aiModelPolicyMapper.selectById(1)).thenReturn(policy(1L));
        when(aiModelMapper.selectById(1L)).thenReturn(deepSeekModel(true));
        when(aiClientRegistry.isModelAvailable("deepseek", "deepseek-v4-flash")).thenReturn(true);

        EffectiveAiModel actual = userAiPreferenceService.resolveEffectiveModel(7L);

        assertThat(actual).isEqualTo(new EffectiveAiModel(
                1L,
                "deepseek",
                "deepseek-v4-flash",
                "DeepSeek V4 Flash",
                true
        ));
    }

    @Test
    void shouldFallbackToPolicyDefaultWhenStoredPreferenceIsDisabled() {
        when(userAiPreferenceMapper.selectById(7L)).thenReturn(preference(7L, 2L));
        when(aiModelMapper.selectById(2L)).thenReturn(change2ProModel(false));
        when(aiModelPolicyMapper.selectById(1)).thenReturn(policy(1L));
        when(aiModelMapper.selectById(1L)).thenReturn(deepSeekModel(true));
        when(aiClientRegistry.isModelAvailable("deepseek", "deepseek-v4-flash")).thenReturn(true);

        EffectiveAiModel actual = userAiPreferenceService.resolveEffectiveModel(7L);

        assertThat(actual.id()).isEqualTo(1L);
        assertThat(actual.defaultSelection()).isTrue();
    }

    @Test
    void shouldReturnEffectivePreferenceResponse() {
        when(userAiPreferenceMapper.selectById(7L)).thenReturn(null);
        when(aiModelPolicyMapper.selectById(1)).thenReturn(policy(1L));
        when(aiModelMapper.selectById(1L)).thenReturn(deepSeekModel(true));
        when(aiClientRegistry.isModelAvailable("deepseek", "deepseek-v4-flash")).thenReturn(true);

        AiModelPreferenceResponse actual = userAiPreferenceService.getEffectivePreference(7L);

        assertThat(actual).isEqualTo(new AiModelPreferenceResponse(
                1L,
                "deepseek",
                "deepseek-v4-flash",
                "DeepSeek V4 Flash",
                true
        ));
    }

    @Test
    void shouldAtomicallyUpsertEnabledModelPreference() {
        when(aiModelMapper.selectById(2L)).thenReturn(change2ProModel(true));
        when(aiClientRegistry.isModelAvailable("change2proapi", "gpt-5.6-luna")).thenReturn(true);
        when(userAiPreferenceMapper.upsert(any())).thenReturn(1);

        AiModelPreferenceResponse actual = userAiPreferenceService.updatePreference(7L, 2L);

        assertThat(actual).isEqualTo(new AiModelPreferenceResponse(
                2L,
                "change2proapi",
                "gpt-5.6-luna",
                "GPT-5.6 Luna",
                false
        ));

        ArgumentCaptor<UserAiPreference> preferenceCaptor =
                ArgumentCaptor.forClass(UserAiPreference.class);
        verify(userAiPreferenceMapper).upsert(preferenceCaptor.capture());
        assertThat(preferenceCaptor.getValue().getUserId()).isEqualTo(7L);
        assertThat(preferenceCaptor.getValue().getAiModelId()).isEqualTo(2L);
    }

    @Test
    void shouldRejectUnavailableModelWhenUpdatingPreference() {
        when(aiModelMapper.selectById(2L)).thenReturn(change2ProModel(false));

        assertThatThrownBy(() -> userAiPreferenceService.updatePreference(7L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("所选 AI 模型不可用");

        verifyNoInteractions(userAiPreferenceMapper);
    }

    @Test
    void shouldRejectEnabledModelWhenProviderConfigurationIsUnavailable() {
        when(aiModelMapper.selectById(2L)).thenReturn(change2ProModel(true));
        when(aiClientRegistry.isModelAvailable("change2proapi", "gpt-5.6-luna")).thenReturn(false);

        assertThatThrownBy(() -> userAiPreferenceService.updatePreference(7L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getCode())
                .isEqualTo(503);

        verifyNoInteractions(userAiPreferenceMapper);
    }

    @Test
    void shouldFallbackToDefaultWhenPreferredProviderConfigurationIsUnavailable() {
        when(userAiPreferenceMapper.selectById(7L)).thenReturn(preference(7L, 2L));
        when(aiModelMapper.selectById(2L)).thenReturn(change2ProModel(true));
        when(aiClientRegistry.isModelAvailable("change2proapi", "gpt-5.6-luna")).thenReturn(false);
        when(aiModelPolicyMapper.selectById(1)).thenReturn(policy(1L));
        when(aiModelMapper.selectById(1L)).thenReturn(deepSeekModel(true));
        when(aiClientRegistry.isModelAvailable("deepseek", "deepseek-v4-flash")).thenReturn(true);

        EffectiveAiModel actual = userAiPreferenceService.resolveEffectiveModel(7L);

        assertThat(actual.id()).isEqualTo(1L);
        assertThat(actual.defaultSelection()).isTrue();
    }

    @Test
    void shouldRejectDefaultModelWhenProviderConfigurationIsUnavailable() {
        when(aiModelPolicyMapper.selectById(1)).thenReturn(policy(1L));
        when(aiModelMapper.selectById(1L)).thenReturn(deepSeekModel(true));
        when(aiClientRegistry.isModelAvailable("deepseek", "deepseek-v4-flash")).thenReturn(false);

        assertThatThrownBy(userAiPreferenceService::resolveDefaultModel)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getCode())
                .isEqualTo(503);
    }

    @Test
    void shouldRejectMissingModelIdWhenUpdatingPreference() {
        assertThatThrownBy(() -> userAiPreferenceService.updatePreference(7L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("所选 AI 模型不可用");

        verifyNoInteractions(aiModelMapper, userAiPreferenceMapper);
    }

    @Test
    void shouldRejectMissingDefaultModelPolicy() {
        when(aiModelPolicyMapper.selectById(1)).thenReturn(null);

        assertThatThrownBy(userAiPreferenceService::resolveDefaultModel)
                .isInstanceOf(BusinessException.class)
                .hasMessage("系统默认 AI 模型配置缺失");

        verifyNoInteractions(aiModelMapper);
    }

    @Test
    void shouldRejectUnauthenticatedPreferenceRequest() {
        assertThatThrownBy(() -> userAiPreferenceService.resolveEffectiveModel(null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请先登录");

        verifyNoInteractions(aiModelMapper, aiModelPolicyMapper, userAiPreferenceMapper);
    }

    private UserAiPreference preference(Long userId, Long modelId) {
        UserAiPreference preference = new UserAiPreference();
        preference.setUserId(userId);
        preference.setAiModelId(modelId);
        return preference;
    }

    private AiModelPolicy policy(Long defaultModelId) {
        AiModelPolicy policy = new AiModelPolicy();
        policy.setId(1);
        policy.setDefaultAiModelId(defaultModelId);
        return policy;
    }

    private AiModel deepSeekModel(boolean enabled) {
        return aiModel(
                1L,
                "deepseek",
                "deepseek-v4-flash",
                "DeepSeek V4 Flash",
                enabled
        );
    }

    private AiModel change2ProModel(boolean enabled) {
        return aiModel(
                2L,
                "change2proapi",
                "gpt-5.6-luna",
                "GPT-5.6 Luna",
                enabled
        );
    }

    private AiModel aiModel(
            Long id,
            String provider,
            String modelCode,
            String displayName,
            boolean enabled) {
        AiModel aiModel = new AiModel();
        aiModel.setId(id);
        aiModel.setProvider(provider);
        aiModel.setModelCode(modelCode);
        aiModel.setDisplayName(displayName);
        aiModel.setEnabled(enabled);
        return aiModel;
    }
}
