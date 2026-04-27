package com.cms.service;

import java.util.List;

import com.cms.dto.CourseOfferingDto;

public interface CourseOfferingService {
    int generateOfferingsForTermInstance(Long termInstanceId);
    List<CourseOfferingDto> getOfferingsByTermInstance(Long termInstanceId);
    List<CourseOfferingDto> getOfferingsByTermInstanceAndSemester(Long termInstanceId, Integer semesterNumber);
    CourseOfferingDto getById(Long id);
    CourseOfferingDto updateOffering(Long id, Long facultyId, String sectionLabel);
    void deactivateOffering(Long id);
    void deactivateAllOfferingsForTermInstance(Long termInstanceId);
}
