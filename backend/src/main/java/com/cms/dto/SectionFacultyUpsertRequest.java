package com.cms.dto;

/** {@code facultyId} null clears the override for this section, falling back to the offering's
 *  own primary faculty. */
public record SectionFacultyUpsertRequest(Long facultyId) {}
