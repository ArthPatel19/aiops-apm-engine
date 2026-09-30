package com.aiops.aiops_apm.service;

import com.aiops.aiops_apm.entity.ApmLog;
import com.aiops.aiops_apm.entity.Incident;
import com.aiops.aiops_apm.repository.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final ClusteringService clusteringService;
    private final RootCauseClassifierService classifier;
    private final LogPruningService pruningService;

    public IncidentService(
            IncidentRepository incidentRepository,
            ClusteringService clusteringService,
            RootCauseClassifierService classifier,
            LogPruningService pruningService) {

        this.incidentRepository = incidentRepository;
        this.clusteringService = clusteringService;
        this.classifier = classifier;
        this.pruningService = pruningService;
    }

    /**
     * Tells the caller whether a NEW incident was created,
     * so the AI is called at most once per incident.
     */
    public record IncidentOutcome(
            Incident incident,
            boolean newlyCreated
    ) {}

    @Transactional
    public IncidentOutcome processLog(ApmLog log) {

        String fingerprint =
                clusteringService.createFingerprint(log);

        LogPruningService.PrunedLog pruned =
                pruningService.prune(log);

        /*
         * Estimate the complete raw diagnostic volume.
         *
         * This is cumulative incident-level volume and is kept
         * separately from the representative-sample comparison.
         */
        int rawTokens =
                approxRawDiagnosticTokens(log);

        return incidentRepository.findByFingerprint(fingerprint)
                .map(existing ->
                        new IncidentOutcome(
                                updateIncident(
                                        existing,
                                        log,
                                        rawTokens
                                ),
                                false
                        )
                )
                .orElseGet(() ->
                        new IncidentOutcome(
                                createIncident(
                                        log,
                                        fingerprint,
                                        pruned,
                                        rawTokens
                                ),
                                true
                        )
                );
    }

    private Incident createIncident(
            ApmLog log,
            String fingerprint,
            LogPruningService.PrunedLog pruned,
            int rawTokens) {

        RootCauseClassifierService.RootCause rootCause =
                classifier.classify(log);

        Incident incident = new Incident();

        incident.setFingerprint(fingerprint);

        incident.setRootCause(
                rootCause.name()
        );

        incident.setStatusClass(
                extractStatusClass(fingerprint)
        );

        incident.setLogCount(1);

        incident.setSeverity("LOW");

        incident.setHealthPenalty(0);

        incident.setFirstSeenAt(
                log.getOccurredAt()
        );

        incident.setLastSeenAt(
                log.getOccurredAt()
        );

        /*
         * Cumulative estimated raw diagnostic volume
         * across all logs belonging to this incident.
         */
        incident.setEstimatedRawTokens(
                rawTokens
        );

        /*
         * First occurrence becomes the representative sample.
         * It is not overwritten when additional logs join
         * the same incident.
         */
        incident.setSampleLogId(
                pruned.logId()
        );

        incident.setSampleErrorType(
                pruned.errorType()
        );

        incident.setAffectedFeature(
                pruned.affectedFeature()
        );

        incident.setAffectedApi(
                pruned.affectedApi()
        );

        incident.setSampleMessage(
                pruned.message()
        );

        incident.setSampleStackFrames(
                String.join(
                        " -> ",
                        pruned.frames()
                )
        );

        incident.setCompactContext(
                pruned.compactContext()
        );

        /*
         * Token-efficiency measurement.
         *
         * These values come directly from LogPruningService,
         * which calculates both values using the same
         * token-estimation method.
         *
         * sampleRawTokens:
         * estimated tokens for the representative log
         * before pruning.
         *
         * compactedContextTokens:
         * estimated tokens for that same representative log
         * after pruning and normalization.
         *
         * Because both measurements come from the same
         * PrunedLog, they are directly comparable.
         */
        incident.setSampleRawTokens(
                pruned.rawContextTokens()
        );

        incident.setCompactedContextTokens(
                pruned.compactedContextTokens()
        );

        return incidentRepository.save(
                incident
        );
    }

    private Incident updateIncident(
            Incident incident,
            ApmLog log,
            int rawTokens) {

        /*
         * Additional logs increase the incident's cumulative
         * raw volume.
         */
        incident.setLogCount(
                incident.getLogCount() + 1
        );

        incident.setEstimatedRawTokens(
                incident.getEstimatedRawTokens()
                        + rawTokens
        );

        Instant occurredAt =
                log.getOccurredAt();

        if (occurredAt != null) {

            if (incident.getFirstSeenAt() == null
                    || occurredAt.isBefore(
                    incident.getFirstSeenAt()
            )) {

                incident.setFirstSeenAt(
                        occurredAt
                );
            }

            if (incident.getLastSeenAt() == null
                    || occurredAt.isAfter(
                    incident.getLastSeenAt()
            )) {

                incident.setLastSeenAt(
                        occurredAt
                );
            }
        }

        /*
         * The representative sample and its token measurements
         * intentionally remain unchanged.
         */
        return incidentRepository.save(
                incident
        );
    }

    /**
     * Estimates the token size of the complete diagnostic
     * payload BEFORE pruning.
     *
     * This is used for cumulative incident-level volume.
     *
     * It contains:
     * - error type
     * - status
     * - affected feature
     * - affected API
     * - original message
     * - complete stack trace
     */
    private int approxRawDiagnosticTokens(
            ApmLog log) {

        StringBuilder raw =
                new StringBuilder();

        raw.append("type=")
                .append(
                        safe(log.getErrorType())
                );

        raw.append(" | status=")
                .append(
                        safe(log.getStatusCode())
                );

        raw.append(" | feature=")
                .append(
                        safe(log.getAffectedFeature())
                );

        raw.append(" | api=")
                .append(
                        safe(log.getAffectedApi())
                );

        raw.append(" | message=")
                .append(
                        safe(log.getErrorMessage())
                );

        if (log.getStackTrace() != null
                && !log.getStackTrace().isBlank()) {

            raw.append(" | frames=")
                    .append(
                            log.getStackTrace()
                    );
        }

        return approxTokens(
                raw.toString()
        );
    }

    /**
     * Rough token estimate.
     *
     * Approximately 4 characters are treated as 1 token.
     *
     * This is only an estimate for comparing payload sizes.
     * It is NOT Gemini's actual tokenizer.
     */
    private int approxTokens(
            String text) {

        return (
                safe(text).length() + 3
        ) / 4;
    }

    private String safe(
            String value) {

        return value == null
                ? ""
                : value;
    }

    private String extractStatusClass(
            String fingerprint) {

        int separator =
                fingerprint.indexOf(':');

        if (separator >= 0
                && separator < fingerprint.length() - 1) {

            return fingerprint.substring(
                    separator + 1
            );
        }

        return "UNKNOWN";
    }

    public Incident save(
            Incident incident) {

        return incidentRepository.save(
                incident
        );
    }
}