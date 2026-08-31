package com.cms.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** Additive-only: adds {@code venueId} (a {@code Lab} when {@code venueType} is {@code "LAB"},
 *  otherwise a {@code ClinicalVenue}) to every listed subject's eligible-venue set, without
 *  touching any other field on those subjects — unlike {@code SubjectService#update}, which
 *  requires (and overwrites with) a full {@code SubjectRequest}. Exists specifically so the
 *  "Add a second venue" remedy on the Lab/Clinical venue-capacity checklist (see
 *  {@code VenueOverCapacity#affectedSubjectIds}) can immediately make a freshly created venue
 *  usable by the exact subjects the checklist already knows are stuck on the over-capacity one,
 *  instead of leaving the admin to separately open each Subject's edit form. */
public record AddEligibleVenueRequest(
    @NotEmpty List<Long> subjectIds,
    @NotNull String venueType,
    @NotNull Long venueId
) {}
