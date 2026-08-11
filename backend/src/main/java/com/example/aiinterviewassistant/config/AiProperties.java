package com.example.aiinterviewassistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private String dashscopeApiKey;

    private String dashscopeEndpoint;

    private String change2proApiKey;

    private String change2proEndpoint;

    private String change2proReasoningEffort;

    private boolean change2proDisableResponseStorage = true;

    private String deepseekApiKey;

    private String deepseekEndpoint;

    private String customApiKey;

    private String customEndpoint;

    private String customReasoningEffort;

    private boolean customDisableResponseStorage = true;
}
