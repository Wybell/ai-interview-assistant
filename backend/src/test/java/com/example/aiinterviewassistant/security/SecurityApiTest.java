package com.example.aiinterviewassistant.security;

import com.example.aiinterviewassistant.config.SecurityConfig;
import com.example.aiinterviewassistant.controller.AiController;
import com.example.aiinterviewassistant.dto.AiScoreResult;
import com.example.aiinterviewassistant.dto.MistakeResponse;
import com.example.aiinterviewassistant.dto.StudyProgressResponse;
import com.example.aiinterviewassistant.mapper.AiModelMapper;
import com.example.aiinterviewassistant.mapper.AiModelPolicyMapper;
import com.example.aiinterviewassistant.mapper.AnswerRecordMapper;
import com.example.aiinterviewassistant.mapper.UserAiPreferenceMapper;
import com.example.aiinterviewassistant.mapper.UserMapper;
import com.example.aiinterviewassistant.sse.InterviewScoreSseAdapter;
import com.example.aiinterviewassistant.service.InterviewService;
import com.example.aiinterviewassistant.service.StudyService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiController.class)
@Import({SecurityConfig.class, RestSecurityExceptionHandler.class})
class SecurityApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserContext userContext;

    @MockBean
    private InterviewService interviewService;

    @MockBean
    private InterviewScoreSseAdapter interviewScoreSseAdapter;

    @MockBean
    private StudyService studyService;

    @MockBean
    private AnswerRecordMapper answerRecordMapper;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private AiModelMapper aiModelMapper;

    @MockBean
    private AiModelPolicyMapper aiModelPolicyMapper;

    @MockBean
    private UserAiPreferenceMapper userAiPreferenceMapper;

    @Test
    void shouldReturnUnauthorizedApiResponseWhenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/mistakes"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未登录或登录状态已失效"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void shouldReturnUnauthorizedApiResponseWhenTokenIsInvalid() throws Exception {
        when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

        mockMvc.perform(get("/api/mistakes")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未登录或登录状态已失效"));
    }

    @Test
    void shouldReturnMistakeResponseWhenTokenIsValid() throws Exception {
        stubValidAuthentication();
        when(userContext.getCurrentUserId()).thenReturn(1L);
        when(studyService.getMistakes(1L)).thenReturn(List.of(new MistakeResponse(
                1L,
                "Java",
                "What is JVM?",
                "My answer",
                5,
                "Standard answer",
                "Add details",
                null
        )));

        mockMvc.perform(get("/api/mistakes")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].tag").value("Java"))
                .andExpect(jsonPath("$.data[0].score").value(5))
                .andExpect(jsonPath("$.data[0].userId").doesNotExist());
    }

    @Test
    void shouldReturnTypedProgressResponseWhenTokenIsValid() throws Exception {
        stubValidAuthentication();
        when(userContext.getCurrentUserId()).thenReturn(1L);
        when(studyService.getProgress(1L)).thenReturn(List.of(
                new StudyProgressResponse("Java", 3L, 8.5)
        ));

        mockMvc.perform(get("/api/progress")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].tag").value("Java"))
                .andExpect(jsonPath("$.data[0].totalCount").value(3))
                .andExpect(jsonPath("$.data[0].avgScore").value(8.5));
    }

    @Test
    void shouldCreateQuestionFromValidatedJsonRequest() throws Exception {
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("valid-token")).thenReturn(1L);
        when(jwtUtil.getUsernameFromToken("valid-token")).thenReturn("alice");
        when(userContext.getCurrentUserId()).thenReturn(1L);
        when(interviewService.askQuestion(1L, "JVM", false))
                .thenReturn("Explain JVM memory areas.");

        mockMvc.perform(post("/api/question/ask")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tag\":\"JVM\",\"refresh\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("Explain JVM memory areas."));
    }

    @Test
    void shouldRejectBlankQuestionTagInJsonRequest() throws Exception {
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("valid-token")).thenReturn(1L);
        when(jwtUtil.getUsernameFromToken("valid-token")).thenReturn("alice");

        mockMvc.perform(post("/api/question/ask")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tag\":\"\",\"refresh\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldRejectOverlongQuestionTagInJsonRequest() throws Exception {
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("valid-token")).thenReturn(1L);
        when(jwtUtil.getUsernameFromToken("valid-token")).thenReturn("alice");

        String tag = "a".repeat(51);

        mockMvc.perform(post("/api/question/ask")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tag\":\"" + tag + "\",\"refresh\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldScoreValidatedJsonRequest() throws Exception {
        stubValidAuthentication();
        when(userContext.getCurrentUserId()).thenReturn(1L);

        AiScoreResult scoreResult = new AiScoreResult(
                8,
                "standard answer",
                "add more details"
        );
        when(interviewService.scoreAnswer(1L, "What is JVM?", "My answer", "JVM"))
                .thenReturn(scoreResult);

        mockMvc.perform(post("/api/question/score")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tag\":\"JVM\",\"question\":\"What is JVM?\",\"answer\":\"My answer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.score").value(8));
    }

    @Test
    void shouldRejectBlankScoreAnswerInJsonRequest() throws Exception {
        stubValidAuthentication();

        mockMvc.perform(post("/api/question/score")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tag\":\"JVM\",\"question\":\"What is JVM?\",\"answer\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldRejectOverlongScoreAnswerInJsonRequest() throws Exception {
        stubValidAuthentication();
        String answer = "a".repeat(5001);

        mockMvc.perform(post("/api/question/score")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tag\":\"JVM\",\"question\":\"What is JVM?\",\"answer\":\"" + answer + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    private void stubValidAuthentication() {
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("valid-token")).thenReturn(1L);
        when(jwtUtil.getUsernameFromToken("valid-token")).thenReturn("alice");
    }
}
