package com.aiops.aiops_apm.service;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class SanitizerService {

    public static final String REDACTED = "[REDACTED_SECRET]";

    private static final String KEYS =
            "password|auth[_-]?token|jwt[_-]?token|bearer[_-]?token|email|aws[_-]?access[_-]?key";

    // "key": "value"
    private static final Pattern JSON_KEY = Pattern.compile(
            "(\"(?:" + KEYS + ")\"\\s*:\\s*\")([^\"]*)(\")", Pattern.CASE_INSENSITIVE);

    // \"key\": \"value\"  (JSON stored as an escaped string)
    private static final Pattern ESCAPED_JSON_KEY = Pattern.compile(
            "(\\\\\"(?:" + KEYS + ")\\\\\"\\s*:\\s*\\\\\")(.*?)(\\\\\")", Pattern.CASE_INSENSITIVE);

    // key=value or key: value in plain text (optional "Bearer " prefix)
    private static final Pattern PLAIN_KEY = Pattern.compile(
            "\\b(" + KEYS + ")(\\s*[=:]\\s*)(?!\\[REDACTED_SECRET\\])((?:Bearer\\s+)?[^\\s,;&\"'}\\]]+)",
            Pattern.CASE_INSENSITIVE);

    // Pattern-based fallbacks
    private static final Pattern BEARER = Pattern.compile(
            "\\bBearer\\s+[A-Za-z0-9\\-._~+/]+=*", Pattern.CASE_INSENSITIVE);
    private static final Pattern JWT = Pattern.compile(
            "\\beyJ[A-Za-z0-9_-]{5,}(?:\\.[A-Za-z0-9_-]+)*(?:\\.{3})?");
    private static final Pattern EMAIL = Pattern.compile(
            "[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}");
    private static final Pattern AWS_KEY = Pattern.compile(
            "\\b(?:AKIA|ASIA)[0-9A-Z]{16}\\b");

    public String sanitize(String input) {
        if (input == null || input.isEmpty()) return input;
        String out = input;
        out = JSON_KEY.matcher(out).replaceAll("$1" + REDACTED + "$3");
        out = ESCAPED_JSON_KEY.matcher(out).replaceAll("$1" + REDACTED + "$3");
        out = PLAIN_KEY.matcher(out).replaceAll("$1$2" + REDACTED);
        for (Pattern p : List.of(BEARER, JWT, EMAIL, AWS_KEY)) {
            out = p.matcher(out).replaceAll(REDACTED);
        }
        return out;
    }
}