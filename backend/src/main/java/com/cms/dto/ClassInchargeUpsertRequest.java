package com.cms.dto;

/** {@code facultyId} null clears this section's Class Incharge -- there is no fallback (unlike
 *  Section Faculty's offering-primary), so a cleared section just has none until reassigned. */
public record ClassInchargeUpsertRequest(Long facultyId) {}
