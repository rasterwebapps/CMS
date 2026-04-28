package com.cms.service;

import java.util.List;

import com.cms.dto.StudentTermEnrollmentDto;
import com.cms.model.Cohort;
import com.cms.model.TermInstance;

public interface StudentTermEnrollmentService {
    int generateEnrollmentsForTermInstance(Long termInstanceId);
    List<StudentTermEnrollmentDto> getEnrollmentsByTermInstance(Long termInstanceId);
    List<StudentTermEnrollmentDto> getEnrollmentsByStudent(Long studentId);
    List<StudentTermEnrollmentDto> getEnrollmentsByTermInstanceAndSemester(Long termInstanceId, Integer semesterNumber);
    StudentTermEnrollmentDto getById(Long id);
    Integer computeSemesterNumber(Cohort cohort, TermInstance termInstance);
}
