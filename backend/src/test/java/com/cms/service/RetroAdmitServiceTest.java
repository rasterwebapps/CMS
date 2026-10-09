package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import com.cms.dto.LegacyYearFeeEntry;
import com.cms.dto.RetroAdmitRequest;
import com.cms.model.AcademicYear;
import com.cms.model.Enquiry;
import com.cms.model.Program;
import com.cms.model.ReferralType;
import com.cms.model.Student;
import com.cms.model.enums.AssessmentPattern;
import com.cms.model.enums.Gender;
import com.cms.model.enums.ProgramStatus;
import com.cms.model.enums.StudentType;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.AdmissionRepository;
import com.cms.repository.AgentRepository;
import com.cms.repository.CourseRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.FeeInstallmentRepository;
import com.cms.repository.ProgramRepository;
import com.cms.repository.ReferralTypeRepository;
import com.cms.repository.SemesterFeeRepository;
import com.cms.repository.StudentFeeAllocationRepository;
import com.cms.repository.StudentRepository;
import com.cms.repository.TermBillingScheduleRepository;

/**
 * Covers the retro-admit fee-history gate added after a production incident: a retro-admitted
 * student with no year-fee data got an Admission but no StudentFeeAllocation and no
 * Enquiry.yearWiseFees -- a permanent dead end on the Student Fee Collection screen with no way
 * to ever finalize fees. See RetroAdmitService.admit() for the guard and the
 * Enquiry.yearWiseFees backfill.
 */
@ExtendWith(MockitoExtension.class)
class RetroAdmitServiceTest {

    @Mock private StudentRepository studentRepository;
    @Mock private AdmissionRepository admissionRepository;
    @Mock private EnquiryRepository enquiryRepository;
    @Mock private ProgramRepository programRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private ReferralTypeRepository referralTypeRepository;
    @Mock private AgentRepository agentRepository;
    @Mock private StudentFeeAllocationRepository allocationRepository;
    @Mock private SemesterFeeRepository semesterFeeRepository;
    @Mock private FeeInstallmentRepository installmentRepository;
    @Mock private TermBillingScheduleRepository billingScheduleRepository;
    @Mock private ApplicationNumberSequenceService numberSequenceService;
    @Mock private UnifiedReceiptService unifiedReceiptService;

    private RetroAdmitService service;

    @BeforeEach
    void setUp() {
        service = new RetroAdmitService(studentRepository, admissionRepository, enquiryRepository,
            programRepository, courseRepository, academicYearRepository, referralTypeRepository,
            agentRepository, allocationRepository, semesterFeeRepository, installmentRepository,
            billingScheduleRepository, numberSequenceService, unifiedReceiptService);

        // Every test reaches the yearFees gate, which sits after Program/AcademicYear resolution.
        Program program = new Program("BSc Nursing", "BSC", 4, ProgramStatus.ACTIVE, AssessmentPattern.YEARLY);
        program.setId(1L);
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));

        AcademicYear joiningYear = new AcademicYear("2026-2027", LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31), true);
        joiningYear.setId(10L);
        when(academicYearRepository.findById(10L)).thenReturn(Optional.of(joiningYear));
    }

    @Test
    void rejectsRetroAdmitWhenYearFeesIsNull() {
        assertThatThrownBy(() -> service.admit(baseRequest(null), "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("At least one year's fee amount is required");

        verify(studentRepository, never()).save(any());
        verify(enquiryRepository, never()).save(any());
    }

    @Test
    void rejectsRetroAdmitWhenYearFeesIsEmptyList() {
        assertThatThrownBy(() -> service.admit(baseRequest(List.of()), "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("At least one year's fee amount is required");

        verify(studentRepository, never()).save(any());
        verify(enquiryRepository, never()).save(any());
    }

    @Test
    void backfillsEnquiryYearWiseFeesFromSubmittedYearFeesInYearOrder() {
        stubHappyPathChain();

        // Entries given out of order on purpose -- the backfilled JSON must still come out
        // sorted by yearNumber, matching what getEnquiryYearFees' parser expects to read back.
        List<LegacyYearFeeEntry> yearFees = List.of(
            new LegacyYearFeeEntry(2, new BigDecimal("60000")),
            new LegacyYearFeeEntry(1, new BigDecimal("50000"))
        );

        service.admit(baseRequest(yearFees), "admin");

        ArgumentCaptor<Enquiry> captor = ArgumentCaptor.forClass(Enquiry.class);
        verify(enquiryRepository).save(captor.capture());

        assertThat(captor.getValue().getYearWiseFees())
            .isEqualTo("[{\"yearNumber\":1,\"amount\":50000},{\"yearNumber\":2,\"amount\":60000}]");
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private void stubHappyPathChain() {
        when(studentRepository.save(any())).thenAnswer(inv -> {
            Student s = inv.getArgument(0);
            s.setId(100L);
            return s;
        });

        when(referralTypeRepository.findByCode("WALK_IN")).thenReturn(Optional.of(new ReferralType()));

        when(enquiryRepository.save(any())).thenAnswer(inv -> {
            Enquiry e = inv.getArgument(0);
            e.setId(200L);
            return e;
        });

        when(allocationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private RetroAdmitRequest baseRequest(List<LegacyYearFeeEntry> yearFees) {
        return new RetroAdmitRequest(
            "Jane", "Doe", "jane.doe@example.com", "9999999999",
            "ROLL-1", "URN-1", null,
            1L, null, 10L,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1), 1,
            null, StudentType.DAY_SCHOLAR,
            LocalDate.of(2006, 1, 1), Gender.FEMALE, null,
            null, null, null, null, null, null,
            null, null, null, null, null, null, null,
            null,
            null, null,
            null, null, null, null, null,
            yearFees, List.of()
        );
    }
}
