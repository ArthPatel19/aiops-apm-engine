package com.aiops.aiops_apm.dto;

public record HealthResponseDto(
        int score,
        int incidentCount,
        int totalPenalty
) {
}