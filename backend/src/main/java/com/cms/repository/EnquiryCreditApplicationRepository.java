package com.cms.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.EnquiryCreditApplication;

public interface EnquiryCreditApplicationRepository extends JpaRepository<EnquiryCreditApplication, Long> {

    @Query("SELECT a FROM EnquiryCreditApplication a WHERE a.student.id = :studentId ORDER BY a.appliedAt DESC")
    List<EnquiryCreditApplication> findByStudentIdOrderByAppliedAtDesc(@Param("studentId") Long studentId);

    @Query("SELECT a FROM EnquiryCreditApplication a WHERE a.enquiry.id = :enquiryId ORDER BY a.appliedAt DESC")
    List<EnquiryCreditApplication> findByEnquiryIdOrderByAppliedAtDesc(@Param("enquiryId") Long enquiryId);

    @Query("SELECT COALESCE(SUM(a.amountApplied), 0) FROM EnquiryCreditApplication a WHERE a.enquiry.id = :enquiryId")
    BigDecimal sumAmountAppliedByEnquiryId(@Param("enquiryId") Long enquiryId);

    @Query("SELECT COALESCE(SUM(a.amountApplied), 0) FROM EnquiryCreditApplication a WHERE a.enquiry.id = :enquiryId AND a.semesterFee.id = :semesterFeeId")
    BigDecimal sumAmountAppliedByEnquiryIdAndSemesterFeeId(@Param("enquiryId") Long enquiryId, @Param("semesterFeeId") Long semesterFeeId);
}
