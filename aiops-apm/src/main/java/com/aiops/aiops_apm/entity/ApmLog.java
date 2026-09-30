package com.aiops.aiops_apm.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "apm_logs", indexes = {
        @Index(name = "idx_apm_logs_occurred_at", columnList = "occurred_at"),
        @Index(name = "idx_apm_logs_is_error", columnList = "is_error")
})
public class ApmLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "log_id", nullable = false, unique = true, length = 100)
    private String logId;

    @Column(name = "trace_id", length = 100)
    private String traceId;

    @Column(name = "span_id", length = 100)
    private String spanId;

    @Column(name = "sdk_language", length = 50)
    private String sdkLanguage;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "is_error", nullable = false)
    private boolean error;

    @Column(name = "error_type", length = 255)
    private String errorType;

    @Column(name = "status_code", length = 20)
    private String statusCode;

    @Column(name = "error_path", length = 500)
    private String errorPath;

    @Column(name = "affected_feature", length = 255)
    private String affectedFeature;

    @Column(name = "affected_api", length = 500)
    private String affectedApi;

    @Column(name = "affected_function", length = 255)
    private String affectedFunction;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "input_information", columnDefinition = "TEXT")
    private String inputInformation;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "stack_trace", columnDefinition = "MEDIUMTEXT")
    private String stackTrace;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getSpanId() { return spanId; }
    public void setSpanId(String spanId) { this.spanId = spanId; }
    public String getSdkLanguage() { return sdkLanguage; }
    public void setSdkLanguage(String sdkLanguage) { this.sdkLanguage = sdkLanguage; }
    public Long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(Long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
    public boolean isError() { return error; }
    public void setError(boolean error) { this.error = error; }
    public String getErrorType() { return errorType; }
    public void setErrorType(String errorType) { this.errorType = errorType; }
    public String getStatusCode() { return statusCode; }
    public void setStatusCode(String statusCode) { this.statusCode = statusCode; }
    public String getErrorPath() { return errorPath; }
    public void setErrorPath(String errorPath) { this.errorPath = errorPath; }
    public String getAffectedFeature() { return affectedFeature; }
    public void setAffectedFeature(String affectedFeature) { this.affectedFeature = affectedFeature; }
    public String getAffectedApi() { return affectedApi; }
    public void setAffectedApi(String affectedApi) { this.affectedApi = affectedApi; }
    public String getAffectedFunction() { return affectedFunction; }
    public void setAffectedFunction(String affectedFunction) { this.affectedFunction = affectedFunction; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public String getInputInformation() { return inputInformation; }
    public void setInputInformation(String inputInformation) { this.inputInformation = inputInformation; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }
    public Instant getCreatedAt() { return createdAt; }
}