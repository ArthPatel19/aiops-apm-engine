package com.aiops.aiops_apm.controller;

import com.aiops.aiops_apm.dto.IncidentResponseDto;
import com.aiops.aiops_apm.entity.Incident;
import com.aiops.aiops_apm.repository.IncidentRepository;
import com.aiops.aiops_apm.service.AiDiagnosticService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentRepository incidentRepository;
    private final AiDiagnosticService aiDiagnosticService;

    public IncidentController(
            IncidentRepository incidentRepository,
            AiDiagnosticService aiDiagnosticService) {

        this.incidentRepository = incidentRepository;
        this.aiDiagnosticService = aiDiagnosticService;
    }

    @GetMapping
    public List<IncidentResponseDto> getIncidents() {

        return incidentRepository.findAll()
                .stream()
                .map(IncidentResponseDto::from)
                .toList();
    }

    @GetMapping("/{id}")
    public IncidentResponseDto getIncident(
            @PathVariable Long id) {

        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Incident not found: " + id
                        )
                );

        return IncidentResponseDto.from(incident);
    }

    @PostMapping("/{id}/analyze")
    public IncidentResponseDto reanalyze(
            @PathVariable Long id) {

        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Incident not found: " + id
                        )
                );

        Incident updated =
                aiDiagnosticService.diagnose(incident);

        return IncidentResponseDto.from(updated);
    }
}

