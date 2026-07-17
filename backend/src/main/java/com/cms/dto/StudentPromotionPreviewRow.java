package com.cms.dto;

import java.util.List;

import com.cms.model.enums.PromotionOutcome;

public record StudentPromotionPreviewRow(
    Long studentId,
    String studentName,
    String rollNumber,
    Long enrollmentId,
    List<AttendanceReportResponse> subjectAttendance,
    List<SubjectExamOutcome> subjectExamOutcomes,
    List<PromotionArrearSubject> carriedArrearSubjects,
    List<PromotionArrearSubject> newArrearSubjects,
    List<PromotionArrearSubject> totalArrearSubjects,
    PromotionOutcome recommendedOutcome,
    List<String> blockReasons
) {}
