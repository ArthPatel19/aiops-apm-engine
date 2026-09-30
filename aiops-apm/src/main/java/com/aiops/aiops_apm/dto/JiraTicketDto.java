package com.aiops.aiops_apm.dto;

public record JiraTicketDto(
        String summary,
        String description,
        String priority
) {
}