package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.CourseOfferingSectionFaculty;

public interface CourseOfferingSectionFacultyRepository extends JpaRepository<CourseOfferingSectionFaculty, Long> {

    List<CourseOfferingSectionFaculty> findByCourseOfferingId(Long courseOfferingId);

    Optional<CourseOfferingSectionFaculty> findByCourseOfferingIdAndCohortSectionId(Long courseOfferingId, Long cohortSectionId);

    void deleteByCourseOfferingIdAndCohortSectionId(Long courseOfferingId, Long cohortSectionId);

    /** The whole-cohort row (no section split) for this offering+cohort, if one exists. */
    Optional<CourseOfferingSectionFaculty> findByCourseOfferingIdAndCohortIdAndCohortSectionIdIsNull(Long courseOfferingId, Long cohortId);

    void deleteByCourseOfferingIdAndCohortIdAndCohortSectionIdIsNull(Long courseOfferingId, Long cohortId);

    /** Every assignment row for every offering in a term instance, in one query -- backs the
     *  Assign Faculty list table's Faculty column summary without an N+1 per-row fetch. */
    List<CourseOfferingSectionFaculty> findByCourseOffering_TermInstanceId(Long termInstanceId);
}
