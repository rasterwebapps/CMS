package com.cms.dto;

import com.cms.model.enums.DocumentType;

/**
 * One retrieved snippet from the Admission document RAG search (OC-277). Answer mode is raw
 * snippets, not a synthesized answer -- the caller shows these directly, each attributable back
 * to its source document and student.
 */
public record AdmissionDocumentRagResult(
    Long admissionDocumentId,
    DocumentType documentType,
    String fileName,
    String studentName,
    String admissionNumber,
    String chunkText,
    double similarity
) {}
