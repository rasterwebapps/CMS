package com.cms.service;

import java.util.List;

import com.cms.dto.CourseStatsDto;
import com.cms.dto.SemesterSummaryDto;
import com.cms.dto.StudentResultSheetDto;

public interface ResultReportService {

    StudentResultSheetDto getResultSheet(Long enrollmentId);

    List<SemesterSummaryDto> getSummaryByTermInstance(Long termInstanceId);

    List<CourseStatsDto> getCourseStatsByTermInstance(Long termInstanceId);
}
