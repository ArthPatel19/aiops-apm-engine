package com.aiops.aiops_apm.dto;

import com.aiops.aiops_apm.entity.ApmLog;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record LogResponseDto(
        Long id,
        String logId,
        String traceId,
        String spanId,
        String sdkLanguage,
        Long executionTimeMs,
        @JsonProperty("is_error") boolean error,
        String errorType,
        String statusCode,
        String errorPath,
        String affectedFeature,
        String affectedApi,
        String affectedFunction,
        Instant timestamp,
        String inputInformation,
        String errorMessage,
        String stackTrace
) {
    public static LogResponseDto from(ApmLog l) {
        return new LogResponseDto(
                l.getId(),
                l.getLogId(),
                l.getTraceId(),
                l.getSpanId(),
                l.getSdkLanguage(),
                l.getExecutionTimeMs(),
                l.isError(),
                l.getErrorType(),
                l.getStatusCode(),
                l.getErrorPath(),
                l.getAffectedFeature(),
                l.getAffectedApi(),
                l.getAffectedFunction(),
                l.getOccurredAt(),
                l.getInputInformation(),
                l.getErrorMessage(),
                l.getStackTrace()
        );
    }
}