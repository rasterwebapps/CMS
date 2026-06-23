package com.cms.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
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

    private final OneBookConfigService config;
    private final OneBookPaymentRequestRepository obRepo;
    private final EnquiryRepository enquiryRepo;
    private final StaffReferrerRepository staffRepo;
    private final FacultyRepository facultyRepo;
    private final FeeRefundRepository refundRepo;
    private final StudentRepository studentRepo;
    private final StudentScholarshipRepository scholarshipRepo;
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
            ObjectMapper objectMapper) {
        this.config = config;
        this.obRepo = obRepo;
        this.enquiryRepo = enquiryRepo;
        this.staffRepo = staffRepo;
        this.facultyRepo = facultyRepo;
        this.refundRepo = refundRepo;
        this.studentRepo = studentRepo;
        this.scholarshipRepo = scholarshipRepo;
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

        // Build and persist the request row
        OneBookPaymentRequest obRequest = new OneBookPaymentRequest();
        obRequest.setReferenceId(generateReferenceId());
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

        Map<String, Object> payload = buildCommissionPayload(obRequest, enquiry, recipient);

        String rawResponse = null;
        try {
            rawResponse = callOneBookApi(payload);

            obRequest.setStatus("TRANSMITTED");
            obRequest.setTransmittedAt(Instant.now());
            obRequest.setOnebookRawResponse(rawResponse);

            String txnId = extractField(rawResponse, "transactionId");
            if (txnId != null) obRequest.setOnebookTxnId(txnId);

            enquiry.setCommissionPaymentStatus(CommissionPaymentStatus.TRANSMITTED);
            log.info("Commission payment transmitted to OneBook. ref={} enquiry={}", obRequest.getReferenceId(), enquiryId);

        } catch (RestClientException e) {
            String errorMsg = "OneBook API call failed: " + e.getMessage();
            log.error("Failed to push commission payment to OneBook. ref={} error={}", obRequest.getReferenceId(), e.getMessage());
            obRequest.setStatus("FAILED");
            obRequest.setErrorMessage(errorMsg);
            if (rawResponse != null) obRequest.setOnebookRawResponse(rawResponse);
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

        Map<String, Object> payload = buildGenericPayload(obRequest, "Fee refund — receipt " + refund.getOriginalReceiptNumber());

        String rawResponse = null;
        try {
            rawResponse = callOneBookApi(payload);
            obRequest.setStatus("TRANSMITTED");
            obRequest.setTransmittedAt(Instant.now());
            obRequest.setOnebookRawResponse(rawResponse);
            String txnId = extractField(rawResponse, "transactionId");
            if (txnId != null) obRequest.setOnebookTxnId(txnId);
            refund.setStatus("TRANSMITTED");
            log.info("Refund transmitted to OneBook. ref={} refund={}", obRequest.getReferenceId(), refundId);
        } catch (RestClientException e) {
            log.error("Failed to push refund to OneBook. ref={} error={}", obRequest.getReferenceId(), e.getMessage());
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

        Map<String, Object> payload = buildGenericPayload(obRequest, "Scholarship disbursement — " + studentName);

        String rawResponse = null;
        try {
            rawResponse = callOneBookApi(payload);
            obRequest.setStatus("TRANSMITTED");
            obRequest.setTransmittedAt(Instant.now());
            obRequest.setOnebookRawResponse(rawResponse);
            String txnId = extractField(rawResponse, "transactionId");
            if (txnId != null) obRequest.setOnebookTxnId(txnId);
            log.info("Scholarship disbursement transmitted to OneBook. ref={} scholarshipId={}", obRequest.getReferenceId(), scholarshipId);
        } catch (RestClientException e) {
            log.error("Failed to push scholarship to OneBook. ref={} error={}", obRequest.getReferenceId(), e.getMessage());
            obRequest.setStatus("FAILED");
            obRequest.setErrorMessage("OneBook API call failed: " + e.getMessage());
        }

        obRepo.save(obRequest);
        return obRequest;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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

    private String callOneBookApi(Map<String, Object> payload) {
        String base = config.getApiUrl();
        if (!base.endsWith("/")) base = base + "/";
        String endpoint = base + "one-book/api/payment-register/add-from-other-app";

        String credentials = config.getUsername() + ":" + config.getPassword();
        String basicToken = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        return restClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + basicToken)
                .body(payload)
                .retrieve()
                .body(String.class);
    }

    private Map<String, Object> buildGenericPayload(OneBookPaymentRequest req, String description) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("referenceId", req.getReferenceId());
        payload.put("orgId", config.getOrgId());
        payload.put("branchId", config.getBranchId());
        payload.put("appName", config.getAppName());
        payload.put("paperName", config.getPaperName());
        payload.put("paymentType", req.getPaymentType());
        payload.put("amount", req.getAmount());
        payload.put("currency", "INR");
        payload.put("recipientName", req.getRecipientName());
        if (req.getRecipientAccount() != null) payload.put("recipientAccount", req.getRecipientAccount());
        if (req.getRecipientIfsc() != null)    payload.put("recipientIfsc", req.getRecipientIfsc());
        if (req.getRecipientBankName() != null) payload.put("recipientBankName", req.getRecipientBankName());
        payload.put("description", description);
        return payload;
    }

    private Map<String, Object> buildCommissionPayload(OneBookPaymentRequest req, Enquiry enquiry, RecipientDetails recipient) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("referenceId", req.getReferenceId());
        payload.put("orgId", config.getOrgId());
        payload.put("branchId", config.getBranchId());
        payload.put("appName", config.getAppName());
        payload.put("paperName", config.getPaperName());
        payload.put("paymentType", req.getPaymentType());
        payload.put("amount", req.getAmount());
        payload.put("currency", "INR");
        payload.put("recipientName", recipient.name());
        payload.put("recipientAccount", recipient.accountNumber());
        payload.put("recipientIfsc", recipient.ifsc());
        payload.put("recipientBankName", recipient.bankName());
        payload.put("description", buildDescription(enquiry));

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("enquiryId", enquiry.getId());
        meta.put("enquiryName", enquiry.getName());
        meta.put("commissionSource", enquiry.getCommissionSource() != null
                ? enquiry.getCommissionSource().name() : null);
        payload.put("metadata", meta);
        return payload;
    }

    private String buildDescription(Enquiry e) {
        String source = e.getCommissionSource() != null ? e.getCommissionSource().name() : "REFERRAL";
        return "Commission payment — " + source + " — " + e.getName() + " (Enquiry #" + e.getId() + ")";
    }

    private RecipientDetails resolveRecipient(Enquiry enquiry) {
        CommissionSource source = enquiry.getCommissionSource();
        if (source == CommissionSource.AGENT && enquiry.getAgent() != null) {
            var a = enquiry.getAgent();
            return new RecipientDetails(a.getName(), a.getBankAccountNumber(),
                    a.getBankIfscCode(), a.getBankName());
        }
        if (source == CommissionSource.STAFF_REFERRER && enquiry.getReferredStaffId() != null) {
            return staffRepo.findById(enquiry.getReferredStaffId())
                    .map(s -> new RecipientDetails(s.getName(), s.getBankAccountNumber(),
                            s.getBankIfscCode(), s.getBankName()))
                    .orElse(RecipientDetails.UNKNOWN);
        }
        if (source == CommissionSource.FACULTY_REFERRER && enquiry.getReferredFacultyId() != null) {
            return facultyRepo.findById(enquiry.getReferredFacultyId())
                    .map(f -> new RecipientDetails(
                            f.getFirstName() + " " + f.getLastName(),
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

    private String generateReferenceId() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "OB-" + date + "-" + unique;
    }

    private String extractField(String json, String field) {
        if (json == null || json.isBlank()) return null;
        try {
            Map<?, ?> map = objectMapper.readValue(json, Map.class);
            Object val = map.get(field);
            return val != null ? val.toString() : null;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    record RecipientDetails(String name, String accountNumber, String ifsc, String bankName) {
        static final RecipientDetails UNKNOWN = new RecipientDetails(null, null, null, null);
    }
}
