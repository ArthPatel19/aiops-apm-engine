package com.aiops.aiops_apm.controller;

import com.aiops.aiops_apm.dto.IngestResponseDto;
import com.aiops.aiops_apm.dto.LogRequestDto;
import com.aiops.aiops_apm.dto.LogResponseDto;
import com.aiops.aiops_apm.service.LogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @PostMapping
    public ResponseEntity<IngestResponseDto> ingest(
            @RequestBody List<LogRequestDto> logs) {

        IngestResponseDto result = logService.ingest(logs);

        HttpStatus status = result.accepted() > 0
                ? HttpStatus.CREATED
                : HttpStatus.OK;

        return ResponseEntity.status(status).body(result);
    }

    @GetMapping
    public List<LogResponseDto> latest(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        return logService.getLatest(page, size);
    }
}