package com.aiops.aiops_apm.repository;

import com.aiops.aiops_apm.entity.ApmLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface LogRepository extends JpaRepository<ApmLog, Long> {

    @Query("select l.logId from ApmLog l where l.logId in :ids")
    List<String> findExistingLogIds(@Param("ids") Collection<String> ids);

    List<ApmLog> findAllByOrderByOccurredAtDesc(Pageable pageable);
}