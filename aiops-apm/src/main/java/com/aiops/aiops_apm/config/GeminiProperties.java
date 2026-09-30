package com.aiops.aiops_apm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.gemini")
public record GeminiProperties(
        String apiKey,
        String model,
        int maxOutputTokens,
        double temperature
) {
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}


