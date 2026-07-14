package com.cms.dto;

import java.util.List;

public record LibraryImportExecuteResult(
    int imported,
    int skipped,
    List<ImportRowError> errors
) {}
