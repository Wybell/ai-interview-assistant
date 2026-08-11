package com.example.aiinterviewassistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.resume")
public class ResumeStorageProperties {

    private String storageDirectory = "./uploads/resumes";
}
