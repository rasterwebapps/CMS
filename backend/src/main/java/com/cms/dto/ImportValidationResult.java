package com.cms.dto;

import java.util.List;

public record ImportValidationResult(
    int studentsTotal,
    int studentsValid,
    int qualificationsTotal,
    int qualificationsValid,
    int feeHistoryTotal,
    int feeHistoryValid,
    List<ImportRowError> errors,
    List<ImportRowError> warnings
) {}
