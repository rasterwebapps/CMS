package com.cms.service;

import java.util.List;

import com.cms.dto.CourseRegistrationDto;
import com.cms.dto.ElectiveBulkAssignmentResponse;
import com.cms.dto.ElectiveGroupSummaryResponse;

public interface CourseRegistrationService {
    int generateRegistrationsForTermInstance(Long termInstanceId);
    /** Assigns a student's elective choice. If the student already has a different active choice
     *  in the same elective group, this changes it (drops the old, registers the new) instead of
     *  rejecting -- subject to the same integrity guard as {@link #bulkAssignElectiveChoice}:
     *  blocked if the group is already scheduled in Skeleton Builder or the student already has
     *  attendance recorded against the subject being dropped, and blocked outright (even for a
     *  first-time pick) if the term is LOCKED. */
    CourseRegistrationDto assignElectiveChoice(Long enrollmentId, Long courseOfferingId);
    /** Institution-decided mode: assigns every eligible student in the group to the same offering,
     *  overwriting (dropping then replacing) any existing choice they already had in that group --
     *  students whose existing choice can't be safely changed (see {@link #assignElectiveChoice})
     *  are skipped and counted in the response's blockedCount rather than failing the whole run. */
    ElectiveBulkAssignmentResponse bulkAssignElectiveChoice(Long termInstanceId, Long electiveGroupId, Long courseOfferingId);
    List<CourseRegistrationDto> getRegistrationsByEnrollment(Long enrollmentId);
    List<CourseRegistrationDto> getRegistrationsByCourseOffering(Long courseOfferingId);
    CourseRegistrationDto getById(Long id);
    CourseRegistrationDto dropRegistration(Long id);
    /** One row per elective group open in this term instance, with eligible/assigned counts and
     *  scheduled status -- backs the Elective Assignment screen's group-launcher view so an admin
     *  doesn't have to click into each group one at a time just to see its progress. */
    List<ElectiveGroupSummaryResponse> getElectiveGroupSummaries(Long termInstanceId);
}
