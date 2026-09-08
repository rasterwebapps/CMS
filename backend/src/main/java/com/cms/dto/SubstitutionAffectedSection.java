package com.cms.dto;

/** One row a {@link FacultySubstitutionTip} actually touched this run. For a THEORY substitution,
 *  {@code batchId} is null and {@code cohortId}/{@code cohortSectionId} identify the (possibly
 *  whole-cohort, i.e. {@code cohortSectionId} null) {@code CourseOfferingSectionFaculty} row. For a
 *  LAB/CLINICAL substitution, {@code batchId} identifies the exact {@code Batch} whose {@code
 *  coordinatorFaculty} this run's fallback covered -- {@code cohortId}/{@code cohortSectionId} are
 *  still carried along for display/grouping but {@code batchId} is what {@code
 *  CourseOfferingSectionFacultyService#confirmSubstitutions} actually matches on for that case.
 *  Carried through to {@link ConfirmFacultySubstitutionItem} unchanged so confirming reassigns
 *  exactly the row(s) this tip's own fallback covered -- never "every row in the offering still on
 *  the original faculty", which would also sweep up a sibling section/batch that independently fell
 *  back to a *different* substitute in the same run and was reported as its own separate tip. */
public record SubstitutionAffectedSection(Long cohortId, Long cohortSectionId, Long batchId) {}
