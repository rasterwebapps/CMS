package com.cms.dto;

import com.cms.model.enums.DocumentVerificationStatus;
import com.cms.model.enums.DocumentType;

public record ProgramTransferDocumentInfo(
    Long documentId,
    DocumentType documentType,
    String documentTypeLabel,
    DocumentVerificationStatus status
) {}
