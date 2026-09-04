package com.cms.dto;

import java.time.Instant;
import java.util.List;

import com.cms.model.enums.ElectiveSelectionMode;
import com.cms.model.enums.SubjectType;

public record CourseOfferingDto(
    Long id,
    Long termInstanceId,
    String termInstanceLabel,
    Long curriculumVersionId,
    String curriculumVersionName,
    Long subjectId,
    String subjectName,
    String subjectCode,
    Long subjectSpecialityId,
    String subjectSpecialityName,
    /** Faculty explicitly widened onto this subject on top of the Speciality-match rule -- see
     *  FacultyEligibility. Empty means Speciality-match-only. */
    List<Long> subjectEligibleFacultyIds,
    Integer termNumber,
    Boolean isActive,
    Long curriculumTermCourseId,
    Boolean isElective,
    SubjectType subjectType,
    Long electiveGroupId,
    String electiveGroupName,
    ElectiveSelectionMode electiveGroupSelectionMode,
    Integer theoryHours,
    Integer labHours,
    Integer clinicalHours,
    /** Subject's on-campus clinical session block size (consecutive periods), set in Subject Master.
     *  Never changed from the Clinical Shift Config dialog -- shown there read-only for context. */
    Integer clinicalSessionBlockPeriods,
    /** Configurable off-campus clinical shift length/travel buffer (OC-175) -- null means this
     *  offering has no shift-based clinical component. */
    Integer clinicalShiftDurationMinutes,
    Integer clinicalTravelBufferMinutes,
    Instant createdAt,
    Instant updatedAt,
    /** CourseOffering has no cohort FK of its own -- it's keyed by curriculum version, which can be
     *  shared by more than one cohort's admission year on the same (program, course). Usually a
     *  single name; more than one means this exact row is shared across cohorts (see
     *  CourseOfferingSectionFacultyService's own resolution of the same fact). Empty when no
     *  cohort is currently enrolled against this offering's curriculum version + semester. */
    List<String> cohortNames
) {}
