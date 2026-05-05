package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.SafetyAudit;
import com.cms.model.enums.AuditStatus;

public interface SafetyAuditRepository extends JpaRepository<SafetyAudit, Long> {

    List<SafetyAudit> findByLabId(Long labId);

    List<SafetyAudit> findByStatus(AuditStatus status);
}

