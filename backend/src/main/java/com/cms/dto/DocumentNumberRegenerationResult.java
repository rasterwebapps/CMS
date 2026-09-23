package com.cms.dto;

import java.util.List;

public record DocumentNumberRegenerationResult(
    int totalChanged,
    List<DocumentNumberChange> changes
) {}
