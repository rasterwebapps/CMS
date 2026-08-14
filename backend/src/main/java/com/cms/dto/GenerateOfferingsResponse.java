package com.cms.dto;

import java.util.List;

/**
 * Explains a zero (or partial) result from generating course offerings, rather than leaving the
 * caller to guess why nothing happened — see CourseOfferingServiceImpl#generateOfferingsForTermInstance.
 */
public record GenerateOfferingsResponse(
    int offeringsCreated,
    int activeCohortCount,
    List<String> cohortsWithoutCurriculumVersion,
    int cohortsWithoutProgramTotalTerms,
    int offeringsAlreadyExisting
) {}
