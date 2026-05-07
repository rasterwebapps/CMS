package com.cms.dto;

/**
 * Response containing generated roll number information.
 *
 * @param rollNumber Generated roll number (e.g., "959652026004")
 * @param studentId Student ID who received this roll number
 * @param studentName Full name of the student
 */
public record RollNumberAssignment(
    String rollNumber,
    Long studentId,
    String studentName
) {}

