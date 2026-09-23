package com.cms.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubjectRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    String name,

    @NotBlank(message = "Code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    String code,

    /** Min is 0, not 1, only so a system-managed subject (an exact two-code allowlist --
     *  SYSTEM-LIBRARY/SYSTEM-SPORTS, not a "SYSTEM-" prefix match, see
     *  {@link com.cms.service.SubjectService#isSystemManaged}) can round-trip its deliberate
     *  credits=0 sentinel through this same endpoint when an admin edits its eligible
     *  faculty/venues. {@link com.cms.service.SubjectService#create}/{@code #update} still reject
     *  credits &lt; 1 for every ordinary subject, and pin it to exactly 0 (not just &gt;= 0) for the
     *  two allowlisted ones. */
    @NotNull(message = "Credits is required")
    @Min(value = 0, message = "Credits must be at least 0")
    @Max(value = 20, message = "Credits must not exceed 20")
    Integer credits,

    @NotNull(message = "Theory credits is required")
    @Min(value = 0, message = "Theory credits must be at least 0")
    @Max(value = 20, message = "Theory credits must not exceed 20")
    Integer theoryCredits,

    @NotNull(message = "Lab credits is required")
    @Min(value = 0, message = "Lab credits must be at least 0")
    @Max(value = 20, message = "Lab credits must not exceed 20")
    Integer labCredits,

    Long specialityId,

    /** Min is 0, not 1, for the same system-managed-subject reason as {@code credits} above. */
    @NotNull(message = "Semester is required")
    @Min(value = 0, message = "Semester must be at least 0")
    @Max(value = 12, message = "Semester must not exceed 12")
    Integer termNumber,

    Boolean isActive,

    /** How many consecutive periods one single Lab/Clinical session must occupy for this subject
     *  (e.g. a 3-hour lab runs as 3 back-to-back periods on the same day, not scattered
     *  single-period placements) -- optional, defaults to 1 (today's existing behavior) when null. */
    @Min(value = 1, message = "Lab session length must be at least 1 period")
    @Max(value = 12, message = "Lab session length must not exceed 12 periods")
    Integer labSessionBlockPeriods,

    @Min(value = 1, message = "Clinical session length must be at least 1 period")
    @Max(value = 12, message = "Clinical session length must not exceed 12 periods")
    Integer clinicalSessionBlockPeriods,

    /** Labs/Clinical Venues suitable for this subject's practical sessions -- a soft preference,
     *  optional. Null/empty means no preference configured, matching pre-existing behavior. */
    List<Long> eligibleLabIds,
    List<Long> eligibleClinicalVenueIds,

    /** Faculty explicitly widened onto this subject on top of the Speciality-match rule -- optional,
     *  additive-only (see FacultyEligibility). Null/empty means Speciality-match-only. */
    List<Long> eligibleFacultyIds
) {}
