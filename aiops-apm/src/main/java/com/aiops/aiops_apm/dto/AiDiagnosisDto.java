package com.aiops.aiops_apm.dto;

import java.util.List;

public record AiDiagnosisDto(
        String rootCauseAnalysis,
        List<String> remediationSteps,
        JiraTicketDto jiraTicket,
        SlackAlertDto slackAlert
) {
}