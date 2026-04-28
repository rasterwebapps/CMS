package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.CurriculumSemesterCourse;

public interface CurriculumSemesterCourseRepository extends JpaRepository<CurriculumSemesterCourse, Long> {

    List<CurriculumSemesterCourse> findByCurriculumVersionId(Long curriculumVersionId);

    List<CurriculumSemesterCourse> findByCurriculumVersionIdAndSemesterNumber(
        Long curriculumVersionId, Integer semesterNumber);

    boolean existsByCurriculumVersionId(Long curriculumVersionId);
}
