package com.aiops.aiops_apm.service;

import com.aiops.aiops_apm.entity.Incident;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HealthScoreService {

    public int calculateHealthScore(List<Incident> incidents) {

        if (incidents == null || incidents.isEmpty()) {
            return 100;
        }

        int totalPenalty = incidents.stream()
                .mapToInt(this::calculatePenalty)
                .sum();

        return Math.max(0, 100 - totalPenalty);
    }

    public int calculatePenalty(Incident incident) {

        if (incident == null ||
                incident.getSeverity() == null) {
            return 0;
        }

        return switch (incident.getSeverity().toUpperCase()) {
            case "CRITICAL" -> 30;
            case "HIGH" -> 20;
            case "MEDIUM" -> 10;
            case "LOW" -> 5;
            default -> 0;
        };
    }
}