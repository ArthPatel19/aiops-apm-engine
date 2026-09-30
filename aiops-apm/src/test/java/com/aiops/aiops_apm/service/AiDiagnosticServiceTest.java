package com.aiops.aiops_apm.service;

import com.aiops.aiops_apm.config.GeminiProperties;
import com.aiops.aiops_apm.entity.Incident;
import com.aiops.aiops_apm.repository.IncidentRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiDiagnosticServiceTest {

    private final GeminiAiClient geminiClient = mock(GeminiAiClient.class);
    private final RuleBasedAiClient fallbackClient = new RuleBasedAiClient();
    private final IncidentRepository incidentRepository = mock(IncidentRepository.class);

    private Incident incident() {
        Incident i = new Incident();

        i.setFingerprint("DB_CONNECTION:5XX");
        i.setRootCause("DB_CONNECTION");
        i.setStatusClass("5XX");
        i.setSeverity("CRITICAL");

        i.setAffectedFeature("Attendance Management");
        i.setAffectedApi("/api/v1/attendance/submit");
        i.setSampleMessage("Connection pool exhausted");
        i.setCompactContext("Database connection pool exhausted");

        i.setLogCount(2);

        return i;
    }

    @Test
    void usesRuleBasedWhenNoApiKeyConfigured() throws Exception {
        GeminiProperties props = new GeminiProperties("", "gemini-3.5-flash-lite", 400, 0.0);
        AiDiagnosticService service = new AiDiagnosticService(geminiClient, fallbackClient, incidentRepository, props);
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));

        Incident result = service.diagnose(incident());

        assertThat(result.getAiProvider()).isEqualTo("rule-based");
        assertThat(result.getAiRootCauseSummary()).isNotBlank();
        verify(geminiClient, never()).diagnose(any());
    }

    @Test
    void fallsBackWhenGeminiThrows() throws Exception {
        GeminiProperties props = new GeminiProperties("fake-key", "gemini-3.5-flash-lite", 400, 0.0);
        AiDiagnosticService service = new AiDiagnosticService(geminiClient, fallbackClient, incidentRepository, props);
        when(geminiClient.diagnose(any())).thenThrow(new RuntimeException("timeout"));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));

        Incident result = service.diagnose(incident());

        assertThat(result.getAiProvider()).isEqualTo("rule-based-fallback");
        assertThat(result.getAiRootCauseSummary()).isNotBlank();
    }
}