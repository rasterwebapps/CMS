package com.cms.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.Enquiry;
import com.cms.model.enums.AdmissionQuota;
import com.cms.model.enums.CommissionPaymentStatus;
import com.cms.model.enums.CommissionSource;
import com.cms.model.enums.EnquiryStatus;
import com.cms.model.enums.Gender;

import jakarta.persistence.LockModeType;

public interface EnquiryRepository extends JpaRepository<Enquiry, Long>, JpaSpecificationExecutor<Enquiry> {

    List<Enquiry> findByStatus(EnquiryStatus status);

    List<Enquiry> findByStatusIn(Collection<EnquiryStatus> statuses);

    List<Enquiry> findByReferralTypeId(Long referralTypeId);

    boolean existsByReferralTypeId(Long referralTypeId);

    List<Enquiry> findByAgentId(Long agentId);

    boolean existsByAgentId(Long agentId);

    @Query("SELECT e FROM Enquiry e LEFT JOIN FETCH e.academicYear WHERE e.id IN :ids")
    List<Enquiry> findByIdInWithAcademicYear(@Param("ids") Collection<Long> ids);

    Optional<Enquiry> findByConvertedStudentId(Long studentId);

    List<Enquiry> findByConvertedStudentIdIn(Collection<Long> studentIds);

    /** Acquires a row-level write lock — use only inside a write @Transactional. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Enquiry e WHERE e.id = :id")
    Optional<Enquiry> findByIdForUpdate(@Param("id") Long id);

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

    @Query("""
        SELECT e FROM Enquiry e
        WHERE e.commissionAmount IS NOT NULL AND e.commissionAmount > 0
          AND (:status IS NULL OR e.commissionPaymentStatus = :status)
          AND (:source IS NULL OR e.commissionSource = :source)
          AND (:referralTypeId IS NULL OR e.referralType.id = :referralTypeId)
          AND (:agentId IS NULL OR e.agent.id = :agentId)
          AND (:fromDate IS NULL OR e.enquiryDate >= :fromDate)
          AND (:toDate IS NULL OR e.enquiryDate <= :toDate)
          AND (:search IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
        ORDER BY e.updatedAt DESC
        """)
    List<Enquiry> findCommissions(
            @Param("status") CommissionPaymentStatus status,
            @Param("source") CommissionSource source,
            @Param("referralTypeId") Long referralTypeId,
            @Param("agentId") Long agentId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("search") String search);

    @Query(value = """
        SELECT e FROM Enquiry e
        WHERE e.commissionAmount IS NOT NULL AND e.commissionAmount > 0
          AND (:status IS NULL OR e.commissionPaymentStatus = :status)
          AND (:source IS NULL OR e.commissionSource = :source)
          AND (:referralTypeId IS NULL OR e.referralType.id = :referralTypeId)
          AND (:agentId IS NULL OR e.agent.id = :agentId)
          AND (:fromDate IS NULL OR e.enquiryDate >= :fromDate)
          AND (:toDate IS NULL OR e.enquiryDate <= :toDate)
          AND (:search IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
        """,
        countQuery = """
        SELECT COUNT(e) FROM Enquiry e
        WHERE e.commissionAmount IS NOT NULL AND e.commissionAmount > 0
          AND (:status IS NULL OR e.commissionPaymentStatus = :status)
          AND (:source IS NULL OR e.commissionSource = :source)
          AND (:referralTypeId IS NULL OR e.referralType.id = :referralTypeId)
          AND (:agentId IS NULL OR e.agent.id = :agentId)
          AND (:fromDate IS NULL OR e.enquiryDate >= :fromDate)
          AND (:toDate IS NULL OR e.enquiryDate <= :toDate)
          AND (:search IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
        """)
    Page<Enquiry> findCommissionsPage(
            @Param("status") CommissionPaymentStatus status,
            @Param("source") CommissionSource source,
            @Param("referralTypeId") Long referralTypeId,
            @Param("agentId") Long agentId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT COUNT(e) FROM Enquiry e WHERE e.program.id = :programId AND e.admissionQuota = :quota AND e.feeState.id = :feeStateId AND e.gender = :gender AND e.status IN :statuses")
    long countByFeeGroupParams(
            @Param("programId") Long programId,
            @Param("quota") AdmissionQuota quota,
            @Param("feeStateId") Long feeStateId,
            @Param("gender") Gender gender,
            @Param("statuses") Collection<EnquiryStatus> statuses);

    @Query("SELECT e.status, COUNT(e) FROM Enquiry e GROUP BY e.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT COALESCE(SUM(e.finalizedNetFee), 0) FROM Enquiry e WHERE e.finalizedNetFee IS NOT NULL")
    BigDecimal sumFinalizedNetFee();

    @Query(value = """
        SELECT COUNT(*) FROM (
            SELECT e.id
            FROM enquiries e
            LEFT JOIN enquiry_payments ep ON ep.enquiry_id = e.id AND ep.refunded_at IS NULL
            LEFT JOIN student_fee_allocations sfa ON sfa.student_id = e.converted_student_id
            WHERE e.status IN ('FEES_FINALIZED','PARTIALLY_PAID','FEES_PAID','DOCUMENTS_SUBMITTED','DOCUMENTS_VERIFIED','ADMITTED')
              AND e.finalized_net_fee IS NOT NULL
              AND (e.converted_student_id IS NULL OR sfa.id IS NULL)
            GROUP BY e.id, e.finalized_net_fee
            HAVING e.finalized_net_fee > COALESCE(SUM(ep.amount_paid), 0)
        ) x
        """, nativeQuery = true)
    long countPaymentEligibleWithOutstanding();
}
