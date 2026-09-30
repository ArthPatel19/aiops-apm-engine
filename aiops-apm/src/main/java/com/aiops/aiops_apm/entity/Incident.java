package com.aiops.aiops_apm.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "incidents",
        indexes = {
                @Index(name = "idx_incidents_created_at", columnList = "created_at"),
                @Index(name = "idx_incidents_fingerprint", columnList = "fingerprint")
        }
)
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String fingerprint;

    @Column(name = "root_cause", nullable = false, length = 100)
    private String rootCause;

    @Column(name = "status_class", nullable = false, length = 20)
    private String statusClass;

    @Column(nullable = false)
    private int logCount;

    @Column(nullable = false, length = 20)
    private String severity;

    @Column(name = "health_penalty", nullable = false)
    private int healthPenalty;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    // Representative sample from the incident
    @Column(name = "sample_log_id", length = 100)
    private String sampleLogId;

    @Column(name = "sample_error_type", length = 255)
    private String sampleErrorType;

    @Column(name = "affected_feature", length = 255)
    private String affectedFeature;

    @Column(name = "affected_api", length = 500)
    private String affectedApi;

    @Column(name = "sample_message", columnDefinition = "TEXT")
    private String sampleMessage;

    @Column(name = "sample_stack_frames", columnDefinition = "TEXT")
    private String sampleStackFrames;

    @Column(name = "compact_context", columnDefinition = "TEXT")
    private String compactContext;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "estimated_raw_tokens", nullable = false)
    private int estimatedRawTokens;

    @Column(name = "ai_root_cause_summary", columnDefinition = "TEXT")
    private String aiRootCauseSummary;

    @Column(name = "ai_remediation_steps", columnDefinition = "TEXT")
    private String aiRemediationSteps;

    @Column(name = "ai_jira_payload", columnDefinition = "TEXT")
    private String aiJiraPayload;

    @Column(name = "ai_slack_payload", columnDefinition = "TEXT")
    private String aiSlackPayload;

    @Column(name = "ai_provider", length = 30)
    private String aiProvider;

    @Column(name = "ai_diagnosed_at")
    private java.time.Instant aiDiagnosedAt;

    @Column(name = "ai_prompt_tokens", nullable = false)
    private int aiPromptTokens;

    @Column(name = "ai_completion_tokens", nullable = false)
    private int aiCompletionTokens;

    @Column(name = "sample_raw_tokens", nullable = false)
    private int sampleRawTokens;

    @Column(name = "compacted_context_tokens", nullable = false)
    private int compactedContextTokens;

    public int getSampleRawTokens() {
        return sampleRawTokens;
    }

    public void setSampleRawTokens(int sampleRawTokens) {
        this.sampleRawTokens = sampleRawTokens;
    }

    public int getCompactedContextTokens() {
        return compactedContextTokens;
    }

    public void setCompactedContextTokens(int compactedContextTokens) {
        this.compactedContextTokens = compactedContextTokens;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getRootCause() {
        return rootCause;
    }

    public void setRootCause(String rootCause) {
        this.rootCause = rootCause;
    }

    public String getStatusClass() {
        return statusClass;
    }

    public void setStatusClass(String statusClass) {
        this.statusClass = statusClass;
    }

    public int getLogCount() {
        return logCount;
    }

    public void setLogCount(int logCount) {
        this.logCount = logCount;
    }

    public String getSeverity() {
        return severity;
    }

    public int getEstimatedRawTokens() {
        return estimatedRawTokens;
    }

    public void setEstimatedRawTokens(int estimatedRawTokens) {
        this.estimatedRawTokens = estimatedRawTokens;
    }

    public String getAiRootCauseSummary() {
        return aiRootCauseSummary;
    }

    public void setAiRootCauseSummary(String aiRootCauseSummary) {
        this.aiRootCauseSummary = aiRootCauseSummary;
    }

    public String getAiRemediationSteps() {
        return aiRemediationSteps;
    }

    public void setAiRemediationSteps(String aiRemediationSteps) {
        this.aiRemediationSteps = aiRemediationSteps;
    }

    public String getAiJiraPayload() {
        return aiJiraPayload;
    }

    public void setAiJiraPayload(String aiJiraPayload) {
        this.aiJiraPayload = aiJiraPayload;
    }

    public String getAiSlackPayload() {
        return aiSlackPayload;
    }

    public void setAiSlackPayload(String aiSlackPayload) {
        this.aiSlackPayload = aiSlackPayload;
    }

    public String getAiProvider() {
        return aiProvider;
    }

    public void setAiProvider(String aiProvider) {
        this.aiProvider = aiProvider;
    }

    public Instant getAiDiagnosedAt() {
        return aiDiagnosedAt;
    }

    public void setAiDiagnosedAt(Instant aiDiagnosedAt) {
        this.aiDiagnosedAt = aiDiagnosedAt;
    }

    public int getAiPromptTokens() {
        return aiPromptTokens;
    }

    public void setAiPromptTokens(int aiPromptTokens) {
        this.aiPromptTokens = aiPromptTokens;
    }

    public int getAiCompletionTokens() {
        return aiCompletionTokens;
    }

    public void setAiCompletionTokens(int aiCompletionTokens) {
        this.aiCompletionTokens = aiCompletionTokens;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public int getHealthPenalty() {
        return healthPenalty;
    }

    public void setHealthPenalty(int healthPenalty) {
        this.healthPenalty = healthPenalty;
    }

    public Instant getFirstSeenAt() {
        return firstSeenAt;
    }

    public void setFirstSeenAt(Instant firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public String getSampleLogId() {
        return sampleLogId;
    }

    public void setSampleLogId(String sampleLogId) {
        this.sampleLogId = sampleLogId;
    }

    public String getSampleErrorType() {
        return sampleErrorType;
    }

    public void setSampleErrorType(String sampleErrorType) {
        this.sampleErrorType = sampleErrorType;
    }

    public String getAffectedFeature() {
        return affectedFeature;
    }

    public void setAffectedFeature(String affectedFeature) {
        this.affectedFeature = affectedFeature;
    }

    public String getAffectedApi() {
        return affectedApi;
    }

    public void setAffectedApi(String affectedApi) {
        this.affectedApi = affectedApi;
    }

    public String getSampleMessage() {
        return sampleMessage;
    }

    public void setSampleMessage(String sampleMessage) {
        this.sampleMessage = sampleMessage;
    }

    public String getSampleStackFrames() {
        return sampleStackFrames;
    }

    public void setSampleStackFrames(String sampleStackFrames) {
        this.sampleStackFrames = sampleStackFrames;
    }

    public String getCompactContext() {
        return compactContext;
    }

    public void setCompactContext(String compactContext) {
        this.compactContext = compactContext;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}