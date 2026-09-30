package com.aiops.aiops_apm.repository;

import com.aiops.aiops_apm.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Optional<Incident> findByFingerprint(String fingerprint);
}