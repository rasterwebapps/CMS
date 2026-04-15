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

    List<Attendance> findBySubjectId(Long subjectId);

    List<Attendance> findByStudentIdAndSubjectId(Long studentId, Long subjectId);

    List<Attendance> findByStudentIdAndDateBetween(Long studentId, LocalDate startDate, LocalDate endDate);

    List<Attendance> findBySubjectIdAndDate(Long subjectId, LocalDate date);

    List<Attendance> findBySubjectIdAndDateAndType(Long subjectId, LocalDate date, AttendanceType type);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.id = :studentId AND a.subject.id = :subjectId")
    long countByStudentIdAndSubjectId(@Param("studentId") Long studentId, @Param("subjectId") Long subjectId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.id = :studentId AND a.subject.id = :subjectId " +
           "AND a.status = :status")
    long countByStudentIdAndSubjectIdAndStatus(@Param("studentId") Long studentId,
                                               @Param("subjectId") Long subjectId,
                                               @Param("status") AttendanceStatus status);

    boolean existsByStudentIdAndSubjectIdAndDateAndType(Long studentId, Long subjectId, LocalDate date, AttendanceType type);
}
