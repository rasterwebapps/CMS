package com.cms.dto;

import java.util.List;

public record LegacyTermOverrideApplyResult(
    int overridesWritten,
    List<String> exceptionDetails
) {}
