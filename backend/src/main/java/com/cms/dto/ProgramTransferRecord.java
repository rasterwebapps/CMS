package com.cms.dto;

import java.time.Instant;

public record ProgramTransferRecord(
    Long id,
    Long studentId,
    String studentName,
    Long oldProgramId,
    String oldProgramName,
    Long newProgramId,
    String newProgramName,
    Instant transferredAt,
    String transferredBy,
    boolean consentConfirmed,
    String notes
) {}
