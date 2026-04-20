package com.cms.dto;

import java.util.List;

public record MissingDocumentsResponse(
    boolean allSubmitted,
    List<String> missingDocumentTypes
) {}
