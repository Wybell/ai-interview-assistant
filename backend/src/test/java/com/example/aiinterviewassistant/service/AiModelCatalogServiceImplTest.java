package com.example.aiinterviewassistant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.aiinterviewassistant.client.AiClientRegistry;
import com.example.aiinterviewassistant.dto.AiModelResponse;
import com.example.aiinterviewassistant.entity.AiModel;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.example.aiinterviewassistant.mapper.AiModelMapper;
import com.example.aiinterviewassistant.model.EffectiveAiModel;
import com.example.aiinterviewassistant.service.impl.AiModelCatalogServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiModelCatalogServiceImplTest {

    @Mock
    private AiModelMapper aiModelMapper;

    @Mock
    private UserAiPreferenceService userAiPreferenceService;

    @Mock
    private AiClientRegistry aiClientRegistry;

    @InjectMocks
    private AiModelCatalogServiceImpl aiModelCatalogService;

    @Captor
    private ArgumentCaptor<QueryWrapper<AiModel>> queryWrapperCaptor;

    @Test
    void shouldReturnEnabledModelsWithDefaultAndSelectionMarkers() {
        AiModel deepSeekModel = aiModel(
                1L,
                "deepseek",
                "deepseek-v4-flash",
                "DeepSeek V4 Flash"
        );
        AiModel terraModel = aiModel(
                2L,
                "custom",
                "gpt-5.6-terra",
                "5.6 Terra"
        );
        AiModel lunaModel = aiModel(
                3L,
                "custom",
                "gpt-5.6-luna",
                "5.6 Luna"
        );
        when(userAiPreferenceService.resolveEffectiveModel(7L)).thenReturn(
                new EffectiveAiModel(
                        3L,
                        "custom",
                        "gpt-5.6-luna",
                        "5.6 Luna",
                        false
                )
        );
        when(userAiPreferenceService.resolveDefaultModel()).thenReturn(
                new EffectiveAiModel(
                        1L,
                        "deepseek",
                        "deepseek-v4-flash",
                        "DeepSeek V4 Flash",
                        true
                )
        );
        when(aiModelMapper.selectList(any())).thenReturn(List.of(deepSeekModel, terraModel, lunaModel));
        when(aiClientRegistry.isModelAvailable("deepseek", "deepseek-v4-flash")).thenReturn(true);
        when(aiClientRegistry.isModelAvailable("custom", "gpt-5.6-terra")).thenReturn(true);
        when(aiClientRegistry.isModelAvailable("custom", "gpt-5.6-luna")).thenReturn(true);

        List<AiModelResponse> actual = aiModelCatalogService.getAvailableModels(7L);

        assertThat(actual).containsExactly(
                new AiModelResponse(
                        1L,
                        "deepseek",
                        "deepseek-v4-flash",
                        "DeepSeek V4 Flash",
                        true,
                        false
                ),
                new AiModelResponse(
                        2L,
                        "custom",
                        "gpt-5.6-terra",
                        "5.6 Terra",
                        false,
                        false
                ),
                new AiModelResponse(
                        3L,
                        "custom",
                        "gpt-5.6-luna",
                        "5.6 Luna",
                        false,
                        true
                )
        );

        verify(aiModelMapper).selectList(queryWrapperCaptor.capture());
        String sql = queryWrapperCaptor.getValue().getSqlSegment();
        assertThat(sql).containsIgnoringCase("enabled");
        assertThat(sql).containsIgnoringCase("sort_order");
    }

    @Test
    void shouldExcludeEnabledModelsWithIncompleteProviderConfiguration() {
        AiModel deepSeekModel = aiModel(
                1L,
                "deepseek",
                "deepseek-v4-flash",
                "DeepSeek V4 Flash"
        );
        AiModel lunaModel = aiModel(
                2L,
                "custom",
                "gpt-5.6-luna",
                "5.6 Luna"
        );
        EffectiveAiModel deepSeekEffectiveModel = new EffectiveAiModel(
                1L,
                "deepseek",
                "deepseek-v4-flash",
                "DeepSeek V4 Flash",
                true
        );
        when(userAiPreferenceService.resolveEffectiveModel(7L)).thenReturn(deepSeekEffectiveModel);
        when(userAiPreferenceService.resolveDefaultModel()).thenReturn(deepSeekEffectiveModel);
        when(aiModelMapper.selectList(any())).thenReturn(List.of(deepSeekModel, lunaModel));
        when(aiClientRegistry.isModelAvailable("deepseek", "deepseek-v4-flash")).thenReturn(true);
        when(aiClientRegistry.isModelAvailable("custom", "gpt-5.6-luna")).thenReturn(false);

        List<AiModelResponse> actual = aiModelCatalogService.getAvailableModels(7L);

        assertThat(actual).containsExactly(new AiModelResponse(
                1L,
                "deepseek",
                "deepseek-v4-flash",
                "DeepSeek V4 Flash",
                true,
                true
        ));
    }

    @Test
    void shouldRejectUnauthenticatedCatalogQuery() {
        assertThatThrownBy(() -> aiModelCatalogService.getAvailableModels(null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请先登录");

        verifyNoInteractions(aiModelMapper, userAiPreferenceService);
    }

    private AiModel aiModel(Long id, String provider, String modelCode, String displayName) {
        AiModel aiModel = new AiModel();
        aiModel.setId(id);
        aiModel.setProvider(provider);
        aiModel.setModelCode(modelCode);
        aiModel.setDisplayName(displayName);
        aiModel.setEnabled(true);
        return aiModel;
    }
}
