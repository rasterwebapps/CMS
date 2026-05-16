package com.cms.dto;

public record AdmissionConfirmationDto(
    Long studentId,
    String studentName,
    String admissionNumber,
    String cohortCode,
    String cohortDisplayName,
    Integer firstSemesterNumber,
    Long firstTermInstanceId,
    Long expectedGraduationTermInstanceId
) {}
