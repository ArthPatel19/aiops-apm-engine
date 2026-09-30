package com.aiops.aiops_apm.service;

import com.aiops.aiops_apm.dto.AiCallResult;
import com.aiops.aiops_apm.dto.AiDiagnosisDto;
import com.aiops.aiops_apm.dto.JiraTicketDto;
import com.aiops.aiops_apm.dto.SlackAlertDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RuleBasedAiClient {

    public AiCallResult diagnose(
            String rootCause,
            String statusClass,
            String severity,
            String compactContext
    ) {

        String rootCauseAnalysis = buildRootCauseAnalysis(
                rootCause,
                statusClass,
                severity
        );

        List<String> remediationSteps = buildRemediationSteps(rootCause);

        JiraTicketDto jiraTicket = new JiraTicketDto(
                buildJiraSummary(rootCause, severity),
                buildJiraDescription(
                        rootCause,
                        statusClass,
                        severity,
                        compactContext
                ),
                mapPriority(severity)
        );

        SlackAlertDto slackAlert = new SlackAlertDto(
                "Incident detected: " + rootCause,
                buildSlackMessage(
                        rootCause,
                        statusClass,
                        severity
                )
        );

        AiDiagnosisDto diagnosis = new AiDiagnosisDto(
                rootCauseAnalysis,
                remediationSteps,
                jiraTicket,
                slackAlert
        );

        return new AiCallResult(
                diagnosis,
                0,
                0
        );
    }

    private String buildRootCauseAnalysis(
            String rootCause,
            String statusClass,
            String severity
    ) {

        return "Rule-based diagnosis: "
                + rootCause
                + " incident with "
                + statusClass
                + " status and "
                + severity
                + " severity.";
    }

    private List<String> buildRemediationSteps(String rootCause) {

        return switch (rootCause) {

            case "DB_CONNECTION" -> List.of(
                    "Check database availability and connectivity.",
                    "Inspect connection pool usage and exhaustion.",
                    "Review database connection errors and recent deployments."
            );

            case "AUTH_TOKEN" -> List.of(
                    "Verify token validity and expiration.",
                    "Check authentication configuration.",
                    "Review recent authentication or authorization changes."
            );

            default -> List.of(
                    "Inspect the incident context and application logs.",
                    "Check recent deployments and configuration changes.",
                    "Investigate the affected service or API."
            );
        };
    }

    private String buildJiraSummary(
            String rootCause,
            String severity
    ) {

        return severity + " " + rootCause + " incident";
    }

    private String buildJiraDescription(
            String rootCause,
            String statusClass,
            String severity,
            String compactContext
    ) {

        return "Root cause: " + rootCause
                + "\nStatus class: " + statusClass
                + "\nSeverity: " + severity
                + "\nContext: " + compactContext;
    }

    private String mapPriority(String severity) {

        return switch (severity) {
            case "CRITICAL" -> "P1";
            case "HIGH" -> "P2";
            case "MEDIUM" -> "P3";
            default -> "P4";
        };
    }

    private String buildSlackMessage(
            String rootCause,
            String statusClass,
            String severity
    ) {

        return "Root cause: " + rootCause
                + " | Status: " + statusClass
                + " | Severity: " + severity;
    }
}