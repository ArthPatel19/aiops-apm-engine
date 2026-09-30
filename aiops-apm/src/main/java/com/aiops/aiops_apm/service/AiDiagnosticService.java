package com.aiops.aiops_apm.service;

import com.aiops.aiops_apm.config.GeminiProperties;
import com.aiops.aiops_apm.dto.AiCallResult;
import com.aiops.aiops_apm.entity.Incident;
import com.aiops.aiops_apm.repository.IncidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

@Service
public class AiDiagnosticService {

    private static final Logger log =
            LoggerFactory.getLogger(AiDiagnosticService.class);

    private final GeminiAiClient geminiAiClient;
    private final RuleBasedAiClient fallbackClient;
    private final IncidentRepository incidentRepository;
    private final GeminiProperties properties;

    private final JsonMapper mapper =
            JsonMapper.builder().build();

    public AiDiagnosticService(
            GeminiAiClient geminiAiClient,
            RuleBasedAiClient fallbackClient,
            IncidentRepository incidentRepository,
            GeminiProperties properties) {

        this.geminiAiClient = geminiAiClient;
        this.fallbackClient = fallbackClient;
        this.incidentRepository = incidentRepository;
        this.properties = properties;
    }

    /**
     * Diagnoses the incident using Gemini when configured.
     * If Gemini is unavailable or fails, the rule-based client is used.
     */
    public Incident diagnose(Incident incident) {

        AiCallResult result;
        String provider;

        if (properties.hasApiKey()) {

            try {

                result = geminiAiClient.diagnose(incident);
                provider = "gemini";

            } catch (Exception e) {

                log.warn(
                        "Gemini diagnosis failed for incident {}, using fallback: {}",
                        incident.getFingerprint(),
                        e.getMessage()
                );

                result = fallbackClient.diagnose(
                        incident.getRootCause(),
                        incident.getStatusClass(),
                        incident.getSeverity(),
                        incident.getCompactContext()
                );

                provider = "rule-based-fallback";
            }

        } else {

            result = fallbackClient.diagnose(
                    incident.getRootCause(),
                    incident.getStatusClass(),
                    incident.getSeverity(),
                    incident.getCompactContext()
            );

            provider = "rule-based";
        }

        applyResult(
                incident,
                result,
                provider
        );

        return incidentRepository.save(incident);
    }

    private void applyResult(
            Incident incident,
            AiCallResult result,
            String provider) {

        List<String> steps =
                result.diagnosis().remediationSteps();

        incident.setAiRootCauseSummary(
                result.diagnosis().rootCauseAnalysis()
        );

        incident.setAiRemediationSteps(
                steps == null
                        ? ""
                        : String.join("\n", steps)
        );

        incident.setAiJiraPayload(
                mapper.writeValueAsString(
                        result.diagnosis().jiraTicket()
                )
        );

        incident.setAiSlackPayload(
                mapper.writeValueAsString(
                        result.diagnosis().slackAlert()
                )
        );

        incident.setAiProvider(provider);

        incident.setAiDiagnosedAt(
                Instant.now()
        );

        incident.setAiPromptTokens(
                result.promptTokens()
        );

        incident.setAiCompletionTokens(
                result.completionTokens()
        );
    }
}