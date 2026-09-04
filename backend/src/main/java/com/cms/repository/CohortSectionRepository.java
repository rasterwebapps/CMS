package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.CohortSection;

public interface CohortSectionRepository extends JpaRepository<CohortSection, Long> {

    List<CohortSection> findByCohortRoomAllocationId(Long cohortRoomAllocationId);

    List<CohortSection> findByCohortRoomAllocationIdAndIsActiveTrue(Long cohortRoomAllocationId);

    List<CohortSection> findByTermInstanceIdAndIsActiveTrue(Long termInstanceId);

    /** Every past-generation, now-inactive section this cohort+term ever committed under this
     *  exact label -- a recommit's reuse lookup takes the most-recently-updated of these (see
     *  {@code CohortRoomAllocationService#reuseOrCreateSection}) rather than always inserting a
     *  fresh row, so satellite tables keyed to a section id (roster-adjacent {@code
     *  CourseOfferingSectionFaculty}, and anything a future recommit-carry-forward gap gets found
     *  in) never go orphaned in the first place. */
    List<CohortSection> findByCohortRoomAllocation_Cohort_IdAndTermInstance_IdAndSectionLabelAndIsActiveFalse(
        Long cohortId, Long termInstanceId, String sectionLabel);
}
