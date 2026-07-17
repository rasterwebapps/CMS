package com.cms.dto;

import com.cms.model.enums.ExamOutcome;

/** outcome is null when no PUBLISHED exam result exists yet for this subject in the term. */
public record SubjectExamOutcome(
    Long subjectId,
    String subjectName,
    String subjectCode,
    ExamOutcome outcome
) {}
