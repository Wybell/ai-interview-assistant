package com.example.aiinterviewassistant.security;

import com.example.aiinterviewassistant.config.SecurityConfig;
import com.example.aiinterviewassistant.controller.AiModelController;
import com.example.aiinterviewassistant.dto.AiModelPreferenceResponse;
import com.example.aiinterviewassistant.dto.AiModelResponse;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.example.aiinterviewassistant.mapper.AiModelMapper;
import com.example.aiinterviewassistant.mapper.AiModelPolicyMapper;
import com.example.aiinterviewassistant.mapper.AnswerRecordMapper;
import com.example.aiinterviewassistant.mapper.UserAiPreferenceMapper;
import com.example.aiinterviewassistant.mapper.UserMapper;
import com.example.aiinterviewassistant.mapper.KnowledgeTopicMapper;
import com.example.aiinterviewassistant.mapper.KnowledgeQuestionMapper;
import com.example.aiinterviewassistant.service.AiModelCatalogService;
import com.example.aiinterviewassistant.service.UserAiPreferenceService;
import com.example.aiinterviewassistant.utils.JwtUtil;
import com.example.aiinterviewassistant.utils.UserContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiModelController.class)
@Import({SecurityConfig.class, RestSecurityExceptionHandler.class})
class AiModelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserContext userContext;

    @MockBean
    private AiModelCatalogService aiModelCatalogService;

    @MockBean
    private UserAiPreferenceService userAiPreferenceService;

    @MockBean
    private AnswerRecordMapper answerRecordMapper;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private KnowledgeTopicMapper knowledgeTopicMapper;

    @MockBean
    private KnowledgeQuestionMapper knowledgeQuestionMapper;

    @MockBean
    private AiModelMapper aiModelMapper;

    @MockBean
    private AiModelPolicyMapper aiModelPolicyMapper;

    @MockBean
    private UserAiPreferenceMapper userAiPreferenceMapper;

    @Test
    void shouldRejectUnauthenticatedModelCatalogRequest() throws Exception {
        mockMvc.perform(get("/api/ai/models"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(401));

        verifyNoInteractions(aiModelCatalogService, userAiPreferenceService);
    }

    @Test
    void shouldReturnAvailableModelsForCurrentUser() throws Exception {
        stubValidAuthentication();
        when(userContext.getCurrentUserId()).thenReturn(7L);
        when(aiModelCatalogService.getAvailableModels(7L)).thenReturn(List.of(
                new AiModelResponse(
                        1L,
                        "deepseek",
                        "deepseek-v4-flash",
                        "DeepSeek V4 Flash",
                        true,
                        true
                ),
                new AiModelResponse(
                        2L,
                        "change2proapi",
                        "gpt-5.6-luna",
                        "GPT-5.6 Luna",
                        false,
                        false
                )
        ));

        mockMvc.perform(get("/api/ai/models")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].modelId").doesNotExist())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].provider").value("deepseek"))
                .andExpect(jsonPath("$.data[0].modelCode").value("deepseek-v4-flash"))
                .andExpect(jsonPath("$.data[0].defaultModel").value(true))
                .andExpect(jsonPath("$.data[0].selected").value(true));

        verify(aiModelCatalogService).getAvailableModels(7L);
    }

    @Test
    void shouldReturnEffectivePreferenceForCurrentUser() throws Exception {
        stubValidAuthentication();
        when(userContext.getCurrentUserId()).thenReturn(7L);
        when(userAiPreferenceService.getEffectivePreference(7L)).thenReturn(
                new AiModelPreferenceResponse(
                        1L,
                        "deepseek",
                        "deepseek-v4-flash",
                        "DeepSeek V4 Flash",
                        true
                )
        );

        mockMvc.perform(get("/api/users/me/ai-preference")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.modelId").value(1))
                .andExpect(jsonPath("$.data.provider").value("deepseek"))
                .andExpect(jsonPath("$.data.modelCode").value("deepseek-v4-flash"))
                .andExpect(jsonPath("$.data.defaultSelection").value(true));

        verify(userAiPreferenceService).getEffectivePreference(7L);
    }

    @Test
    void shouldUpdateOnlyCurrentUsersPreference() throws Exception {
        stubValidAuthentication();
        when(userContext.getCurrentUserId()).thenReturn(7L);
        when(userAiPreferenceService.updatePreference(7L, 2L)).thenReturn(
                new AiModelPreferenceResponse(
                        2L,
                        "change2proapi",
                        "gpt-5.6-luna",
                        "GPT-5.6 Luna",
                        false
                )
        );

        mockMvc.perform(put("/api/users/me/ai-preference")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modelId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.modelId").value(2))
                .andExpect(jsonPath("$.data.provider").value("change2proapi"))
                .andExpect(jsonPath("$.data.defaultSelection").value(false));

        verify(userAiPreferenceService).updatePreference(7L, 2L);
    }

    @Test
    void shouldRejectNonPositiveModelId() throws Exception {
        stubValidAuthentication();

        mockMvc.perform(put("/api/users/me/ai-preference")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modelId\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        verifyNoInteractions(userAiPreferenceService);
    }

    @Test
    void shouldReturnUnavailableModelErrorWhenPreferenceUpdateIsRejected() throws Exception {
        stubValidAuthentication();
        when(userContext.getCurrentUserId()).thenReturn(7L);
        when(userAiPreferenceService.updatePreference(7L, 2L))
                .thenThrow(new BusinessException(400, "所选 AI 模型不可用"));

        mockMvc.perform(put("/api/users/me/ai-preference")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modelId\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    private void stubValidAuthentication() {
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("valid-token")).thenReturn(7L);
        when(jwtUtil.getUsernameFromToken("valid-token")).thenReturn("alice");
    }
}
