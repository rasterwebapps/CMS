package com.cms.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.IncidentReport;
import com.cms.model.enums.IncidentSeverity;
import com.cms.model.enums.IncidentStatus;

public interface IncidentReportRepository extends JpaRepository<IncidentReport, Long> {

    List<IncidentReport> findByLabId(Long labId);

    List<IncidentReport> findByStatus(IncidentStatus status);

    List<IncidentReport> findBySeverity(IncidentSeverity severity);

    List<IncidentReport> findByIncidentDateBetween(LocalDate from, LocalDate to);
}

