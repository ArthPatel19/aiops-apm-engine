package com.aiops.aiops_apm.service;

import com.aiops.aiops_apm.entity.ApmLog;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ClusteringServiceTest {

    private final ClusteringService clusteringService =
            new ClusteringService(
                    new RootCauseClassifierService()
            );

    @Test
    void databaseLogsWithinTimeWindowBelongToSameIncident() {

        ApmLog first = createLog(
                "SQLTransientConnectionException",
                "Connection pool exhausted",
                "500",
                "2026-09-28T14:00:00Z"
        );

        ApmLog second = createLog(
                "CannotGetJdbcConnectionException",
                "Unable to obtain JDBC connection",
                "500",
                "2026-09-28T14:05:00Z"
        );

        assertTrue(
                clusteringService.belongsToSameIncident(first, second)
        );
    }

    @Test
    void differentRootCausesDoNotBelongToSameIncident() {

        ApmLog databaseLog = createLog(
                "SQLTransientConnectionException",
                "Connection pool exhausted",
                "500",
                "2026-09-28T14:00:00Z"
        );

        ApmLog authLog = createLog(
                "AuthenticationException",
                "JWT token expired",
                "401",
                "2026-09-28T14:05:00Z"
        );

        assertFalse(
                clusteringService.belongsToSameIncident(
                        databaseLog,
                        authLog
                )
        );
    }

    @Test
    void logsOutsideTimeWindowDoNotBelongToSameIncident() {

        ApmLog first = createLog(
                "SQLTransientConnectionException",
                "Connection pool exhausted",
                "500",
                "2026-09-28T14:00:00Z"
        );

        ApmLog second = createLog(
                "CannotGetJdbcConnectionException",
                "Unable to obtain JDBC connection",
                "500",
                "2026-09-28T14:20:00Z"
        );

        assertFalse(
                clusteringService.belongsToSameIncident(first, second)
        );
    }

    @Test
    void createsExpectedFingerprint() {

        ApmLog log = createLog(
                "SQLTransientConnectionException",
                "Hikari connection timeout",
                "500",
                "2026-09-28T14:00:00Z"
        );

        assertEquals(
                "DB_CONNECTION:5XX",
                clusteringService.createFingerprint(log)
        );
    }

    @Test
    void jwtErrorCreatesAuthFingerprint() {

        ApmLog log = createLog(
                "AuthenticationException",
                "JWT token expired",
                "401",
                "2026-09-28T14:00:00Z"
        );

        assertEquals(
                "AUTH_TOKEN:4XX",
                clusteringService.createFingerprint(log)
        );
    }

    private ApmLog createLog(
            String errorType,
            String errorMessage,
            String statusCode,
            String timestamp) {

        ApmLog log = new ApmLog();

        log.setLogId(errorType);
        log.setErrorType(errorType);
        log.setErrorMessage(errorMessage);
        log.setStatusCode(statusCode);
        log.setOccurredAt(Instant.parse(timestamp));

        return log;
    }
}