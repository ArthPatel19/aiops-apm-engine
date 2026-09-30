package com.aiops.aiops_apm.service;

import com.aiops.aiops_apm.entity.Incident;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeverityServiceTest {

    private final SeverityService severityService =
            new SeverityService();

    @Test
    void databaseFiveHundredWithRepeatedLogsIsCritical() {

        Incident incident = createIncident(
                "DB_CONNECTION",
                "5XX",
                2
        );

        assertEquals(
                "CRITICAL",
                severityService.calculateSeverity(incident)
        );
    }

    @Test
    void databaseFiveHundredWithOneLogIsHigh() {

        Incident incident = createIncident(
                "DB_CONNECTION",
                "5XX",
                1
        );

        assertEquals(
                "HIGH",
                severityService.calculateSeverity(incident)
        );
    }

    @Test
    void authenticationFourHundredWithOneLogIsLow() {

        Incident incident = createIncident(
                "AUTH_TOKEN",
                "4XX",
                1
        );

        assertEquals(
                "LOW",
                severityService.calculateSeverity(incident)
        );
    }

    @Test
    void authenticationFourHundredWithFiveLogsIsMedium() {

        Incident incident = createIncident(
                "AUTH_TOKEN",
                "4XX",
                5
        );

        assertEquals(
                "MEDIUM",
                severityService.calculateSeverity(incident)
        );
    }

    @Test
    void unknownRootCauseWithFiveHundredIsMedium() {

        Incident incident = createIncident(
                "UNKNOWN",
                "5XX",
                1
        );

        assertEquals(
                "MEDIUM",
                severityService.calculateSeverity(incident)
        );
    }

    private Incident createIncident(
            String rootCause,
            String statusClass,
            int logCount) {

        Incident incident = new Incident();

        incident.setRootCause(rootCause);
        incident.setStatusClass(statusClass);
        incident.setLogCount(logCount);

        return incident;
    }
}