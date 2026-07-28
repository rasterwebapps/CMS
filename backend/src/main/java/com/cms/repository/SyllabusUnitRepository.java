package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.SyllabusUnit;
import com.cms.model.enums.AttendanceType;

public interface SyllabusUnitRepository extends JpaRepository<SyllabusUnit, Long> {

    List<SyllabusUnit> findByCurriculumSemesterCourseIdOrderBySortOrderAscUnitNumberAsc(Long curriculumTermCourseId);

    boolean existsByCurriculumSemesterCourseIdAndUnitNumber(Long curriculumTermCourseId, Integer unitNumber);

    boolean existsByCurriculumSemesterCourseIdAndUnitNumberAndIdNot(
        Long curriculumTermCourseId, Integer unitNumber, Long excludeId);

    List<SyllabusUnit> findByCurriculumSemesterCourseIdAndComponentType(
        Long curriculumTermCourseId, AttendanceType componentType);
}
