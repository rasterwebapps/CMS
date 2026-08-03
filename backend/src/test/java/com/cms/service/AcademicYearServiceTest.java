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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.AcademicYearFullUpdateRequest;
import com.cms.dto.AcademicYearRequest;
import com.cms.dto.AcademicYearResponse;
import com.cms.dto.CohortSeatAllocationRequest;
import com.cms.dto.TermBillingDetailsRequest;
import com.cms.dto.TermBillingScheduleRequest;
import com.cms.dto.TermDatesRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Cohort;
import com.cms.model.Program;
import com.cms.model.TermInstance;
import com.cms.model.enums.LateFeeType;
import com.cms.model.enums.ProgramStatus;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.CohortRepository;
import com.cms.repository.FeeStructureGroupRepository;
import com.cms.model.Course;
import com.cms.repository.CourseRepository;

@ExtendWith(MockitoExtension.class)
class AcademicYearServiceTest {

    @Mock
    private AcademicYearRepository academicYearRepository;

    @Mock
    private FeeStructureGroupRepository feeStructureGroupRepository;

    @Mock
    private TermInstanceService termInstanceService;

    @Mock
    private com.cms.repository.TermInstanceRepository termInstanceRepository;

    @Mock
    private TermBillingScheduleService termBillingScheduleService;

    @Mock
    private CohortRepository cohortRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private HolidayTemplateSeedingService holidayTemplateSeedingService;

    private AcademicYearService academicYearService;

    @BeforeEach
    void setUp() {
        academicYearService = new AcademicYearService(
            academicYearRepository, feeStructureGroupRepository, termInstanceService,
            termInstanceRepository, termBillingScheduleService,
            cohortRepository, courseRepository, holidayTemplateSeedingService);
    }

    /** A name/date pair safely in the future relative to whenever the suite actually runs. */
    private static final int FUTURE_YEAR = LocalDate.now().getYear() + 5;
    private static final String FUTURE_NAME = FUTURE_YEAR + "-" + (FUTURE_YEAR + 1);
    private static final LocalDate FUTURE_START = LocalDate.of(FUTURE_YEAR, 8, 1);
    private static final LocalDate FUTURE_END = LocalDate.of(FUTURE_YEAR + 1, 5, 31);

    @Test
    void shouldCreateAcademicYear() {
        AcademicYearRequest request = new AcademicYearRequest(
            FUTURE_NAME,
            FUTURE_START,
            FUTURE_END,
            false
        );

        AcademicYear savedAcademicYear = createAcademicYear(1L, FUTURE_NAME,
            FUTURE_START, FUTURE_END, false);

        when(academicYearRepository.count()).thenReturn(1L);
        when(academicYearRepository.save(any(AcademicYear.class))).thenReturn(savedAcademicYear);

        AcademicYearResponse response = academicYearService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo(FUTURE_NAME);
        assertThat(response.startDate()).isEqualTo(FUTURE_START);
        assertThat(response.endDate()).isEqualTo(FUTURE_END);
        assertThat(response.isCurrent()).isFalse();

        ArgumentCaptor<AcademicYear> captor = ArgumentCaptor.forClass(AcademicYear.class);
        verify(academicYearRepository).save(captor.capture());
        AcademicYear captured = captor.getValue();
        assertThat(captured.getName()).isEqualTo(FUTURE_NAME);
    }

    @Test
    void shouldCreateAcademicYearWithNullIsCurrent() {
        AcademicYearRequest request = new AcademicYearRequest(
            FUTURE_NAME,
            FUTURE_START,
            FUTURE_END,
            null
        );

        AcademicYear savedAcademicYear = createAcademicYear(1L, FUTURE_NAME,
            FUTURE_START, FUTURE_END, false);

        when(academicYearRepository.count()).thenReturn(1L);
        when(academicYearRepository.save(any(AcademicYear.class))).thenReturn(savedAcademicYear);

        AcademicYearResponse response = academicYearService.create(request);

        assertThat(response.isCurrent()).isFalse();

        ArgumentCaptor<AcademicYear> captor = ArgumentCaptor.forClass(AcademicYear.class);
        verify(academicYearRepository).save(captor.capture());
        assertThat(captor.getValue().getIsCurrent()).isFalse();
    }

    @Test
    void shouldCreateAcademicYearAndSetCurrentClearingOthers() {
        AcademicYearRequest request = new AcademicYearRequest(
            FUTURE_NAME,
            FUTURE_START,
            FUTURE_END,
            true
        );

        AcademicYear savedAcademicYear = createAcademicYear(1L, FUTURE_NAME,
            FUTURE_START, FUTURE_END, true);

        when(academicYearRepository.count()).thenReturn(1L);
        when(academicYearRepository.save(any(AcademicYear.class))).thenReturn(savedAcademicYear);

        AcademicYearResponse response = academicYearService.create(request);

        assertThat(response.isCurrent()).isTrue();
        verify(academicYearRepository).clearCurrentAcademicYear();
    }

    @Test
    void shouldForceFirstEverAcademicYearToBeCurrent() {
        AcademicYearRequest request = new AcademicYearRequest(
            FUTURE_NAME,
            FUTURE_START,
            FUTURE_END,
            false
        );

        AcademicYear savedAcademicYear = createAcademicYear(1L, FUTURE_NAME,
            FUTURE_START, FUTURE_END, true);

        when(academicYearRepository.count()).thenReturn(0L);
        when(academicYearRepository.save(any(AcademicYear.class))).thenReturn(savedAcademicYear);

        academicYearService.create(request);

        ArgumentCaptor<AcademicYear> captor = ArgumentCaptor.forClass(AcademicYear.class);
        verify(academicYearRepository).save(captor.capture());
        assertThat(captor.getValue().getIsCurrent()).isTrue();
        verify(academicYearRepository).clearCurrentAcademicYear();
    }

    @Test
    void shouldRejectCreateWhenEndDateIsInThePast() {
        AcademicYearRequest request = new AcademicYearRequest(
            "2024-2025",
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2025, 5, 31),
            false
        );

        assertThatThrownBy(() -> academicYearService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("past");

        verify(academicYearRepository, never()).save(any(AcademicYear.class));
    }

    @Test
    void shouldRejectCreateWhenDatesOverlapExistingAcademicYear() {
        AcademicYearRequest request = new AcademicYearRequest(
            FUTURE_NAME,
            FUTURE_START,
            FUTURE_END,
            false
        );

        when(academicYearRepository.existsOverlapping(FUTURE_START, FUTURE_END, null)).thenReturn(true);

        assertThatThrownBy(() -> academicYearService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("overlap");

        verify(academicYearRepository, never()).save(any(AcademicYear.class));
    }

    @Test
    void shouldRejectUpdateWhenDatesOverlapAnotherAcademicYear() {
        AcademicYear existing = createAcademicYear(1L, FUTURE_NAME, FUTURE_START, FUTURE_END, false);

        AcademicYearRequest request = new AcademicYearRequest(
            FUTURE_NAME,
            FUTURE_START,
            FUTURE_END,
            false
        );

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(academicYearRepository.existsOverlapping(FUTURE_START, FUTURE_END, 1L)).thenReturn(true);

        assertThatThrownBy(() -> academicYearService.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("overlap");

        verify(academicYearRepository, never()).save(any(AcademicYear.class));
    }

    @Test
    void shouldRejectUnmarkingTheOnlyCurrentAcademicYear() {
        AcademicYear existing = createAcademicYear(1L, FUTURE_NAME, FUTURE_START, FUTURE_END, true);

        AcademicYearRequest request = new AcademicYearRequest(
            FUTURE_NAME,
            FUTURE_START,
            FUTURE_END,
            false
        );

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> academicYearService.update(1L, request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("current");

        verify(academicYearRepository, never()).save(any(AcademicYear.class));
    }

    @Test
    void shouldThrowExceptionWhenEndDateIsBeforeStartDate() {
        AcademicYearRequest request = new AcademicYearRequest(
            "2024-2025",
            LocalDate.of(2025, 5, 31),
            LocalDate.of(2024, 8, 1),
            false
        );

        assertThatThrownBy(() -> academicYearService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("End date must be after start date");

        verify(academicYearRepository, never()).save(any(AcademicYear.class));
    }

    @Test
    void shouldThrowExceptionWhenEndDateEqualsStartDate() {
        AcademicYearRequest request = new AcademicYearRequest(
            "2024-2025",
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2024, 8, 1),
            false
        );

        assertThatThrownBy(() -> academicYearService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("End date must be after start date");

        verify(academicYearRepository, never()).save(any(AcademicYear.class));
    }

    @Test
    void shouldFindAllAcademicYears() {
        AcademicYear ay1 = createAcademicYear(1L, "2023-2024",
            LocalDate.of(2023, 8, 1), LocalDate.of(2024, 5, 31), false);
        AcademicYear ay2 = createAcademicYear(2L, "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), true);

        when(academicYearRepository.findAll()).thenReturn(List.of(ay1, ay2));

        List<AcademicYearResponse> responses = academicYearService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).name()).isEqualTo("2023-2024");
        assertThat(responses.get(1).name()).isEqualTo("2024-2025");
        verify(academicYearRepository).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoAcademicYears() {
        when(academicYearRepository.findAll()).thenReturn(List.of());

        List<AcademicYearResponse> responses = academicYearService.findAll();

        assertThat(responses).isEmpty();
        verify(academicYearRepository).findAll();
    }

    @Test
    void shouldFindAcademicYearById() {
        AcademicYear academicYear = createAcademicYear(1L, "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), true);

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));

        AcademicYearResponse response = academicYearService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("2024-2025");
        verify(academicYearRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenAcademicYearNotFoundById() {
        when(academicYearRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> academicYearService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Academic year not found with id: 999");

        verify(academicYearRepository).findById(999L);
    }

    @Test
    void shouldFindCurrentAcademicYear() {
        AcademicYear academicYear = createAcademicYear(1L, "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), true);

        when(academicYearRepository.findByIsCurrentTrue()).thenReturn(Optional.of(academicYear));

        AcademicYearResponse response = academicYearService.findCurrent();

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.isCurrent()).isTrue();
        verify(academicYearRepository).findByIsCurrentTrue();
    }

    @Test
    void shouldThrowExceptionWhenNoCurrentAcademicYear() {
        when(academicYearRepository.findByIsCurrentTrue()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> academicYearService.findCurrent())
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("No current academic year found");

        verify(academicYearRepository).findByIsCurrentTrue();
    }

    @Test
    void shouldUpdateAcademicYear() {
        AcademicYear existingAcademicYear = createAcademicYear(1L, "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), false);

        AcademicYearRequest updateRequest = new AcademicYearRequest(
            "2025-2026",
            LocalDate.of(2024, 9, 1),
            LocalDate.of(2025, 6, 30),
            false
        );

        AcademicYear updatedAcademicYear = createAcademicYear(1L, "2025-2026",
            LocalDate.of(2024, 9, 1), LocalDate.of(2025, 6, 30), false);

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(existingAcademicYear));
        when(academicYearRepository.existsByNameIgnoreCaseAndIdNot("2025-2026", 1L)).thenReturn(false);
        when(academicYearRepository.save(any(AcademicYear.class))).thenReturn(updatedAcademicYear);

        AcademicYearResponse response = academicYearService.update(1L, updateRequest);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("2025-2026");
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2024, 9, 1));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2025, 6, 30));
        verify(academicYearRepository).findById(1L);
        verify(academicYearRepository).save(any(AcademicYear.class));
    }

    @Test
    void shouldThrowWhenUpdatingAcademicYearWithDuplicateName() {
        AcademicYear existing = createAcademicYear(1L, "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), false);

        AcademicYearRequest request = new AcademicYearRequest(
            "2023-2024",
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2025, 5, 31),
            false
        );

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(academicYearRepository.existsByNameIgnoreCaseAndIdNot("2023-2024", 1L)).thenReturn(true);

        assertThatThrownBy(() -> academicYearService.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("2023-2024")
            .hasMessageContaining("already exists");

        verify(academicYearRepository, never()).save(any(AcademicYear.class));
    }

    @Test
    void shouldUpdateAcademicYearAndSetCurrentClearingOthers() {
        AcademicYear existingAcademicYear = createAcademicYear(1L, "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), false);

        AcademicYearRequest updateRequest = new AcademicYearRequest(
            "2024-2025",
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2025, 5, 31),
            true
        );

        AcademicYear updatedAcademicYear = createAcademicYear(1L, "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), true);

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(existingAcademicYear));
        when(academicYearRepository.existsByNameIgnoreCaseAndIdNot("2024-2025", 1L)).thenReturn(false);
        when(academicYearRepository.save(any(AcademicYear.class))).thenReturn(updatedAcademicYear);

        AcademicYearResponse response = academicYearService.update(1L, updateRequest);

        assertThat(response.isCurrent()).isTrue();
        verify(academicYearRepository).clearCurrentAcademicYear();
    }

    @Test
    void shouldNotClearCurrentWhenUpdatingAlreadyCurrentAcademicYear() {
        AcademicYear existingAcademicYear = createAcademicYear(1L, "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), true);

        AcademicYearRequest updateRequest = new AcademicYearRequest(
            "2025-2026",
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2025, 5, 31),
            true
        );

        AcademicYear updatedAcademicYear = createAcademicYear(1L, "2025-2026",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), true);

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(existingAcademicYear));
        when(academicYearRepository.existsByNameIgnoreCaseAndIdNot("2025-2026", 1L)).thenReturn(false);
        when(academicYearRepository.save(any(AcademicYear.class))).thenReturn(updatedAcademicYear);

        academicYearService.update(1L, updateRequest);

        verify(academicYearRepository, never()).clearCurrentAcademicYear();
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentAcademicYear() {
        AcademicYearRequest request = new AcademicYearRequest(
            "2024-2025",
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2025, 5, 31),
            false
        );

        when(academicYearRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> academicYearService.update(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Academic year not found with id: 999");

        verify(academicYearRepository).findById(999L);
        verify(academicYearRepository, never()).save(any(AcademicYear.class));
    }

    @Test
    void shouldRejectUpdateThatShrinksAcademicYearBelowExistingTermInstance() {
        AcademicYear existing = createAcademicYear(1L, "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), false);

        AcademicYearRequest request = new AcademicYearRequest(
            "2024-2025",
            LocalDate.of(2024, 9, 1),
            LocalDate.of(2025, 5, 31),
            false
        );

        com.cms.dto.TermInstanceDto oddTerm = new com.cms.dto.TermInstanceDto(
            10L, 1L, "2024-2025", com.cms.model.enums.TermType.ODD,
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 1, 31),
            com.cms.model.enums.TermInstanceStatus.PLANNED, Instant.now(), Instant.now());

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(termInstanceService.getTermInstancesByAcademicYear(1L)).thenReturn(List.of(oddTerm));

        // Shrinking the start to Sep 1 would exclude the ODD term, which still starts Aug 1.
        assertThatThrownBy(() -> academicYearService.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ODD");

        verify(academicYearRepository, never()).save(any(AcademicYear.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingWithInvalidDates() {
        AcademicYear existingAcademicYear = createAcademicYear(1L, "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), false);

        AcademicYearRequest request = new AcademicYearRequest(
            "2024-2025",
            LocalDate.of(2025, 5, 31),
            LocalDate.of(2024, 8, 1),
            false
        );

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(existingAcademicYear));

        assertThatThrownBy(() -> academicYearService.update(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("End date must be after start date");

        verify(academicYearRepository, never()).save(any(AcademicYear.class));
    }

    // ── updateFull ──────────────────────────────────────────────────────────────────

    @Test
    void shouldFixDeadlockWhenShrinkingAcademicYearAndItsTermTogether() {
        // The old separate-calls flow deadlocked here: AcademicYearService.update() would check
        // the new (narrower) AY start against the ODD term's OLD (wider) start and reject, before
        // the term ever got a chance to narrow too. updateFull() must validate the combined target
        // state instead and succeed.
        AcademicYear existing = createAcademicYear(1L, "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), false);
        TermInstance odd = createTermInstance(1L, existing, TermType.ODD,
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 1, 31));
        TermInstance even = createTermInstance(2L, existing, TermType.EVEN,
            LocalDate.of(2025, 2, 1), LocalDate.of(2025, 5, 31));

        AcademicYearFullUpdateRequest request = new AcademicYearFullUpdateRequest(
            "2024-2025",
            LocalDate.of(2024, 9, 1), LocalDate.of(2025, 5, 31), false,
            new TermDatesRequest(LocalDate.of(2024, 9, 1), LocalDate.of(2025, 1, 31)),
            new TermDatesRequest(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 5, 31)),
            new TermBillingDetailsRequest(LocalDate.of(2024, 9, 15), LateFeeType.FLAT, new BigDecimal("500"), 5),
            new TermBillingDetailsRequest(LocalDate.of(2025, 2, 15), LateFeeType.FLAT, new BigDecimal("500"), 5));

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(academicYearRepository.existsByNameIgnoreCaseAndIdNot("2024-2025", 1L)).thenReturn(false);
        when(academicYearRepository.save(any(AcademicYear.class))).thenReturn(existing);
        when(termInstanceRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD)).thenReturn(Optional.of(odd));
        when(termInstanceRepository.findByAcademicYearIdAndTermType(1L, TermType.EVEN)).thenReturn(Optional.of(even));

        academicYearService.updateFull(1L, request);

        assertThat(odd.getStartDate()).isEqualTo(LocalDate.of(2024, 9, 1));
        verify(termInstanceRepository).save(odd);
        verify(termInstanceRepository).save(even);
        verify(termBillingScheduleService, org.mockito.Mockito.times(2))
            .createOrUpdate(any(TermBillingScheduleRequest.class));
    }

    @Test
    void shouldValidateCombinedNewBoundsNotStalePersistedTermDates() {
        // The crux of the fix: assertTermWithinAcademicYear must be called with the REQUEST's new
        // term dates, not whatever is still sitting on the persisted TermInstance entity.
        AcademicYear existing = createAcademicYear(1L, "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), false);
        TermInstance odd = createTermInstance(1L, existing, TermType.ODD,
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 1, 31));
        TermInstance even = createTermInstance(2L, existing, TermType.EVEN,
            LocalDate.of(2025, 2, 1), LocalDate.of(2025, 5, 31));

        AcademicYearFullUpdateRequest request = new AcademicYearFullUpdateRequest(
            "2024-2025",
            LocalDate.of(2024, 9, 1), LocalDate.of(2025, 6, 30), false,
            new TermDatesRequest(LocalDate.of(2024, 9, 1), LocalDate.of(2025, 1, 31)),
            new TermDatesRequest(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 6, 30)),
            new TermBillingDetailsRequest(LocalDate.of(2024, 9, 15), LateFeeType.FLAT, new BigDecimal("500"), 5),
            new TermBillingDetailsRequest(LocalDate.of(2025, 2, 15), LateFeeType.FLAT, new BigDecimal("500"), 5));

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(academicYearRepository.existsByNameIgnoreCaseAndIdNot("2024-2025", 1L)).thenReturn(false);
        when(academicYearRepository.save(any(AcademicYear.class))).thenReturn(existing);
        when(termInstanceRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD)).thenReturn(Optional.of(odd));
        when(termInstanceRepository.findByAcademicYearIdAndTermType(1L, TermType.EVEN)).thenReturn(Optional.of(even));

        academicYearService.updateFull(1L, request);

        verify(termInstanceService).assertTermWithinAcademicYear(
            LocalDate.of(2024, 9, 1), LocalDate.of(2025, 1, 31),
            LocalDate.of(2024, 9, 1), LocalDate.of(2025, 6, 30));
        verify(termInstanceService).assertTermWithinAcademicYear(
            LocalDate.of(2025, 2, 1), LocalDate.of(2025, 6, 30),
            LocalDate.of(2024, 9, 1), LocalDate.of(2025, 6, 30));
    }

    @Test
    void shouldRejectFullUpdateWhenTermsOverlapEachOther() {
        AcademicYear existing = createAcademicYear(1L, "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), false);
        TermInstance odd = createTermInstance(1L, existing, TermType.ODD,
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 1, 31));
        TermInstance even = createTermInstance(2L, existing, TermType.EVEN,
            LocalDate.of(2025, 2, 1), LocalDate.of(2025, 5, 31));

        AcademicYearFullUpdateRequest request = new AcademicYearFullUpdateRequest(
            "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), false,
            new TermDatesRequest(LocalDate.of(2024, 8, 1), LocalDate.of(2025, 2, 10)),
            new TermDatesRequest(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 5, 31)),
            new TermBillingDetailsRequest(LocalDate.of(2024, 8, 15), LateFeeType.FLAT, new BigDecimal("500"), 5),
            new TermBillingDetailsRequest(LocalDate.of(2025, 2, 15), LateFeeType.FLAT, new BigDecimal("500"), 5));

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(academicYearRepository.existsByNameIgnoreCaseAndIdNot("2024-2025", 1L)).thenReturn(false);
        when(termInstanceRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD)).thenReturn(Optional.of(odd));
        when(termInstanceRepository.findByAcademicYearIdAndTermType(1L, TermType.EVEN)).thenReturn(Optional.of(even));
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Term dates overlap with the EVEN term"))
            .when(termInstanceService).assertTermsDoNotOverlap(
                LocalDate.of(2024, 8, 1), LocalDate.of(2025, 2, 10),
                LocalDate.of(2025, 2, 1), LocalDate.of(2025, 5, 31), "EVEN");

        assertThatThrownBy(() -> academicYearService.updateFull(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("overlap");

        verify(academicYearRepository, never()).save(any(AcademicYear.class));
        verify(termInstanceRepository, never()).save(any(TermInstance.class));
    }

    @Test
    void shouldThrowWhenAcademicYearNotFoundForFullUpdate() {
        AcademicYearFullUpdateRequest request = new AcademicYearFullUpdateRequest(
            "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), false,
            new TermDatesRequest(LocalDate.of(2024, 8, 1), LocalDate.of(2025, 1, 31)),
            new TermDatesRequest(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 5, 31)),
            new TermBillingDetailsRequest(LocalDate.of(2024, 8, 15), LateFeeType.FLAT, new BigDecimal("500"), 5),
            new TermBillingDetailsRequest(LocalDate.of(2025, 2, 15), LateFeeType.FLAT, new BigDecimal("500"), 5));

        when(academicYearRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> academicYearService.updateFull(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void shouldThrowWhenOddTermInstanceMissingForFullUpdate() {
        AcademicYear existing = createAcademicYear(1L, "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), false);

        AcademicYearFullUpdateRequest request = new AcademicYearFullUpdateRequest(
            "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), false,
            new TermDatesRequest(LocalDate.of(2024, 8, 1), LocalDate.of(2025, 1, 31)),
            new TermDatesRequest(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 5, 31)),
            new TermBillingDetailsRequest(LocalDate.of(2024, 8, 15), LateFeeType.FLAT, new BigDecimal("500"), 5),
            new TermBillingDetailsRequest(LocalDate.of(2025, 2, 15), LateFeeType.FLAT, new BigDecimal("500"), 5));

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(academicYearRepository.existsByNameIgnoreCaseAndIdNot("2024-2025", 1L)).thenReturn(false);
        when(termInstanceRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> academicYearService.updateFull(1L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("ODD");
    }

    @Test
    void shouldRejectFullUpdateUnmarkingTheOnlyCurrentAcademicYear() {
        AcademicYear existing = createAcademicYear(1L, "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), true);

        AcademicYearFullUpdateRequest request = new AcademicYearFullUpdateRequest(
            "2024-2025",
            LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31), false,
            new TermDatesRequest(LocalDate.of(2024, 8, 1), LocalDate.of(2025, 1, 31)),
            new TermDatesRequest(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 5, 31)),
            new TermBillingDetailsRequest(LocalDate.of(2024, 8, 15), LateFeeType.FLAT, new BigDecimal("500"), 5),
            new TermBillingDetailsRequest(LocalDate.of(2025, 2, 15), LateFeeType.FLAT, new BigDecimal("500"), 5));

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(academicYearRepository.existsByNameIgnoreCaseAndIdNot("2024-2025", 1L)).thenReturn(false);

        assertThatThrownBy(() -> academicYearService.updateFull(1L, request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("current");

        verify(academicYearRepository, never()).save(any(AcademicYear.class));
    }

    private TermInstance createTermInstance(Long id, AcademicYear academicYear, TermType termType,
                                             LocalDate startDate, LocalDate endDate) {
        TermInstance ti = new TermInstance(academicYear, termType, startDate, endDate, TermInstanceStatus.PLANNED);
        ti.setId(id);
        ti.setCreatedAt(Instant.now());
        ti.setUpdatedAt(Instant.now());
        return ti;
    }

    @Test
    void shouldDeleteAcademicYear() {
        AcademicYear academicYear = createAcademicYear(1L, "2023-2024",
            LocalDate.of(2023, 8, 1), LocalDate.of(2024, 5, 31), false);
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(feeStructureGroupRepository.existsByAcademicYearId(1L)).thenReturn(false);

        academicYearService.delete(1L);

        verify(academicYearRepository).findById(1L);
        verify(academicYearRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeletingAcademicYearWithFeeStructures() {
        AcademicYear academicYear = createAcademicYear(1L, "2023-2024",
            LocalDate.of(2023, 8, 1), LocalDate.of(2024, 5, 31), false);
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(feeStructureGroupRepository.existsByAcademicYearId(1L)).thenReturn(true);

        assertThatThrownBy(() -> academicYearService.delete(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("fee structures");

        verify(academicYearRepository, never()).deleteById(any());
    }

    @Test
    void shouldThrowWhenDeletingTheCurrentAcademicYear() {
        AcademicYear current = createAcademicYear(1L, FUTURE_NAME, FUTURE_START, FUTURE_END, true);
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(current));

        assertThatThrownBy(() -> academicYearService.delete(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("current");

        verify(academicYearRepository, never()).deleteById(any());
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentAcademicYear() {
        when(academicYearRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> academicYearService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Academic year not found with id: 999");

        verify(academicYearRepository).findById(999L);
        verify(academicYearRepository, never()).deleteById(any());
    }

    @Test
    void shouldThrowWhenAcademicYearNameDoesNotMatchFormat() {
        AcademicYearRequest request = new AcademicYearRequest(
            "AY-2024-2025",
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2025, 5, 31),
            false
        );

        assertThatThrownBy(() -> academicYearService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("YYYY-YYYY");
    }

    @Test
    void shouldThrowWhenAcademicYearEndIsNotStartPlusOne() {
        AcademicYearRequest request = new AcademicYearRequest(
            "2024-2026",
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2026, 5, 31),
            false
        );

        assertThatThrownBy(() -> academicYearService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("one year");
    }

    @Test
    void shouldAutoCreateTermInstancesWhenAcademicYearIsCreated() {
        AcademicYearRequest request = new AcademicYearRequest(
            "2026-2027",
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2027, 5, 31),
            false
        );

        AcademicYear saved = createAcademicYear(1L, "2026-2027",
            LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31), false);

        when(academicYearRepository.save(any(AcademicYear.class))).thenReturn(saved);

        academicYearService.create(request);

        verify(termInstanceService).createTermInstancesForAcademicYear(any(AcademicYear.class));
    }

    @Test
    void shouldCreateCohortsWithSeatAllocationsWhenAcademicYearIsCreated() {
        AcademicYearRequest request = new AcademicYearRequest(
            "2026-2027",
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2027, 5, 31),
            false,
            List.of(new CohortSeatAllocationRequest(10L, 60, new java.math.BigDecimal("75")))
        );

        AcademicYear saved = createAcademicYear(1L, "2026-2027",
            LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31), false);
        Program bca = createProgram(10L, "BCA", "BCA", 3, ProgramStatus.ACTIVE);

        when(academicYearRepository.save(any(AcademicYear.class))).thenReturn(saved);
        Course bcaCourse = createCourse(10L, bca);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(bcaCourse));
        when(academicYearRepository.findByName("2029-2030")).thenReturn(Optional.empty());

        academicYearService.create(request);

        ArgumentCaptor<Cohort> captor = ArgumentCaptor.forClass(Cohort.class);
        verify(cohortRepository).save(captor.capture());
        Cohort cohort = captor.getValue();
        assertThat(cohort.getProgram()).isEqualTo(bca);
        assertThat(cohort.getAdmissionAcademicYear()).isEqualTo(saved);
        assertThat(cohort.getCohortCode()).isEqualTo("BCA-2026-2029");
        assertThat(cohort.getManagementSeats()).isEqualTo(45);
        assertThat(cohort.getCounsellingSeats()).isEqualTo(15);
    }

    @Test
    void shouldRejectSeatAllocationForInactiveProgram() {
        AcademicYearRequest request = new AcademicYearRequest(
            "2026-2027",
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2027, 5, 31),
            false,
            List.of(new CohortSeatAllocationRequest(10L, 60, new java.math.BigDecimal("75")))
        );

        AcademicYear saved = createAcademicYear(1L, "2026-2027",
            LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31), false);
        Program inactive = createProgram(10L, "BCA", "BCA", 3, ProgramStatus.INACTIVE);

        when(academicYearRepository.save(any(AcademicYear.class))).thenReturn(saved);
        Course inactiveCourse = createCourse(10L, inactive);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(inactiveCourse));

        assertThatThrownBy(() -> academicYearService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("active programs");
    }

    private AcademicYear createAcademicYear(Long id, String name, LocalDate startDate,
                                             LocalDate endDate, Boolean isCurrent) {
        AcademicYear academicYear = new AcademicYear(name, startDate, endDate, isCurrent);
        academicYear.setId(id);
        Instant now = Instant.now();
        academicYear.setCreatedAt(now);
        academicYear.setUpdatedAt(now);
        return academicYear;
    }

    private Program createProgram(Long id, String name, String code, Integer durationYears,
                                  ProgramStatus status) {
        Program program = new Program(name, code, durationYears, status);
        program.setId(id);
        return program;
    }

    private Course createCourse(Long id, Program program) {
        Course course = new Course(program.getName(), program.getCode(), null, program);
        course.setId(id);
        return course;
    }
}
