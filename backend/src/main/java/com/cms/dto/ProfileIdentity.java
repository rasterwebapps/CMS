package com.cms.dto;

/**
 * Resolved identity of the currently authenticated user, mapped to a
 * faculty or student record via their Keycloak email claim.
 *
 * entityType: FACULTY | STUDENT | ADMIN
 * entityId:   faculty.id or student.id (null for ADMIN)
 * admissionId: populated only for STUDENT
 * programId:   populated only for STUDENT
 */
public record ProfileIdentity(
    String entityType,
    Long entityId,
    Long admissionId,
    Long programId,
    String displayName,
    String email,
    String bio,
    String phone,
    String bloodGroup
) {}
