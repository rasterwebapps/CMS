package com.cms.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.DisbursementRequest;
import com.cms.model.AcademicYear;
import com.cms.model.FeeRefund;
import com.cms.model.Program;
import com.cms.model.Student;
import com.cms.model.StudentScholarship;
import com.cms.model.enums.DisbursementMode;
import com.cms.model.enums.ScholarshipStatus;
import com.cms.model.enums.StudentStatus;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.FeeRefundRepository;
import com.cms.repository.OneBookPaymentRequestRepository;
import com.cms.repository.StaffReferrerRepository;
import com.cms.repository.StudentRepository;
import com.cms.repository.StudentScholarshipRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests guard clauses in OneBookIntegrationService — pre-condition checks that
 * throw before any HTTP call is made. Happy-path HTTP flows are covered by
 * integration tests (requiring WireMock).
 */
@ExtendWith(MockitoExtension.class)
class OneBookIntegrationServiceTest {

    @Mock private OneBookConfigService config;
    @Mock private OneBookPaymentRequestRepository obRepo;
    @Mock private EnquiryRepository enquiryRepo;
    @Mock private StaffReferrerRepository staffRepo;
    @Mock private FacultyRepository facultyRepo;
    @Mock private FeeRefundRepository refundRepo;
    @Mock private StudentRepository studentRepo;
    @Mock private StudentScholarshipRepository scholarshipRepo;
    @Mock private ApplicationNumberSequenceService numberSequenceService;

    private OneBookIntegrationService service;

    @BeforeEach
    void setUp() {
        service = new OneBookIntegrationService(
                config, obRepo, enquiryRepo, staffRepo, facultyRepo,
                refundRepo, studentRepo, scholarshipRepo,
                numberSequenceService, new ObjectMapper());
    }

    // ── pushScholarshipPayment — guard clauses ────────────────────────────────

    @Test
    void pushScholarshipPayment_throwsWhenIntegrationIsDisabled() {
        when(config.isEnabled()).thenReturn(false);

        StudentScholarship app = approvedApplication();
        when(scholarshipRepo.findById(1L)).thenReturn(Optional.of(app));

        assertThatThrownBy(() ->
                service.pushScholarshipPayment(1L, validDisbursementRequest(), "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not enabled");
    }

    @Test
    void pushScholarshipPayment_throwsWhenApplicationIsNotApproved() {
        StudentScholarship app = approvedApplication();
        app.setStatus(ScholarshipStatus.PENDING);

        when(scholarshipRepo.findById(1L)).thenReturn(Optional.of(app));

        assertThatThrownBy(() ->
                service.pushScholarshipPayment(1L, validDisbursementRequest(), "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APPROVED");
    }

    @Test
    void pushScholarshipPayment_throwsWhenStudentHasNoBankAccount() {
        StudentScholarship app = approvedApplication();
        app.getStudent().setBankAccountNumber(null);

        when(scholarshipRepo.findById(1L)).thenReturn(Optional.of(app));
        when(config.isEnabled()).thenReturn(true);
        when(config.getApiUrl()).thenReturn("http://onebook.local");
        when(config.getUsername()).thenReturn("user");

        assertThatThrownBy(() ->
                service.pushScholarshipPayment(1L, validDisbursementRequest(), "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bank details");
    }

    @Test
    void pushScholarshipPayment_throwsWhenStudentHasNoBankName() {
        StudentScholarship app = approvedApplication();
        app.getStudent().setBankName(null);

        when(scholarshipRepo.findById(1L)).thenReturn(Optional.of(app));
        when(config.isEnabled()).thenReturn(true);
        when(config.getApiUrl()).thenReturn("http://onebook.local");
        when(config.getUsername()).thenReturn("user");

        assertThatThrownBy(() ->
                service.pushScholarshipPayment(1L, validDisbursementRequest(), "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bank details");
    }

    // ── pushRefundPayment — guard clauses ─────────────────────────────────────

    @Test
    void pushRefundPayment_throwsWhenRefundIsEnquiryType() {
        FeeRefund refund = new FeeRefund();
        refund.setEntityType("ENQUIRY");
        refund.setStatus("PENDING");

        when(refundRepo.findById(10L)).thenReturn(Optional.of(refund));
        when(config.isEnabled()).thenReturn(true);
        when(config.getApiUrl()).thenReturn("http://onebook.local");
        when(config.getUsername()).thenReturn("user");

        assertThatThrownBy(() -> service.pushRefundPayment(10L, "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Enquiry refunds cannot be pushed to OneBook");
    }

    @Test
    void pushRefundPayment_throwsWhenStatusIsNeitherPendingNorPaymentFailed() {
        FeeRefund refund = new FeeRefund();
        refund.setEntityType("STUDENT");
        refund.setStatus("APPROVED");

        when(refundRepo.findById(10L)).thenReturn(Optional.of(refund));

        assertThatThrownBy(() -> service.pushRefundPayment(10L, "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APPROVED");
    }

    // ── pushCommissionPayment — guard clauses ─────────────────────────────────

    @Test
    void pushCommissionPayment_throwsWhenIntegrationIsDisabled() {
        com.cms.model.Enquiry enquiry = new com.cms.model.Enquiry();
        enquiry.setId(20L);
        enquiry.setCommissionPaymentStatus(com.cms.model.enums.CommissionPaymentStatus.PENDING);

        when(enquiryRepo.findById(20L)).thenReturn(Optional.of(enquiry));
        when(config.isEnabled()).thenReturn(false);

        assertThatThrownBy(() -> service.pushCommissionPayment(20L, "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not enabled");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private StudentScholarship approvedApplication() {
        Program program = new Program("Bachelor", "BACH", 4);
        Student student = new Student("S1", "Alice", "Smith", "alice@test.com",
                program, 1, LocalDate.now(), StudentStatus.ACTIVE);
        student.setId(1L);
        student.setBankAccountNumber("1234567890");
        student.setBankIfscCode("SBIN0001234");
        student.setBankName("State Bank");

        com.cms.model.ScholarshipType type = new com.cms.model.ScholarshipType();
        type.setId(2L); type.setName("Govt SC"); type.setCode("GOV-SC");

        AcademicYear ay = new AcademicYear("2026-2027",
                LocalDate.now(), LocalDate.now().plusYears(1), true);
        ay.setId(10L);

        StudentScholarship app = new StudentScholarship();
        app.setId(1L);
        app.setStudent(student);
        app.setScholarshipType(type);
        app.setAcademicYear(ay);
        app.setStatus(ScholarshipStatus.APPROVED);
        return app;
    }

    private DisbursementRequest validDisbursementRequest() {
        return new DisbursementRequest(
                10L, 1, new BigDecimal("5000"),
                LocalDate.now(), DisbursementMode.DIRECT_CREDIT,
                null, null, null, null);
    }
}
