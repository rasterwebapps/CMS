package com.cms.service;

import java.util.List;

import com.cms.dto.StudentMarkDto;
import com.cms.dto.StudentMarkRequest;

public interface StudentMarkService {

    StudentMarkDto upsert(StudentMarkRequest request);

    StudentMarkDto getById(Long id);

    List<StudentMarkDto> getByExamEvent(Long examEventId);

    List<StudentMarkDto> getByEnrollment(Long enrollmentId);
}
