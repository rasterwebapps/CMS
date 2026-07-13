package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.AttendanceThreshold;
import com.cms.model.enums.AttendanceType;

public interface AttendanceThresholdRepository extends JpaRepository<AttendanceThreshold, Long> {

    List<AttendanceThreshold> findByCurriculumSemesterCourseId(Long curriculumTermCourseId);

    Optional<AttendanceThreshold> findByCurriculumSemesterCourseIdAndAttendanceType(
        Long curriculumTermCourseId, AttendanceType attendanceType);
}
