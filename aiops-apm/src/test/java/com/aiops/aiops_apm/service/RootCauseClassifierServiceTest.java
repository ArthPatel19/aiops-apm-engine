package com.aiops.aiops_apm.service;

import com.aiops.aiops_apm.entity.ApmLog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RootCauseClassifierServiceTest {

    private final RootCauseClassifierService classifier =
            new RootCauseClassifierService();

    @Test
    void classifiesDatabaseConnectionErrors() {

        ApmLog log = createLog(
                "SQLTransientConnectionException",
                "Connection pool exhausted while waiting for Hikari connection"
        );

        assertEquals(
                RootCauseClassifierService.RootCause.DB_CONNECTION,
                classifier.classify(log)
        );
    }

    @Test
    void classifiesJdbcErrors() {

        ApmLog log = createLog(
                "CannotGetJdbcConnectionException",
                "Unable to obtain JDBC connection"
        );

        assertEquals(
                RootCauseClassifierService.RootCause.DB_CONNECTION,
                classifier.classify(log)
        );
    }

    @Test
    void classifiesJwtErrors() {

        ApmLog log = createLog(
                "AuthenticationException",
                "JWT token has expired"
        );

        assertEquals(
                RootCauseClassifierService.RootCause.AUTH_TOKEN,
                classifier.classify(log)
        );
    }

    @Test
    void classifiesUnknownErrors() {

        ApmLog log = createLog(
                "NullPointerException",
                "Unexpected null value"
        );

        assertEquals(
                RootCauseClassifierService.RootCause.UNKNOWN,
                classifier.classify(log)
        );
    }

    @Test
    void classificationIsCaseInsensitive() {

        ApmLog log = createLog(
                "Database Error",
                "HIKARI CONNECTION TIMEOUT"
        );

        assertEquals(
                RootCauseClassifierService.RootCause.DB_CONNECTION,
                classifier.classify(log)
        );
    }

    private ApmLog createLog(
            String errorType,
            String errorMessage) {

        ApmLog log = new ApmLog();

        log.setLogId("test-log");
        log.setErrorType(errorType);
        log.setErrorMessage(errorMessage);

        return log;
    }
}