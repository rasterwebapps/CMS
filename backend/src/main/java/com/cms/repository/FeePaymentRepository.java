package com.cms.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.FeePayment;
import com.cms.model.enums.PaymentStatus;

public interface FeePaymentRepository extends JpaRepository<FeePayment, Long> {

    List<FeePayment> findByStudentId(Long studentId);

    boolean existsByFeeStructureId(Long feeStructureId);

    List<FeePayment> findByFeeStructureId(Long feeStructureId);

    List<FeePayment> findByStudentIdAndFeeStructureId(Long studentId, Long feeStructureId);

    List<FeePayment> findByStatus(PaymentStatus status);

    List<FeePayment> findByPaymentDateBetween(LocalDate startDate, LocalDate endDate);

    Optional<FeePayment> findByReceiptNumber(String receiptNumber);

    @Query("SELECT COALESCE(SUM(fp.amountPaid), 0) FROM FeePayment fp " +
           "WHERE fp.student.id = :studentId AND fp.feeStructure.id = :feeStructureId")
    java.math.BigDecimal sumAmountPaidByStudentIdAndFeeStructureId(
        @Param("studentId") Long studentId,
        @Param("feeStructureId") Long feeStructureId);

    @Query("SELECT fp FROM FeePayment fp " +
           "WHERE fp.student.id = :studentId AND fp.feeStructure.academicYear.id = :academicYearId")
    List<FeePayment> findByStudentIdAndAcademicYearId(
        @Param("studentId") Long studentId,
        @Param("academicYearId") Long academicYearId);
}
