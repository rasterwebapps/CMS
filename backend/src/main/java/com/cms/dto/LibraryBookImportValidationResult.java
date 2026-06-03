package com.cms.dto;

import java.util.List;

public record LibraryBookImportValidationResult(
    int totalRows,
    int validRows,
    int invalidRows,
    List<ImportRowError> errors,
    List<ImportRowError> warnings
) {}
