package com.cms.dto;

import java.time.Instant;
import java.time.LocalDateTime;

import com.cms.model.enums.DocumentType;
import com.cms.model.enums.DocumentVerificationStatus;

public record AdmissionDocumentResponse(
    Long id,
    Long admissionId,
    DocumentType documentType,
    String fileName,
    String storageKey,
    LocalDateTime uploadedAt,
    Boolean originalSubmitted,
    String verifiedBy,
    LocalDateTime verifiedAt,
    DocumentVerificationStatus verificationStatus,
    Instant createdAt,
    Instant updatedAt
) {}
