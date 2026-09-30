package com.aiops.aiops_apm.service;

import com.aiops.aiops_apm.entity.ApmLog;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
public class ClusteringService {

    private static final long CLUSTER_WINDOW_MINUTES = 15;

    private final RootCauseClassifierService classifier;

    public ClusteringService(RootCauseClassifierService classifier) {
        this.classifier = classifier;
    }

    public boolean belongsToSameIncident(
            ApmLog first,
            ApmLog second) {

        if (first == null || second == null) {
            return false;
        }

        if (!sameTimeWindow(first.getOccurredAt(), second.getOccurredAt())) {
            return false;
        }

        String firstFingerprint = createFingerprint(first);
        String secondFingerprint = createFingerprint(second);

        return firstFingerprint.equals(secondFingerprint);
    }

    public String createFingerprint(ApmLog log) {

        RootCauseClassifierService.RootCause rootCause =
                classifier.classify(log);

        String statusClass = statusClass(log.getStatusCode());

        return rootCause.name() + ":" + statusClass;
    }

    private boolean sameTimeWindow(
            Instant first,
            Instant second) {

        if (first == null || second == null) {
            return false;
        }

        long minutes = Math.abs(
                Duration.between(first, second).toMinutes()
        );

        return minutes <= CLUSTER_WINDOW_MINUTES;
    }

    private String statusClass(String statusCode) {

        if (statusCode == null || statusCode.isBlank()) {
            return "UNKNOWN";
        }

        String status = statusCode.trim();

        if (status.length() >= 1) {
            char first = status.charAt(0);

            if (first >= '1' && first <= '5') {
                return first + "XX";
            }
        }

        return status.toUpperCase(Locale.ROOT);
    }
}