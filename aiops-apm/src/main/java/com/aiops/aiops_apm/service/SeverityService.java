package com.aiops.aiops_apm.service;

import com.aiops.aiops_apm.entity.Incident;
import org.springframework.stereotype.Service;

@Service
public class SeverityService {

    public String calculateSeverity(Incident incident) {

        int score = 0;

        // 5XX errors are more severe than 4XX errors.
        if ("5XX".equalsIgnoreCase(incident.getStatusClass())) {
            score += 3;
        } else if ("4XX".equalsIgnoreCase(incident.getStatusClass())) {
            score += 1;
        }

        // Database connection failures can affect application availability.
        if ("DB_CONNECTION".equalsIgnoreCase(
                incident.getRootCause())) {
            score += 2;
        }

        // Repeated occurrences increase severity.
        if (incident.getLogCount() >= 10) {
            score += 3;
        } else if (incident.getLogCount() >= 5) {
            score += 2;
        } else if (incident.getLogCount() >= 2) {
            score += 1;
        }

        if (score >= 6) {
            return "CRITICAL";
        }

        if (score >= 4) {
            return "HIGH";
        }

        if (score >= 2) {
            return "MEDIUM";
        }

        return "LOW";
    }
}