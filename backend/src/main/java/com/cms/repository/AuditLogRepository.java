package com.cms.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.AuditLog;

/** Repository for querying and persisting {@link AuditLog} records. */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}

