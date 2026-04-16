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

import com.cms.dto.EnquiryRequest;
import com.cms.dto.EnquiryResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Agent;
import com.cms.model.Enquiry;
import com.cms.model.Program;
import com.cms.model.Student;
import com.cms.model.enums.EnquirySource;
import com.cms.model.enums.EnquiryStatus;
import com.cms.repository.AgentRepository;
import com.cms.repository.EnquiryRepository;
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
    private ReferralTypeRepository referralTypeRepository;

    private EnquiryService enquiryService;

    private Program testProgram;
    private Agent testAgent;

    @BeforeEach
    void setUp() {
        enquiryService = new EnquiryService(enquiryRepository, programRepository, agentRepository, studentRepository, referralTypeRepository);

        testProgram = new Program();
        testProgram.setId(1L);
        testProgram.setName("B.Tech CS");

        testAgent = new Agent("John Agent", "9876543210", "john@agent.com", "Salem", "Local Area", true);
        testAgent.setId(1L);
    }

    @Test
    void shouldCreateEnquiry() {
        EnquiryRequest request = new EnquiryRequest(
            "Ravi Kumar", "ravi@email.com", "9876543210", 1L,
            LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.ENQUIRED,
            1L, null, "Admin", "Interested in CS", new BigDecimal("50000.00"),
            null, null, null, null
        );

        Enquiry saved = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.ENQUIRED);
        saved.setAgent(testAgent);

        when(programRepository.findById(1L)).thenReturn(Optional.of(testProgram));
        when(agentRepository.findById(1L)).thenReturn(Optional.of(testAgent));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(saved);

        EnquiryResponse response = enquiryService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Ravi Kumar");
        assertThat(response.source()).isEqualTo(EnquirySource.WALK_IN);
        assertThat(response.status()).isEqualTo(EnquiryStatus.ENQUIRED);
        verify(enquiryRepository).save(any(Enquiry.class));
    }

    @Test
    void shouldCreateEnquiryWithoutProgramAndAgent() {
        EnquiryRequest request = new EnquiryRequest(
            "Ravi Kumar", "ravi@email.com", "9876543210", null,
            LocalDate.of(2024, 6, 15), EnquirySource.PHONE, null,
            null, null, null, null, null,
            null, null, null, null
        );

        Enquiry saved = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            null, LocalDate.of(2024, 6, 15), EnquirySource.PHONE, EnquiryStatus.ENQUIRED);

        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(saved);

        EnquiryResponse response = enquiryService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.programId()).isNull();
        assertThat(response.agentId()).isNull();
    }

    @Test
    void shouldThrowWhenProgramNotFoundOnCreate() {
        EnquiryRequest request = new EnquiryRequest(
            "Ravi Kumar", "ravi@email.com", "9876543210", 999L,
            LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.ENQUIRED,
            null, null, null, null, null,
            null, null, null, null
        );

        when(programRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enquiryService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Program not found with id: 999");
    }

    @Test
    void shouldThrowWhenAgentNotFoundOnCreate() {
        EnquiryRequest request = new EnquiryRequest(
            "Ravi Kumar", "ravi@email.com", "9876543210", null,
            LocalDate.of(2024, 6, 15), EnquirySource.AGENT_REFERRAL, EnquiryStatus.ENQUIRED,
            999L, null, null, null, null,
            null, null, null, null
        );

        when(agentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enquiryService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Agent not found with id: 999");
    }

    @Test
    void shouldFindAllEnquiries() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.ENQUIRED);

        when(enquiryRepository.findAll()).thenReturn(List.of(enquiry));

        List<EnquiryResponse> responses = enquiryService.findAll();

        assertThat(responses).hasSize(1);
        verify(enquiryRepository).findAll();
    }

    @Test
    void shouldFindById() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.ENQUIRED);

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
            testProgram, LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.ENQUIRED);

        when(enquiryRepository.findByStatus(EnquiryStatus.ENQUIRED)).thenReturn(List.of(enquiry));

        List<EnquiryResponse> responses = enquiryService.findByStatus(EnquiryStatus.ENQUIRED);

        assertThat(responses).hasSize(1);
        verify(enquiryRepository).findByStatus(EnquiryStatus.ENQUIRED);
    }

    @Test
    void shouldFindBySource() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.ENQUIRED);

        when(enquiryRepository.findBySource(EnquirySource.WALK_IN)).thenReturn(List.of(enquiry));

        List<EnquiryResponse> responses = enquiryService.findBySource(EnquirySource.WALK_IN);

        assertThat(responses).hasSize(1);
        verify(enquiryRepository).findBySource(EnquirySource.WALK_IN);
    }

    @Test
    void shouldFindByAgentId() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), EnquirySource.AGENT_REFERRAL, EnquiryStatus.ENQUIRED);
        enquiry.setAgent(testAgent);

        when(enquiryRepository.findByAgentId(1L)).thenReturn(List.of(enquiry));

        List<EnquiryResponse> responses = enquiryService.findByAgentId(1L);

        assertThat(responses).hasSize(1);
        verify(enquiryRepository).findByAgentId(1L);
    }

    @Test
    void shouldUpdateEnquiry() {
        Enquiry existing = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.ENQUIRED);

        EnquiryRequest updateRequest = new EnquiryRequest(
            "Ravi Kumar Updated", "ravi.updated@email.com", "1234567890", 1L,
            LocalDate.of(2024, 6, 20), EnquirySource.PHONE, EnquiryStatus.INTERESTED,
            null, null, "Staff", "Called back", new BigDecimal("45000.00"),
            null, null, null, null
        );

        Enquiry updated = createEnquiry(1L, "Ravi Kumar Updated", "ravi.updated@email.com", "1234567890",
            testProgram, LocalDate.of(2024, 6, 20), EnquirySource.PHONE, EnquiryStatus.INTERESTED);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(programRepository.findById(1L)).thenReturn(Optional.of(testProgram));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(updated);

        EnquiryResponse response = enquiryService.update(1L, updateRequest);

        assertThat(response.name()).isEqualTo("Ravi Kumar Updated");
        assertThat(response.status()).isEqualTo(EnquiryStatus.INTERESTED);
    }

    @Test
    void shouldThrowWhenNotFoundOnUpdate() {
        EnquiryRequest request = new EnquiryRequest(
            "Ravi Kumar", "ravi@email.com", "9876543210", null,
            LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.ENQUIRED,
            null, null, null, null, null,
            null, null, null, null
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
            testProgram, LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.INTERESTED);

        Student student = new Student();
        student.setId(10L);

        Enquiry converted = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.CONVERTED);
        converted.setConvertedStudentId(10L);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(converted);

        EnquiryResponse response = enquiryService.convertToStudent(1L, 10L);

        assertThat(response.status()).isEqualTo(EnquiryStatus.CONVERTED);
        assertThat(response.convertedStudentId()).isEqualTo(10L);
    }

    @Test
    void shouldConvertToStudentWhenFeeDiscussed() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.FEES_FINALIZED);

        Student student = new Student();
        student.setId(10L);

        Enquiry converted = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.CONVERTED);
        converted.setConvertedStudentId(10L);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(converted);

        EnquiryResponse response = enquiryService.convertToStudent(1L, 10L);

        assertThat(response.status()).isEqualTo(EnquiryStatus.CONVERTED);
    }

    @Test
    void shouldThrowWhenConvertingWithInvalidStatus() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.ENQUIRED);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));

        assertThatThrownBy(() -> enquiryService.convertToStudent(1L, 10L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Enquiry must be in an eligible status to convert");
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
            testProgram, LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.INTERESTED);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enquiryService.convertToStudent(1L, 999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Student not found with id: 999");
    }

    @Test
    void shouldDeleteEnquiry() {
        when(enquiryRepository.existsById(1L)).thenReturn(true);

        enquiryService.delete(1L);

        verify(enquiryRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeletingNonExistent() {
        when(enquiryRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> enquiryService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Enquiry not found with id: 999");

        verify(enquiryRepository, never()).deleteById(any());
    }

    @Test
    void shouldFindByDateRange() {
        LocalDate from = LocalDate.of(2024, 6, 1);
        LocalDate to = LocalDate.of(2024, 6, 30);
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.ENQUIRED);

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
            testProgram, LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.ENQUIRED);

        when(enquiryRepository.findByEnquiryDateBetweenAndStatus(from, to, EnquiryStatus.ENQUIRED))
            .thenReturn(List.of(enquiry));

        List<EnquiryResponse> responses = enquiryService.findByDateRangeAndStatus(from, to, EnquiryStatus.ENQUIRED);

        assertThat(responses).hasSize(1);
        verify(enquiryRepository).findByEnquiryDateBetweenAndStatus(from, to, EnquiryStatus.ENQUIRED);
    }

    @Test
    void shouldUpdateStatus() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.ENQUIRED);

        Enquiry updated = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.INTERESTED);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(updated);

        EnquiryResponse response = enquiryService.updateStatus(1L, EnquiryStatus.INTERESTED);

        assertThat(response.status()).isEqualTo(EnquiryStatus.INTERESTED);
        verify(enquiryRepository).save(any(Enquiry.class));
    }

    @Test
    void shouldThrowWhenUpdatingStatusOfNonExistent() {
        when(enquiryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enquiryService.updateStatus(999L, EnquiryStatus.INTERESTED))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Enquiry not found with id: 999");

        verify(enquiryRepository, never()).save(any());
    }

    private Enquiry createEnquiry(Long id, String name, String email, String phone,
                                   Program program, LocalDate enquiryDate,
                                   EnquirySource source, EnquiryStatus status) {
        Enquiry enquiry = new Enquiry(name, email, phone, program, enquiryDate, source, status);
        enquiry.setId(id);
        Instant now = Instant.now();
        enquiry.setCreatedAt(now);
        enquiry.setUpdatedAt(now);
        return enquiry;
    }

    @Test
    void shouldFinalizeFees() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.INTERESTED);

        com.cms.dto.FeeFinalizationRequest request = new com.cms.dto.FeeFinalizationRequest(
            new BigDecimal("100000.00"), new BigDecimal("5000.00"), "Early bird discount", null
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
    void shouldFinalizeFeesFromEnquiredStatus() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.ENQUIRED);

        com.cms.dto.FeeFinalizationRequest request = new com.cms.dto.FeeFinalizationRequest(
            new BigDecimal("50000.00"), null, null, null
        );

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));
        when(enquiryRepository.save(any(Enquiry.class))).thenAnswer(inv -> inv.getArgument(0));

        com.cms.dto.FeeFinalizationResponse response = enquiryService.finalizeFees(1L, request, "admin");

        assertThat(response.finalizedNetFee()).isEqualTo(new BigDecimal("50000.00"));
        assertThat(response.finalizedDiscountAmount()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void shouldThrowWhenFinalizingFeesWithWrongStatus() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.CONVERTED);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));

        com.cms.dto.FeeFinalizationRequest request = new com.cms.dto.FeeFinalizationRequest(
            new BigDecimal("50000.00"), null, null, null
        );

        assertThatThrownBy(() -> enquiryService.finalizeFees(1L, request, "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Enquiry must be in ENQUIRED or INTERESTED status");
    }

    @Test
    void shouldThrowWhenFinalizingFeesForNonExistent() {
        when(enquiryRepository.findById(999L)).thenReturn(Optional.empty());

        com.cms.dto.FeeFinalizationRequest request = new com.cms.dto.FeeFinalizationRequest(
            new BigDecimal("50000.00"), null, null, null
        );

        assertThatThrownBy(() -> enquiryService.finalizeFees(999L, request, "admin"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Enquiry not found with id: 999");
    }

    @Test
    void shouldCreateEnquiryWithReferralType() {
        com.cms.model.ReferralType referralType = new com.cms.model.ReferralType(
            "Staff", "STAFF", new BigDecimal("5000.00"), true, "Staff referral", true
        );
        referralType.setId(1L);
        referralType.setCreatedAt(Instant.now());
        referralType.setUpdatedAt(Instant.now());

        EnquiryRequest request = new EnquiryRequest(
            "Ravi Kumar", "ravi@email.com", "9876543210", null,
            LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, null,
            null, 1L, null, null, null, null, null, null, null
        );

        Enquiry saved = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            null, LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.ENQUIRED);
        saved.setReferralType(referralType);

        when(referralTypeRepository.findById(1L)).thenReturn(Optional.of(referralType));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(saved);

        EnquiryResponse response = enquiryService.create(request);

        assertThat(response.referralTypeId()).isEqualTo(1L);
        assertThat(response.referralTypeName()).isEqualTo("Staff");
        assertThat(response.referralCommissionAmount()).isEqualTo(new BigDecimal("5000.00"));
    }

    @Test
    void shouldThrowWhenReferralTypeNotFound() {
        EnquiryRequest request = new EnquiryRequest(
            "Ravi Kumar", "ravi@email.com", "9876543210", null,
            LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, null,
            null, 999L, null, null, null, null, null, null, null
        );

        when(referralTypeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enquiryService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Referral type not found with id: 999");
    }

    @Test
    void shouldConvertFromDocumentsSubmittedStatus() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.DOCUMENTS_SUBMITTED);

        Student student = new Student();
        student.setId(10L);

        Enquiry converted = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.CONVERTED);
        converted.setConvertedStudentId(10L);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(converted);

        EnquiryResponse response = enquiryService.convertToStudent(1L, 10L);

        assertThat(response.status()).isEqualTo(EnquiryStatus.CONVERTED);
    }

    @Test
    void shouldConvertFromFeesPaidStatus() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.FEES_PAID);

        Student student = new Student();
        student.setId(10L);

        Enquiry converted = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.CONVERTED);
        converted.setConvertedStudentId(10L);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(converted);

        EnquiryResponse response = enquiryService.convertToStudent(1L, 10L);

        assertThat(response.status()).isEqualTo(EnquiryStatus.CONVERTED);
    }

    @Test
    void shouldConvertFromPartiallyPaidStatus() {
        Enquiry enquiry = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.PARTIALLY_PAID);

        Student student = new Student();
        student.setId(10L);

        Enquiry converted = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.CONVERTED);
        converted.setConvertedStudentId(10L);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(converted);

        EnquiryResponse response = enquiryService.convertToStudent(1L, 10L);

        assertThat(response.status()).isEqualTo(EnquiryStatus.CONVERTED);
    }

    @Test
    void shouldCreateEnquiryWithFeeGuidelineData() {
        EnquiryRequest request = new EnquiryRequest(
            "Ravi Kumar", "ravi@email.com", "9876543210", null,
            LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, null,
            null, null, null, null, null,
            new BigDecimal("100000.00"), new BigDecimal("5000.00"),
            new BigDecimal("105000.00"), "[50000,55000]"
        );

        Enquiry saved = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            null, LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.ENQUIRED);
        saved.setFeeGuidelineTotal(new BigDecimal("100000.00"));
        saved.setReferralAdditionalAmount(new BigDecimal("5000.00"));
        saved.setFinalCalculatedFee(new BigDecimal("105000.00"));
        saved.setYearWiseFees("[50000,55000]");

        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(saved);

        EnquiryResponse response = enquiryService.create(request);

        assertThat(response.feeGuidelineTotal()).isEqualTo(new BigDecimal("100000.00"));
        assertThat(response.referralAdditionalAmount()).isEqualTo(new BigDecimal("5000.00"));
        assertThat(response.finalCalculatedFee()).isEqualTo(new BigDecimal("105000.00"));
        assertThat(response.yearWiseFees()).isEqualTo("[50000,55000]");
    }

    @Test
    void shouldUpdateEnquiryWithReferralType() {
        Enquiry existing = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.ENQUIRED);

        com.cms.model.ReferralType referralType = new com.cms.model.ReferralType(
            "Staff", "STAFF", BigDecimal.ZERO, false, "Staff referral", true
        );
        referralType.setId(1L);
        referralType.setCreatedAt(Instant.now());
        referralType.setUpdatedAt(Instant.now());

        EnquiryRequest updateRequest = new EnquiryRequest(
            "Ravi Kumar", "ravi@email.com", "9876543210", 1L,
            LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, null,
            null, 1L, null, null, null, null, null, null, null
        );

        Enquiry updated = createEnquiry(1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            testProgram, LocalDate.of(2024, 6, 15), EnquirySource.WALK_IN, EnquiryStatus.ENQUIRED);
        updated.setReferralType(referralType);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(programRepository.findById(1L)).thenReturn(Optional.of(testProgram));
        when(referralTypeRepository.findById(1L)).thenReturn(Optional.of(referralType));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(updated);

        EnquiryResponse response = enquiryService.update(1L, updateRequest);

        assertThat(response.referralTypeId()).isEqualTo(1L);
    }
}
