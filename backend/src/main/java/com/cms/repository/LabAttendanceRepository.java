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

    List<LabAttendance> findBySubjectId(Long subjectId);

    List<LabAttendance> findByLabId(Long labId);

    List<LabAttendance> findByLabBatch(String labBatch);

    List<LabAttendance> findByStudentIdAndSubjectId(Long studentId, Long subjectId);

    List<LabAttendance> findBySubjectIdAndDate(Long subjectId, LocalDate date);

    List<LabAttendance> findByLabIdAndDate(Long labId, LocalDate date);

    List<LabAttendance> findByLabBatchAndDate(String labBatch, LocalDate date);

    @Query("SELECT COUNT(la) FROM LabAttendance la WHERE la.student.id = :studentId AND la.subject.id = :subjectId")
    long countByStudentIdAndSubjectId(@Param("studentId") Long studentId, @Param("subjectId") Long subjectId);

    @Query("SELECT COUNT(la) FROM LabAttendance la WHERE la.student.id = :studentId AND la.subject.id = :subjectId " +
           "AND la.status = :status")
    long countByStudentIdAndSubjectIdAndStatus(@Param("studentId") Long studentId,
                                               @Param("subjectId") Long subjectId,
                                               @Param("status") AttendanceStatus status);

    boolean existsByStudentIdAndSubjectIdAndDate(Long studentId, Long subjectId, LocalDate date);
}
