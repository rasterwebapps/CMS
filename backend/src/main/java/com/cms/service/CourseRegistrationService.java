package com.cms.service;

import java.util.List;

import com.cms.dto.CourseRegistrationDto;
import com.cms.dto.ElectiveBulkAssignmentResponse;

public interface CourseRegistrationService {
    int generateRegistrationsForTermInstance(Long termInstanceId);
    CourseRegistrationDto assignElectiveChoice(Long enrollmentId, Long courseOfferingId);
    /** Institution-decided mode: assigns every eligible student in the group to the same offering,
     *  overwriting (dropping then replacing) any existing choice they already had in that group --
     *  unlike assignElectiveChoice, which rejects a conflicting reassignment. */
    ElectiveBulkAssignmentResponse bulkAssignElectiveChoice(Long termInstanceId, Long electiveGroupId, Long courseOfferingId);
    List<CourseRegistrationDto> getRegistrationsByEnrollment(Long enrollmentId);
    List<CourseRegistrationDto> getRegistrationsByCourseOffering(Long courseOfferingId);
    CourseRegistrationDto getById(Long id);
    CourseRegistrationDto dropRegistration(Long id);
}
