package com.aiops.aiops_apm.service;

import com.aiops.aiops_apm.entity.ApmLog;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class LogPruningServiceTest {

    private final LogPruningService pruningService =
            new LogPruningService();

    @Test
    void keepsMaximumThreeRelevantFrames() {

        ApmLog log = createLog(
                "java.lang.Exception: Database failure",
                """
                com.musterdekho.AttendanceService.submit(AttendanceService.java:10)
                com.musterdekho.AttendanceController.handle(AttendanceController.java:20)
                com.musterdekho.AttendanceRepository.save(AttendanceRepository.java:30)
                org.springframework.web.DispatcherServlet.doDispatch(DispatcherServlet.java:100)
                org.hibernate.Session.get(Session.java:200)
                """
        );

        LogPruningService.PrunedLog result =
                pruningService.prune(log);

        assertEquals(3, result.frames().size());

        assertTrue(
                result.frames().get(0).contains("com.musterdekho")
        );

        assertTrue(
                result.frames().get(1).contains("com.musterdekho")
        );

        assertTrue(
                result.frames().get(2).contains("com.musterdekho")
        );
    }

    @Test
    void prefersApplicationFramesOverFrameworkFrames() {

        ApmLog log = createLog(
                "Database connection timeout",
                """
                org.springframework.web.DispatcherServlet.doDispatch(DispatcherServlet.java:100)
                com.musterdekho.UserService.getUser(UserService.java:50)
                org.hibernate.Session.get(Session.java:200)
                com.musterdekho.UserController.profile(UserController.java:30)
                """
        );

        LogPruningService.PrunedLog result =
                pruningService.prune(log);

        assertTrue(
                result.frames().get(0).contains("com.musterdekho")
        );

        assertTrue(
                result.frames().get(1).contains("com.musterdekho")
        );
    }

    @Test
    void collapsesRepeatedFrames() {

        ApmLog log = createLog(
                "Connection failed",
                """
                com.musterdekho.DbService.connect(DbService.java:40)
                com.musterdekho.DbService.connect(DbService.java:40)
                com.musterdekho.DbService.connect(DbService.java:40)
                org.springframework.jdbc.DataSourceUtils.getConnection(DataSourceUtils.java:80)
                """
        );

        LogPruningService.PrunedLog result =
                pruningService.prune(log);

        assertTrue(
                result.frames().get(0).contains("[x3]")
        );
    }

    @Test
    void normalizesMessage() {

        ApmLog log = createLog(
                "Request failed at 2026-09-28T14:30:00Z for id " +
                        "12345678-1234-1234-1234-123456789012",
                ""
        );

        LogPruningService.PrunedLog result =
                pruningService.prune(log);

        System.out.println(
                "PRUNED MESSAGE = " + result.message()
        );

        assertTrue(
                result.message().contains("<TIME>")
        );

        assertTrue(
                result.message().contains("<ID>")
        );

        assertFalse(
                result.message().contains(
                        "2026-09-28T14:30:00Z"
                )
        );

        assertFalse(
                result.message().contains(
                        "12345678-1234-1234-1234-123456789012"
                )
        );
    }

    @Test
    void limitsMessageLength() {

        String longMessage = "A".repeat(1000);

        ApmLog log = createLog(
                longMessage,
                ""
        );

        LogPruningService.PrunedLog result =
                pruningService.prune(log);

        /*
         * Current production limit:
         * 300 characters + "..."
         */
        assertEquals(
                303,
                result.message().length()
        );

        assertTrue(
                result.message().endsWith("...")
        );
    }

    @Test
    void compactContextContainsOnlyRelevantInformation() {

        ApmLog log = createLog(
                "Database connection failed",
                """
                org.springframework.web.DispatcherServlet.doDispatch(DispatcherServlet.java:100)
                com.musterdekho.AttendanceService.submit(AttendanceService.java:84)
                org.hibernate.Session.get(Session.java:200)
                """
        );

        LogPruningService.PrunedLog result =
                pruningService.prune(log);

        String context =
                result.compactContext();

        assertTrue(
                context.contains("TestException")
        );

        assertTrue(
                context.contains("500")
        );

        assertTrue(
                context.contains("Test Feature")
        );

        assertTrue(
                context.contains("/api/test")
        );

        assertTrue(
                context.contains("Database connection failed")
        );

        assertTrue(
                context.contains("com.musterdekho.AttendanceService")
        );

        assertTrue(
                context.contains("DispatcherServlet")
        );

        assertTrue(
                context.contains("org.hibernate.Session")
        );
    }

    @Test
    void compactContextUsesSelectedFramesOnly() {

        ApmLog log = createLog(
                "Connection failed",
                """
                com.musterdekho.Service.one(Service.java:10)
                com.musterdekho.Service.two(Service.java:20)
                com.musterdekho.Service.three(Service.java:30)
                com.musterdekho.Service.four(Service.java:40)
                org.springframework.web.DispatcherServlet.doDispatch(DispatcherServlet.java:100)
                org.hibernate.Session.get(Session.java:200)
                """
        );

        LogPruningService.PrunedLog result =
                pruningService.prune(log);

        String context =
                result.compactContext();

        assertTrue(
                context.contains("Service.one")
        );

        assertTrue(
                context.contains("Service.two")
        );

        assertTrue(
                context.contains("Service.three")
        );

        assertFalse(
                context.contains("Service.four")
        );

        assertFalse(
                context.contains("DispatcherServlet")
        );

        assertFalse(
                context.contains("Session.get")
        );
    }

    @Test
    void repeatedFramesDoNotConsumeSeparateFrameSlots() {

        ApmLog log = createLog(
                "Database connection failed",
                """
                com.musterdekho.DbService.connect(DbService.java:40)
                com.musterdekho.DbService.connect(DbService.java:40)
                com.musterdekho.DbService.connect(DbService.java:40)
                com.musterdekho.AttendanceService.submit(AttendanceService.java:84)
                com.musterdekho.AttendanceController.handle(AttendanceController.java:30)
                """
        );

        LogPruningService.PrunedLog result =
                pruningService.prune(log);

        /*
         * The repeated frame is collapsed into one logical frame.
         */
        assertEquals(
                3,
                result.frames().size()
        );

        assertTrue(
                result.frames().get(0).contains("[x3]")
        );

        assertTrue(
                result.frames().get(1).contains("AttendanceService")
        );

        assertTrue(
                result.frames().get(2).contains("AttendanceController")
        );
    }

    @Test
    void tokenEstimateIsCalculatedForBothContexts() {

        ApmLog log = createLog(
                "Database connection timeout",
                """
                com.musterdekho.AttendanceService.submit(AttendanceService.java:84)
                org.springframework.jdbc.DataSourceUtils.getConnection(DataSourceUtils.java:82)
                org.hibernate.Session.get(Session.java:200)
                """
        );

        LogPruningService.PrunedLog result =
                pruningService.prune(log);

        assertTrue(
                result.rawContextTokens() > 0
        );

        assertTrue(
                result.compactedContextTokens() > 0
        );
    }

    @Test
    void cappedOutputNeverExceedsLimitsRegardlessOfInputSize() {

        StringBuilder hugeStackTrace =
                new StringBuilder();

        for (int i = 0; i < 50; i++) {

            hugeStackTrace
                    .append("com.musterdekho.api.service.Handler")
                    .append(i % 5)
                    .append(".process(Handler.java:")
                    .append(i)
                    .append(")\n");
        }

        String noisyMessage =
                "Failed at 2026-09-28T14:32:00Z for user 8821 " +
                        "with request-id a1b2c3d4-e5f6-7890-abcd-ef1234567890 " +
                        "after 3450 attempts and 9999 retries across 42 nodes " +
                        "with timestamp 2026-09-28T14:33:00Z and id 55512345 " +
                        "repeated many times to make this message far longer " +
                        "than the three hundred character cap so we can prove " +
                        "the pruner actually truncates it instead of just " +
                        "estimating a number that happens to be smaller.";

        ApmLog log = new ApmLog();

        log.setLogId("stress-test");
        log.setErrorType("SimulatedCascadeException");
        log.setStatusCode("500");
        log.setAffectedFeature("Test Feature");
        log.setAffectedApi("/api/test");
        log.setErrorMessage(noisyMessage);
        log.setStackTrace(
                hugeStackTrace.toString()
        );

        log.setOccurredAt(
                Instant.parse(
                        "2026-09-28T14:30:00Z"
                )
        );

        LogPruningService.PrunedLog result =
                pruningService.prune(log);

        /*
         * Hard pruning guarantee:
         * no more than 3 stack frames are forwarded.
         */
        assertTrue(
                result.frames().size() <= 3,
                "Frame count must never exceed the cap, got "
                        + result.frames().size()
        );

        /*
         * Current message limit:
         * 300 characters + "..."
         */
        assertTrue(
                result.message().length() <= 303,
                "Message must never exceed the length cap, got "
                        + result.message().length()
        );

        /*
         * Compact context should be substantially smaller
         * than the intentionally huge raw input.
         */
        assertTrue(
                result.compactContext().length()
                        < noisyMessage.length()
                        + hugeStackTrace.length(),
                "Compact context should be smaller than the large raw input"
        );
    }

    @Test
    void handlesEmptyMessageAndStackTrace() {

        ApmLog log = createLog(
                "",
                ""
        );

        LogPruningService.PrunedLog result =
                pruningService.prune(log);

        assertEquals(
                "",
                result.message()
        );

        assertTrue(
                result.frames().isEmpty()
        );

        assertTrue(
                result.compactContext().contains(
                        "TestException"
                )
        );
    }

    @Test
    void handlesNullMessageAndStackTrace() {

        ApmLog log = new ApmLog();

        log.setLogId("null-test");
        log.setErrorType("TestException");
        log.setStatusCode("500");
        log.setAffectedFeature("Test Feature");
        log.setAffectedApi("/api/test");
        log.setErrorMessage(null);
        log.setStackTrace(null);

        log.setOccurredAt(
                Instant.parse(
                        "2026-09-28T14:30:00Z"
                )
        );

        LogPruningService.PrunedLog result =
                pruningService.prune(log);

        assertEquals(
                "",
                result.message()
        );

        assertTrue(
                result.frames().isEmpty()
        );

        assertNotNull(
                result.compactContext()
        );
    }

    @Test
    void removesDuplicateErrorTypeFromMessage() {

        ApmLog log = createLog(
                "SQLTransientConnectionException: Could not acquire JDBC Connection",
                ""
        );

        log.setErrorType("SQLTransientConnectionException");

        LogPruningService.PrunedLog result =
                pruningService.prune(log);

        assertEquals(
                "Could not acquire JDBC Connection",
                result.message()
        );
    }

    @Test
    void removesDuplicateErrorTypeFromMiddleOfMessage() {

        ApmLog log = createLog(
                "Could not acquire JDBC Connection from HikariPool-<N>; "
                        + "SQLTransientConnectionException: "
                        + "HikariPool-<N> - Connection is not available.",
                ""
        );

        log.setErrorType("SQLTransientConnectionException");

        LogPruningService.PrunedLog result =
                pruningService.prune(log);

        assertEquals(
                "Could not acquire JDBC Connection from HikariPool-<N>; "
                        + "HikariPool-<N> - Connection is not available.",
                result.message()
        );
    }

    private ApmLog createLog(
            String message,
            String stackTrace) {

        ApmLog log = new ApmLog();

        log.setLogId("test-log");
        log.setErrorType("TestException");
        log.setStatusCode("500");
        log.setAffectedFeature("Test Feature");
        log.setAffectedApi("/api/test");
        log.setErrorMessage(message);
        log.setStackTrace(stackTrace);

        log.setOccurredAt(
                Instant.parse(
                        "2026-09-28T14:30:00Z"
                )
        );

        return log;
    }
}