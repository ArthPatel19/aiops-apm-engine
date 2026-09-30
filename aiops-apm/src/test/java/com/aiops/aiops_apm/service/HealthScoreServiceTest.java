package com.aiops.aiops_apm.service;

import com.aiops.aiops_apm.entity.Incident;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthScoreServiceTest {

    private final HealthScoreService healthScoreService =
            new HealthScoreService();

    @Test
    void noIncidentsShouldReturnPerfectHealth() {

        int score = healthScoreService.calculateHealthScore(
                List.of()
        );

        assertEquals(100, score);
    }

    @Test
    void criticalIncidentShouldReduceHealthByThirty() {

        Incident incident = createIncident("CRITICAL");

        int score = healthScoreService.calculateHealthScore(
                List.of(incident)
        );

        assertEquals(70, score);
    }

    @Test
    void multipleIncidentsShouldCombinePenalties() {

        Incident critical = createIncident("CRITICAL");
        Incident high = createIncident("HIGH");
        Incident low = createIncident("LOW");

        int score = healthScoreService.calculateHealthScore(
                List.of(critical, high, low)
        );

        assertEquals(45, score);
    }

    @Test
    void healthScoreShouldNeverGoBelowZero() {

        Incident critical1 = createIncident("CRITICAL");
        Incident critical2 = createIncident("CRITICAL");
        Incident critical3 = createIncident("CRITICAL");
        Incident critical4 = createIncident("CRITICAL");

        int score = healthScoreService.calculateHealthScore(
                List.of(
                        critical1,
                        critical2,
                        critical3,
                        critical4
                )
        );

        assertEquals(0, score);
    }

    @Test
    void unknownSeverityShouldHaveNoPenalty() {

        Incident incident = createIncident("UNKNOWN");

        int score = healthScoreService.calculateHealthScore(
                List.of(incident)
        );

        assertEquals(100, score);
    }

    private Incident createIncident(String severity) {

        Incident incident = new Incident();
        incident.setSeverity(severity);

        return incident;
    }
}