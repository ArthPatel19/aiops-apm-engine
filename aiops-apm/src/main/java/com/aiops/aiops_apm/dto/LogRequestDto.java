package com.aiops.aiops_apm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record LogRequestDto(
        @NotBlank @Size(max = 100) String logId,
        String traceId,
        String spanId,
        String sdkLanguage,
        Long executionTimeMs,
        @JsonProperty("is_error") Boolean error,
        String errorType,
        String statusCode,
        String errorPath,
        String affectedFeature,
        String affectedApi,
        String affectedFunction,
        @NotNull Instant timestamp,
        String inputInformation,
        String errorMessage,
        String stackTrace
) {}