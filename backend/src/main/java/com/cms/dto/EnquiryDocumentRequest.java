package com.cms.dto;

import com.cms.model.enums.DocumentType;
import com.cms.model.enums.DocumentVerificationStatus;

import jakarta.validation.constraints.NotNull;

public record EnquiryDocumentRequest(
    @NotNull(message = "Document type is required")
    DocumentType documentType,

    DocumentVerificationStatus status,

    String remarks
) {}
