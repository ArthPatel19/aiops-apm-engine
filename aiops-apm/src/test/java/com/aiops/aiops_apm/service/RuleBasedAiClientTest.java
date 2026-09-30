package com.aiops.aiops_apm.service;

import com.aiops.aiops_apm.dto.AiCallResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuleBasedAiClientTest {

    private final RuleBasedAiClient client =
            new RuleBasedAiClient();

    @Test
    void createsDatabaseDiagnosis() {

        AiCallResult result = client.diagnose(
                "DB_CONNECTION",
                "5XX",
                "CRITICAL",
                "type=SQLTransientConnectionException | status=500"
        );

        assertNotNull(result);
        assertNotNull(result.diagnosis());

        assertTrue(
                result.diagnosis()
                        .rootCauseAnalysis()
                        .contains("DB_CONNECTION")
        );

        assertFalse(
                result.diagnosis()
                        .remediationSteps()
                        .isEmpty()
        );

        assertEquals(
                "P1",
                result.diagnosis()
                        .jiraTicket()
                        .priority()
        );

        assertNotNull(
                result.diagnosis()
                        .slackAlert()
        );

        assertEquals(0, result.promptTokens());
        assertEquals(0, result.completionTokens());
    }

    @Test
    void createsAuthenticationDiagnosis() {

        AiCallResult result = client.diagnose(
                "AUTH_TOKEN",
                "4XX",
                "LOW",
                "type=AuthenticationException | status=401"
        );

        assertNotNull(result.diagnosis());

        assertTrue(
                result.diagnosis()
                        .rootCauseAnalysis()
                        .contains("AUTH_TOKEN")
        );

        assertFalse(
                result.diagnosis()
                        .remediationSteps()
                        .isEmpty()
        );

        assertEquals(
                "P4",
                result.diagnosis()
                        .jiraTicket()
                        .priority()
        );
    }

    @Test
    void handlesUnknownRootCause() {

        AiCallResult result = client.diagnose(
                "UNKNOWN",
                "5XX",
                "HIGH",
                "type=UnknownException"
        );

        assertNotNull(result.diagnosis());

        assertFalse(
                result.diagnosis()
                        .remediationSteps()
                        .isEmpty()
        );
    }
}