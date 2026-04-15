package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.Enquiry;
import com.cms.model.enums.EnquirySource;
import com.cms.model.enums.EnquiryStatus;

public interface EnquiryRepository extends JpaRepository<Enquiry, Long> {

    List<Enquiry> findByStatus(EnquiryStatus status);

    List<Enquiry> findBySource(EnquirySource source);

    List<Enquiry> findByAgentId(Long agentId);

    Optional<Enquiry> findByConvertedStudentId(Long studentId);
}
