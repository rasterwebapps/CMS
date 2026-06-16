package com.cms.dto;

import java.util.Set;

public record ProgramDocumentRequirementsRequest(
    Set<String> mandatory,
    Set<String> optional
) {}
