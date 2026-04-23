package com.cms.dto;

/**
 * Carries a stored document's binary content along with the metadata needed
 * to stream it back to the client (filename for Content-Disposition, MIME
 * type for Content-Type).
 */
public record DocumentFileDownload(
    String fileName,
    String contentType,
    byte[] data
) {}
