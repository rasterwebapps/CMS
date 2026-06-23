package com.cms.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.config.PermSecurityBean;
import com.cms.dto.CommissionExplorerResponse;
import com.cms.dto.CommissionPayoutRequest;
import com.cms.dto.CommissionPayoutResponse;
import com.cms.model.CommissionPayout;
import com.cms.model.Enquiry;
import com.cms.model.OneBookPaymentRequest;
import com.cms.model.enums.CommissionPaymentStatus;
import com.cms.model.enums.CommissionSource;
import com.cms.model.enums.PaymentMode;
import com.cms.repository.CommissionPayoutRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.OneBookPaymentRequestRepository;
import com.cms.repository.StaffReferrerRepository;
import com.cms.repository.StudentRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional(readOnly = true)
public class CommissionExplorerService {

    private final EnquiryRepository enquiryRepository;
    private final CommissionPayoutRepository payoutRepository;
    private final OneBookPaymentRequestRepository obRepo;
    private final OneBookIntegrationService obService;
    private final StaffReferrerRepository staffReferrerRepository;
    private final FacultyRepository facultyRepository;
    private final StudentRepository studentRepository;
    private final OneBookConfigService oneBookConfigService;
    private final PermSecurityBean permSecurityBean;

    public CommissionExplorerService(
            EnquiryRepository enquiryRepository,
            CommissionPayoutRepository payoutRepository,
            OneBookPaymentRequestRepository obRepo,
            OneBookIntegrationService obService,
            StaffReferrerRepository staffReferrerRepository,
            FacultyRepository facultyRepository,
            StudentRepository studentRepository,
            OneBookConfigService oneBookConfigService,
            PermSecurityBean permSecurityBean) {
        this.enquiryRepository = enquiryRepository;
        this.payoutRepository = payoutRepository;
        this.obRepo = obRepo;
        this.obService = obService;
        this.staffReferrerRepository = staffReferrerRepository;
        this.facultyRepository = facultyRepository;
        this.studentRepository = studentRepository;
        this.oneBookConfigService = oneBookConfigService;
        this.permSecurityBean = permSecurityBean;
    }

    public List<CommissionExplorerResponse> findAll(
            String status,
            String source,
            Long referralTypeId,
            Long agentId,
            LocalDate fromDate,
            LocalDate toDate,
            String search) {

        CommissionPaymentStatus statusEnum = status != null ? CommissionPaymentStatus.valueOf(status) : null;
        CommissionSource sourceEnum = source != null ? CommissionSource.valueOf(source) : null;
        String searchTrim = (search != null && !search.isBlank()) ? search.trim() : null;

        List<Enquiry> enquiries = enquiryRepository.findCommissions(
                statusEnum, sourceEnum, referralTypeId, agentId, fromDate, toDate, searchTrim);

        // A settle-only user (no full view/manage rights) must not see commissions
        // that haven't been approved yet — only rows ready for cash/other-mode settlement.
        if (!permSecurityBean.hasAny("COMMISSION_VIEW", "COMMISSION_MANAGE")) {
            enquiries = enquiries.stream()
                    .filter(e -> e.getCommissionPaymentStatus() == CommissionPaymentStatus.PAYMENT_REQUESTED
                              || e.getCommissionPaymentStatus() == CommissionPaymentStatus.PARTIAL)
                    .toList();
        }

        return enquiries.stream().map(this::toResponse).toList();
    }

    /**
     * Admin/manager approves a PENDING commission (or retries a FAILED OneBook
     * transmission). If OneBook is enabled the payment is transmitted immediately;
     * otherwise it moves to PAYMENT_REQUESTED (awaiting cash/other-mode settlement
     * via {@link #recordPayout}).
     */
    @Transactional
    public CommissionExplorerResponse approve(Long enquiryId, String approvedBy) {
        Enquiry enquiry = enquiryRepository.findById(enquiryId)
                .orElseThrow(() -> new EntityNotFoundException("Enquiry not found: " + enquiryId));

        CommissionPaymentStatus currentStatus = enquiry.getCommissionPaymentStatus();
        if (currentStatus != CommissionPaymentStatus.PENDING
                && currentStatus != CommissionPaymentStatus.FAILED) {
            throw new IllegalStateException(
                    "Commission can only be approved from PENDING (or retried from FAILED). Current: "
                    + currentStatus);
        }

        if (oneBookConfigService.isEnabled()) {
            obService.pushCommissionPayment(enquiryId, approvedBy);
        } else {
            enquiry.setCommissionPaymentStatus(CommissionPaymentStatus.PAYMENT_REQUESTED);
            enquiryRepository.save(enquiry);
        }
        return toResponse(enquiry);
    }

    @Transactional
    public CommissionExplorerResponse reject(Long enquiryId, String reason, String rejectedBy) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A rejection reason is required.");
        }

        Enquiry enquiry = enquiryRepository.findById(enquiryId)
                .orElseThrow(() -> new EntityNotFoundException("Enquiry not found: " + enquiryId));

        if (enquiry.getCommissionPaymentStatus() != CommissionPaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Commission can only be rejected from PENDING. Current: "
                    + enquiry.getCommissionPaymentStatus());
        }

        enquiry.setCommissionPaymentStatus(CommissionPaymentStatus.REJECTED);
        enquiry.setCommissionRejectionReason(reason.trim());
        enquiry.setCommissionRejectedBy(rejectedBy);
        enquiry.setCommissionRejectedAt(Instant.now());
        enquiryRepository.save(enquiry);
        return toResponse(enquiry);
    }

    @Transactional
    public CommissionExplorerResponse reopen(Long enquiryId) {
        Enquiry enquiry = enquiryRepository.findById(enquiryId)
                .orElseThrow(() -> new EntityNotFoundException("Enquiry not found: " + enquiryId));

        if (enquiry.getCommissionPaymentStatus() != CommissionPaymentStatus.REJECTED) {
            throw new IllegalStateException(
                    "Only a rejected commission can be reopened. Current: "
                    + enquiry.getCommissionPaymentStatus());
        }

        enquiry.setCommissionPaymentStatus(CommissionPaymentStatus.PENDING);
        enquiryRepository.save(enquiry);
        return toResponse(enquiry);
    }

    @Transactional
    public CommissionExplorerResponse recordPayout(Long enquiryId, CommissionPayoutRequest request, String paidBy) {
        Enquiry enquiry = enquiryRepository.findById(enquiryId)
                .orElseThrow(() -> new EntityNotFoundException("Enquiry not found: " + enquiryId));

        CommissionPaymentStatus currentStatus = enquiry.getCommissionPaymentStatus();
        if (currentStatus != CommissionPaymentStatus.PAYMENT_REQUESTED
                && currentStatus != CommissionPaymentStatus.PARTIAL) {
            throw new IllegalStateException(
                    "Payout can only be recorded once a commission is approved (PAYMENT_REQUESTED) "
                    + "or partially paid. Current: " + currentStatus);
        }

        PaymentMode mode;
        try {
            mode = PaymentMode.valueOf(request.paymentMode().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid payment mode: " + request.paymentMode());
        }

        CommissionPayout payout = new CommissionPayout();
        payout.setEnquiry(enquiry);
        payout.setAmount(request.amount());
        payout.setPayoutDate(request.payoutDate());
        payout.setPaymentMode(mode);
        payout.setTransactionReference(
                request.transactionReference() != null ? request.transactionReference().trim() : null);
        payout.setRemarks(request.remarks() != null ? request.remarks().trim() : null);
        payout.setPaidBy(paidBy);

        // Link to the referral source
        CommissionSource commissionSource = enquiry.getCommissionSource();
        if (commissionSource == CommissionSource.AGENT && enquiry.getAgent() != null) {
            payout.setAgent(enquiry.getAgent());
        } else if (commissionSource == CommissionSource.STAFF_REFERRER && enquiry.getReferredStaffId() != null) {
            staffReferrerRepository.findById(enquiry.getReferredStaffId()).ifPresent(payout::setStaffReferrer);
        } else if (commissionSource == CommissionSource.FACULTY_REFERRER && enquiry.getReferredFacultyId() != null) {
            facultyRepository.findById(enquiry.getReferredFacultyId()).ifPresent(payout::setFaculty);
        }

        payoutRepository.save(payout);

        // Recalculate paid amount and update status
        BigDecimal totalPaid = payoutRepository.sumAmountByEnquiryId(enquiryId);
        enquiry.setCommissionPaidAmount(totalPaid);

        BigDecimal commissionDue = enquiry.getCommissionAmount() != null
                ? enquiry.getCommissionAmount() : BigDecimal.ZERO;

        if (totalPaid.compareTo(commissionDue) >= 0) {
            enquiry.setCommissionPaymentStatus(CommissionPaymentStatus.PAID);
        } else {
            enquiry.setCommissionPaymentStatus(CommissionPaymentStatus.PARTIAL);
        }

        enquiryRepository.save(enquiry);
        return toResponse(enquiry);
    }

    private CommissionExplorerResponse toResponse(Enquiry e) {
        List<CommissionPayoutResponse> payouts = payoutRepository
                .findByEnquiryIdOrderByPayoutDateDesc(e.getId())
                .stream()
                .map(p -> new CommissionPayoutResponse(
                        p.getId(),
                        p.getAmount(),
                        p.getPayoutDate(),
                        p.getPaymentMode() != null ? p.getPaymentMode().name() : null,
                        p.getTransactionReference(),
                        p.getRemarks(),
                        p.getPaidBy(),
                        p.getCreatedAt()))
                .toList();

        BigDecimal commissionAmount = e.getCommissionAmount() != null ? e.getCommissionAmount() : BigDecimal.ZERO;
        BigDecimal paidAmount = e.getCommissionPaidAmount() != null ? e.getCommissionPaidAmount() : BigDecimal.ZERO;
        BigDecimal outstanding = commissionAmount.subtract(paidAmount).max(BigDecimal.ZERO);

        String agentName = e.getAgent() != null ? e.getAgent().getName() : null;
        String staffName = resolveStaffName(e.getReferredStaffId());
        String facultyName = resolveFacultyName(e.getReferredFacultyId());
        String admissionNumber = e.getConvertedStudentId() != null
                ? studentRepository.findById(e.getConvertedStudentId())
                        .map(s -> s.getAdmissionNumber()).orElse(null)
                : null;

        // OneBook tracking — most recent request for this enquiry
        OneBookPaymentRequest latestOb = obRepo
                .findTopByEntityIdAndPaymentTypeOrderByCreatedAtDesc(e.getId(), "COMMISSION")
                .orElse(null);

        return new CommissionExplorerResponse(
                e.getId(),
                e.getName(),
                admissionNumber,
                e.getStatus() != null ? e.getStatus().name() : null,
                e.getProgram() != null ? e.getProgram().getName() : null,
                e.getCourse() != null ? e.getCourse().getName() : null,
                e.getEnquiryDate() != null ? e.getEnquiryDate().toString() : null,
                e.getReferralType() != null ? e.getReferralType().getId() : null,
                e.getReferralType() != null ? e.getReferralType().getName() : null,
                e.getCommissionSource() != null ? e.getCommissionSource().name() : null,
                e.getAgent() != null ? e.getAgent().getId() : null,
                agentName,
                e.getReferredStaffId(),
                staffName,
                e.getReferredFacultyId(),
                facultyName,
                commissionAmount,
                paidAmount,
                outstanding,
                e.getCommissionPaymentStatus() != null ? e.getCommissionPaymentStatus().name() : null,
                payouts,
                latestOb != null ? latestOb.getReferenceId() : null,
                latestOb != null ? latestOb.getStatus() : null,
                latestOb != null ? latestOb.getTransmittedAt() : null,
                latestOb != null ? latestOb.getOnebookTxnId() : null,
                e.getCommissionRejectionReason(),
                e.getCommissionRejectedBy(),
                e.getCommissionRejectedAt());
    }

    private String resolveStaffName(Long staffId) {
        if (staffId == null) return null;
        return staffReferrerRepository.findById(staffId).map(s -> s.getName()).orElse(null);
    }

    private String resolveFacultyName(Long facultyId) {
        if (facultyId == null) return null;
        return facultyRepository.findById(facultyId)
                .map(f -> {
                    String first = f.getFirstName() != null ? f.getFirstName() : "";
                    String last = f.getLastName() != null ? f.getLastName() : "";
                    return (first + " " + last).trim();
                })
                .filter(name -> !name.isBlank())
                .orElse(null);
    }
}
