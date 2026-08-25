package com.cms.dto;

import java.util.List;

public record FacultyPoolUpdateRequest(
    List<Long> facultyIds
) {}
