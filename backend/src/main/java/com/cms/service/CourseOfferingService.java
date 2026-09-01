package com.cms.service;

import java.util.List;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.dto.ClinicalShiftConfigUpdateRequest;
import com.cms.dto.CourseOfferingDto;
import com.cms.dto.EligibleFacultyCandidateDto;
import com.cms.dto.GenerateOfferingsResponse;
import com.cms.model.Cohort;

public interface CourseOfferingService {
    GenerateOfferingsResponse generateOfferingsForTermInstance(Long termInstanceId);
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
    /** Replaces the offering's admin-curated faculty pool wholesale -- see {@code
     *  CourseOfferingServiceImpl#updateFacultyPool} for the eligibility/removal-block rules. Returns
     *  the refreshed eligible-faculty list (same shape as the eligible-faculty GET) so the frontend
     *  can swap it in directly. */
    List<EligibleFacultyCandidateDto> updateFacultyPool(Long id, List<Long> facultyIds);
    /** Bidirectional -- deactivating (true -> false) is blocked when the offering already has
     *  sessions placed in Skeleton Builder or batches with students rostered, since flipping the
     *  flag out from under either would silently orphan them. Reactivating (false -> true) has no
     *  such guard: deactivation never touches anything else, so restoring the flag restores
     *  exactly the prior state. */
    ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request);
    void deactivateAllOfferingsForTermInstance(Long termInstanceId);
    /** Sets/clears this offering's off-campus clinical shift duration + travel buffer (OC-175) --
     *  both null means the offering has no shift-based clinical component. */
    CourseOfferingDto updateClinicalShiftConfig(Long id, ClinicalShiftConfigUpdateRequest request);
}
