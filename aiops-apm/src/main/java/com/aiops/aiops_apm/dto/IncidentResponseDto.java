package com.aiops.aiops_apm.dto;

import com.aiops.aiops_apm.entity.Incident;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public record IncidentResponseDto(
        Long id,
        String fingerprint,
        String rootCause,
        String statusClass,
        int logCount,
        String severity,
        int healthPenalty,
        Instant firstSeenAt,
        Instant lastSeenAt,

        String affectedFeature,
        String affectedApi,
        String sampleErrorType,
        String sampleMessage,

        String aiProvider,
        String aiRootCauseSummary,
        List<String> aiRemediationSteps,
        String aiJiraPayload,
        String aiSlackPayload,
        Instant aiDiagnosedAt,

        int estimatedRawTokens,
        int aiPromptTokens,
        int sampleRawTokens,
        int compactedContextTokens
) {

    public static IncidentResponseDto from(Incident incident) {

        String steps = incident.getAiRemediationSteps();

        List<String> stepList =
                (steps == null || steps.isBlank())
                        ? List.of()
                        : Arrays.asList(steps.split("\n"));

        return new IncidentResponseDto(
                incident.getId(),
                incident.getFingerprint(),
                incident.getRootCause(),
                incident.getStatusClass(),
                incident.getLogCount(),
                incident.getSeverity(),
                incident.getHealthPenalty(),
                incident.getFirstSeenAt(),
                incident.getLastSeenAt(),

                incident.getAffectedFeature(),
                incident.getAffectedApi(),
                incident.getSampleErrorType(),
                incident.getSampleMessage(),

                incident.getAiProvider(),
                incident.getAiRootCauseSummary(),
                stepList,
                incident.getAiJiraPayload(),
                incident.getAiSlackPayload(),
                incident.getAiDiagnosedAt(),

                incident.getEstimatedRawTokens(),
                incident.getAiPromptTokens(),
                incident.getSampleRawTokens(),
                incident.getCompactedContextTokens()
        );
    }
}

