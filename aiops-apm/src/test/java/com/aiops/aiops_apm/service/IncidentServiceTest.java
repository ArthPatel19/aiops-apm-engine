package com.aiops.aiops_apm.service;

import com.aiops.aiops_apm.entity.ApmLog;
import com.aiops.aiops_apm.entity.Incident;
import com.aiops.aiops_apm.repository.IncidentRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IncidentServiceTest {

    private final IncidentRepository incidentRepository =
            mock(IncidentRepository.class);

    private final ClusteringService clusteringService =
            new ClusteringService(
                    new RootCauseClassifierService()
            );

    private final RootCauseClassifierService classifier =
            new RootCauseClassifierService();

    private final LogPruningService pruningService =
            new LogPruningService();

    private final IncidentService incidentService =
            new IncidentService(
                    incidentRepository,
                    clusteringService,
                    classifier,
                    pruningService
            );

    @Test
    void createsNewIncidentForFirstLog() {

        ApmLog log = createLog(
                "SQLTransientConnectionException",
                "Connection pool exhausted. " +
                        "HikariPool-1 - Connection is not available, " +
                        "request timed out after 3000ms. " +
                        "The application was unable to obtain a JDBC connection " +
                        "while processing the attendance submission request. " +
                        "This occurred after multiple database operations and " +
                        "the connection pool reached its configured maximum size. " +
                        "Additional diagnostic information indicates that several " +
                        "requests were waiting for database connections.",
                "500",
                "2026-09-28T14:00:00Z"
        );

        // Add realistic stack-trace information so the raw sample
        // represents a real application log and pruning has content to reduce.
        log.setStackTrace(
                "com.musterdekho.api.attendance.business.AttendanceService." +
                        "submitAttendance(AttendanceService.java:84) -> " +
                        "com.zaxxer.hikari.pool.HikariPool.getConnection(HikariPool.java:213) -> " +
                        "org.hibernate.engine.jdbc.connections.internal." +
                        "DatasourceConnectionProviderImpl.getConnection(" +
                        "DatasourceConnectionProviderImpl.java:122)"
        );

        when(incidentRepository.findByFingerprint(
                "DB_CONNECTION:5XX"
        )).thenReturn(Optional.empty());

        when(incidentRepository.save(any(Incident.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IncidentService.IncidentOutcome outcome =
                incidentService.processLog(log);

        Incident result = outcome.incident();

        assertTrue(outcome.newlyCreated());

        assertEquals(
                "DB_CONNECTION:5XX",
                result.getFingerprint()
        );

        assertEquals(
                "DB_CONNECTION",
                result.getRootCause()
        );

        assertEquals(
                "5XX",
                result.getStatusClass()
        );

        assertEquals(
                1,
                result.getLogCount()
        );

        assertEquals(
                "2026-09-28T14:00:00Z",
                result.getFirstSeenAt().toString()
        );

        assertEquals(
                "2026-09-28T14:00:00Z",
                result.getLastSeenAt().toString()
        );

        assertEquals(
                "SQLTransientConnectionException",
                result.getSampleErrorType()
        );

        assertTrue(
                result.getSampleMessage().contains(
                        "Connection pool exhausted"
                )
        );

        assertNotNull(
                result.getCompactContext()
        );

        // Token measurements must exist and be positive.
        assertTrue(
                result.getSampleRawTokens() > 0,
                "Sample raw token estimate should be greater than zero"
        );

        assertTrue(
                result.getCompactedContextTokens() > 0,
                "Compacted context token estimate should be greater than zero"
        );

        verify(incidentRepository).save(any(Incident.class));
    }

    @Test
    void updatesExistingIncidentForSameFingerprint() {

        ApmLog log = createLog(
                "CannotGetJdbcConnectionException",
                "Unable to obtain JDBC connection",
                "500",
                "2026-09-28T14:05:00Z"
        );

        Incident existing = new Incident();

        existing.setFingerprint("DB_CONNECTION:5XX");
        existing.setRootCause("DB_CONNECTION");
        existing.setStatusClass("5XX");
        existing.setLogCount(1);
        existing.setSeverity("LOW");
        existing.setHealthPenalty(0);

        existing.setFirstSeenAt(
                Instant.parse("2026-09-28T14:00:00Z")
        );

        existing.setLastSeenAt(
                Instant.parse("2026-09-28T14:00:00Z")
        );

        when(incidentRepository.findByFingerprint(
                "DB_CONNECTION:5XX"
        )).thenReturn(Optional.of(existing));

        when(incidentRepository.save(any(Incident.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IncidentService.IncidentOutcome outcome =
                incidentService.processLog(log);

        Incident result = outcome.incident();

        assertFalse(outcome.newlyCreated());

        assertEquals(
                2,
                result.getLogCount()
        );

        assertEquals(
                "2026-09-28T14:00:00Z",
                result.getFirstSeenAt().toString()
        );

        assertEquals(
                "2026-09-28T14:05:00Z",
                result.getLastSeenAt().toString()
        );

        // Existing representative sample should remain unchanged.
        assertNull(
                result.getSampleErrorType()
        );

        verify(incidentRepository).save(existing);
    }

    @Test
    void createsSeparateIncidentForDifferentRootCause() {

        ApmLog log = createLog(
                "AuthenticationException",
                "JWT token expired",
                "401",
                "2026-09-28T14:05:00Z"
        );

        when(incidentRepository.findByFingerprint(
                "AUTH_TOKEN:4XX"
        )).thenReturn(Optional.empty());

        when(incidentRepository.save(any(Incident.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IncidentService.IncidentOutcome outcome =
                incidentService.processLog(log);

        Incident result = outcome.incident();

        assertTrue(outcome.newlyCreated());

        assertEquals(
                "AUTH_TOKEN:4XX",
                result.getFingerprint()
        );

        assertEquals(
                "AUTH_TOKEN",
                result.getRootCause()
        );

        assertEquals(
                "4XX",
                result.getStatusClass()
        );

        assertEquals(
                1,
                result.getLogCount()
        );

        assertEquals(
                "AuthenticationException",
                result.getSampleErrorType()
        );

        assertEquals(
                "JWT token expired",
                result.getSampleMessage()
        );

        assertNotNull(
                result.getCompactContext()
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
        log.setOccurredAt(
                Instant.parse(timestamp)
        );

        return log;
    }
}