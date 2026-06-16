package com.cms.dto;

import java.util.Set;

public record ProgramDocumentRequirementsResponse(
    Set<String> mandatory,
    Set<String> optional
) {}
