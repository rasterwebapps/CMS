package com.cms.dto;

import com.cms.model.enums.DocumentType;

/**
 * One retrieved snippet from the applicant document RAG search (OC-277/OC-278). Answer mode is
 * raw snippets, not a synthesized answer -- the caller shows these directly, each attributable
 * back to its source document and applicant. {@code admissionId} is null for a document still at
 * Enquiry stage (no Admission yet) -- {@code enquiryId} is populated instead so the UI can still
 * link through to the applicant's record.
 */
public record AdmissionDocumentRagResult(
    Long admissionDocumentId,
    Long admissionId,
    Long enquiryId,
    DocumentType documentType,
    String fileName,
    String studentName,
    String admissionNumber,
    String chunkText,
    double similarity
) {}
