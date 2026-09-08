package com.cms.dto;

import java.util.List;

public record ConfirmFacultySubstitutionsRequest(
    List<ConfirmFacultySubstitutionItem> items
) {}
