package com.cms.dto;

import java.time.Instant;

/** One guardian linked to a specific student (ward), as shown on the Student Detail screen's
 *  Guardians tab -- includes the {@code isPrimary} flag carried by the link itself, unlike the
 *  plain admin-wide {@link GuardianResponse}. */
public record StudentGuardianResponse(
    Long id,
    String firstName,
    String lastName,
    String email,
    String phone,
    String relationshipHint,
    boolean isPrimary,
    Instant createdAt
) {}
