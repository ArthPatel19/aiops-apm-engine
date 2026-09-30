package com.aiops.aiops_apm.controller;

import com.aiops.aiops_apm.dto.HealthResponseDto;
import com.aiops.aiops_apm.entity.Incident;
import com.aiops.aiops_apm.repository.IncidentRepository;
import com.aiops.aiops_apm.service.HealthScoreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final IncidentRepository incidentRepository;
    private final HealthScoreService healthScoreService;

    public HealthController(
            IncidentRepository incidentRepository,
            HealthScoreService healthScoreService) {

        this.incidentRepository = incidentRepository;
        this.healthScoreService = healthScoreService;
    }

    @GetMapping
    public HealthResponseDto getHealth() {

        List<Incident> incidents =
                incidentRepository.findAll();

        int totalPenalty = incidents.stream()
                .mapToInt(healthScoreService::calculatePenalty)
                .sum();

        int score =
                healthScoreService.calculateHealthScore(incidents);

        return new HealthResponseDto(
                score,
                incidents.size(),
                totalPenalty
        );
    }
}