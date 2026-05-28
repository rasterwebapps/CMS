package com.cms.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.Enquiry;
import com.cms.model.enums.EnquiryStatus;

public interface EnquiryRepository extends JpaRepository<Enquiry, Long> {

    List<Enquiry> findByStatus(EnquiryStatus status);

    List<Enquiry> findByStatusIn(Collection<EnquiryStatus> statuses);

    List<Enquiry> findByReferralTypeId(Long referralTypeId);

    List<Enquiry> findByAgentId(Long agentId);

    Optional<Enquiry> findByConvertedStudentId(Long studentId);

    List<Enquiry> findByEnquiryDateBetween(LocalDate fromDate, LocalDate toDate);

    List<Enquiry> findByEnquiryDateBetweenAndStatus(LocalDate fromDate, LocalDate toDate, EnquiryStatus status);

    /**
     * Returns all enquiries that either:
     *   (a) were created within the given date range, OR
     *   (b) are still in an active pipeline status (not yet admitted/closed/not-interested).
     *
     * This prevents pipeline enquiries from disappearing from the Enquiry List
     * when the date range rolls over to a new month.
     */
    @Query("SELECT e FROM Enquiry e WHERE e.enquiryDate BETWEEN :fromDate AND :toDate OR e.status NOT IN :terminalStatuses")
    List<Enquiry> findByDateRangeOrActivePipeline(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("terminalStatuses") Collection<EnquiryStatus> terminalStatuses);
}
