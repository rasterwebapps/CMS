package com.cms.dto;

import java.time.Instant;

import com.cms.model.enums.DocumentType;
import com.cms.model.enums.DocumentVerificationStatus;

public record FacultyDocumentHistoryResponse(
    Long id,
    Long facultyDocumentId,
    DocumentType documentType,
    String documentTypeLabel,
    DocumentVerificationStatus previousStatus,
    DocumentVerificationStatus newStatus,
    String fileName,
    Long fileSize,
    String contentType,
    String remarks,
    String changedBy,
    Instant changedAt
) {}
