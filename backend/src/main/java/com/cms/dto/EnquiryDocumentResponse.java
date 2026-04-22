package com.cms.dto;

import java.time.Instant;

import com.cms.model.enums.DocumentType;
import com.cms.model.enums.DocumentVerificationStatus;

public record EnquiryDocumentResponse(
    Long id,
    Long enquiryId,
    DocumentType documentType,
    DocumentVerificationStatus status,
    String remarks,
    String verifiedBy,
    Instant verifiedAt,
    Instant createdAt,
    Instant updatedAt,
    String fileName,
    String contentType,
    Long fileSize,
    Instant uploadedAt,
    boolean hasFile
) {}
