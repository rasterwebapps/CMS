package com.cms.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.LabAttendance;
import com.cms.model.enums.AttendanceStatus;

public interface LabAttendanceRepository extends JpaRepository<LabAttendance, Long> {

    List<LabAttendance> findByStudentId(Long studentId);

    List<LabAttendance> findByCourseId(Long courseId);

    List<LabAttendance> findByLabId(Long labId);

    List<LabAttendance> findByLabBatch(String labBatch);

    List<LabAttendance> findByStudentIdAndCourseId(Long studentId, Long courseId);

    List<LabAttendance> findByCourseIdAndDate(Long courseId, LocalDate date);

    List<LabAttendance> findByLabIdAndDate(Long labId, LocalDate date);

    List<LabAttendance> findByLabBatchAndDate(String labBatch, LocalDate date);

    @Query("SELECT COUNT(la) FROM LabAttendance la WHERE la.student.id = :studentId AND la.course.id = :courseId")
    long countByStudentIdAndCourseId(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    @Query("SELECT COUNT(la) FROM LabAttendance la WHERE la.student.id = :studentId AND la.course.id = :courseId " +
           "AND la.status = :status")
    long countByStudentIdAndCourseIdAndStatus(@Param("studentId") Long studentId,
                                               @Param("courseId") Long courseId,
                                               @Param("status") AttendanceStatus status);

    boolean existsByStudentIdAndCourseIdAndDate(Long studentId, Long courseId, LocalDate date);
}
