package com.cms.dto;

public record DocumentNumberChange(
    Long entityId,
    String entityLabel,
    String oldNumber,
    String newNumber
) {}
