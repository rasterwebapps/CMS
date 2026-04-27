package com.cms.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.FeeInstallment;

public interface FeeInstallmentRepository extends JpaRepository<FeeInstallment, Long> {

    List<FeeInstallment> findBySemesterFeeId(Long semesterFeeId);

    List<FeeInstallment> findByStudentId(Long studentId);

    List<FeeInstallment> findByReceiptNumber(String receiptNumber);

    @Query("SELECT COALESCE(SUM(fi.amountPaid), 0) FROM FeeInstallment fi " +
           "WHERE fi.semesterFee.id = :semesterFeeId")
    BigDecimal sumAmountPaidBySemesterFeeId(@Param("semesterFeeId") Long semesterFeeId);

    @Query("SELECT fi FROM FeeInstallment fi WHERE fi.student.id = :studentId " +
           "ORDER BY fi.paymentDate DESC, fi.id DESC")
    List<FeeInstallment> findByStudentIdOrderByPaymentDateDesc(@Param("studentId") Long studentId);
}
