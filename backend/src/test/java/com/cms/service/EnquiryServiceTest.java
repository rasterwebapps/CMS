package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.EnquiryConversionPrefillResponse;
import com.cms.dto.EnquiryConversionRequest;
import com.cms.dto.DocumentVerificationStatusResponse;
import com.cms.dto.EnquiryRequest;
import com.cms.dto.EnquiryResponse;
import com.cms.dto.FeeStructureResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Agent;
import com.cms.model.Admission;
import com.cms.model.AcademicYear;
import com.cms.model.Course;
import com.cms.model.Enquiry;
import com.cms.model.EnquiryDocument;
import com.cms.model.Program;
import com.cms.model.ReferralType;
import com.cms.model.Student;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.DocumentVerificationStatus;
import com.cms.model.enums.EnquiryStatus;
import com.cms.model.enums.FeeType;
import com.cms.model.enums.StudentType;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.AdmissionRepository;
import com.cms.repository.AgentRepository;
import com.cms.repository.CourseRepository;
import com.cms.repository.EnquiryDocumentRepository;
import com.cms.repository.EnquiryPaymentRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.EnquiryStatusHistoryRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.LocationCountryRepository;
import com.cms.repository.ProgramRepository;
import com.cms.repository.ReferralTypeRepository;
import com.cms.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class EnquiryServiceTest {

    @Mock
    private EnquiryRepository enquiryRepository;
    @Mock
    private ProgramRepository programRepository;
    @Mock
    private AgentRepository agentRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private FacultyRepository facultyRepository;
    @Mock
    private ReferralTypeRepository referralTypeRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private EnquiryStatusHistoryRepository statusHistoryRepository;
    @Mock
    private AdmissionRepository admissionRepository;
    @Mock
    private EnquiryPaymentRepository enquiryPaymentRepository;
    @Mock
    private EnquiryDocumentRepository enquiryDocumentRepository;
    @Mock
    private AcademicYearRepository academicYearRepository;
    @Mock
    private FeeStructureService feeStructureService;
    @Mock
    private com.cms.repository.FeeStateRepository feeStateRepository;
    @Mock
    private com.cms.service.EnquiryDocumentService enquiryDocumentService;
    @Mock
    private ApplicationNumberSequenceService numberSequenceService;
    @Mock
    private LocationCountryRepository countryRepository;

    private EnquiryService enquiryService;

    private Program testProgram;
    private Agent testAgent;
    private ReferralType testReferralType;

    @BeforeEach
    void setUp() {
        enquiryService = new EnquiryService(
            enquiryRepository,
            programRepository,
            agentRepository,
            studentRepository,
            facultyRepository,
            referralTypeRepository,
            courseRepository,
            statusHistoryRepository,
            admissionRepository,
            enquiryPaymentRepository,
            enquiryDocumentRepository,
            academicYearRepository,
            feeStructureService,
            feeStateRepository,
            enquiryDocumentService,
            numberSequenceService,
            countryRepository
        );
        org.mockito.Mockito.lenient()
            .when(enquiryPaymentRepository.sumAmountPaidByEnquiryId(org.mockito.ArgumentMatchers.any()))
            .thenReturn(java.math.BigDecimal.ZERO);
        org.mockito.Mockito.lenient()
            .when(enquiryPaymentRepository.paidTotalsForIds(org.mockito.ArgumentMatchers.any()))
            .thenReturn(java.util.Map.of());
        AcademicYear joiningAY = new AcademicYear("2024-2025", LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31), true);
        joiningAY.setId(100L);
        org.mockito.Mockito.lenient()
            .when(academicYearRepository.findById(100L))
            .thenReturn(java.util.Optional.of(joiningAY));
        org.mockito.Mockito.lenient()
            .when(numberSequenceService.nextAdmissionNumber(
                org.mockito.ArgumentMatchers.any(AcademicYear.class),
                org.mockito.ArgumentMatchers.any()))
            .thenReturn("2024650001");
        org.mockito.Mockito.lenient()
            .when(enquiryDocumentRepository.findByEnquiryId(org.mockito.ArgumentMatchers.anyLong()))
            .thenReturn(List.of());

        testProgram = new Program();
        testProgram.setId(1L);
        testProgram.setName("B.Tech CS");

        testAgent = new Agent("John Agent", "9876543210", "john@agent.com", "Salem", "Local Area", true);
        testAgent.setId(1L);

        testReferralType = new ReferralType("Walk In", "WALK_IN", BigDecimal.ZERO, false, "Walk in enquiry", true);
        testReferralType.setId(1L);
        testReferralType.setCreatedAt(Instant.now());
        testReferralType.setUpdatedAt(Instant.now());
    }

    @Test
    void shouldCreateEnquiry() {
        EnquiryRequest request = basicEnquiryRequest(
            "Ravi Kumar", "ravi@email.com", "9876543210", 1L, null,
            LocalDate.of(2024, 6, 15), 1L, EnquiryStatus.ENQUIRED,
            1L, "Interested in CS", new BigDecimal("50000.00"),
            null, null, null, null, null,
            null, null, null
        );

        Enquiry saved = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ENQUIRED);
        saved.setAgent(testAgent);

        when(programRepository.findById(1L)).thenReturn(Optional.of(testProgram));
        when(agentRepository.findById(1L)).thenReturn(Optional.of(testAgent));
        when(referralTypeRepository.findById(1L)).thenReturn(Optional.of(testReferralType));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(saved);

        EnquiryResponse response = enquiryService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Ravi Kumar");
        assertThat(response.status()).isEqualTo(EnquiryStatus.ENQUIRED);
        verify(enquiryRepository).save(any(Enquiry.class));
    }

    @Test
    void shouldUseAuthoritativeCurrentYearFeeForCourseEnquiry() {
        Course nursingCourse = new Course("BSc Nursing", "BSCN", null, testProgram);
        nursingCourse.setId(2L);
        testReferralType.setHasCommission(true);
        testReferralType.setCommissionAmount(new BigDecimal("25000.00"));
        EnquiryRequest request = basicEnquiryRequest(
            "Mani", "mani@email.com", "9876543210", 1L, 2L,
            LocalDate.of(2024, 6, 15), 1L, EnquiryStatus.ENQUIRED,
            null, "BSc Nursing", null,
            new BigDecimal("2345000.00"), new BigDecimal("25000.00"), new BigDecimal("2345000.00"), null,
            StudentType.DAY_SCHOLAR,
            null, "Tamil Nadu", null
        );
        FeeStructureResponse currentYearTuition = feeResponse(1L, FeeType.TUITION, new BigDecimal("1000000.00"));

        when(programRepository.findById(1L)).thenReturn(Optional.of(testProgram));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(nursingCourse));
        when(referralTypeRepository.findById(1L)).thenReturn(Optional.of(testReferralType));
        when(feeStructureService.findByProgramIdAndCourseId(1L, 2L)).thenReturn(List.of(currentYearTuition));
        when(enquiryRepository.save(any(Enquiry.class))).thenAnswer(inv -> {
            Enquiry saved = inv.getArgument(0);
            saved.setId(1L);
            saved.setCreatedAt(Instant.now());
            saved.setUpdatedAt(Instant.now());
            return saved;
        });

        EnquiryResponse response = enquiryService.create(request);

        assertThat(response.finalCalculatedFee()).isEqualByComparingTo(new BigDecimal("1000000.00"));
        assertThat(response.feeDiscussedAmount()).isEqualByComparingTo(new BigDecimal("1000000.00"));
        verify(feeStructureService).findByProgramIdAndCourseId(1L, 2L);
    }

    @Test
    void shouldCreateEnquiryWithoutProgramAndAgent() {
        EnquiryRequest request = basicEnquiryRequest(
            "Ravi Kumar", "ravi@email.com", "9876543210", null, null,
            LocalDate.of(2024, 6, 15), 1L, null,
            null, null, null,
            null, null, null, null, null,
            null, null, null
        );

        Enquiry saved = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            null, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ENQUIRED);

        when(referralTypeRepository.findById(1L)).thenReturn(Optional.of(testReferralType));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(saved);

        EnquiryResponse response = enquiryService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.programId()).isNull();
        assertThat(response.agentId()).isNull();
    }

    @Test
    void shouldThrowWhenProgramNotFoundOnCreate() {
        EnquiryRequest request = basicEnquiryRequest(
            "Ravi Kumar", "ravi@email.com", "9876543210", 999L, null,
            LocalDate.of(2024, 6, 15), 1L, EnquiryStatus.ENQUIRED,
            null, null, null,
            null, null, null, null, null,
            null, null, null
        );

        when(programRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enquiryService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Program not found with id: 999");
    }

    @Test
    void shouldThrowWhenAgentNotFoundOnCreate() {
        EnquiryRequest request = basicEnquiryRequest(
            "Ravi Kumar", "ravi@email.com", "9876543210", null, null,
            LocalDate.of(2024, 6, 15), 1L, EnquiryStatus.ENQUIRED,
            999L, null, null,
            null, null, null, null, null,
            null, null, null
        );

        when(agentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enquiryService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Agent not found with id: 999");
    }

    @Test
    void shouldFindAllEnquiries() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ENQUIRED);

        when(enquiryRepository.findAll()).thenReturn(List.of(enquiry));

        List<EnquiryResponse> responses = enquiryService.findAll();

        assertThat(responses).hasSize(1);
        verify(enquiryRepository).findAll();
    }

    @Test
    void shouldFindById() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ENQUIRED);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));

        EnquiryResponse response = enquiryService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Ravi Kumar");
        verify(enquiryRepository).findById(1L);
    }

    @Test
    void shouldThrowWhenNotFoundById() {
        when(enquiryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enquiryService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Enquiry not found with id: 999");

        verify(enquiryRepository).findById(999L);
    }

    @Test
    void shouldFindByStatus() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ENQUIRED);

        when(enquiryRepository.findByStatus(EnquiryStatus.ENQUIRED)).thenReturn(List.of(enquiry));

        List<EnquiryResponse> responses = enquiryService.findByStatus(EnquiryStatus.ENQUIRED);

        assertThat(responses).hasSize(1);
        verify(enquiryRepository).findByStatus(EnquiryStatus.ENQUIRED);
    }

    @Test
    void shouldFindByReferralTypeId() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ENQUIRED);

        when(enquiryRepository.findByReferralTypeId(1L)).thenReturn(List.of(enquiry));

        List<EnquiryResponse> responses = enquiryService.findByReferralTypeId(1L);

        assertThat(responses).hasSize(1);
        verify(enquiryRepository).findByReferralTypeId(1L);
    }

    @Test
    void shouldFindByAgentId() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ENQUIRED);
        enquiry.setAgent(testAgent);

        when(enquiryRepository.findByAgentId(1L)).thenReturn(List.of(enquiry));

        List<EnquiryResponse> responses = enquiryService.findByAgentId(1L);

        assertThat(responses).hasSize(1);
        verify(enquiryRepository).findByAgentId(1L);
    }

    @Test
    void shouldUpdateEnquiry() {
        Enquiry existing = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ENQUIRED);

        EnquiryRequest updateRequest = basicEnquiryRequest(
            "Ravi Kumar Updated", "ravi.updated@email.com", "1234567890", 1L, null,
            LocalDate.of(2024, 6, 20), 1L, EnquiryStatus.INTERESTED,
            null, "Called back", new BigDecimal("45000.00"),
            null, null, null, null, null,
            null, null, null
        );

        Enquiry updated = createEnquiry(1L, "Ravi Kumar Updated", "ravi.updated@email.com", "1234567890",
            testProgram, LocalDate.of(2024, 6, 20), testReferralType, EnquiryStatus.INTERESTED);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(programRepository.findById(1L)).thenReturn(Optional.of(testProgram));
        when(referralTypeRepository.findById(1L)).thenReturn(Optional.of(testReferralType));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(updated);

        EnquiryResponse response = enquiryService.update(1L, updateRequest);

        assertThat(response.name()).isEqualTo("Ravi Kumar Updated");
        assertThat(response.status()).isEqualTo(EnquiryStatus.INTERESTED);
    }

    @Test
    void shouldThrowWhenNotFoundOnUpdate() {
        EnquiryRequest request = basicEnquiryRequest(
            "Ravi Kumar", "ravi@email.com", "9876543210", null, null,
            LocalDate.of(2024, 6, 15), 1L, EnquiryStatus.ENQUIRED,
            null, null, null,
            null, null, null, null, null,
            null, null, null
        );

        when(enquiryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enquiryService.update(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Enquiry not found with id: 999");

        verify(enquiryRepository, never()).save(any());
    }

    @Test
    void shouldConvertToStudent() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.DOCUMENTS_VERIFIED);

        Student student = new Student();
        student.setId(10L);

        Enquiry converted = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ADMITTED);
        converted.setConvertedStudentId(10L);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));
        when(enquiryDocumentService.allMandatoryDocumentsVerified(1L))
            .thenReturn(new DocumentVerificationStatusResponse(true, true, List.of(), List.of()));
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(converted);

        EnquiryResponse response = enquiryService.convertToStudent(1L, 10L);

        assertThat(response.status()).isEqualTo(EnquiryStatus.ADMITTED);
        assertThat(response.convertedStudentId()).isEqualTo(10L);
    }

    @Test
    void shouldThrowWhenConvertingFromFeesFinalizedStatus() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.FEES_FINALIZED);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));

        assertThatThrownBy(() -> enquiryService.convertToStudent(1L, 10L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("DOCUMENTS_VERIFIED");
    }

    @Test
    void shouldThrowWhenConvertingWithInvalidStatus() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ENQUIRED);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));

        assertThatThrownBy(() -> enquiryService.convertToStudent(1L, 10L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("DOCUMENTS_VERIFIED");
    }

    @Test
    void shouldThrowWhenEnquiryNotFoundOnConvert() {
        when(enquiryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enquiryService.convertToStudent(999L, 10L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Enquiry not found with id: 999");
    }

    @Test
    void shouldThrowWhenStudentNotFoundOnConvert() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.DOCUMENTS_VERIFIED);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));
        when(enquiryDocumentService.allMandatoryDocumentsVerified(1L))
            .thenReturn(new DocumentVerificationStatusResponse(true, true, List.of(), List.of()));
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enquiryService.convertToStudent(1L, 999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Student not found with id: 999");
    }

    @Test
    void shouldDeleteEnquiry() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ENQUIRED);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));

        enquiryService.delete(1L);

        verify(enquiryRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeletingNonExistent() {
        when(enquiryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enquiryService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Enquiry not found with id: 999");

        verify(enquiryRepository, never()).deleteById(any());
    }

    @Test
    void shouldThrowWhenDeletingEnquiryBeyondEnquiredStatus() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.INTERESTED);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));

        assertThatThrownBy(() -> enquiryService.delete(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ENQUIRED status")
            .hasMessageContaining("INTERESTED");

        verify(enquiryRepository, never()).deleteById(any());
    }

    @Test
    void shouldFindDocumentPending() {
        Enquiry feesPaid = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.FEES_PAID);
        Enquiry partiallyPaid = createEnquiry(2L, "Priya Singh", "priya@email.com", "8765432109",
            testProgram, LocalDate.of(2024, 6, 20), testReferralType, EnquiryStatus.PARTIALLY_PAID);

        when(enquiryRepository.findByStatusIn(
            List.of(EnquiryStatus.FEES_PAID, EnquiryStatus.PARTIALLY_PAID)
        )).thenReturn(List.of(feesPaid, partiallyPaid));

        List<EnquiryResponse> responses = enquiryService.findDocumentPending();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).status()).isEqualTo(EnquiryStatus.FEES_PAID);
        assertThat(responses.get(1).status()).isEqualTo(EnquiryStatus.PARTIALLY_PAID);
    }

    @Test
    void shouldFindAdmissionPending() {
        Enquiry docsVerified = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.DOCUMENTS_VERIFIED);

        when(enquiryRepository.findByStatus(EnquiryStatus.DOCUMENTS_VERIFIED))
            .thenReturn(List.of(docsVerified));

        List<EnquiryResponse> responses = enquiryService.findAdmissionPending();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).status()).isEqualTo(EnquiryStatus.DOCUMENTS_VERIFIED);
    }

    @Test
    void shouldFindByDateRange() {
        LocalDate from = LocalDate.of(2024, 6, 1);
        LocalDate to = LocalDate.of(2024, 6, 30);
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ENQUIRED);

        when(enquiryRepository.findByEnquiryDateBetween(from, to)).thenReturn(List.of(enquiry));

        List<EnquiryResponse> responses = enquiryService.findByDateRange(from, to);

        assertThat(responses).hasSize(1);
        verify(enquiryRepository).findByEnquiryDateBetween(from, to);
    }

    @Test
    void shouldFindByDateRangeAndStatus() {
        LocalDate from = LocalDate.of(2024, 6, 1);
        LocalDate to = LocalDate.of(2024, 6, 30);
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ENQUIRED);

        when(enquiryRepository.findByEnquiryDateBetweenAndStatus(from, to, EnquiryStatus.ENQUIRED))
            .thenReturn(List.of(enquiry));

        List<EnquiryResponse> responses = enquiryService.findByDateRangeAndStatus(from, to, EnquiryStatus.ENQUIRED);

        assertThat(responses).hasSize(1);
        verify(enquiryRepository).findByEnquiryDateBetweenAndStatus(from, to, EnquiryStatus.ENQUIRED);
    }

    @Test
    void shouldUpdateStatus() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ENQUIRED);

        Enquiry updated = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.INTERESTED);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(updated);

        EnquiryResponse response = enquiryService.updateStatus(1L, EnquiryStatus.INTERESTED, "admin");

        assertThat(response.status()).isEqualTo(EnquiryStatus.INTERESTED);
        verify(enquiryRepository).save(any(Enquiry.class));
    }

    @Test
    void shouldThrowWhenUpdatingStatusOfNonExistent() {
        when(enquiryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enquiryService.updateStatus(999L, EnquiryStatus.INTERESTED, "admin"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Enquiry not found with id: 999");

        verify(enquiryRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenManualTransitionIsNotAllowed() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.INTERESTED);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));

        assertThatThrownBy(() -> enquiryService.updateStatus(1L, EnquiryStatus.FEES_FINALIZED, "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot manually transition from INTERESTED to FEES_FINALIZED");

        verify(enquiryRepository, never()).save(any());
    }

    @Test
    void shouldAllowManualTransitionFromEnquiredToNotInterested() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ENQUIRED);

        Enquiry updated = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.NOT_INTERESTED);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(updated);

        EnquiryResponse response = enquiryService.updateStatus(1L, EnquiryStatus.NOT_INTERESTED, "admin");

        assertThat(response.status()).isEqualTo(EnquiryStatus.NOT_INTERESTED);
    }

    @Test
    void shouldAllowManualTransitionFromFeesFinalizedToNotInterested() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.FEES_FINALIZED);

        Enquiry updated = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.NOT_INTERESTED);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(updated);

        EnquiryResponse response = enquiryService.updateStatus(1L, EnquiryStatus.NOT_INTERESTED, "admin");

        assertThat(response.status()).isEqualTo(EnquiryStatus.NOT_INTERESTED);
    }

    /** Build an EnquiryRequest with the legacy 19-argument signature; referredStudentId and referredFacultyId are null. */
    private static EnquiryRequest basicEnquiryRequest(
            String name, String email, String phone, Long programId, Long courseId,
            LocalDate enquiryDate, Long referralTypeId, EnquiryStatus status,
            Long agentId, String remarks, java.math.BigDecimal feeDiscussedAmount,
            java.math.BigDecimal feeGuidelineTotal, java.math.BigDecimal referralAdditionalAmount,
            java.math.BigDecimal finalCalculatedFee, String yearWiseFees,
            com.cms.model.enums.StudentType studentType,
            Long countryId, String state, String district) {
        return new EnquiryRequest(
            name, email, phone, programId, courseId, enquiryDate, referralTypeId, status,
            agentId, remarks, feeDiscussedAmount, feeGuidelineTotal, referralAdditionalAmount,
            finalCalculatedFee, yearWiseFees, studentType, countryId, state, district,
            null, null, null,
            LocalDate.of(2000, 1, 1), com.cms.model.enums.Gender.FEMALE,
            null, null
        );
    }

    private Enquiry createEnquiry(Long id, String name, String email, String phone,
                                   Program program, LocalDate enquiryDate,
                                   ReferralType referralType, EnquiryStatus status) {
        Enquiry enquiry = new Enquiry(name, email, phone, program, enquiryDate, referralType, status);
        enquiry.setId(id);
        Instant now = Instant.now();
        enquiry.setCreatedAt(now);
        enquiry.setUpdatedAt(now);
        return enquiry;
    }

    private FeeStructureResponse feeResponse(Long id, FeeType feeType, BigDecimal amount) {
        return new FeeStructureResponse(
            id, 1L, 1L, testProgram.getName(), 2L, "BSc Nursing", 100L, "2024-2025",
            com.cms.model.enums.AdmissionQuota.MANAGEMENT, 1L, "Tamil Nadu",
            com.cms.model.enums.Gender.FEMALE, com.cms.model.enums.StudentType.DAY_SCHOLAR,
            feeType, amount, null, true, true, List.of(), Instant.now(), Instant.now()
        );
    }

    @Test
    void shouldFinalizeFees() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.INTERESTED);
        enquiry.setFinalCalculatedFee(new BigDecimal("100000.00"));

        com.cms.dto.FeeFinalizationRequest request = new com.cms.dto.FeeFinalizationRequest(
            new BigDecimal("100000.00"), new BigDecimal("5000.00"), "Early bird discount", null, null
        );

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));
        when(enquiryRepository.save(any(Enquiry.class))).thenAnswer(inv -> inv.getArgument(0));

        com.cms.dto.FeeFinalizationResponse response = enquiryService.finalizeFees(1L, request, "admin");

        assertThat(response.enquiryId()).isEqualTo(1L);
        assertThat(response.finalizedTotalFee()).isEqualTo(new BigDecimal("100000.00"));
        assertThat(response.finalizedDiscountAmount()).isEqualTo(new BigDecimal("5000.00"));
        assertThat(response.finalizedNetFee()).isEqualTo(new BigDecimal("95000.00"));
        assertThat(response.finalizedBy()).isEqualTo("admin");
        assertThat(response.status()).isEqualTo("FEES_FINALIZED");
    }

    @Test
    void shouldThrowWhenFinalizingFeesFromEnquiredStatus() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ENQUIRED);

        com.cms.dto.FeeFinalizationRequest request = new com.cms.dto.FeeFinalizationRequest(
            new BigDecimal("50000.00"), null, null, null, null
        );

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));

        assertThatThrownBy(() -> enquiryService.finalizeFees(1L, request, "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("INTERESTED status to finalize fees");
    }

    @Test
    void shouldThrowWhenFinalizingFeesWithWrongStatus() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ADMITTED);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));

        com.cms.dto.FeeFinalizationRequest request = new com.cms.dto.FeeFinalizationRequest(
            new BigDecimal("50000.00"), null, null, null, null
        );

        assertThatThrownBy(() -> enquiryService.finalizeFees(1L, request, "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("INTERESTED status to finalize fees");
    }

    @Test
    void shouldThrowWhenFinalizingFeesForNonExistent() {
        when(enquiryRepository.findById(999L)).thenReturn(Optional.empty());

        com.cms.dto.FeeFinalizationRequest request = new com.cms.dto.FeeFinalizationRequest(
            new BigDecimal("50000.00"), null, null, null, null
        );

        assertThatThrownBy(() -> enquiryService.finalizeFees(999L, request, "admin"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Enquiry not found with id: 999");
    }

    @Test
    void shouldThrowWhenAttemptingToIncreaseFee() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.INTERESTED);
        enquiry.setFinalCalculatedFee(new BigDecimal("100000.00"));

        // Trying to finalize with a higher fee (increase not allowed)
        com.cms.dto.FeeFinalizationRequest request = new com.cms.dto.FeeFinalizationRequest(
            new BigDecimal("120000.00"), null, null, null, null
        );

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));

        assertThatThrownBy(() -> enquiryService.finalizeFees(1L, request, "admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Fee increase is not allowed")
            .hasMessageContaining("Only discounts can be applied");
    }

    @Test
    void shouldThrowWhenNoCalculatedFeeExists() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.INTERESTED);
        // finalCalculatedFee is null

        com.cms.dto.FeeFinalizationRequest request = new com.cms.dto.FeeFinalizationRequest(
            new BigDecimal("100000.00"), null, null, null, null
        );

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));

        assertThatThrownBy(() -> enquiryService.finalizeFees(1L, request, "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("no calculated fee found");
    }

    @Test
    void shouldAllowFeeFinalizationAtOriginalAmount() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.INTERESTED);
        enquiry.setFinalCalculatedFee(new BigDecimal("100000.00"));

        // Finalizing at exact original amount (no increase, no discount)
        com.cms.dto.FeeFinalizationRequest request = new com.cms.dto.FeeFinalizationRequest(
            new BigDecimal("100000.00"), null, null, null, null
        );

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));
        when(enquiryRepository.save(any(Enquiry.class))).thenAnswer(inv -> inv.getArgument(0));

        com.cms.dto.FeeFinalizationResponse response = enquiryService.finalizeFees(1L, request, "admin");

        assertThat(response.finalizedTotalFee()).isEqualTo(new BigDecimal("100000.00"));
        assertThat(response.finalizedNetFee()).isEqualTo(new BigDecimal("100000.00"));
        assertThat(response.status()).isEqualTo("FEES_FINALIZED");
    }

    @Test
    void shouldAllowFeeFinalizationWithDiscount() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.INTERESTED);
        enquiry.setFinalCalculatedFee(new BigDecimal("100000.00"));

        // Finalizing with discount (reduction allowed)
        com.cms.dto.FeeFinalizationRequest request = new com.cms.dto.FeeFinalizationRequest(
            new BigDecimal("100000.00"), new BigDecimal("10000.00"), "Merit scholarship", null, null
        );

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));
        when(enquiryRepository.save(any(Enquiry.class))).thenAnswer(inv -> inv.getArgument(0));

        com.cms.dto.FeeFinalizationResponse response = enquiryService.finalizeFees(1L, request, "admin");

        assertThat(response.finalizedTotalFee()).isEqualTo(new BigDecimal("100000.00"));
        assertThat(response.finalizedDiscountAmount()).isEqualTo(new BigDecimal("10000.00"));
        assertThat(response.finalizedNetFee()).isEqualTo(new BigDecimal("90000.00"));
        assertThat(response.status()).isEqualTo("FEES_FINALIZED");
    }

    @Test
    void shouldThrowWhenDiscountProvidedWithoutReason() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.INTERESTED);
        enquiry.setFinalCalculatedFee(new BigDecimal("100000.00"));

        // Discount provided but no reason — must be rejected
        com.cms.dto.FeeFinalizationRequest request = new com.cms.dto.FeeFinalizationRequest(
            new BigDecimal("100000.00"), new BigDecimal("5000.00"), null, null, null
        );

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));

        assertThatThrownBy(() -> enquiryService.finalizeFees(1L, request, "admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Discount reason is required");
    }

    @Test
    void shouldThrowWhenDiscountProvidedWithBlankReason() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.INTERESTED);
        enquiry.setFinalCalculatedFee(new BigDecimal("100000.00"));

        // Blank reason — must also be rejected
        com.cms.dto.FeeFinalizationRequest request = new com.cms.dto.FeeFinalizationRequest(
            new BigDecimal("100000.00"), new BigDecimal("5000.00"), "   ", null, null
        );

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));

        assertThatThrownBy(() -> enquiryService.finalizeFees(1L, request, "admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Discount reason is required");
    }

    @Test
    void shouldSubmitDocumentsFromFeesPaidStatus() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.FEES_PAID);

        Enquiry updated = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.DOCUMENTS_SUBMITTED);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(updated);

        EnquiryResponse response = enquiryService.submitDocuments(1L);

        assertThat(response.status()).isEqualTo(EnquiryStatus.DOCUMENTS_SUBMITTED);
        verify(enquiryRepository).save(any(Enquiry.class));
    }

    @Test
    void shouldSubmitDocumentsFromPartiallyPaidStatus() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.PARTIALLY_PAID);

        Enquiry updated = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.DOCUMENTS_SUBMITTED);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(updated);

        EnquiryResponse response = enquiryService.submitDocuments(1L);

        assertThat(response.status()).isEqualTo(EnquiryStatus.DOCUMENTS_SUBMITTED);
    }

    @Test
    void shouldThrowWhenSubmittingDocumentsWithWrongStatus() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.INTERESTED);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));

        assertThatThrownBy(() -> enquiryService.submitDocuments(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("FEES_PAID or PARTIALLY_PAID status to submit documents");

        verify(enquiryRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenSubmittingDocumentsForNonExistent() {
        when(enquiryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enquiryService.submitDocuments(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Enquiry not found with id: 999");
    }

    @Test
    void shouldCreateEnquiryWithReferralType() {
        com.cms.model.ReferralType referralType = new com.cms.model.ReferralType(
            "Staff", "STAFF", new BigDecimal("5000.00"), true, "Staff referral", true
        );
        referralType.setId(2L);
        referralType.setCreatedAt(Instant.now());
        referralType.setUpdatedAt(Instant.now());

        EnquiryRequest request = basicEnquiryRequest(
            "Ravi Kumar", "ravi@email.com", "9876543210", null, null,
            LocalDate.of(2024, 6, 15), 2L, null,
            null, null, null, null, null, null, null, null,
            null, null, null
        );

        Enquiry saved = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            null, LocalDate.of(2024, 6, 15), referralType, EnquiryStatus.ENQUIRED);

        when(referralTypeRepository.findById(2L)).thenReturn(Optional.of(referralType));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(saved);

        EnquiryResponse response = enquiryService.create(request);

        assertThat(response.referralTypeId()).isEqualTo(2L);
        assertThat(response.referralTypeName()).isEqualTo("Staff");
        assertThat(response.referralCommissionAmount()).isEqualTo(new BigDecimal("5000.00"));
    }

    @Test
    void shouldThrowWhenReferralTypeNotFound() {
        EnquiryRequest request = basicEnquiryRequest(
            "Ravi Kumar", "ravi@email.com", "9876543210", null, null,
            LocalDate.of(2024, 6, 15), 999L, null,
            null, null, null, null, null, null, null, null,
            null, null, null
        );

        when(referralTypeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enquiryService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Referral type not found with id: 999");
    }

    @Test
    void shouldConvertFromDocumentsVerifiedStatus() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.DOCUMENTS_VERIFIED);

        Student student = new Student();
        student.setId(10L);

        Enquiry converted = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ADMITTED);
        converted.setConvertedStudentId(10L);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));
        when(enquiryDocumentService.allMandatoryDocumentsVerified(1L))
            .thenReturn(new DocumentVerificationStatusResponse(true, true, List.of(), List.of()));
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(converted);

        EnquiryResponse response = enquiryService.convertToStudent(1L, 10L);

        assertThat(response.status()).isEqualTo(EnquiryStatus.ADMITTED);
    }

    @Test
    void shouldRejectLegacyConversionFromDocumentsSubmittedStatus() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.DOCUMENTS_SUBMITTED);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));

        assertThatThrownBy(() -> enquiryService.convertToStudent(1L, 10L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("DOCUMENTS_VERIFIED");
    }

    @Test
    void shouldThrowWhenConvertingFromFeesPaidStatus() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.FEES_PAID);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));

        assertThatThrownBy(() -> enquiryService.convertToStudent(1L, 10L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("DOCUMENTS_VERIFIED");
    }

    @Test
    void shouldThrowWhenConvertingFromPartiallyPaidStatus() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.PARTIALLY_PAID);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));

        assertThatThrownBy(() -> enquiryService.convertToStudent(1L, 10L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("DOCUMENTS_VERIFIED");
    }

    @Test
    void shouldCreateEnquiryWithFeeGuidelineData() {
        EnquiryRequest request = basicEnquiryRequest(
            "Ravi Kumar", "ravi@email.com", "9876543210", null, null,
            LocalDate.of(2024, 6, 15), 1L, null,
            null, null, null,
            new BigDecimal("100000.00"), new BigDecimal("5000.00"),
            new BigDecimal("105000.00"), "[50000,55000]", null,
            null, null, null
        );

        Enquiry saved = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            null, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ENQUIRED);
        saved.setFinalCalculatedFee(new BigDecimal("105000.00"));
        saved.setYearWiseFees("[50000,55000]");

        when(referralTypeRepository.findById(1L)).thenReturn(Optional.of(testReferralType));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(saved);

        EnquiryResponse response = enquiryService.create(request);

        assertThat(response.finalCalculatedFee()).isEqualTo(new BigDecimal("105000.00"));
        assertThat(response.yearWiseFees()).isEqualTo("[50000,55000]");
    }

    @Test
    void shouldUpdateEnquiryWithReferralType() {
        Enquiry existing = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ENQUIRED);

        com.cms.model.ReferralType referralType = new com.cms.model.ReferralType(
            "Staff", "STAFF", BigDecimal.ZERO, false, "Staff referral", true
        );
        referralType.setId(2L);
        referralType.setCreatedAt(Instant.now());
        referralType.setUpdatedAt(Instant.now());

        EnquiryRequest updateRequest = basicEnquiryRequest(
            "Ravi Kumar", "ravi@email.com", "9876543210", 1L, null,
            LocalDate.of(2024, 6, 15), 2L, null,
            null, null, null, null, null, null, null, null,
            null, null, null
        );

        Enquiry updated = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), referralType, EnquiryStatus.ENQUIRED);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(programRepository.findById(1L)).thenReturn(Optional.of(testProgram));
        when(referralTypeRepository.findById(2L)).thenReturn(Optional.of(referralType));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(updated);

        EnquiryResponse response = enquiryService.update(1L, updateRequest);

        assertThat(response.referralTypeId()).isEqualTo(2L);
    }

    @Test
    void shouldCreateEnquiryWithCourse() {
        com.cms.model.Course testCourse = new com.cms.model.Course("CS101", "CS101",
            null, testProgram);
        testCourse.setId(1L);

        EnquiryRequest request = basicEnquiryRequest(
            "Ravi Kumar", "ravi@email.com", "9876543210", 1L, 1L,
            LocalDate.of(2024, 6, 15), 1L, null,
            null, null, null, null, null, null, null, null,
            null, null, null
        );

        Enquiry saved = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ENQUIRED);
        saved.setCourse(testCourse);

        when(programRepository.findById(1L)).thenReturn(Optional.of(testProgram));
        when(referralTypeRepository.findById(1L)).thenReturn(Optional.of(testReferralType));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(saved);

        EnquiryResponse response = enquiryService.create(request);

        assertThat(response.courseId()).isEqualTo(1L);
        assertThat(response.courseName()).isEqualTo("CS101");
    }

    @Test
    void shouldThrowWhenCourseNotFoundOnCreate() {
        EnquiryRequest request = basicEnquiryRequest(
            "Ravi Kumar", "ravi@email.com", "9876543210", null, 999L,
            LocalDate.of(2024, 6, 15), 1L, null,
            null, null, null, null, null, null, null, null,
            null, null, null
        );

        when(referralTypeRepository.findById(1L)).thenReturn(Optional.of(testReferralType));
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enquiryService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Course not found with id: 999");
    }

    @Test
    void shouldUpdateEnquiryWithCourse() {
        Enquiry existing = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ENQUIRED);

        com.cms.model.Course testCourse = new com.cms.model.Course("CS101", "CS101",
            null, testProgram);
        testCourse.setId(1L);

        EnquiryRequest updateRequest = basicEnquiryRequest(
            "Ravi Kumar", "ravi@email.com", "9876543210", 1L, 1L,
            LocalDate.of(2024, 6, 15), 1L, null,
            null, null, null, null, null, null, null, null,
            null, null, null
        );

        Enquiry updated = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ENQUIRED);
        updated.setCourse(testCourse);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(programRepository.findById(1L)).thenReturn(Optional.of(testProgram));
        when(referralTypeRepository.findById(1L)).thenReturn(Optional.of(testReferralType));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(updated);

        EnquiryResponse response = enquiryService.update(1L, updateRequest);

        assertThat(response.courseId()).isEqualTo(1L);
    }

    @Test
    void shouldThrowWhenCourseNotFoundOnUpdate() {
        Enquiry existing = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ENQUIRED);

        EnquiryRequest updateRequest = basicEnquiryRequest(
            "Ravi Kumar", "ravi@email.com", "9876543210", null, 999L,
            LocalDate.of(2024, 6, 15), 1L, null,
            null, null, null, null, null, null, null, null,
            null, null, null
        );

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(referralTypeRepository.findById(1L)).thenReturn(Optional.of(testReferralType));
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enquiryService.update(1L, updateRequest))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Course not found with id: 999");
    }

    @Test
    void shouldFinalizeFeesWithYearWiseFees() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.INTERESTED);
        enquiry.setFinalCalculatedFee(new BigDecimal("100000.00"));

        com.cms.dto.FeeFinalizationRequest request = new com.cms.dto.FeeFinalizationRequest(
            new BigDecimal("100000.00"), null, null, "[50000,50000]", null
        );

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));
        when(enquiryRepository.save(any(Enquiry.class))).thenAnswer(inv -> inv.getArgument(0));

        com.cms.dto.FeeFinalizationResponse response = enquiryService.finalizeFees(1L, request, "admin");

        assertThat(response.finalizedTotalFee()).isEqualTo(new BigDecimal("100000.00"));
    }

    @Test
    void shouldCreateEnquiryWithStudentType() {
        EnquiryRequest request = basicEnquiryRequest(
            "Ravi Kumar", "ravi@email.com", "9876543210", null, null,
            LocalDate.of(2024, 6, 15), 1L, null,
            null, null, null, null, null, null, null, StudentType.HOSTELER,
            null, null, null
        );

        Enquiry saved = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            null, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ENQUIRED);
        saved.setStudentType(StudentType.HOSTELER);

        when(referralTypeRepository.findById(1L)).thenReturn(Optional.of(testReferralType));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(saved);

        EnquiryResponse response = enquiryService.create(request);

        assertThat(response.studentType()).isEqualTo(StudentType.HOSTELER);
    }

    @Test
    void shouldCreateEnquiryWithDayScholarType() {
        EnquiryRequest request = basicEnquiryRequest(
            "Priya", "priya@email.com", "9876543211", null, null,
            LocalDate.of(2024, 6, 15), 1L, null,
            null, null, null, null, null, null, null, StudentType.DAY_SCHOLAR,
            null, null, null
        );

        Enquiry saved = createEnquiry(1L, "Priya", "priya@email.com", "9876543211",
            null, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ENQUIRED);
        saved.setStudentType(StudentType.DAY_SCHOLAR);

        when(referralTypeRepository.findById(1L)).thenReturn(Optional.of(testReferralType));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(saved);

        EnquiryResponse response = enquiryService.create(request);

        assertThat(response.studentType()).isEqualTo(StudentType.DAY_SCHOLAR);
    }

    @Test
    void shouldRecordHistoryOnCreate() {
        EnquiryRequest request = basicEnquiryRequest(
            "Ravi Kumar", "ravi@email.com", "9876543210", null, null,
            LocalDate.of(2024, 6, 15), 1L, EnquiryStatus.ENQUIRED,
            null, null, null, null, null, null, null, null,
            null, null, null
        );

        Enquiry saved = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            null, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ENQUIRED);

        when(referralTypeRepository.findById(1L)).thenReturn(Optional.of(testReferralType));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(saved);

        enquiryService.create(request);

        verify(statusHistoryRepository).save(any());
    }

    @Test
    void shouldConvertToStudentWithData() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.DOCUMENTS_VERIFIED);

        EnquiryConversionRequest request = new EnquiryConversionRequest(
            "Ravi", "Kumar", "ravi@college.edu", "9876543210", 1, LocalDate.of(2024, 7, 1),
            100L, LocalDate.of(2024, 7, 1), true, true,
            null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null
        );

        Student savedStudent = new Student(null, "Ravi", "Kumar", "ravi@college.edu",
            testProgram, 1, LocalDate.of(2024, 7, 1), com.cms.model.enums.StudentStatus.ACTIVE);
        savedStudent.setId(10L);
        savedStudent.setCreatedAt(Instant.now());
        savedStudent.setUpdatedAt(Instant.now());

        Enquiry admitted = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ADMITTED);
        admitted.setConvertedStudentId(10L);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));
        when(enquiryDocumentService.allMandatoryDocumentsVerified(1L))
            .thenReturn(new DocumentVerificationStatusResponse(true, true, List.of(), List.of()));
        when(studentRepository.existsByEmail("ravi@college.edu")).thenReturn(false);
        when(studentRepository.save(any(Student.class))).thenReturn(savedStudent);
        when(admissionRepository.save(any(Admission.class))).thenReturn(new Admission());
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(admitted);

        EnquiryResponse response = enquiryService.convertToStudentWithData(1L, request, "admin");

        assertThat(response.status()).isEqualTo(EnquiryStatus.ADMITTED);
        assertThat(response.convertedStudentId()).isEqualTo(10L);
        verify(admissionRepository).save(any(Admission.class));
        verify(statusHistoryRepository).save(any());
    }

    @Test
    void shouldMigrateEnquiryDocumentsToAdmissionOnConversion() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.DOCUMENTS_VERIFIED);

        EnquiryDocument verifiedDoc = new EnquiryDocument(enquiry, DocumentType.TENTH_MARKSHEET, DocumentVerificationStatus.VERIFIED);
        verifiedDoc.setFileName("10th.pdf");
        verifiedDoc.setUploadedAt(Instant.parse("2026-05-01T10:15:30Z"));
        verifiedDoc.setVerifiedBy("admin");
        verifiedDoc.setVerifiedAt(Instant.parse("2026-05-02T08:00:00Z"));

        EnquiryDocument uploadedDoc = new EnquiryDocument(enquiry, DocumentType.AADHAR_CARD, DocumentVerificationStatus.UPLOADED);
        uploadedDoc.setFileName("aadhar.pdf");
        uploadedDoc.setUploadedAt(Instant.parse("2026-05-01T10:20:30Z"));

        EnquiryDocument notUploadedDoc = new EnquiryDocument(enquiry, DocumentType.PASSPORT_PHOTO, DocumentVerificationStatus.NOT_UPLOADED);

        EnquiryConversionRequest request = new EnquiryConversionRequest(
            "Ravi", "Kumar", "ravi@college.edu", "9876543210", 1, LocalDate.of(2024, 7, 1),
            100L, LocalDate.of(2024, 7, 1), true, true,
            null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null
        );

        Student savedStudent = new Student(null, "Ravi", "Kumar", "ravi@college.edu",
            testProgram, 1, LocalDate.of(2024, 7, 1), com.cms.model.enums.StudentStatus.ACTIVE);
        savedStudent.setId(10L);

        Admission savedAdmission = new Admission(savedStudent, new AcademicYear(), LocalDate.of(2024, 7, 1));
        savedAdmission.setId(999L);

        Enquiry admitted = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ADMITTED);
        admitted.setConvertedStudentId(10L);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));
        when(enquiryDocumentService.allMandatoryDocumentsVerified(1L))
            .thenReturn(new DocumentVerificationStatusResponse(true, true, List.of(), List.of()));
        when(studentRepository.existsByEmail("ravi@college.edu")).thenReturn(false);
        when(studentRepository.save(any(Student.class))).thenReturn(savedStudent);
        when(admissionRepository.save(any(Admission.class))).thenReturn(savedAdmission);
        when(enquiryDocumentRepository.findByEnquiryId(1L)).thenReturn(List.of(verifiedDoc, uploadedDoc, notUploadedDoc));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(admitted);

        enquiryService.convertToStudentWithData(1L, request, "admin");

        verify(enquiryDocumentRepository).saveAll(List.of(verifiedDoc, uploadedDoc, notUploadedDoc));
        assertThat(verifiedDoc.getAdmission()).isEqualTo(savedAdmission);
        assertThat(uploadedDoc.getAdmission()).isEqualTo(savedAdmission);
        assertThat(notUploadedDoc.getAdmission()).isEqualTo(savedAdmission);
    }

    @Test
    void shouldThrowWhenConvertingWithUnverifiedDocuments() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.DOCUMENTS_VERIFIED);

        EnquiryConversionRequest request = new EnquiryConversionRequest(
            "Ravi", "Kumar", "ravi@college.edu", "9876543210", 1, LocalDate.of(2024, 7, 1),
            100L, LocalDate.of(2024, 7, 1), true, true,
            null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null
        );

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));
        when(enquiryDocumentService.allMandatoryDocumentsVerified(1L))
            .thenReturn(new DocumentVerificationStatusResponse(
                false, false,
                List.of("TENTH_MARKSHEET", "AADHAR_CARD"),
                List.of("TENTH_MARKSHEET", "AADHAR_CARD")));

        assertThatThrownBy(() -> enquiryService.convertToStudentWithData(1L, request, "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("mandatory documents must be verified");
    }

    @Test
    void shouldConvertToStudentWithFullStudentAndAdmissionData() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.DOCUMENTS_VERIFIED);

        com.cms.dto.AddressRequest address = new com.cms.dto.AddressRequest(
            1L, "Door 12", "MG Road", "Salem", "Salem", "TN", "636001"
        );

        EnquiryConversionRequest request = new EnquiryConversionRequest(
            "Ravi", "Kumar", "ravi@college.edu", "9876543210", 1, LocalDate.of(2024, 7, 1),
            100L, LocalDate.of(2024, 7, 1), true, true,
            LocalDate.of(2005, 5, 10),
            com.cms.model.enums.Gender.MALE,
            "1234-5678-9012",
            "Indian",
            "Hindu",
            "BC",
            "Vanniyar",
            "O+",
            "Kumar S",
            null,
            null,
            "Latha K",
            null,
            null,
            "9000000001",
            address,
            "Salem",
            LocalDate.of(2024, 7, 1)
        );

        Student savedStudent = new Student(null, "Ravi", "Kumar", "ravi@college.edu",
            testProgram, 1, LocalDate.of(2024, 7, 1), com.cms.model.enums.StudentStatus.ACTIVE);
        savedStudent.setId(10L);

        Enquiry admitted = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ADMITTED);
        admitted.setConvertedStudentId(10L);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));
        when(enquiryDocumentService.allMandatoryDocumentsVerified(1L))
            .thenReturn(new DocumentVerificationStatusResponse(true, true, List.of(), List.of()));
        when(studentRepository.existsByEmail("ravi@college.edu")).thenReturn(false);
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> {
            Student s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });
        when(admissionRepository.save(any(Admission.class))).thenAnswer(inv -> inv.getArgument(0));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(admitted);

        enquiryService.convertToStudentWithData(1L, request, "admin");

        org.mockito.ArgumentCaptor<Student> studentCaptor = org.mockito.ArgumentCaptor.forClass(Student.class);
        verify(studentRepository, org.mockito.Mockito.times(2)).save(studentCaptor.capture());
        Student persistedStudent = studentCaptor.getValue();
        assertThat(persistedStudent.getAdmissionNumber()).isEqualTo("ADM-2425-0001");
        assertThat(persistedStudent.getDateOfBirth()).isEqualTo(LocalDate.of(2005, 5, 10));
        assertThat(persistedStudent.getGender()).isEqualTo(com.cms.model.enums.Gender.MALE);
        assertThat(persistedStudent.getAadharNumber()).isEqualTo("1234-5678-9012");
        assertThat(persistedStudent.getNationality()).isEqualTo("Indian");
        assertThat(persistedStudent.getReligion()).isEqualTo("Hindu");
        assertThat(persistedStudent.getCommunityCategory()).isEqualTo("BC");
        assertThat(persistedStudent.getCaste()).isEqualTo("Vanniyar");
        assertThat(persistedStudent.getBloodGroup()).isEqualTo("O+");
        assertThat(persistedStudent.getFatherName()).isEqualTo("Kumar S");
        assertThat(persistedStudent.getMotherName()).isEqualTo("Latha K");
        assertThat(persistedStudent.getParentMobile()).isEqualTo("9000000001");
        assertThat(persistedStudent.getAddress()).isNotNull();
        assertThat(persistedStudent.getAddress().getCity()).isEqualTo("Salem");
        assertThat(persistedStudent.getAddress().getPincode()).isEqualTo("636001");

        org.mockito.ArgumentCaptor<Admission> admissionCaptor = org.mockito.ArgumentCaptor.forClass(Admission.class);
        verify(admissionRepository).save(admissionCaptor.capture());
        Admission persistedAdmission = admissionCaptor.getValue();
        assertThat(persistedAdmission.getDeclarationPlace()).isEqualTo("Salem");
        assertThat(persistedAdmission.getDeclarationDate()).isEqualTo(LocalDate.of(2024, 7, 1));
        assertThat(persistedAdmission.getParentConsentGiven()).isTrue();
        assertThat(persistedAdmission.getApplicantConsentGiven()).isTrue();
        assertThat(persistedAdmission.getJoiningAcademicYear()).isNotNull();
        assertThat(persistedAdmission.getJoiningAcademicYear().getId()).isEqualTo(100L);
    }

    @Test
    void shouldGetConversionPrefill() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.DOCUMENTS_SUBMITTED);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));

        EnquiryConversionPrefillResponse prefill = enquiryService.getConversionPrefill(1L);

        assertThat(prefill.firstName()).isEqualTo("Ravi");
        assertThat(prefill.lastName()).isEqualTo("Kumar");
        assertThat(prefill.email()).isEqualTo("ravi@email.com");
        assertThat(prefill.programId()).isEqualTo(1L);
        assertThat(prefill.suggestedSemester()).isEqualTo(1);
        assertThat(prefill.suggestedAcademicYearFrom()).isEqualTo(LocalDate.now().getYear());
        assertThat(prefill.suggestedAcademicYearTo()).isEqualTo(LocalDate.now().getYear() + 1);
        assertThat(prefill.suggestedApplicationDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void shouldRejectConversionWhenNotDocumentsVerified() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.FEES_PAID);

        EnquiryConversionRequest request = new EnquiryConversionRequest(
            "Ravi", "Kumar", "ravi@college.edu", "9876543210", 1, LocalDate.of(2024, 7, 1),
            100L, LocalDate.of(2024, 7, 1), null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null
        );

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));

        assertThatThrownBy(() -> enquiryService.convertToStudentWithData(1L, request, "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("DOCUMENTS_VERIFIED");
    }

    @Test
    void shouldRejectConversionWhenDocumentsSubmittedButNotVerified() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.DOCUMENTS_SUBMITTED);

        EnquiryConversionRequest request = new EnquiryConversionRequest(
            "Ravi", "Kumar", "ravi@college.edu", "9876543210", 1, LocalDate.of(2024, 7, 1),
            100L, LocalDate.of(2024, 7, 1), null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null
        );

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));

        assertThatThrownBy(() -> enquiryService.convertToStudentWithData(1L, request, "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("DOCUMENTS_VERIFIED");
    }

    @Test
    void shouldRejectConversionWhenEmailAlreadyExists() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.DOCUMENTS_VERIFIED);

        EnquiryConversionRequest request = new EnquiryConversionRequest(
            "Ravi", "Kumar", "existing@college.edu", "9876543210", 1, LocalDate.of(2024, 7, 1),
            100L, LocalDate.of(2024, 7, 1), null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null
        );

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));
        when(enquiryDocumentService.allMandatoryDocumentsVerified(1L))
            .thenReturn(new DocumentVerificationStatusResponse(true, true, List.of(), List.of()));
        when(studentRepository.existsByEmail("existing@college.edu")).thenReturn(true);

        assertThatThrownBy(() -> enquiryService.convertToStudentWithData(1L, request, "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("email already exists");
    }

    @Test
    void shouldRejectConversionWhenNoProgramOnEnquiry() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            null, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.DOCUMENTS_VERIFIED);

        EnquiryConversionRequest request = new EnquiryConversionRequest(
            "Ravi", "Kumar", "ravi@college.edu", "9876543210", 1, LocalDate.of(2024, 7, 1),
            100L, LocalDate.of(2024, 7, 1), null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null
        );

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));
        when(enquiryDocumentService.allMandatoryDocumentsVerified(1L))
            .thenReturn(new DocumentVerificationStatusResponse(true, true, List.of(), List.of()));
        when(studentRepository.existsByEmail("ravi@college.edu")).thenReturn(false);

        assertThatThrownBy(() -> enquiryService.convertToStudentWithData(1L, request, "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("must have a program");
    }

    @Test
    void shouldGetStatusHistory() {
        com.cms.model.EnquiryStatusHistory history = new com.cms.model.EnquiryStatusHistory();
        history.setId(1L);
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.ENQUIRED);
        history.setEnquiry(enquiry);
        history.setFromStatus(null);
        history.setToStatus(com.cms.model.enums.EnquiryStatus.ENQUIRED);
        history.setChangedBy("system");
        history.setChangedAt(java.time.Instant.now());

        when(enquiryRepository.existsById(1L)).thenReturn(true);
        when(statusHistoryRepository.findByEnquiryIdOrderByChangedAtAsc(1L)).thenReturn(List.of(history));

        List<com.cms.dto.EnquiryStatusHistoryResponse> responses = enquiryService.getStatusHistory(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).toStatus()).isEqualTo("ENQUIRED");
        assertThat(responses.get(0).changedBy()).isEqualTo("system");
    }

    @Test
    void shouldGetConversionPrefillWithSingleWordName() {
        Enquiry enquiry = createEnquiry(1L, "Ravi", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), testReferralType, EnquiryStatus.DOCUMENTS_SUBMITTED);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));

        EnquiryConversionPrefillResponse prefill = enquiryService.getConversionPrefill(1L);

        assertThat(prefill.firstName()).isEqualTo("Ravi");
        assertThat(prefill.lastName()).isEqualTo("");
    }
}
