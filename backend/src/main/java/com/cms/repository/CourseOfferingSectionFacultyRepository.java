package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.CourseOfferingSectionFaculty;

public interface CourseOfferingSectionFacultyRepository extends JpaRepository<CourseOfferingSectionFaculty, Long> {

    List<CourseOfferingSectionFaculty> findByCourseOfferingId(Long courseOfferingId);

    Optional<CourseOfferingSectionFaculty> findByCourseOfferingIdAndCohortSectionId(Long courseOfferingId, Long cohortSectionId);

    void deleteByCourseOfferingIdAndCohortSectionId(Long courseOfferingId, Long cohortSectionId);
}
