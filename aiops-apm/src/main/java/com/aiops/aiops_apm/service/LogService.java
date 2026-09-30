package com.aiops.aiops_apm.service;

import com.aiops.aiops_apm.dto.IngestResponseDto;
import com.aiops.aiops_apm.dto.LogRequestDto;
import com.aiops.aiops_apm.dto.LogResponseDto;
import com.aiops.aiops_apm.entity.ApmLog;
import com.aiops.aiops_apm.entity.Incident;
import com.aiops.aiops_apm.repository.LogRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LogService {

    static final int MAX_BATCH_SIZE = 1000;

    private static final int MAX_STACK_TRACE = 50_000;
    private static final int MAX_TEXT = 10_000;

    private final LogRepository logRepository;
    private final SanitizerService sanitizer;
    private final Validator validator;
    private final IncidentService incidentService;
    private final SeverityService severityService;
    private final HealthScoreService healthScoreService;
    private final AiDiagnosticService aiDiagnosticService;

    public LogService(
            LogRepository logRepository,
            SanitizerService sanitizer,
            Validator validator,
            IncidentService incidentService,
            SeverityService severityService,
            HealthScoreService healthScoreService,
            AiDiagnosticService aiDiagnosticService) {

        this.logRepository = logRepository;
        this.sanitizer = sanitizer;
        this.validator = validator;
        this.incidentService = incidentService;
        this.severityService = severityService;
        this.healthScoreService = healthScoreService;
        this.aiDiagnosticService = aiDiagnosticService;
    }

    @Transactional
    public IngestResponseDto ingest(List<LogRequestDto> requests) {

        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException(
                    "Log array must not be empty"
            );
        }

        if (requests.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException(
                    "Batch too large: max "
                            + MAX_BATCH_SIZE
                            + " logs per request"
            );
        }

        List<String> errors = new ArrayList<>();
        List<LogRequestDto> valid = new ArrayList<>();

        // 1) Validate each log independently
        for (int i = 0; i < requests.size(); i++) {

            LogRequestDto dto = requests.get(i);

            if (dto == null) {
                errors.add(
                        "index " + i + ": log is null"
                );
                continue;
            }

            Set<ConstraintViolation<LogRequestDto>> violations =
                    validator.validate(dto);

            if (violations.isEmpty()) {

                valid.add(dto);

            } else {

                String msg = violations.stream()
                        .map(v ->
                                v.getPropertyPath()
                                        + " "
                                        + v.getMessage()
                        )
                        .sorted()
                        .collect(Collectors.joining("; "));

                errors.add(
                        "index " + i + ": " + msg
                );
            }
        }

        int rejected =
                requests.size() - valid.size();

        // 2) Drop duplicates:
        //    - already stored
        //    - repeated inside this batch
        int duplicates = 0;

        List<ApmLog> toSave =
                new ArrayList<>();

        if (!valid.isEmpty()) {

            Set<String> ids = valid.stream()
                    .map(d -> d.logId().trim())
                    .collect(Collectors.toSet());

            Set<String> existing =
                    new HashSet<>(
                            logRepository.findExistingLogIds(ids)
                    );

            Set<String> seen =
                    new HashSet<>();

            for (LogRequestDto dto : valid) {

                String id =
                        dto.logId().trim();

                if (existing.contains(id)
                        || !seen.add(id)) {

                    duplicates++;

                } else {

                    toSave.add(
                            toEntity(dto)
                    );
                }
            }

            List<ApmLog> savedLogs =
                    logRepository.saveAll(toSave);

            // 3) Process each newly saved log
            for (ApmLog log : savedLogs) {

                IncidentService.IncidentOutcome outcome =
                        incidentService.processLog(log);

                Incident incident =
                        outcome.incident();

                // 4) Calculate severity
                String severity =
                        severityService.calculateSeverity(
                                incident
                        );

                incident.setSeverity(
                        severity
                );

                // 5) Calculate health penalty
                incident.setHealthPenalty(
                        healthScoreService.calculatePenalty(
                                incident
                        )
                );

                // 6) AI diagnosis ONLY for a brand-new incident
                if (outcome.newlyCreated()) {

                    aiDiagnosticService.diagnose(
                            incident
                    );

                } else {

                    // Existing incident:
                    // update severity/health only.
                    // DO NOT call Gemini again.
                    incidentService.save(
                            incident
                    );
                }
            }
        }

        return new IngestResponseDto(
                requests.size(),
                toSave.size(),
                duplicates,
                rejected,
                errors
        );
    }

    @Transactional(readOnly = true)
    public List<LogResponseDto> getLatest(
            int page,
            int size) {

        int safePage =
                Math.max(page, 0);

        int safeSize =
                Math.min(
                        Math.max(size, 1),
                        200
                );

        return logRepository
                .findAllByOrderByOccurredAtDesc(
                        PageRequest.of(
                                safePage,
                                safeSize
                        )
                )
                .stream()
                .map(LogResponseDto::from)
                .toList();
    }

    private ApmLog toEntity(
            LogRequestDto d) {

        ApmLog e =
                new ApmLog();

        e.setLogId(
                d.logId().trim()
        );

        e.setTraceId(
                clean(
                        d.traceId(),
                        100
                )
        );

        e.setSpanId(
                clean(
                        d.spanId(),
                        100
                )
        );

        e.setSdkLanguage(
                clean(
                        d.sdkLanguage(),
                        50
                )
        );

        e.setExecutionTimeMs(
                d.executionTimeMs()
        );

        e.setError(
                Boolean.TRUE.equals(
                        d.error()
                )
        );

        e.setErrorType(
                clean(
                        d.errorType(),
                        255
                )
        );

        e.setStatusCode(
                clean(
                        d.statusCode(),
                        20
                )
        );

        e.setErrorPath(
                clean(
                        d.errorPath(),
                        500
                )
        );

        e.setAffectedFeature(
                clean(
                        d.affectedFeature(),
                        255
                )
        );

        e.setAffectedApi(
                clean(
                        d.affectedApi(),
                        500
                )
        );

        e.setAffectedFunction(
                clean(
                        d.affectedFunction(),
                        255
                )
        );

        e.setOccurredAt(
                d.timestamp()
        );

        e.setInputInformation(
                clean(
                        d.inputInformation(),
                        MAX_TEXT
                )
        );

        e.setErrorMessage(
                clean(
                        d.errorMessage(),
                        MAX_TEXT
                )
        );

        e.setStackTrace(
                clean(
                        d.stackTrace(),
                        MAX_STACK_TRACE
                )
        );

        return e;
    }

    /**
     * Sanitize FIRST, then truncate,
     * so a secret can never be cut in half
     * and leak a fragment.
     */
    private String clean(
            String value,
            int maxLength) {

        String sanitized =
                sanitizer.sanitize(value);

        if (sanitized == null
                || sanitized.length() <= maxLength) {

            return sanitized;
        }

        return sanitized.substring(
                0,
                maxLength
        );
    }
}