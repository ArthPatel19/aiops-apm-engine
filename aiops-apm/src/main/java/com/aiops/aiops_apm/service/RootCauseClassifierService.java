package com.aiops.aiops_apm.service;

import com.aiops.aiops_apm.entity.ApmLog;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class RootCauseClassifierService {

    public RootCause classify(ApmLog log) {

        String text = buildSearchText(log);

        if (containsAny(
                text,
                "connection pool",
                "jdbc",
                "hikari",
                "timeout"
        )) {
            return RootCause.DB_CONNECTION;
        }

        if (containsAny(
                text,
                "jwt",
                "expired",
                "token"
        )) {
            return RootCause.AUTH_TOKEN;
        }

        return RootCause.UNKNOWN;
    }

    private String buildSearchText(ApmLog log) {

        return String.join(
                " ",
                safe(log.getErrorType()),
                safe(log.getErrorMessage()),
                safe(log.getStackTrace()),
                safe(log.getAffectedApi()),
                safe(log.getAffectedFeature())
        ).toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String text, String... keywords) {

        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    public enum RootCause {
        DB_CONNECTION,
        AUTH_TOKEN,
        UNKNOWN
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}