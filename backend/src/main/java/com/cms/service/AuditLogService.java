package com.cms.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.cms.model.AuditLog;
import com.cms.repository.AuditLogRepository;

/**
 * Records immutable audit events for user/role/permission mutations.
 *
 * <p>Each {@link #record} call runs in its own transaction ({@link Propagation#REQUIRES_NEW})
 * so that the audit entry is saved even if the calling transaction rolls back.
 */
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Persists a single audit event.
     *
     * @param actor      Keycloak username of the person who performed the action
     * @param action     Verb describing the action (e.g. {@code "ROLE_CREATED"})
     * @param entityType High-level resource type (e.g. {@code "AppRole"})
     * @param entityId   String form of the entity's primary key
     * @param detail     Human-readable or JSON description of the change
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String actor, String action, String entityType,
                       String entityId, String detail) {
        AuditLog entry = AuditLog.builder()
            .actor(actor)
            .action(action)
            .entityType(entityType)
            .entityId(entityId)
            .detail(detail)
            .build();
        auditLogRepository.save(entry);
    }
}

