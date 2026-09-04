package com.cms.dto;

/** {@code facultyId} null clears the override for this section, falling back to the offering's
 *  own primary faculty. {@code version} is the row's version as last seen by the client (null if
 *  the client saw no row yet, i.e. "Unassigned") -- checked against the current row before
 *  applying, so a stale save is rejected instead of silently overwriting a concurrent change. */
public record SectionFacultyUpsertRequest(Long facultyId, Long version) {}
