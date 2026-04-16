package com.cms.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.Enquiry;
import com.cms.model.enums.EnquiryStatus;

public interface EnquiryRepository extends JpaRepository<Enquiry, Long> {

    List<Enquiry> findByStatus(EnquiryStatus status);

    List<Enquiry> findByReferralTypeId(Long referralTypeId);

    List<Enquiry> findByAgentId(Long agentId);

    Optional<Enquiry> findByConvertedStudentId(Long studentId);

    List<Enquiry> findByEnquiryDateBetween(LocalDate fromDate, LocalDate toDate);

    List<Enquiry> findByEnquiryDateBetweenAndStatus(LocalDate fromDate, LocalDate toDate, EnquiryStatus status);
}
