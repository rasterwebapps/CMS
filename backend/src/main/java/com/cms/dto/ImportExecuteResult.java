package com.cms.dto;

import java.util.List;

public record ImportExecuteResult(
    int studentsImported,
    int studentsSkipped,
    int admissionsCreated,
    int qualificationsImported,
    int feeAllocationsCreated,
    int paymentsImported,
    List<ImportRowError> errors
) {}
