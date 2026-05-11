package com.cms.dto;

import java.util.List;

/**
 * Summarises whether all mandatory enquiry documents have been uploaded
 * and/or verified. Returned by the pre-admission verification-status endpoint.
 */
public record DocumentVerificationStatusResponse(
    boolean allVerified,
    boolean allUploaded,
    List<String> unverifiedDocumentTypes,
    List<String> notUploadedDocumentTypes
) {}
