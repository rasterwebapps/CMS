package com.cms.service;

import java.util.List;

import com.cms.dto.CourseRegistrationDto;

public interface CourseRegistrationService {
    int generateRegistrationsForTermInstance(Long termInstanceId);
    List<CourseRegistrationDto> getRegistrationsByEnrollment(Long enrollmentId);
    List<CourseRegistrationDto> getRegistrationsByCourseOffering(Long courseOfferingId);
    CourseRegistrationDto getById(Long id);
    CourseRegistrationDto dropRegistration(Long id);
}
