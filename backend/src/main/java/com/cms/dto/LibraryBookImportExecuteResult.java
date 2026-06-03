package com.cms.dto;

import java.util.List;

public record LibraryBookImportExecuteResult(
    int booksImported,
    int booksSkipped,
    List<ImportRowError> errors
) {}
