package com.cms.dto;

import java.util.List;

public record StudentPerformanceReportResponse(
    Long studentId,
    String studentName,
    List<ExamResultResponse> examResults,
    List<LabContinuousEvaluationResponse> labEvaluations
) {}
