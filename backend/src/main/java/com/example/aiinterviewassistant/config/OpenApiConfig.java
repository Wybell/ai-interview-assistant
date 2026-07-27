package com.example.aiinterviewassistant.config;

import com.example.aiinterviewassistant.common.ApiResponse;
import com.example.aiinterviewassistant.dto.AiScoreResult;
import com.example.aiinterviewassistant.dto.MistakeResponse;
import com.example.aiinterviewassistant.dto.QuestionRequest;
import com.example.aiinterviewassistant.dto.ScoreRequest;
import com.example.aiinterviewassistant.dto.StudyProgressResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";
    private static final String COMPONENT_SCHEMA_PREFIX = "#/components/schemas/";

    @Bean
    public OpenAPI aiInterviewAssistantOpenApi() {
        Components components = new Components()
                .addSecuritySchemes(
                        SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                );
        registerModelSchemas(components);
        registerTypedApiResponseSchemas(components);

        return new OpenAPI()
                .info(new Info()
                        .title("AI Interview Assistant API")
                        .version("1.0.0")
                        .description("AI 面试训练平台后端接口。受保护接口使用 JWT Bearer Token 认证。"))
                .components(components);
    }

    private void registerModelSchemas(Components components) {
        registerModelSchema(components, ApiResponse.class);
        registerModelSchema(components, QuestionRequest.class);
        registerModelSchema(components, ScoreRequest.class);
        registerModelSchema(components, AiScoreResult.class);
        registerModelSchema(components, MistakeResponse.class);
        registerModelSchema(components, StudyProgressResponse.class);
    }

    private void registerModelSchema(Components components, Class<?> modelType) {
        Map<String, Schema> schemas = ModelConverters.getInstance().readAll(modelType);
        schemas.forEach(components::addSchemas);
    }

    private void registerTypedApiResponseSchemas(Components components) {
        addTypedApiResponseSchema(components, "ApiResponseString", new StringSchema());
        addTypedApiResponseSchema(
                components,
                "ApiResponseAiScoreResult",
                schemaReference("AiScoreResult")
        );
        addTypedApiResponseSchema(
                components,
                "ApiResponseMistakeList",
                new ArraySchema().items(schemaReference("MistakeResponse"))
        );
        addTypedApiResponseSchema(
                components,
                "ApiResponseStudyProgressList",
                new ArraySchema().items(schemaReference("StudyProgressResponse"))
        );
    }

    private void addTypedApiResponseSchema(
            Components components,
            String schemaName,
            Schema<?> dataSchema) {
        ObjectSchema typedDataSchema = new ObjectSchema();
        typedDataSchema.addProperty("data", dataSchema);

        ComposedSchema responseSchema = new ComposedSchema();
        responseSchema.addAllOfItem(schemaReference("ApiResponse"));
        responseSchema.addAllOfItem(typedDataSchema);
        components.addSchemas(schemaName, responseSchema);
    }

    private Schema<?> schemaReference(String schemaName) {
        return new Schema<>().$ref(COMPONENT_SCHEMA_PREFIX + schemaName);
    }
}
