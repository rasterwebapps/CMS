package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.CourseOffering;

public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long> {

    List<CourseOffering> findByTermInstanceId(Long termInstanceId);

    List<CourseOffering> findByTermInstanceIdAndSemesterNumber(Long termInstanceId, Integer semesterNumber);

    List<CourseOffering> findByTermInstanceIdAndCurriculumVersionId(Long termInstanceId, Long cvId);

    List<CourseOffering> findByTermInstanceIdAndIsActiveTrue(Long termInstanceId);

    Optional<CourseOffering> findByTermInstanceIdAndCurriculumVersionIdAndSubjectIdAndSemesterNumber(
        Long termInstanceId, Long curriculumVersionId, Long subjectId, Integer semesterNumber);
}
