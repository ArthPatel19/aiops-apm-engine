package com.aiops.aiops_apm.dto;

public record AiCallResult(
        AiDiagnosisDto diagnosis,
        int promptTokens,
        int completionTokens
) {
}