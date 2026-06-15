package com.cms.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.FeeDemand;
import com.cms.model.enums.DemandStatus;

public interface FeeDemandRepository extends JpaRepository<FeeDemand, Long> {

    List<FeeDemand> findByTermInstanceId(Long termInstanceId);

    /** Bulk-load all demands for a set of students — JOIN FETCH avoids N+1 when accessing ste.student. */
    @Query("SELECT d FROM FeeDemand d JOIN FETCH d.studentTermEnrollment ste JOIN FETCH ste.student WHERE ste.student.id IN :studentIds")
    List<FeeDemand> findByStudentIdIn(@Param("studentIds") Collection<Long> studentIds);

    Optional<FeeDemand> findByStudentTermEnrollmentId(Long enrollmentId);

    List<FeeDemand> findByTermInstanceIdAndStatus(Long termInstanceId, DemandStatus status);

    List<FeeDemand> findByTermInstanceIdAndStatusNot(Long termInstanceId, DemandStatus status);

    List<FeeDemand> findByAcademicYearId(Long academicYearId);

    List<FeeDemand> findByStudentTermEnrollmentStudentId(Long studentId);

    @Query("SELECT COALESCE(SUM(d.paidAmount), 0) FROM FeeDemand d " +
           "JOIN d.studentTermEnrollment ste WHERE ste.student.id = :studentId")
    BigDecimal sumPaidAmountByStudentId(@Param("studentId") Long studentId);
}
