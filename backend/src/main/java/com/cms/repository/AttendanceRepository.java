package com.cms.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.Attendance;
import com.cms.model.enums.AttendanceStatus;
import com.cms.model.enums.AttendanceType;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByStudentId(Long studentId);

    List<Attendance> findByCourseId(Long courseId);

    List<Attendance> findByStudentIdAndCourseId(Long studentId, Long courseId);

    List<Attendance> findByStudentIdAndDateBetween(Long studentId, LocalDate startDate, LocalDate endDate);

    List<Attendance> findByCourseIdAndDate(Long courseId, LocalDate date);

    List<Attendance> findByCourseIdAndDateAndType(Long courseId, LocalDate date, AttendanceType type);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.id = :studentId AND a.course.id = :courseId")
    long countByStudentIdAndCourseId(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.id = :studentId AND a.course.id = :courseId " +
           "AND a.status = :status")
    long countByStudentIdAndCourseIdAndStatus(@Param("studentId") Long studentId,
                                               @Param("courseId") Long courseId,
                                               @Param("status") AttendanceStatus status);

    boolean existsByStudentIdAndCourseIdAndDateAndType(Long studentId, Long courseId, LocalDate date, AttendanceType type);
}
