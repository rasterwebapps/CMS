package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.TermInstanceDto;
import com.cms.dto.TermInstanceUpdateRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.TermInstance;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.TermInstanceRepository;
import com.cms.service.CourseOfferingService;
import com.cms.service.CourseRegistrationService;
import com.cms.service.FeeDemandService;
import com.cms.service.StudentTermEnrollmentService;

@ExtendWith(MockitoExtension.class)
class TermInstanceServiceTest {

    @Mock
    private TermInstanceRepository termInstanceRepository;

    @Mock
    private AcademicYearRepository academicYearRepository;

    @Mock
    private StudentTermEnrollmentService studentTermEnrollmentService;

    @Mock
    private CourseOfferingService courseOfferingService;

    @Mock
    private CourseRegistrationService courseRegistrationService;

    @Mock
    private FeeDemandService feeDemandService;

    private TermInstanceService termInstanceService;

    private AcademicYear testAcademicYear;

    @BeforeEach
    void setUp() {
        termInstanceService = new TermInstanceService(termInstanceRepository, academicYearRepository);
        termInstanceService.setStudentTermEnrollmentService(studentTermEnrollmentService);
        termInstanceService.setCourseOfferingService(courseOfferingService);
        termInstanceService.setCourseRegistrationService(courseRegistrationService);
        termInstanceService.setFeeDemandService(feeDemandService);
        testAcademicYear = createAcademicYear(1L, "2026-2027",
            LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31));
    }

    @Test
    void shouldCreateTermInstancesForAcademicYear() {
        when(termInstanceRepository.save(any(TermInstance.class))).thenAnswer(inv -> {
            TermInstance ti = inv.getArgument(0);
            ti.setId(1L);
            ti.setCreatedAt(Instant.now());
            ti.setUpdatedAt(Instant.now());
            return ti;
        });

        termInstanceService.createTermInstancesForAcademicYear(testAcademicYear);

        ArgumentCaptor<TermInstance> captor = ArgumentCaptor.forClass(TermInstance.class);
        verify(termInstanceRepository, org.mockito.Mockito.times(2)).save(captor.capture());

        List<TermInstance> saved = captor.getAllValues();
        assertThat(saved).hasSize(2);

        TermInstance odd = saved.get(0);
        assertThat(odd.getTermType()).isEqualTo(TermType.ODD);
        assertThat(odd.getStartDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(odd.getEndDate()).isEqualTo(LocalDate.of(2026, 11, 30));
        assertThat(odd.getStatus()).isEqualTo(TermInstanceStatus.PLANNED);

        TermInstance even = saved.get(1);
        assertThat(even.getTermType()).isEqualTo(TermType.EVEN);
        assertThat(even.getStartDate()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(even.getEndDate()).isEqualTo(LocalDate.of(2027, 5, 31));
        assertThat(even.getStatus()).isEqualTo(TermInstanceStatus.PLANNED);
    }

    @Test
    void shouldDeriveTermWindowsFromNonJuneStartDate() {
        // This college's academic year start month varies year to year (follows the
        // NEET/counselling calendar) — verify a Nov 1 start derives correct windows rather
        // than the old hardcoded June/December pattern.
        AcademicYear novemberStart = createAcademicYear(2L, "2026-2027",
            LocalDate.of(2026, 11, 1), LocalDate.of(2027, 10, 31));

        when(termInstanceRepository.save(any(TermInstance.class))).thenAnswer(inv -> {
            TermInstance ti = inv.getArgument(0);
            ti.setId(1L);
            ti.setCreatedAt(Instant.now());
            ti.setUpdatedAt(Instant.now());
            return ti;
        });

        termInstanceService.createTermInstancesForAcademicYear(novemberStart);

        ArgumentCaptor<TermInstance> captor = ArgumentCaptor.forClass(TermInstance.class);
        verify(termInstanceRepository, org.mockito.Mockito.times(2)).save(captor.capture());

        List<TermInstance> saved = captor.getAllValues();
        TermInstance odd = saved.get(0);
        assertThat(odd.getStartDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(odd.getEndDate()).isEqualTo(LocalDate.of(2027, 4, 30));

        TermInstance even = saved.get(1);
        assertThat(even.getStartDate()).isEqualTo(LocalDate.of(2027, 5, 1));
        assertThat(even.getEndDate()).isEqualTo(LocalDate.of(2027, 10, 31));
    }

    @Test
    void shouldGetTermInstancesByAcademicYear() {
        TermInstance ti = createTermInstance(1L, testAcademicYear, TermType.ODD,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 30), TermInstanceStatus.PLANNED);

        when(academicYearRepository.existsById(1L)).thenReturn(true);
        when(termInstanceRepository.findByAcademicYearId(1L)).thenReturn(List.of(ti));

        List<TermInstanceDto> dtos = termInstanceService.getTermInstancesByAcademicYear(1L);

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).termType()).isEqualTo(TermType.ODD);
        assertThat(dtos.get(0).status()).isEqualTo(TermInstanceStatus.PLANNED);
        assertThat(dtos.get(0).academicYearId()).isEqualTo(1L);
        assertThat(dtos.get(0).academicYearName()).isEqualTo("2026-2027");
    }

    @Test
    void shouldThrowWhenAcademicYearNotFoundForGetTermInstances() {
        when(academicYearRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> termInstanceService.getTermInstancesByAcademicYear(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");

        verify(termInstanceRepository, never()).findByAcademicYearId(any());
    }

    @Test
    void shouldGetTermInstanceById() {
        TermInstance ti = createTermInstance(1L, testAcademicYear, TermType.ODD,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 30), TermInstanceStatus.PLANNED);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(ti));

        TermInstanceDto dto = termInstanceService.getById(1L);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.termType()).isEqualTo(TermType.ODD);
    }

    @Test
    void shouldThrowWhenTermInstanceNotFoundById() {
        when(termInstanceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> termInstanceService.getById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void shouldUpdateTermInstanceDates() {
        TermInstance ti = createTermInstance(1L, testAcademicYear, TermType.ODD,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 30), TermInstanceStatus.PLANNED);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(ti));
        when(termInstanceRepository.save(any(TermInstance.class))).thenReturn(ti);

        TermInstanceUpdateRequest request = new TermInstanceUpdateRequest(
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 15), null);

        TermInstanceDto dto = termInstanceService.updateTermInstance(1L, request);

        assertThat(ti.getStartDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(ti.getEndDate()).isEqualTo(LocalDate.of(2026, 12, 15));
        assertThat(ti.getStatus()).isEqualTo(TermInstanceStatus.PLANNED);
    }

    @Test
    void shouldAllowTermDatesEqualToAcademicYearBoundaries() {
        TermInstance ti = createTermInstance(1L, testAcademicYear, TermType.ODD,
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 11, 30), TermInstanceStatus.PLANNED);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(ti));
        when(termInstanceRepository.save(any(TermInstance.class))).thenReturn(ti);

        // testAcademicYear spans 2026-06-01 to 2027-05-31 — start/end here touch those exact bounds.
        TermInstanceUpdateRequest request = new TermInstanceUpdateRequest(
            LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31), null);

        termInstanceService.updateTermInstance(1L, request);

        assertThat(ti.getStartDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(ti.getEndDate()).isEqualTo(LocalDate.of(2027, 5, 31));
    }

    @Test
    void shouldRejectTermStartDateBeforeAcademicYearStart() {
        TermInstance ti = createTermInstance(1L, testAcademicYear, TermType.ODD,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 30), TermInstanceStatus.PLANNED);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(ti));

        TermInstanceUpdateRequest request = new TermInstanceUpdateRequest(
            LocalDate.of(2026, 5, 1), null, null);

        assertThatThrownBy(() -> termInstanceService.updateTermInstance(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("academic year");

        verify(termInstanceRepository, never()).save(any());
    }

    @Test
    void shouldRejectTermEndDateAfterAcademicYearEnd() {
        TermInstance ti = createTermInstance(1L, testAcademicYear, TermType.EVEN,
            LocalDate.of(2026, 12, 1), LocalDate.of(2027, 5, 31), TermInstanceStatus.PLANNED);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(ti));

        TermInstanceUpdateRequest request = new TermInstanceUpdateRequest(
            null, LocalDate.of(2027, 6, 15), null);

        assertThatThrownBy(() -> termInstanceService.updateTermInstance(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("academic year");

        verify(termInstanceRepository, never()).save(any());
    }

    @Test
    void shouldRejectTermEndDateNotAfterStartDate() {
        TermInstance ti = createTermInstance(1L, testAcademicYear, TermType.ODD,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 30), TermInstanceStatus.PLANNED);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(ti));

        TermInstanceUpdateRequest request = new TermInstanceUpdateRequest(
            LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 1), null);

        assertThatThrownBy(() -> termInstanceService.updateTermInstance(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Term end date must be after start date");

        verify(termInstanceRepository, never()).save(any());
    }

    @Test
    void shouldRejectTermDatesOverlappingSiblingTerm() {
        TermInstance odd = createTermInstance(1L, testAcademicYear, TermType.ODD,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 30), TermInstanceStatus.PLANNED);
        TermInstance even = createTermInstance(2L, testAcademicYear, TermType.EVEN,
            LocalDate.of(2026, 12, 1), LocalDate.of(2027, 5, 31), TermInstanceStatus.PLANNED);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(odd));
        when(termInstanceRepository.findByAcademicYearIdAndTermType(1L, TermType.EVEN))
            .thenReturn(Optional.of(even));

        // Stretching the ODD term's end into December collides with the EVEN term's Dec 1 start.
        TermInstanceUpdateRequest request = new TermInstanceUpdateRequest(
            null, LocalDate.of(2026, 12, 5), null);

        assertThatThrownBy(() -> termInstanceService.updateTermInstance(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("EVEN");

        verify(termInstanceRepository, never()).save(any());
    }

    @Test
    void shouldAllowGapBetweenSiblingTerms() {
        TermInstance odd = createTermInstance(1L, testAcademicYear, TermType.ODD,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 30), TermInstanceStatus.PLANNED);
        TermInstance even = createTermInstance(2L, testAcademicYear, TermType.EVEN,
            LocalDate.of(2026, 12, 15), LocalDate.of(2027, 5, 31), TermInstanceStatus.PLANNED);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(odd));
        when(termInstanceRepository.findByAcademicYearIdAndTermType(1L, TermType.EVEN))
            .thenReturn(Optional.of(even));
        when(termInstanceRepository.save(any(TermInstance.class))).thenReturn(odd);

        // ODD shrinks to end Nov 20, leaving a gap before EVEN starts Dec 15 — gaps are allowed.
        TermInstanceUpdateRequest request = new TermInstanceUpdateRequest(
            null, LocalDate.of(2026, 11, 20), null);

        termInstanceService.updateTermInstance(1L, request);

        assertThat(odd.getEndDate()).isEqualTo(LocalDate.of(2026, 11, 20));
    }

    @Test
    void shouldAllowAssertTermWithinAcademicYearAtExactBoundaries() {
        termInstanceService.assertTermWithinAcademicYear(
            LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31),
            LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31));
        // No exception — inclusive boundary is allowed.
    }

    @Test
    void shouldRejectAssertTermWithinAcademicYearWhenStartBeforeBounds() {
        assertThatThrownBy(() -> termInstanceService.assertTermWithinAcademicYear(
            LocalDate.of(2026, 5, 1), LocalDate.of(2026, 11, 30),
            LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("academic year");
    }

    @Test
    void shouldRejectAssertTermWithinAcademicYearWhenEndAfterBounds() {
        assertThatThrownBy(() -> termInstanceService.assertTermWithinAcademicYear(
            LocalDate.of(2026, 6, 1), LocalDate.of(2027, 6, 15),
            LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("academic year");
    }

    @Test
    void shouldRejectAssertTermWithinAcademicYearWhenEndNotAfterStart() {
        assertThatThrownBy(() -> termInstanceService.assertTermWithinAcademicYear(
            LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Term end date must be after start date");
    }

    @Test
    void shouldRejectAssertTermsDoNotOverlapWhenRangesIntersect() {
        assertThatThrownBy(() -> termInstanceService.assertTermsDoNotOverlap(
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 5),
            LocalDate.of(2026, 12, 1), LocalDate.of(2027, 5, 31), "EVEN"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("EVEN");
    }

    @Test
    void shouldAllowAssertTermsDoNotOverlapWhenThereIsAGap() {
        termInstanceService.assertTermsDoNotOverlap(
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 20),
            LocalDate.of(2026, 12, 15), LocalDate.of(2027, 5, 31), "EVEN");
        // No exception — a gap between the two terms is allowed.
    }

    @Test
    void shouldUpdateTermInstanceStatusFromPlannedToOpen() {
        TermInstance ti = createTermInstance(1L, testAcademicYear, TermType.ODD,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 30), TermInstanceStatus.PLANNED);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(ti));
        when(termInstanceRepository.save(any(TermInstance.class))).thenReturn(ti);

        TermInstanceUpdateRequest request = new TermInstanceUpdateRequest(null, null, TermInstanceStatus.OPEN);

        termInstanceService.updateTermInstance(1L, request);

        assertThat(ti.getStatus()).isEqualTo(TermInstanceStatus.OPEN);
        verify(courseOfferingService).generateOfferingsForTermInstance(1L);
        // Enrollment/registration/fee-demand generation moved to StudentPromotionService —
        // opening a term no longer blindly advances every student by calendar math.
        verify(studentTermEnrollmentService, never()).generateEnrollmentsForTermInstance(anyLong());
        verify(courseRegistrationService, never()).generateRegistrationsForTermInstance(anyLong());
        verify(feeDemandService, never()).generateDemandsForTermInstance(anyLong());
    }

    @Test
    void shouldUpdateTermInstanceStatusFromOpenToLocked() {
        TermInstance ti = createTermInstance(1L, testAcademicYear, TermType.ODD,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 30), TermInstanceStatus.OPEN);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(ti));
        when(termInstanceRepository.save(any(TermInstance.class))).thenReturn(ti);

        TermInstanceUpdateRequest request = new TermInstanceUpdateRequest(null, null, TermInstanceStatus.LOCKED);

        termInstanceService.updateTermInstance(1L, request);

        assertThat(ti.getStatus()).isEqualTo(TermInstanceStatus.LOCKED);
        verify(courseOfferingService).deactivateAllOfferingsForTermInstance(1L);
    }

    @Test
    void shouldRejectInvalidStatusTransitionFromPlannedToLocked() {
        TermInstance ti = createTermInstance(1L, testAcademicYear, TermType.ODD,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 30), TermInstanceStatus.PLANNED);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(ti));

        TermInstanceUpdateRequest request = new TermInstanceUpdateRequest(null, null, TermInstanceStatus.LOCKED);

        assertThatThrownBy(() -> termInstanceService.updateTermInstance(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("PLANNED")
            .hasMessageContaining("LOCKED");

        verify(termInstanceRepository, never()).save(any());
    }

    @Test
    void shouldRejectAnyTransitionFromLocked() {
        TermInstance ti = createTermInstance(1L, testAcademicYear, TermType.ODD,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 30), TermInstanceStatus.LOCKED);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(ti));

        TermInstanceUpdateRequest request = new TermInstanceUpdateRequest(null, null, TermInstanceStatus.OPEN);

        assertThatThrownBy(() -> termInstanceService.updateTermInstance(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("LOCKED");

        verify(termInstanceRepository, never()).save(any());
    }

    @Test
    void shouldRejectBackwardTransitionFromOpenToPlanned() {
        TermInstance ti = createTermInstance(1L, testAcademicYear, TermType.ODD,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 30), TermInstanceStatus.OPEN);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(ti));

        TermInstanceUpdateRequest request = new TermInstanceUpdateRequest(null, null, TermInstanceStatus.PLANNED);

        assertThatThrownBy(() -> termInstanceService.updateTermInstance(1L, request))
            .isInstanceOf(IllegalArgumentException.class);

        verify(termInstanceRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentTermInstance() {
        when(termInstanceRepository.findById(999L)).thenReturn(Optional.empty());

        TermInstanceUpdateRequest request = new TermInstanceUpdateRequest(
            LocalDate.of(2026, 7, 1), null, null);

        assertThatThrownBy(() -> termInstanceService.updateTermInstance(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    private AcademicYear createAcademicYear(Long id, String name,
                                             LocalDate startDate, LocalDate endDate) {
        AcademicYear ay = new AcademicYear(name, startDate, endDate, false);
        ay.setId(id);
        ay.setCreatedAt(Instant.now());
        ay.setUpdatedAt(Instant.now());
        return ay;
    }

    private TermInstance createTermInstance(Long id, AcademicYear academicYear,
                                             TermType termType, LocalDate startDate,
                                             LocalDate endDate, TermInstanceStatus status) {
        TermInstance ti = new TermInstance(academicYear, termType, startDate, endDate, status);
        ti.setId(id);
        ti.setCreatedAt(Instant.now());
        ti.setUpdatedAt(Instant.now());
        return ti;
    }
}
