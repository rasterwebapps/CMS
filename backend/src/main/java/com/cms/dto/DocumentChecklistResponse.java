package com.cms.dto;

import java.util.Map;

public record DocumentChecklistResponse(
    Map<String, String> mandatory,
    Map<String, String> optional
) {}
