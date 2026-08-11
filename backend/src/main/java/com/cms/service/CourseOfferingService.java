package com.cms.service;

import java.util.List;

import com.cms.dto.CourseOfferingDto;
import com.cms.model.Cohort;

public interface CourseOfferingService {
    int generateOfferingsForTermInstance(Long termInstanceId);
    /** Every ACTIVE cohort {@link #generateOfferingsForTermInstance} would silently skip (zero
     *  offerings generated for it) because it has no active curriculum version mapped to its
     *  course — surfaced ahead of time by the term-advance checklist rather than discovered after
     *  the fact. */
    List<Cohort> findActiveCohortsWithoutCurriculumVersion();
    List<CourseOfferingDto> getOfferingsByTermInstance(Long termInstanceId);
    List<CourseOfferingDto> getOfferingsByTermInstanceAndSemester(Long termInstanceId, Integer semesterNumber);
    /** Scoped to one cohort's own curriculum version + its actual enrolled semester(s) for this
     *  term — the precise filter, unlike {@link #getOfferingsByTermInstanceAndSemester} which can
     *  mix in another cohort/program's offerings sharing the same term+semesterNumber. */
    List<CourseOfferingDto> getOfferingsByTermInstanceAndCohort(Long termInstanceId, Long cohortId);
    List<CourseOfferingDto> getOfferingsByTermInstanceAndElectiveGroup(Long termInstanceId, Long electiveGroupId);
    CourseOfferingDto getById(Long id);
    CourseOfferingDto updateOffering(Long id, Long facultyId, Long secondaryFacultyId, String sectionLabel);
    void deactivateOffering(Long id);
    void deactivateAllOfferingsForTermInstance(Long termInstanceId);
}
