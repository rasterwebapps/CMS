package com.cms.dto;

import java.time.Instant;
import java.util.List;

/** HTTP body for {@link com.cms.exception.SectionFacultyCapacityException} -- {@code failures}
 *  is never empty. */
public record SectionFacultyCapacityResponse(
    int status,
    String message,
    List<SectionFacultyCapacityFailure> failures,
    Instant timestamp
) {
}
