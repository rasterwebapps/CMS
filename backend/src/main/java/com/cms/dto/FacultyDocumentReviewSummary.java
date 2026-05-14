
package com.cms.dto;

public record FacultyDocumentReviewSummary(
    int totalDocumentCount,
    int requiredDocumentCount,
    int pendingVerificationCount,
    int rejectedCount,
    int missingRequiredCount,
    int verifiedRequiredCount,
    boolean hasAnyDocuments,
    boolean hasPendingVerification,
    boolean hasRejectedDocuments,
    boolean allRequiredDocumentsVerified
) {}
