package com.aiops.aiops_apm.service;

import com.aiops.aiops_apm.config.GeminiProperties;
import com.aiops.aiops_apm.dto.AiCallResult;
import com.aiops.aiops_apm.dto.AiDiagnosisDto;
import com.aiops.aiops_apm.entity.Incident;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiAiClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    private final GeminiProperties properties;
    private final HttpClient httpClient;
    private final JsonMapper mapper = JsonMapper.builder().build();

    public GeminiAiClient(GeminiProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
    }

    /** Throws on any failure — network, non-2xx, or unparseable response — so the caller can fall back. */
    public AiCallResult diagnose(Incident incident) throws Exception {

        String prompt = buildPrompt(incident);
        Map<String, Object> requestBody = buildRequestBody(prompt);
        String requestJson = mapper.writeValueAsString(requestBody);

        String url = String.format(ENDPOINT, properties.model());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", properties.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Gemini call failed: HTTP " + response.statusCode() + " - " + response.body());
        }

        JsonNode root = mapper.readTree(response.body());

        String diagnosisJson = root
                .path("candidates").path(0)
                .path("content").path("parts").path(0)
                .path("text").asString();

        if (diagnosisJson == null || diagnosisJson.isBlank()) {
            throw new IllegalStateException("Gemini returned no diagnostic text");
        }

        AiDiagnosisDto diagnosis = mapper.readValue(diagnosisJson, AiDiagnosisDto.class);

        int promptTokens = root.path("usageMetadata").path("promptTokenCount").asInt(0);
        int completionTokens = root.path("usageMetadata").path("candidatesTokenCount").asInt(0);

        return new AiCallResult(diagnosis, promptTokens, completionTokens);
    }


    private String buildPrompt(Incident incident) {

        String compactContext = incident.getCompactContext();

        if (compactContext == null || compactContext.isBlank()) {
            compactContext = buildFallbackContext(incident);
        }

        return """
        Diagnose this incident in 2-3 sentences. Respond ONLY with the JSON schema.

        Root cause category: %s
        Occurrences: %d
        Incident context: %s
        """.formatted(
                safe(incident.getRootCause()),
                incident.getLogCount(),
                compactContext
        );
    }



    private String buildFallbackContext(Incident incident) {

        return """
            %s (%s) on %s [%s]. %s | at: %s
            """.formatted(
                safe(incident.getSampleErrorType()),
                safe(incident.getStatusClass()),
                safe(incident.getAffectedFeature()),
                safe(incident.getAffectedApi()),
                safe(incident.getSampleMessage()),
                safe(incident.getSampleStackFrames())
        ).trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }


    private Map<String, Object> buildRequestBody(String prompt) {

        Map<String, Object> schema = buildResponseSchema();

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("responseSchema", schema);
        generationConfig.put("maxOutputTokens", properties.maxOutputTokens());
        generationConfig.put("temperature", properties.temperature());

        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(part));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", List.of(content));
        body.put("generationConfig", generationConfig);
        return body;
    }

    private Map<String, Object> buildResponseSchema() {

        Map<String, Object> jiraTicket = Map.of(
                "type", "object",
                "properties", Map.of(
                        "summary", Map.of("type", "string"),
                        "description", Map.of("type", "string"),
                        "priority", Map.of("type", "string")
                ),
                "required", List.of("summary", "description", "priority")
        );

        Map<String, Object> slackAlert = Map.of(
                "type", "object",
                "properties", Map.of(
                        "title", Map.of("type", "string"),
                        "message", Map.of("type", "string")
                ),
                "required", List.of("title", "message")
        );

        Map<String, Object> remediationSteps = Map.of(
                "type", "array",
                "items", Map.of("type", "string")
        );

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("rootCauseAnalysis", Map.of("type", "string"));
        properties.put("remediationSteps", remediationSteps);
        properties.put("jiraTicket", jiraTicket);
        properties.put("slackAlert", slackAlert);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of("rootCauseAnalysis", "remediationSteps", "jiraTicket", "slackAlert"));
        return schema;
    }
}