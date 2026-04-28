package com.cms.service;

import java.util.List;

import com.cms.dto.SemesterResultDto;

public interface SemesterResultService {

    SemesterResultDto computeForEnrollment(Long enrollmentId);

    void computeResultsForTermInstance(Long termInstanceId);

    SemesterResultDto getByEnrollment(Long enrollmentId);

    List<SemesterResultDto> getByTermInstance(Long termInstanceId);

    List<SemesterResultDto> getByStudent(Long studentId);

    SemesterResultDto lockResult(Long id);
}
