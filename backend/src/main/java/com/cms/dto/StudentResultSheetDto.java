package com.cms.dto;

import java.math.BigDecimal;
import java.util.List;

import com.cms.model.enums.ResultStatus;

public record StudentResultSheetDto(
    Long studentId,
    String studentName,
    String rollNumber,
    Long termInstanceId,
    String termInstanceLabel,
    List<SubjectMarkRow> subjectMarks,
    BigDecimal totalMaxMarks,
    BigDecimal totalMarksObtained,
    BigDecimal percentage,
    ResultStatus resultStatus
) {
    public record SubjectMarkRow(
        String subjectName,
        String subjectCode,
        String sessionType,
        BigDecimal maxMarks,
        BigDecimal marksObtained,
        String markStatus
    ) {}
}
