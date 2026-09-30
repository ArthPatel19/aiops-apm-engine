package com.aiops.aiops_apm.service;

import com.aiops.aiops_apm.config.GeminiProperties;
import com.aiops.aiops_apm.dto.IngestResponseDto;
import com.aiops.aiops_apm.dto.LogRequestDto;
import com.aiops.aiops_apm.entity.ApmLog;
import com.aiops.aiops_apm.repository.IncidentRepository;
import com.aiops.aiops_apm.repository.LogRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LogServiceTest {

    private final LogRepository repo =
            mock(LogRepository.class);

    private final Validator validator =
            Validation.buildDefaultValidatorFactory()
                    .getValidator();

    private final LogPruningService pruningService =
            new LogPruningService();

    private final IncidentRepository incidentRepository =
            mock(IncidentRepository.class);

    private final RootCauseClassifierService classifier =
            new RootCauseClassifierService();

    private final ClusteringService clusteringService =
            new ClusteringService(classifier);

    private final IncidentService incidentService =
            new IncidentService(
                    incidentRepository,
                    clusteringService,
                    classifier,
                    pruningService
            );

    private final SeverityService severityService =
            new SeverityService();

    private final HealthScoreService healthScoreService =
            new HealthScoreService();

    /*
     * Blank API key means:
     *
     * Gemini will NOT be called.
     * Rule-based diagnosis will be used.
     *
     * This keeps LogServiceTest completely offline.
     */
    private final GeminiProperties geminiProperties =
            new GeminiProperties(
                    "",
                    "gemini-3.5-flash-lite",
                    400,
                    0.0
            );

    private final GeminiAiClient geminiAiClient =
            new GeminiAiClient(geminiProperties);

    private final RuleBasedAiClient fallbackClient =
            new RuleBasedAiClient();

    private final AiDiagnosticService aiDiagnosticService =
            new AiDiagnosticService(
                    geminiAiClient,
                    fallbackClient,
                    incidentRepository,
                    geminiProperties
            );

    private final LogService service =
            new LogService(
                    repo,
                    new SanitizerService(),
                    validator,
                    incidentService,
                    severityService,
                    healthScoreService,
                    aiDiagnosticService
            );

    @Test
    void sanitizesAllTextFieldsBeforeSaving() {

        LogRequestDto dto = new LogRequestDto(
                "log-1",
                "trace-1",
                "span-1",
                "java",
                100L,
                true,
                "SQLException",
                "500",
                "/api/test",
                "Login",
                "/api/login",
                "login",
                Instant.parse("2026-09-28T14:30:00Z"),
                "{\"password\":\"mySecretPassword123\", \"email\":\"user8821@gmail.com\"}",
                "Authentication failed for user8821@gmail.com",
                "Authorization: Bearer abc123XYZ"
        );

        when(repo.findExistingLogIds(any()))
                .thenReturn(List.of());

        service.ingest(List.of(dto));

        verify(repo).saveAll(any());

        var captor =
                org.mockito.ArgumentCaptor.forClass(List.class);

        verify(repo).saveAll(captor.capture());

        @SuppressWarnings("unchecked")
        List<ApmLog> saved =
                captor.getValue();

        assertThat(saved)
                .hasSize(1);

        ApmLog log =
                saved.get(0);

        assertThat(log.getInputInformation())
                .doesNotContain("mySecretPassword123")
                .doesNotContain("user8821@gmail.com")
                .contains(SanitizerService.REDACTED);

        assertThat(log.getErrorMessage())
                .doesNotContain("user8821@gmail.com")
                .contains(SanitizerService.REDACTED);

        assertThat(log.getStackTrace())
                .doesNotContain("abc123XYZ")
                .contains(SanitizerService.REDACTED);
    }

    @Test
    void detectsDuplicatesAlreadyStoredAndInsideBatch() {

        LogRequestDto first =
                validLog("log-1");

        LogRequestDto second =
                validLog("log-2");

        LogRequestDto repeated =
                validLog("log-2");

        when(repo.findExistingLogIds(any()))
                .thenReturn(List.of("log-1"));

        IngestResponseDto result =
                service.ingest(
                        List.of(
                                first,
                                second,
                                repeated
                        )
                );

        assertThat(result.received())
                .isEqualTo(3);

        assertThat(result.accepted())
                .isEqualTo(1);

        assertThat(result.duplicates())
                .isEqualTo(2);

        assertThat(result.rejected())
                .isEqualTo(0);

        assertThat(result.errors())
                .isEmpty();

        verify(repo)
                .saveAll(any());
    }

    @Test
    void rejectsInvalidLogWithoutSaving() {

        LogRequestDto invalid =
                new LogRequestDto(
                        "",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        IngestResponseDto result =
                service.ingest(
                        List.of(invalid)
                );

        assertThat(result.received())
                .isEqualTo(1);

        assertThat(result.accepted())
                .isEqualTo(0);

        assertThat(result.duplicates())
                .isEqualTo(0);

        assertThat(result.rejected())
                .isEqualTo(1);

        assertThat(result.errors())
                .hasSize(1);

        verify(
                repo,
                never()
        ).findExistingLogIds(any());

        verify(
                repo,
                never()
        ).saveAll(any());
    }

    @Test
    void rejectsEmptyBatch() {

        assertThatThrownBy(
                () -> service.ingest(List.of())
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "Log array must not be empty"
                );

        verifyNoInteractions(repo);
    }

    private LogRequestDto validLog(
            String logId) {

        return new LogRequestDto(
                logId,
                "trace-" + logId,
                "span-" + logId,
                "java",
                100L,
                false,
                null,
                "200",
                "/api/test",
                "Test",
                "/api/test",
                "test",
                Instant.parse(
                        "2026-09-28T14:30:00Z"
                ),
                "normal input",
                null,
                null
        );
    }
}