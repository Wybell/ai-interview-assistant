package com.example.aiinterviewassistant.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.aiinterviewassistant.controller.AiController;
import com.example.aiinterviewassistant.controller.AiModelController;
import com.example.aiinterviewassistant.controller.AuthController;
import com.example.aiinterviewassistant.mapper.AiModelMapper;
import com.example.aiinterviewassistant.mapper.AiModelPolicyMapper;
import com.example.aiinterviewassistant.mapper.AnswerRecordMapper;
import com.example.aiinterviewassistant.mapper.UserAiPreferenceMapper;
import com.example.aiinterviewassistant.mapper.UserMapper;
import com.example.aiinterviewassistant.security.RestSecurityExceptionHandler;
import com.example.aiinterviewassistant.service.AiModelCatalogService;
import com.example.aiinterviewassistant.service.AuthService;
import com.example.aiinterviewassistant.service.InterviewService;
import com.example.aiinterviewassistant.service.StudyService;
import com.example.aiinterviewassistant.service.UserAiPreferenceService;
import com.example.aiinterviewassistant.sse.InterviewScoreSseAdapter;
import com.example.aiinterviewassistant.utils.JwtUtil;
import com.example.aiinterviewassistant.utils.UserContext;
import org.junit.jupiter.api.Test;
import org.springdoc.core.SpringDocConfigProperties;
import org.springdoc.core.SpringDocConfiguration;
import org.springdoc.core.SpringDocUIConfiguration;
import org.springdoc.core.SwaggerUiConfigParameters;
import org.springdoc.core.SwaggerUiConfigProperties;
import org.springdoc.core.SwaggerUiOAuthProperties;
import org.springdoc.webmvc.core.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.SpringDocWebMvcConfiguration;
import org.springdoc.webmvc.ui.SwaggerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {
                AuthController.class,
                AiController.class,
                AiModelController.class
        },
        properties = {
                "springdoc.api-docs.enabled=true",
                "springdoc.swagger-ui.enabled=true"
        }
)
@Import({OpenApiConfig.class, SecurityConfig.class, RestSecurityExceptionHandler.class})
@ImportAutoConfiguration({
        SpringDocConfigProperties.class,
        SpringDocConfiguration.class,
        SpringDocWebMvcConfiguration.class,
        MultipleOpenApiSupportConfiguration.class,
        SwaggerUiConfigProperties.class,
        SwaggerUiConfigParameters.class,
        SpringDocUIConfiguration.class,
        SwaggerUiOAuthProperties.class,
        SwaggerConfig.class
})
class OpenApiDocumentationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserContext userContext;

    @MockBean
    private AuthService authService;

    @MockBean
    private InterviewService interviewService;

    @MockBean
    private StudyService studyService;

    @MockBean
    private InterviewScoreSseAdapter interviewScoreSseAdapter;

    @MockBean
    private AiModelCatalogService aiModelCatalogService;

    @MockBean
    private UserAiPreferenceService userAiPreferenceService;

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
    void shouldExposeOpenApiDocumentationWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.info.title").value("AI Interview Assistant API"))
                .andExpect(jsonPath("$.info.version").value("1.0.0"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/question/ask']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/register'].post.summary").value("用户注册"))
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.summary").value("用户登录"))
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.requestBody.content['application/x-www-form-urlencoded']").exists())
                .andExpect(jsonPath("$.components.schemas.AuthRequest.properties.password.writeOnly").value(true))
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/ai/models'].get.summary").value("查询可选 AI 模型"))
                .andExpect(jsonPath("$.paths['/api/ai/models'].get.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/users/me/ai-preference'].put.summary").value("更新当前用户的 AI 模型偏好"))
                .andExpect(jsonPath("$.paths['/api/users/me/ai-preference'].put.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/users/me/ai-preference'].put.requestBody.content['application/json']").exists());
    }

    @Test
    void shouldRedirectAnonymousSwaggerUiRequest() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/swagger-ui/index.html")));
    }

    @Test
    void shouldKeepBusinessApiProtectedWhenDocumentationIsPublic() throws Exception {
        mockMvc.perform(get("/api/mistakes"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(401));

        verifyNoInteractions(studyService);
    }

    @Test
    void shouldDescribeInterviewTrainingApisAndLegacySseContract() throws Exception {
        JsonNode document = getOpenApiDocument();
        JsonNode paths = document.path("paths");

        assertBearerProtectedJsonOperation(paths, "/api/question/ask", "post");
        assertBearerProtectedJsonOperation(paths, "/api/question/score", "post");
        assertBearerProtectedOperation(paths, "/api/mistakes", "get");
        assertBearerProtectedOperation(paths, "/api/progress", "get");
        assertResponseSchemaReference(paths, "/api/question/ask", "post", "ApiResponseString");
        assertResponseSchemaReference(paths, "/api/question/score", "post", "ApiResponseAiScoreResult");
        assertResponseSchemaReference(paths, "/api/mistakes", "get", "ApiResponseMistakeList");
        assertResponseSchemaReference(paths, "/api/progress", "get", "ApiResponseStudyProgressList");

        assertLegacyOperation(paths, "/api/question/ask", "get");
        assertLegacyOperation(paths, "/api/question/score", "get");

        JsonNode streamOperation = paths.path("/api/question/score/stream").path("post");
        assertFalse(streamOperation.path("deprecated").asBoolean());
        assertBearerSecurity(streamOperation);
        assertTrue(streamOperation.path("responses").path("200").path("content")
                .has(MediaType.TEXT_EVENT_STREAM_VALUE));
        assertFalse(streamOperation.path("responses").has("401"));
        assertTrue(streamOperation.path("description").asText().contains("event: done"));
        assertTrue(streamOperation.path("description").asText().contains("event: error"));

        assertFalse(findParameter(streamOperation, "token") != null);

        JsonNode schemas = document.path("components").path("schemas");
        assertSchemaWithProperties(schemas, "QuestionRequest", "tag", "refresh");
        assertSchemaWithProperties(schemas, "ScoreRequest", "tag", "question", "answer");
        assertSchemaWithProperties(schemas, "AiScoreResult", "score", "correct_answer", "suggestion");
        assertSchemaWithProperties(
                schemas,
                "MistakeResponse",
                "id",
                "tag",
                "question",
                "userAnswer",
                "score",
                "correctAnswer",
                "suggestion",
                "createTime"
        );
        assertSchemaWithProperties(schemas, "StudyProgressResponse", "tag", "totalCount", "avgScore");
        assertSchemaWithProperties(schemas, "ApiResponse", "code", "message", "data");
        assertEquals(
                "string",
                schemas.path("ApiResponseString").path("allOf").get(1)
                        .path("properties").path("data").path("type").asText()
        );
        assertEquals(
                "#/components/schemas/AiScoreResult",
                schemas.path("ApiResponseAiScoreResult").path("allOf").get(1)
                        .path("properties").path("data").path("$ref").asText()
        );
    }

    private JsonNode getOpenApiDocument() throws Exception {
        String responseBody = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return new ObjectMapper().readTree(responseBody);
    }

    private void assertBearerProtectedJsonOperation(JsonNode paths, String path, String method) {
        JsonNode operation = paths.path(path).path(method);
        assertBearerSecurity(operation);
        assertTrue(operation.path("requestBody").path("content")
                .has(MediaType.APPLICATION_JSON_VALUE));
    }

    private void assertBearerProtectedOperation(JsonNode paths, String path, String method) {
        assertBearerSecurity(paths.path(path).path(method));
    }

    private void assertLegacyOperation(JsonNode paths, String path, String method) {
        JsonNode operation = paths.path(path).path(method);
        assertTrue(operation.path("deprecated").asBoolean());
        assertBearerSecurity(operation);
    }

    private void assertResponseSchemaReference(
            JsonNode paths,
            String path,
            String method,
            String schemaName) {
        String schemaReference = paths.path(path).path(method)
                .path("responses").path("200").path("content").path("*/*")
                .path("schema").path("$ref").asText();
        assertEquals("#/components/schemas/" + schemaName, schemaReference);
    }

    private void assertBearerSecurity(JsonNode operation) {
        assertTrue(operation.path("security").isArray());
        assertTrue(operation.path("security").size() > 0);
        assertTrue(operation.path("security").get(0).has("bearerAuth"));
    }

    private JsonNode findParameter(JsonNode operation, String parameterName) {
        for (JsonNode parameter : operation.path("parameters")) {
            if (parameterName.equals(parameter.path("name").asText())) {
                return parameter;
            }
        }
        return null;
    }

    private void assertSchemaWithProperties(
            JsonNode schemas,
            String schemaName,
            String... propertyNames) {
        JsonNode schema = schemas.path(schemaName);
        assertTrue(schema.isObject());
        for (String propertyName : propertyNames) {
            assertTrue(schema.path("properties").path(propertyName).isObject());
        }
    }
}
