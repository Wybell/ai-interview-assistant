package com.example.aiinterviewassistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.sse")
public class SseProperties {

    private long scoreTimeoutMillis = 45_000L;
}
