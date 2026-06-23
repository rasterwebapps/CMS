package com.cms.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.cms.dto.DisbursementRequest;
import com.cms.model.Enquiry;
import com.cms.model.FeeRefund;
import com.cms.model.OneBookPaymentRequest;
import com.cms.model.StudentScholarship;
import com.cms.model.enums.CommissionPaymentStatus;
import com.cms.model.enums.CommissionSource;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.FeeRefundRepository;
import com.cms.repository.OneBookPaymentRequestRepository;
import com.cms.repository.StaffReferrerRepository;
import com.cms.repository.StudentRepository;
import com.cms.repository.StudentScholarshipRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;

@Service
public class OneBookIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(OneBookIntegrationService.class);

    private static final DateTimeFormatter ONEBOOK_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final OneBookConfigService config;
    private final OneBookPaymentRequestRepository obRepo;
    private final EnquiryRepository enquiryRepo;
    private final StaffReferrerRepository staffRepo;
    private final FacultyRepository facultyRepo;
    private final FeeRefundRepository refundRepo;
    private final StudentRepository studentRepo;
    private final StudentScholarshipRepository scholarshipRepo;
    private final ApplicationNumberSequenceService numberSequenceService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    public OneBookIntegrationService(
            OneBookConfigService config,
            OneBookPaymentRequestRepository obRepo,
            EnquiryRepository enquiryRepo,
            StaffReferrerRepository staffRepo,
            FacultyRepository facultyRepo,
            FeeRefundRepository refundRepo,
            StudentRepository studentRepo,
            StudentScholarshipRepository scholarshipRepo,
            ApplicationNumberSequenceService numberSequenceService,
            ObjectMapper objectMapper) {
        this.config = config;
        this.obRepo = obRepo;
        this.enquiryRepo = enquiryRepo;
        this.staffRepo = staffRepo;
        this.facultyRepo = facultyRepo;
        this.refundRepo = refundRepo;
        this.studentRepo = studentRepo;
        this.scholarshipRepo = scholarshipRepo;
        this.numberSequenceService = numberSequenceService;
        this.objectMapper = objectMapper;
    }

    /**
     * Transmits a commission payment for the given enquiry to OneBook.
     * Creates an onebook_payment_requests row, calls the API, and updates
     * the enquiry's commission status to TRANSMITTED or FAILED.
     */
    @Transactional
    public OneBookPaymentRequest pushCommissionPayment(Long enquiryId, String approvedBy) {
        Enquiry enquiry = enquiryRepo.findById(enquiryId)
                .orElseThrow(() -> new EntityNotFoundException("Enquiry not found: " + enquiryId));

        CommissionPaymentStatus currentStatus = enquiry.getCommissionPaymentStatus();
        if (currentStatus != CommissionPaymentStatus.PENDING
                && currentStatus != CommissionPaymentStatus.PAYMENT_REQUESTED
                && currentStatus != CommissionPaymentStatus.FAILED) {
            throw new IllegalStateException(
                    "Cannot transmit to OneBook from status: " + currentStatus);
        }

        assertIntegrationReady();

        // Resolve recipient bank details from referral source
        RecipientDetails recipient = resolveRecipient(enquiry);
        assertBankDetails(
                "Commission recipient for enquiry #" + enquiryId,
                recipient.name(), recipient.accountNumber(), recipient.bankName(), recipient.ifsc());

        OneBookPaymentRequest obRequest = new OneBookPaymentRequest();
        obRequest.setReferenceId(generateReferenceId());
        obRequest.setInvoiceNumber(numberSequenceService.nextCommissionNumber(LocalDate.now().getYear()));
        obRequest.setPaymentType("COMMISSION");
        obRequest.setEntityId(enquiryId);
        obRequest.setEntityTable("enquiries");
        obRequest.setRecipientName(recipient.name());
        obRequest.setRecipientAccount(recipient.accountNumber());
        obRequest.setRecipientIfsc(recipient.ifsc());
        obRequest.setRecipientBankName(recipient.bankName());
        obRequest.setAmount(enquiry.getCommissionAmount());
        obRequest.setStatus("PENDING");
        obRequest.setApprovedBy(approvedBy);
        obRequest.setApprovedAt(Instant.now());
        obRequest = obRepo.save(obRequest);

        Map<String, Object> payload = buildPaymentRegisterPayload(
                obRequest, recipient.id(), recipient.name(), "PAYMENT");

        try {
            createPaymentRegister(payload);
            obRequest.setStatus("TRANSMITTED");
            obRequest.setTransmittedAt(Instant.now());
            enquiry.setCommissionPaymentStatus(CommissionPaymentStatus.TRANSMITTED);
            log.info("Commission payment register sent to OneBook. invoiceNumber={} enquiry={}",
                    obRequest.getInvoiceNumber(), enquiryId);
        } catch (RestClientException e) {
            log.error("Failed to push commission payment to OneBook. invoiceNumber={} error={}",
                    obRequest.getInvoiceNumber(), e.getMessage());
            obRequest.setStatus("FAILED");
            obRequest.setErrorMessage("OneBook API call failed: " + e.getMessage());
            enquiry.setCommissionPaymentStatus(CommissionPaymentStatus.FAILED);
        }

        obRepo.save(obRequest);
        enquiryRepo.save(enquiry);
        return obRequest;
    }

    // ── Refund push ───────────────────────────────────────────────────────────

    @Transactional
    public OneBookPaymentRequest pushRefundPayment(Long refundId, String approvedBy) {
        FeeRefund refund = refundRepo.findById(refundId)
                .orElseThrow(() -> new EntityNotFoundException("Refund not found: " + refundId));

        if (!"PENDING".equals(refund.getStatus()) && !"PAYMENT_FAILED".equals(refund.getStatus())) {
            throw new IllegalStateException(
                    "Cannot transmit refund to OneBook from status: " + refund.getStatus());
        }
        assertIntegrationReady();

        if ("ENQUIRY".equals(refund.getEntityType())) {
            throw new IllegalStateException(
                    "Enquiry refunds cannot be pushed to OneBook — bank details are not stored for enquiry records. Process this refund manually.");
        }

        com.cms.model.Student student = refund.getStudentId() != null
                ? studentRepo.findById(refund.getStudentId()).orElse(null) : null;

        assertBankDetails(
                "Student for refund #" + refundId,
                refund.getStudentName(),
                student != null ? student.getBankAccountNumber() : null,
                student != null ? student.getBankName() : null,
                student != null ? student.getBankIfscCode() : null);

        OneBookPaymentRequest obRequest = new OneBookPaymentRequest();
        obRequest.setReferenceId(generateReferenceId());
        obRequest.setInvoiceNumber(numberSequenceService.nextRefundNumber(LocalDate.now().getYear()));
        obRequest.setPaymentType("REFUND");
        obRequest.setEntityId(refundId);
        obRequest.setEntityTable("fee_refunds");
        obRequest.setRecipientName(refund.getStudentName());
        if (student != null) {
            obRequest.setRecipientAccount(student.getBankAccountNumber());
            obRequest.setRecipientIfsc(student.getBankIfscCode());
            obRequest.setRecipientBankName(student.getBankName());
        }
        obRequest.setAmount(refund.getRefundAmount());
        obRequest.setStatus("PENDING");
        obRequest.setApprovedBy(approvedBy);
        obRequest.setApprovedAt(Instant.now());
        obRequest = obRepo.save(obRequest);

        Map<String, Object> payload = buildPaymentRegisterPayload(
                obRequest, student != null ? student.getId() : null, refund.getStudentName(), "REFUND");

        try {
            createPaymentRegister(payload);
            obRequest.setStatus("TRANSMITTED");
            obRequest.setTransmittedAt(Instant.now());
            log.info("Refund payment register sent to OneBook. invoiceNumber={} refund={}",
                    obRequest.getInvoiceNumber(), refundId);
        } catch (RestClientException e) {
            log.error("Failed to push refund to OneBook. invoiceNumber={} error={}",
                    obRequest.getInvoiceNumber(), e.getMessage());
            obRequest.setStatus("FAILED");
            obRequest.setErrorMessage("OneBook API call failed: " + e.getMessage());
            refund.setStatus("PAYMENT_FAILED");
        }

        obRepo.save(obRequest);
        refundRepo.save(refund);
        return obRequest;
    }

    // ── Scholarship disbursement push ─────────────────────────────────────────

    @Transactional
    public OneBookPaymentRequest pushScholarshipPayment(Long scholarshipId, DisbursementRequest request, String disbursedBy) {
        StudentScholarship scholarship = scholarshipRepo.findById(scholarshipId)
                .orElseThrow(() -> new EntityNotFoundException("Scholarship application not found: " + scholarshipId));

        if (scholarship.getStatus() != com.cms.model.enums.ScholarshipStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED scholarship applications can be disbursed via OneBook");
        }
        assertIntegrationReady();

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("academicYearId", request.academicYearId() != null
                ? request.academicYearId() : scholarship.getAcademicYear().getId());
        meta.put("termNumber", request.termNumber());
        meta.put("remarks", request.remarks());

        com.cms.model.Student student = scholarship.getStudent();
        String studentName = student.getFullName();

        assertBankDetails(
                "Student for scholarship #" + scholarshipId,
                studentName, student.getBankAccountNumber(), student.getBankName(), student.getBankIfscCode());

        OneBookPaymentRequest obRequest = new OneBookPaymentRequest();
        obRequest.setReferenceId(generateReferenceId());
        obRequest.setInvoiceNumber(numberSequenceService.nextDisbursementNumber(LocalDate.now().getYear()));
        obRequest.setPaymentType("SCHOLARSHIP");
        obRequest.setEntityId(scholarshipId);
        obRequest.setEntityTable("student_scholarships");
        obRequest.setRecipientName(studentName);
        obRequest.setRecipientAccount(student.getBankAccountNumber());
        obRequest.setRecipientIfsc(student.getBankIfscCode());
        obRequest.setRecipientBankName(student.getBankName());
        obRequest.setAmount(request.amount());
        obRequest.setStatus("PENDING");
        obRequest.setApprovedBy(disbursedBy);
        obRequest.setApprovedAt(Instant.now());
        try { obRequest.setRequestMetadata(objectMapper.writeValueAsString(meta)); }
        catch (JsonProcessingException ignored) {}
        obRequest = obRepo.save(obRequest);

        Map<String, Object> payload = buildPaymentRegisterPayload(
                obRequest, student.getId(), studentName, "PAYMENT");

        try {
            createPaymentRegister(payload);
            obRequest.setStatus("TRANSMITTED");
            obRequest.setTransmittedAt(Instant.now());
            log.info("Scholarship disbursement register sent to OneBook. invoiceNumber={} scholarshipId={}",
                    obRequest.getInvoiceNumber(), scholarshipId);
        } catch (RestClientException e) {
            log.error("Failed to push scholarship to OneBook. invoiceNumber={} error={}",
                    obRequest.getInvoiceNumber(), e.getMessage());
            obRequest.setStatus("FAILED");
            obRequest.setErrorMessage("OneBook API call failed: " + e.getMessage());
        }

        obRepo.save(obRequest);
        return obRequest;
    }

    // ── OneBook API calls ───────────────────────────────────────────────────────

    private void assertIntegrationReady() {
        if (!config.isEnabled()) {
            throw new IllegalStateException("OneBook integration is not enabled.");
        }
        String apiUrl = config.getApiUrl();
        if (apiUrl == null || apiUrl.isBlank()) {
            throw new IllegalStateException("OneBook API URL is not configured.");
        }
        if (config.getUsername() == null || config.getUsername().isBlank()) {
            throw new IllegalStateException("OneBook username is not configured.");
        }
    }

    /** Fetches a fresh JWT from OneBook's auth server. Not cached — a new token is requested for every push. */
    private String authenticate() {
        String endpoint = normalizeBase(config.getApiUrl()) + "authserver/api/auth";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("Username", config.getUsername());
        body.put("password", config.getPassword());
        body.put("branchId", config.getBranchId());
        body.put("organizationId", config.getOrgId());
        body.put("zoneName", config.getZoneName());

        Map<?, ?> response = restClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        Object token = response != null ? response.get("token") : null;
        if (token == null) {
            throw new IllegalStateException("OneBook authentication response did not contain a token");
        }
        return token.toString();
    }

    /**
     * Creates a payment register in OneBook. OneBook does not return the
     * assigned register id synchronously — it calls back into
     * /webhooks/onebook/posting-track-update with the id once created, and
     * /webhooks/onebook/posting-track-completion once the payment itself is
     * completed. A 2xx response here only confirms OneBook accepted the
     * register, not that it has been paid.
     */
    private void createPaymentRegister(Map<String, Object> registerPayload) {
        String token = authenticate();
        String endpoint = normalizeBase(config.getApiUrl()) + "one-book/api/payment-registers-add-from-other-applications";

        restClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .body(List.of(registerPayload))
                .retrieve()
                .toBodilessEntity();
    }

    private String normalizeBase(String apiUrl) {
        return apiUrl.endsWith("/") ? apiUrl : apiUrl + "/";
    }

    // ── Payload building ─────────────────────────────────────────────────────

    /**
     * Builds the payment-register payload per OneBook's real API contract.
     * payeeType is always OTHERS — commission/refund/scholarship recipients
     * (agents, staff, faculty, students) have no supplier-master equivalent
     * in OneBook, unlike OnePharmacy's supplier-purchase use case this
     * contract was originally documented for. sourcePayeeId/supplierId reuse
     * the recipient's own entity id since there's no supplier master to
     * reference. invoiceNumber/documentNumber are the same generated
     * refund/commission/disbursement number; documentId is the source
     * entity's own primary key (enquiry/refund/scholarship application id).
     */
    private Map<String, Object> buildPaymentRegisterPayload(
            OneBookPaymentRequest req, Long payeeId, String payeeName, String documentType) {
        String nowIso = LocalDate.now().atStartOfDay().format(ONEBOOK_DATETIME);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("applicationName", config.getAppName());
        payload.put("payerName", config.getPaperName());
        payload.put("payeeType", "OTHERS");
        payload.put("sourcePayeeId", payeeId);
        payload.put("payeeName", payeeName);
        payload.put("supplierId", payeeId);
        payload.put("invoiceNumber", req.getInvoiceNumber());
        payload.put("invoiceDate", nowIso);
        payload.put("paymentRegisterDocumentType", documentType);
        payload.put("documentId", req.getEntityId());
        payload.put("documentNumber", req.getInvoiceNumber());
        payload.put("dueDate", nowIso);
        payload.put("netBillAmount", req.getAmount());
        payload.put("payableAmount", req.getAmount());
        payload.put("paidAmount", BigDecimal.ZERO);
        payload.put("cancelled", false);
        payload.put("transactionType", "CREDIT");
        payload.put("invoiceFilePath", "");
        payload.put("branchId", config.getBranchId());
        payload.put("organizationId", config.getOrgId());
        payload.put("createdBy", req.getApprovedBy());
        payload.put("modifiedBy", req.getApprovedBy());
        return payload;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private RecipientDetails resolveRecipient(Enquiry enquiry) {
        CommissionSource source = enquiry.getCommissionSource();
        if (source == CommissionSource.AGENT && enquiry.getAgent() != null) {
            var a = enquiry.getAgent();
            return new RecipientDetails(a.getId(), a.getName(), a.getBankAccountNumber(),
                    a.getBankIfscCode(), a.getBankName());
        }
        if (source == CommissionSource.STAFF_REFERRER && enquiry.getReferredStaffId() != null) {
            return staffRepo.findById(enquiry.getReferredStaffId())
                    .map(s -> new RecipientDetails(s.getId(), s.getName(), s.getBankAccountNumber(),
                            s.getBankIfscCode(), s.getBankName()))
                    .orElse(RecipientDetails.UNKNOWN);
        }
        if (source == CommissionSource.FACULTY_REFERRER && enquiry.getReferredFacultyId() != null) {
            return facultyRepo.findById(enquiry.getReferredFacultyId())
                    .map(f -> new RecipientDetails(
                            f.getId(), f.getFirstName() + " " + f.getLastName(),
                            f.getBankAccountNumber(), f.getBankIfscCode(), f.getBankName()))
                    .orElse(RecipientDetails.UNKNOWN);
        }
        return RecipientDetails.UNKNOWN;
    }

    private void assertBankDetails(String entityLabel, String name, String accountNumber, String bankName, String ifsc) {
        StringBuilder missing = new StringBuilder();
        if (name == null || name.isBlank())          missing.append("name, ");
        if (accountNumber == null || accountNumber.isBlank()) missing.append("account number, ");
        if (bankName == null || bankName.isBlank())  missing.append("bank name, ");
        if (ifsc == null || ifsc.isBlank())          missing.append("IFSC code, ");
        if (!missing.isEmpty()) {
            String fields = missing.toString().replaceAll(", $", "");
            throw new IllegalStateException(
                    entityLabel + " is missing required bank details: " + fields +
                    ". Please add them via the profile edit screen before pushing to OneBook.");
        }
    }

    /** Internal-only correlation id for our own logs/audit — no longer sent to or matched against OneBook. */
    private String generateReferenceId() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "OB-" + date + "-" + unique;
    }

    record RecipientDetails(Long id, String name, String accountNumber, String ifsc, String bankName) {
        static final RecipientDetails UNKNOWN = new RecipientDetails(null, null, null, null, null);
    }
}
