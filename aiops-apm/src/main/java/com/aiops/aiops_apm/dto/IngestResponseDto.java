package com.aiops.aiops_apm.dto;

import java.util.List;

public record IngestResponseDto(
        int received,
        int accepted,
        int duplicates,
        int rejected,
        List<String> errors
) {}