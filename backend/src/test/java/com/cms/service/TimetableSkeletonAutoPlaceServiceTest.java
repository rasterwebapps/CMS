package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.AutoPlaceResult;
import com.cms.dto.SkeletonBuilderResponse;
import com.cms.dto.SkeletonCellPlacementRequest;
import com.cms.dto.SkeletonCellResponse;
import com.cms.dto.SkeletonSubjectBudget;
import com.cms.dto.SkeletonSubjectResponse;
import com.cms.exception.TimetableConstraintViolationException;
import com.cms.model.AcademicYear;
import com.cms.model.CourseOffering;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.Period;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.PeriodRepository;
import com.cms.repository.TermInstanceRepository;

@ExtendWith(MockitoExtension.class)
class TimetableSkeletonAutoPlaceServiceTest {

    @Mock private TimetableSkeletonService timetableSkeletonService;
    @Mock private CourseOfferingRepository courseOfferingRepository;
    @Mock private PeriodRepository periodRepository;
    @Mock private TimetableBlockedPeriodChecker blockedPeriodChecker;
    @Mock private TermInstanceRepository termInstanceRepository;

    private TimetableSkeletonAutoPlaceService service;
    private TermInstance termInstance;
    private Period period1;

    @BeforeEach
    void setUp() {
        service = new TimetableSkeletonAutoPlaceService(timetableSkeletonService, courseOfferingRepository,
            periodRepository, blockedPeriodChecker, termInstanceRepository);

        AcademicYear ay = new AcademicYear("2024-2025", LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31), false);
        ay.setId(1L);
        termInstance = new TermInstance(ay, TermType.ODD, LocalDate.of(2024, 6, 1), LocalDate.of(2024, 11, 30), TermInstanceStatus.OPEN);
        termInstance.setId(10L);
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));

        period1 = new Period("1st Period", LocalTime.of(9, 0), LocalTime.of(9, 50), 1);
        period1.setId(1L);
        when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(period1));
    }

    private CourseOffering nonElectiveOffering(Long id) {
        CurriculumSemesterCourse csc = new CurriculumSemesterCourse();
        csc.setIsElective(false);
        CourseOffering offering = new CourseOffering();
        offering.setId(id);
        offering.setCurriculumSemesterCourse(csc);
        return offering;
    }

    private SkeletonSubjectResponse subjectWithShortfall(Long offeringId, String name, int required, int placed) {
        SkeletonSubjectBudget budget = new SkeletonSubjectBudget(ClassSessionType.THEORY, null, null, null, null,
            10, 10, required, placed);
        return new SkeletonSubjectResponse(offeringId, name, name.substring(0, 4).toUpperCase(), List.of(budget), null, null);
    }

    private SkeletonCellResponse cellResponse(Long id, Long offeringId, DayOfWeek day, String subjectName) {
        return new SkeletonCellResponse(id, ClassSessionType.THEORY, day, 1L, "1st Period",
            LocalTime.of(9, 0), LocalTime.of(9, 50), null, null, null, null, false, null, null, List.of(),
            offeringId, subjectName, subjectName.substring(0, 4).toUpperCase(), null, null, null);
    }

    @Test
    void shouldFillASimpleShortfall() {
        CourseOffering offering = nonElectiveOffering(100L);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(timetableSkeletonService.isElectiveOffering(offering)).thenReturn(false);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(5L, "Cohort", "Term",
            List.of(subjectWithShortfall(100L, "Anatomy", 1, 0)), List.of(), List.of(), List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 5L)).thenReturn(skeleton);
        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class)))
            .thenReturn(cellResponse(900L, 100L, DayOfWeek.MONDAY, "Anatomy"));

        AutoPlaceResult result = service.autoPlace(10L, 5L);

        assertThat(result.placedCount()).isEqualTo(1);
        assertThat(result.unplaced()).isEmpty();
        verify(timetableSkeletonService).placeCell(new SkeletonCellPlacementRequest(
            100L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, null, 5L, null, null));
    }

    @Test
    void shouldSkipElectiveOfferings() {
        CourseOffering offering = nonElectiveOffering(100L);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(timetableSkeletonService.isElectiveOffering(offering)).thenReturn(true);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(5L, "Cohort", "Term",
            List.of(subjectWithShortfall(100L, "Anatomy", 1, 0)), List.of(), List.of(), List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 5L)).thenReturn(skeleton);

        AutoPlaceResult result = service.autoPlace(10L, 5L);

        assertThat(result.placedCount()).isZero();
        assertThat(result.unplaced()).isEmpty();
        verify(timetableSkeletonService, times(0)).placeCell(any());
    }

    @Test
    void shouldReportUnplacedWhenEveryCandidateIsRejected() {
        CourseOffering offering = nonElectiveOffering(100L);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offering));
        when(timetableSkeletonService.isElectiveOffering(offering)).thenReturn(false);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(5L, "Cohort", "Term",
            List.of(subjectWithShortfall(100L, "Anatomy", 1, 0)), List.of(), List.of(), List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 5L)).thenReturn(skeleton);
        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class)))
            .thenThrow(new TimetableConstraintViolationException(
                List.of(new com.cms.dto.ConstraintViolation("SKELETON_CELL_COHORT_CLASH", "blocked"))));

        AutoPlaceResult result = service.autoPlace(10L, 5L);

        assertThat(result.placedCount()).isZero();
        assertThat(result.unplaced()).hasSize(1);
        assertThat(result.unplaced().get(0).subjectName()).isEqualTo("Anatomy");
        // 6 days x 1 period tried for the initial attempt; no other row exists to backtrack against.
        verify(timetableSkeletonService, times(6)).placeCell(any());
    }

    @Test
    void shouldBacktrackAndSucceedNetPositiveWhenDisplacingAnEarlierRowFreesTheOnlyWorkingSlot() {
        CourseOffering offeringA = nonElectiveOffering(100L);
        CourseOffering offeringB = nonElectiveOffering(200L);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offeringA));
        when(courseOfferingRepository.findById(200L)).thenReturn(Optional.of(offeringB));
        when(timetableSkeletonService.isElectiveOffering(offeringA)).thenReturn(false);
        when(timetableSkeletonService.isElectiveOffering(offeringB)).thenReturn(false);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(5L, "Cohort", "Term",
            List.of(subjectWithShortfall(100L, "Anatomy", 1, 0), subjectWithShortfall(200L, "Physiology", 1, 0)),
            List.of(), List.of(), List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 5L)).thenReturn(skeleton);

        // Row B (Physiology) can ONLY succeed at MONDAY/period1 -- exactly where Row A (Anatomy)
        // is initially placed -- simulating a cohort-exclusivity clash that only the backtrack
        // (removing A, retrying B, then restoring A) can resolve.
        AtomicBoolean mondaySlotFreed = new AtomicBoolean(false);
        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class))).thenAnswer(inv -> {
            SkeletonCellPlacementRequest req = inv.getArgument(0);
            if (req.courseOfferingId().equals(100L)) {
                if (req.dayOfWeek() == DayOfWeek.MONDAY) {
                    return cellResponse(900L, 100L, DayOfWeek.MONDAY, "Anatomy");
                }
                throw new TimetableConstraintViolationException(
                    List.of(new com.cms.dto.ConstraintViolation("X", "no")));
            }
            if (req.dayOfWeek() == DayOfWeek.MONDAY && mondaySlotFreed.get()) {
                return cellResponse(901L, 200L, DayOfWeek.MONDAY, "Physiology");
            }
            throw new TimetableConstraintViolationException(List.of(new com.cms.dto.ConstraintViolation("X", "no")));
        });
        doAnswer(inv -> {
            mondaySlotFreed.set(true);
            return null;
        }).when(timetableSkeletonService).removeCell(900L);

        AutoPlaceResult result = service.autoPlace(10L, 5L);

        assertThat(result.unplaced()).isEmpty();
        assertThat(result.placedCount()).isEqualTo(2);
        verify(timetableSkeletonService).removeCell(900L);
        // Anatomy's exact-slot restore attempt after Physiology's successful retry.
        verify(timetableSkeletonService, times(2)).placeCell(new SkeletonCellPlacementRequest(
            100L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, null, 5L, null, null));
    }

    @Test
    void shouldBacktrackAndRestoreWithoutRegressingWhenTheRetryStillFails() {
        CourseOffering offeringA = nonElectiveOffering(100L);
        CourseOffering offeringB = nonElectiveOffering(200L);
        when(courseOfferingRepository.findById(100L)).thenReturn(Optional.of(offeringA));
        when(courseOfferingRepository.findById(200L)).thenReturn(Optional.of(offeringB));
        when(timetableSkeletonService.isElectiveOffering(offeringA)).thenReturn(false);
        when(timetableSkeletonService.isElectiveOffering(offeringB)).thenReturn(false);
        SkeletonBuilderResponse skeleton = new SkeletonBuilderResponse(5L, "Cohort", "Term",
            List.of(subjectWithShortfall(100L, "Anatomy", 1, 0), subjectWithShortfall(200L, "Physiology", 1, 0)),
            List.of(), List.of(), List.of());
        when(timetableSkeletonService.getCohortSkeleton(10L, 5L)).thenReturn(skeleton);

        // Row A always succeeds at MONDAY; Row B never succeeds anywhere, even after A is removed.
        when(timetableSkeletonService.placeCell(any(SkeletonCellPlacementRequest.class))).thenAnswer(inv -> {
            SkeletonCellPlacementRequest req = inv.getArgument(0);
            if (req.courseOfferingId().equals(100L) && req.dayOfWeek() == DayOfWeek.MONDAY) {
                return cellResponse(900L, 100L, DayOfWeek.MONDAY, "Anatomy");
            }
            throw new TimetableConstraintViolationException(List.of(new com.cms.dto.ConstraintViolation("X", "no")));
        });

        AutoPlaceResult result = service.autoPlace(10L, 5L);

        assertThat(result.placedCount()).isEqualTo(1);
        assertThat(result.unplaced()).hasSize(1);
        assertThat(result.unplaced().get(0).subjectName()).isEqualTo("Physiology");
        verify(timetableSkeletonService).removeCell(900L);
        // Placed once initially, then restored once after the failed backtrack retry.
        verify(timetableSkeletonService, times(2)).placeCell(new SkeletonCellPlacementRequest(
            100L, ClassSessionType.THEORY, DayOfWeek.MONDAY, 1L, null, 5L, null, null));
    }
}
