
package com.aiops.aiops_apm.service;

import com.aiops.aiops_apm.entity.ApmLog;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class LogPruningService {

    /*
     * Hard limits for the AI context.
     *
     * These are deliberately small because the purpose of this service
     * is to remove irrelevant/repetitive log data before it reaches the LLM.
     */
    private static final int MAX_MESSAGE_LENGTH = 300;
    private static final int MAX_FRAMES = 3;

    /*
     * Application package prefix.
     *
     * Keep this broad enough to include:
     *   com.musterdekho.AttendanceService
     *   com.musterdekho.api.attendance.business.AttendanceService
     *   com.musterdekho.api.security.JwtAuthenticationFilter
     *
     * Framework classes such as Spring/Hibernate/Hikari do not match this.
     */
    private static final String APPLICATION_PACKAGE = "com.musterdekho";

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b"
    );

    private static final Pattern NUMBER_PATTERN = Pattern.compile(
            "\\b\\d+\\b"
    );

    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile(
            "\\b\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?Z\\b"
    );

    /**
     * Creates the compact representation that will be stored with the
     * incident and eventually sent to the LLM.
     *
     * The original log is NOT modified.
     */
    public PrunedLog prune(ApmLog log) {

        String originalMessage = safe(log.getErrorMessage());
        String originalStackTrace = safe(log.getStackTrace());

        /*
         * Build the same raw diagnostic context that would otherwise
         * have been passed to the AI.
         *
         * This is used only for measuring the token-saving effect.
         */
        String rawContext = buildRawContext(
                log,
                originalMessage,
                originalStackTrace
        );

        /*
         * 1. Normalize and cap the error message.
         */
        String message = normalizeMessage(originalMessage,
                log.getErrorType());

        /*
         * 2. Select only useful stack frames.
         *
         * Application frames are preferred over framework frames.
         * Repeated frames are collapsed before selecting the maximum
         * number of frames.
         */
        List<String> frames =
                selectRelevantFrames(originalStackTrace);

        /*
         * 3. Build the final compact representation.
         */
        String compactContext =
                buildCompactContext(
                        log,
                        message,
                        frames
                );

        int rawContextTokens =
                approxTokens(rawContext);

        int compactedContextTokens =
                approxTokens(compactContext);


        return new PrunedLog(
                log.getLogId(),
                log.getErrorType(),
                log.getStatusCode(),
                log.getAffectedFeature(),
                log.getAffectedApi(),
                message,
                frames,
                compactContext,
                rawContextTokens,
                compactedContextTokens
        );
    }

    /**
     * Raw representation used for comparison.
     *
     * IMPORTANT:
     * This does not alter the actual log.
     */
    private String buildRawContext(
            ApmLog log,
            String message,
            String stackTrace) {

        StringBuilder context =
                new StringBuilder();

        context.append("type=")
                .append(safe(log.getErrorType()));

        context.append(" | status=")
                .append(safe(log.getStatusCode()));

        context.append(" | feature=")
                .append(safe(log.getAffectedFeature()));

        context.append(" | api=")
                .append(safe(log.getAffectedApi()));

        context.append(" | message=")
                .append(message);

        context.append(" | stackTrace=")
                .append(stackTrace);

        return context.toString();
    }

    /**
     * Removes dynamic values that provide little diagnostic value
     * but consume tokens.
     *
     * Examples:
     *
     * 2026-09-28T14:30:00Z -> <TIME>
     *
     * UUID -> <ID>
     *
     * 3000 -> <N>
     *
     * The final message is capped at 300 characters.
     */
    private String normalizeMessage(
            String message,
            String errorType) {

        if (message == null || message.isBlank()) {
            return "";
        }

        String normalized = TIMESTAMP_PATTERN.matcher(message)
                .replaceAll("<TIME>");

        normalized = UUID_PATTERN.matcher(normalized)
                .replaceAll("<ID>");

        normalized = NUMBER_PATTERN.matcher(normalized)
                .replaceAll("<N>");

        /*
         * The error type is already sent separately in the compact context.
         * Remove a leading "ErrorType:" repetition from the message.
         */
        if (errorType != null && !errorType.isBlank()) {

            String escapedErrorType =
                    Pattern.quote(errorType.trim());

            // Remove the error type when it appears at the beginning.
            normalized = normalized.replaceFirst(
                    "^" + escapedErrorType + "\\s*:\\s*",
                    ""
            );

            // Remove a repeated error type appearing later in the message.
            normalized = normalized.replaceFirst(
                    "(;\\s*)" + escapedErrorType + "\\s*:\\s*",
                    "$1"
            );
        }

        normalized = normalized
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.length() > MAX_MESSAGE_LENGTH) {
            normalized =
                    normalized.substring(0, MAX_MESSAGE_LENGTH) + "...";
        }

        return normalized;
    }

    /**
     * Selects the most useful stack frames.
     *
     * Strategy:
     *
     * 1. Remove empty lines.
     * 2. Collapse identical repeated frames.
     * 3. Prefer application frames.
     * 4. If fewer than 3 application frames exist,
     *    fill the remaining slots with framework frames.
     * 5. Never return more than 3 frames.
     */
    private List<String> selectRelevantFrames(
            String stackTrace) {

        if (stackTrace == null ||
                stackTrace.isBlank()) {

            return List.of();
        }

        String[] lines =
                stackTrace.split("\\R");

        /*
         * LinkedHashMap preserves original stack-trace order
         * while allowing us to count duplicate frames.
         */
        Map<String, Integer> frameCounts =
                new LinkedHashMap<>();

        for (String line : lines) {

            String frame =
                    line.trim();

            if (frame.isEmpty()) {
                continue;
            }

            frameCounts.merge(
                    frame,
                    1,
                    Integer::sum
            );
        }

        /*
         * First collect application frames.
         */
        List<String> applicationFrames =
                new ArrayList<>();

        for (Map.Entry<String, Integer> entry
                : frameCounts.entrySet()) {

            String frame =
                    entry.getKey();

            if (isApplicationFrame(frame)) {

                applicationFrames.add(
                        formatFrame(
                                frame,
                                entry.getValue()
                        )
                );
            }
        }

        /*
         * Application frames always get priority.
         */
        List<String> selected =
                new ArrayList<>();

        for (String frame :
                applicationFrames) {

            if (selected.size() >= MAX_FRAMES) {
                break;
            }

            selected.add(frame);
        }

        /*
         * If we don't have 3 application frames,
         * use framework frames to fill the remaining
         * diagnostic slots.
         */
        if (selected.size() < MAX_FRAMES) {

            for (Map.Entry<String, Integer> entry
                    : frameCounts.entrySet()) {

                String frame =
                        entry.getKey();

                if (isApplicationFrame(frame)) {
                    continue;
                }

                if (selected.size() >= MAX_FRAMES) {
                    break;
                }

                selected.add(
                        formatFrame(
                                frame,
                                entry.getValue()
                        )
                );
            }
        }

        return selected;
    }

    /**
     * Determines whether a stack frame belongs to our application.
     *
     * We intentionally use startsWith() rather than an exact package
     * match so nested packages such as com.musterdekho.api.* are included.
     */
    private boolean isApplicationFrame(String frame) {

        return frame.startsWith(
                APPLICATION_PACKAGE
        );
    }

    /**
     * Represents repeated identical frames compactly.
     *
     * Example:
     *
     * frame
     * frame
     * frame
     *
     * becomes:
     *
     * frame [x3]
     */
    private String formatFrame(
            String frame,
            int count) {

        if (count > 1) {

            return frame
                    + " [x"
                    + count
                    + "]";
        }

        return frame;
    }

    /**
     * Builds the final compact context.
     *
     * Only information useful for diagnosis is retained:
     *
     * - exception type
     * - HTTP status
     * - affected feature
     * - API
     * - normalized/capped message
     * - maximum 3 relevant stack frames
     */
    private String buildCompactContext(
            ApmLog log,
            String message,
            List<String> frames) {

        StringBuilder context =
                new StringBuilder();

        context.append(
                        safe(log.getErrorType())
                )
                .append(" (")
                .append(
                        safe(log.getStatusCode())
                )
                .append(") on ")
                .append(
                        safe(log.getAffectedFeature())
                )
                .append(" [")
                .append(
                        safe(log.getAffectedApi())
                )
                .append("]. ")
                .append(message);

        if (!frames.isEmpty()) {

            context.append(" | at: ")
                    .append(
                            String.join(
                                    " -> ",
                                    frames
                            )
                    );
        }

        return context.toString();
    }

    /**
     * Simple local token estimate.
     *
     * Approximately:
     *
     * 4 characters ≈ 1 token
     *
     * This is NOT the Gemini tokenizer.
     *
     * The important thing is that the SAME calculation is used
     * for rawContextTokens and compactedContextTokens, allowing
     * us to measure relative reduction consistently.
     */
    private int approxTokens(String text) {

        if (text == null ||
                text.isBlank()) {

            return 0;
        }

        return (text.length() + 3) / 4;
    }

    private String safe(String value) {

        return value == null
                ? ""
                : value;
    }

    public record PrunedLog(
            String logId,
            String errorType,
            String statusCode,
            String affectedFeature,
            String affectedApi,
            String message,
            List<String> frames,
            String compactContext,
            int rawContextTokens,
            int compactedContextTokens
    ) {
    }
}