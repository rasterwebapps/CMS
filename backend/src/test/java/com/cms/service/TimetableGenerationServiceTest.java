package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.ConflictScanResponse;
import com.cms.dto.ConstraintViolation;
import com.cms.dto.TimetableActionResponse;
import com.cms.dto.TimetableConflictRow;
import com.cms.dto.TimetableCoverageGap;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.exception.TimetableConstraintViolationException;
import com.cms.exception.TimetableCoverageGapException;
import com.cms.model.ClassSchedule;
import com.cms.model.DesignationMaster;
import com.cms.model.Faculty;
import com.cms.model.Speciality;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.FacultyStatus;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.LabAttendanceRepository;
import com.cms.repository.TermInstanceRepository;

@ExtendWith(MockitoExtension.class)
class TimetableGenerationServiceTest {

    private static final Long COHORT_ID = 1L;
    private static final List<Long> COHORT_IDS = List.of(COHORT_ID);

    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private TermInstanceRepository termInstanceRepository;
    @Mock private LabAttendanceRepository labAttendanceRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private TimetableConflictInspectorService timetableConflictInspectorService;
    @Mock private CourseOfferingSectionFacultyService courseOfferingSectionFacultyService;
    @Mock private TimetableStaffingAutoAssignService timetableStaffingAutoAssignService;
    @Mock private TimetableCoverageService timetableCoverageService;
    @Mock private TimetableSkeletonService timetableSkeletonService;
    @Mock private CourseOfferingService courseOfferingService;

    private TimetableGenerationService service;

    private Faculty faculty;

    private static ConflictScanResponse cleanScan() {
        return new ConflictScanResponse(10L, "Test Term", Instant.now(), 2, 0, 0, Map.of(), List.of());
    }

    /** OC-260: the service resolves "which rows belong to this cohort" via {@link
     *  TimetableSkeletonService#getCohortActiveClassSchedules} — every test that previously relied
     *  on the whole-term repository finders directly must also stub this to return the exact same
     *  rows, so the id-membership filter inside the service keeps them. */
    private void stubCohortSchedules(List<ClassSchedule> cells) {
        lenient().when(timetableSkeletonService.getCohortActiveClassSchedules(eq(10L), eq(COHORT_ID))).thenReturn(cells);
    }

    @BeforeEach
    void setUp() {
        service = new TimetableGenerationService(classScheduleRepository, termInstanceRepository,
            labAttendanceRepository, auditLogService, timetableConflictInspectorService,
            courseOfferingSectionFacultyService, timetableStaffingAutoAssignService, timetableCoverageService,
            timetableSkeletonService, courseOfferingService);

        Speciality speciality = new Speciality("Nursing", "NUR", "Nursing Dept", null, null);
        speciality.setId(1L);

        DesignationMaster designation = new DesignationMaster("Assistant Professor", "ASSISTANT_PROFESSOR", null);
        designation.setId(1L);

        faculty = new Faculty("EMP001", "John", "Doe", "john@college.edu", "1234567890",
            speciality, designation, "Nursing", null, null, FacultyStatus.ACTIVE);
        faculty.setId(1L);

        // Default every test to an already-satisfied conflict-acknowledgment gate (OC-258) so only
        // the tests specifically about that gate need to override it to false — otherwise every
        // pre-existing "successful approve" test below would fail on a mock's default `false`.
        lenient().when(timetableConflictInspectorService.isCohortAcknowledgmentValid(anyLong(), anyLong())).thenReturn(true);
        lenient().when(courseOfferingSectionFacultyService.getAssignmentSummaryForTermInstance(anyLong())).thenReturn(List.of());
        lenient().when(timetableCoverageService.findGaps(anyLong())).thenReturn(List.of());
    }

    private TermInstance termWithStatus(Long id, TermInstanceStatus status) {
        TermInstance term = new TermInstance();
        term.setId(id);
        term.setStatus(status);
        return term;
    }

    @Test
    void shouldClearAllRowsForTerm() {
        ClassSchedule row = new ClassSchedule();
        row.setId(1L);
        stubCohortSchedules(List.of(row));
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termWithStatus(10L, TermInstanceStatus.OPEN)));
        when(labAttendanceRepository.existsByLabScheduleIdIn(anyList())).thenReturn(false);
        when(classScheduleRepository.findByTermInstanceId(10L)).thenReturn(List.of(row));

        TimetableActionResponse response = service.clear(10L, COHORT_IDS, "admin");

        assertThat(response.affectedCount()).isEqualTo(1);
        verify(classScheduleRepository).deleteAll(List.of(row));
        verify(auditLogService).record(eq("admin"), eq("TIMETABLE_DISCARDED"), eq("TermInstance"), eq("10"),
            org.mockito.ArgumentMatchers.contains("1 session(s) discarded"));
    }

    @Test
    void shouldThrowWhenClearingNonExistentTerm() {
        when(termInstanceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.clear(999L, COHORT_IDS, "admin"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldBlockClearWhenTermIsLocked() {
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termWithStatus(10L, TermInstanceStatus.LOCKED)));

        assertThatThrownBy(() -> service.clear(10L, COHORT_IDS, "admin"))
            .isInstanceOf(LifecycleConflictException.class);

        verify(classScheduleRepository, never()).deleteAll(anyList());
    }

    @Test
    void shouldBlockClearWhenAttendanceAlreadyRecorded() {
        ClassSchedule row = new ClassSchedule();
        row.setId(1L);
        stubCohortSchedules(List.of(row));
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termWithStatus(10L, TermInstanceStatus.OPEN)));
        when(labAttendanceRepository.existsByLabScheduleIdIn(anyList())).thenReturn(true);

        assertThatThrownBy(() -> service.clear(10L, COHORT_IDS, "admin"))
            .isInstanceOf(LifecycleConflictException.class);

        verify(classScheduleRepository, never()).deleteAll(anyList());
    }

    @Test
    void shouldRejectClearApproveRevertWithNoCohortsSelected() {
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termWithStatus(10L, TermInstanceStatus.OPEN)));

        assertThatThrownBy(() -> service.clear(10L, List.of(), "admin"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.approve(10L, List.of(), "admin", false, null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.revertToDraft(10L, List.of(), "admin"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldApproveAllDraftRows() {
        ClassSchedule draft1 = new ClassSchedule();
        draft1.setId(1L);
        draft1.setStatus(ClassScheduleStatus.DRAFT);
        draft1.setFaculty(faculty);
        ClassSchedule draft2 = new ClassSchedule();
        draft2.setId(2L);
        draft2.setStatus(ClassScheduleStatus.DRAFT);
        draft2.setFaculty(faculty);
        stubCohortSchedules(List.of(draft1, draft2));

        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termWithStatus(10L, TermInstanceStatus.OPEN)));
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndIsActiveTrue(10L, ClassScheduleStatus.DRAFT))
            .thenReturn(List.of(draft1, draft2));
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));
        when(timetableConflictInspectorService.scanCohorts(eq(10L), eq(COHORT_IDS))).thenReturn(cleanScan());

        TimetableActionResponse response = service.approve(10L, COHORT_IDS, "admin", false, null);

        assertThat(response.affectedCount()).isEqualTo(2);
        assertThat(draft1.getStatus()).isEqualTo(ClassScheduleStatus.PUBLISHED);
        assertThat(draft2.getStatus()).isEqualTo(ClassScheduleStatus.PUBLISHED);
        verify(auditLogService).record(eq("admin"), eq("TIMETABLE_APPROVED"), eq("TermInstance"), eq("10"),
            org.mockito.ArgumentMatchers.contains("2 session(s) approved"));
    }

    @Test
    void shouldNeverPublishSwitchedOffLeftoversFromEarlierAutoScheduleRuns() {
        // Every auto-schedule rebuild switches the previous draft off rather than deleting it. Those
        // leftovers are still DRAFT, so an unfiltered draft query handed them to approve, which
        // published every one of them onto the live timetable alongside the real week.
        ClassSchedule current = new ClassSchedule();
        current.setId(1L);
        current.setStatus(ClassScheduleStatus.DRAFT);
        current.setFaculty(faculty);
        ClassSchedule leftover = new ClassSchedule();
        leftover.setId(2L);
        leftover.setStatus(ClassScheduleStatus.DRAFT);
        leftover.setFaculty(faculty);
        leftover.setIsActive(false);
        stubCohortSchedules(List.of(current));

        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termWithStatus(10L, TermInstanceStatus.OPEN)));
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndIsActiveTrue(10L, ClassScheduleStatus.DRAFT))
            .thenReturn(List.of(current));
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));
        when(timetableConflictInspectorService.scanCohorts(eq(10L), eq(COHORT_IDS))).thenReturn(cleanScan());

        TimetableActionResponse response = service.approve(10L, COHORT_IDS, "admin", false, null);

        assertThat(response.affectedCount()).isEqualTo(1);
        assertThat(current.getStatus()).isEqualTo(ClassScheduleStatus.PUBLISHED);
        assertThat(leftover.getStatus()).isEqualTo(ClassScheduleStatus.DRAFT);
        verify(classScheduleRepository, never()).save(leftover);
    }

    @Test
    void shouldBlockApproveWhenTermIsLocked() {
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termWithStatus(10L, TermInstanceStatus.LOCKED)));

        assertThatThrownBy(() -> service.approve(10L, COHORT_IDS, "admin", false, null))
            .isInstanceOf(LifecycleConflictException.class);

        verify(classScheduleRepository, never()).save(any());
    }

    @Test
    void shouldBlockApproveWhenAnyDraftRowIsUnstaffed() {
        // R3 Phase 5: an unstaffed skeleton cell (no faculty yet) must be rejected with a clear
        // actionable error here, not left to fail as a raw chk_class_schedule_session_shape
        // violation the moment its status flips to PUBLISHED.
        ClassSchedule staffed = new ClassSchedule();
        staffed.setId(1L);
        staffed.setStatus(ClassScheduleStatus.DRAFT);
        staffed.setFaculty(faculty);
        ClassSchedule unstaffed = new ClassSchedule();
        unstaffed.setId(2L);
        unstaffed.setStatus(ClassScheduleStatus.DRAFT);
        stubCohortSchedules(List.of(staffed, unstaffed));

        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termWithStatus(10L, TermInstanceStatus.OPEN)));
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndIsActiveTrue(10L, ClassScheduleStatus.DRAFT))
            .thenReturn(List.of(staffed, unstaffed));

        assertThatThrownBy(() -> service.approve(10L, COHORT_IDS, "admin", false, null))
            .isInstanceOf(LifecycleConflictException.class);

        verify(classScheduleRepository, never()).save(any());
    }

    @Test
    void shouldBlockApproveWhenAnOfferingHasNoFacultyAssigned() {
        // An offering with zero Theory faculty never gets a ClassSchedule row placed at all --
        // Global Auto-Schedule just drops it into the unplaced-sessions report -- so the
        // unstaffedCount gate above has nothing to catch. This is a separate, offering-level check.
        ClassSchedule staffed = new ClassSchedule();
        staffed.setId(1L);
        staffed.setStatus(ClassScheduleStatus.DRAFT);
        staffed.setFaculty(faculty);
        stubCohortSchedules(List.of(staffed));

        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termWithStatus(10L, TermInstanceStatus.OPEN)));
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndIsActiveTrue(10L, ClassScheduleStatus.DRAFT))
            .thenReturn(List.of(staffed));
        when(courseOfferingService.getOfferingsByTermInstanceAndCohort(10L, COHORT_ID)).thenReturn(List.of(
            new com.cms.dto.CourseOfferingDto(1L, 10L, null, null, null, null, null, null, null, null, List.of(),
                null, true, null, false, null, null, null, null, null, null, null, null, null, null, null, null, List.of())));
        when(courseOfferingSectionFacultyService.getAssignmentSummaryForTermInstance(10L)).thenReturn(List.of(
            new com.cms.dto.CourseOfferingFacultySummaryDto(1L, List.of(), com.cms.model.enums.OfferingAssignmentStatus.NONE)));

        assertThatThrownBy(() -> service.approve(10L, COHORT_IDS, "admin", false, null))
            .isInstanceOf(LifecycleConflictException.class);

        verify(classScheduleRepository, never()).save(any());
    }

    @Test
    void shouldBlockApproveWhenConflictScanFindsViolations() {
        // OC-125: approve() now re-runs the same structural scan the Conflict Inspector dashboard
        // shows, filtered to the selected cohort(s) (OC-260) -- a staffed-but-still-conflicting
        // draft (e.g. two subjects double-booking the same faculty) must refuse here, not silently
        // publish.
        ClassSchedule staffed = new ClassSchedule();
        staffed.setId(1L);
        staffed.setStatus(ClassScheduleStatus.DRAFT);
        staffed.setFaculty(faculty);
        stubCohortSchedules(List.of(staffed));

        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termWithStatus(10L, TermInstanceStatus.OPEN)));
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndIsActiveTrue(10L, ClassScheduleStatus.DRAFT))
            .thenReturn(List.of(staffed));
        ConstraintViolation violation = new ConstraintViolation(
            "STAFFING_FACULTY_CONFLICT", "This faculty member is already scheduled for another session at this exact day and time.");
        TimetableConflictRow row = new TimetableConflictRow(
            1L, "Anatomy", "ANAT101", null, null, "Period 1", null, null,
            "John Doe", "Room 101", null, ClassScheduleStatus.DRAFT, List.of(violation));
        ConflictScanResponse dirtyScan = new ConflictScanResponse(
            10L, "Test Term", Instant.now(), 1, 1, 1, Map.of("STAFFING_FACULTY_CONFLICT", 1), List.of(row));
        when(timetableConflictInspectorService.scanCohorts(eq(10L), eq(COHORT_IDS))).thenReturn(dirtyScan);

        assertThatThrownBy(() -> service.approve(10L, COHORT_IDS, "admin", false, null))
            .isInstanceOf(TimetableConstraintViolationException.class);

        verify(classScheduleRepository, never()).save(any());
        verify(auditLogService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void shouldBlockApproveWhenConflictAcknowledgmentIsMissingOrStale() {
        // OC-258/OC-260: a clean scan alone isn't enough -- an admin must have actually revisited
        // Conflict Inspector for this exact cohort after the current skeleton (see
        // TimetableConflictInspectorService#acknowledgeCohort).
        ClassSchedule staffed = new ClassSchedule();
        staffed.setId(1L);
        staffed.setStatus(ClassScheduleStatus.DRAFT);
        staffed.setFaculty(faculty);
        stubCohortSchedules(List.of(staffed));

        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termWithStatus(10L, TermInstanceStatus.OPEN)));
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndIsActiveTrue(10L, ClassScheduleStatus.DRAFT))
            .thenReturn(List.of(staffed));
        when(timetableConflictInspectorService.scanCohorts(eq(10L), eq(COHORT_IDS))).thenReturn(cleanScan());
        when(timetableConflictInspectorService.isCohortAcknowledgmentValid(10L, COHORT_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.approve(10L, COHORT_IDS, "admin", false, null))
            .isInstanceOf(LifecycleConflictException.class);

        verify(classScheduleRepository, never()).save(any());
        verify(auditLogService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void shouldBlockApproveWhenCurriculumHoursCoverageIsIncomplete() {
        // OC-256: a cohort whose Theory/Lab sessions were never placed at all (as opposed to
        // placed-but-unstaffed) has nothing for unstaffedCount/unassignedOfferingCount to catch --
        // this is the dedicated gate for that gap, and it must refuse by default (no override).
        ClassSchedule staffed = new ClassSchedule();
        staffed.setId(1L);
        staffed.setStatus(ClassScheduleStatus.DRAFT);
        staffed.setFaculty(faculty);
        stubCohortSchedules(List.of(staffed));

        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termWithStatus(10L, TermInstanceStatus.OPEN)));
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndIsActiveTrue(10L, ClassScheduleStatus.DRAFT))
            .thenReturn(List.of(staffed));
        when(timetableConflictInspectorService.scanCohorts(eq(10L), eq(COHORT_IDS))).thenReturn(cleanScan());
        when(timetableCoverageService.findGaps(10L)).thenReturn(List.of(
            new TimetableCoverageGap(COHORT_ID, "BSc Nursing", com.cms.model.enums.ClassSessionType.THEORY, 340, 0, 340)));

        assertThatThrownBy(() -> service.approve(10L, COHORT_IDS, "admin", false, null))
            .isInstanceOf(TimetableCoverageGapException.class);

        verify(classScheduleRepository, never()).save(any());
        verify(auditLogService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void shouldRejectCoverageOverrideWithoutAReason() {
        ClassSchedule staffed = new ClassSchedule();
        staffed.setId(1L);
        staffed.setStatus(ClassScheduleStatus.DRAFT);
        staffed.setFaculty(faculty);
        stubCohortSchedules(List.of(staffed));

        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termWithStatus(10L, TermInstanceStatus.OPEN)));
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndIsActiveTrue(10L, ClassScheduleStatus.DRAFT))
            .thenReturn(List.of(staffed));
        when(timetableConflictInspectorService.scanCohorts(eq(10L), eq(COHORT_IDS))).thenReturn(cleanScan());
        when(timetableCoverageService.findGaps(10L)).thenReturn(List.of(
            new TimetableCoverageGap(COHORT_ID, "BSc Nursing", com.cms.model.enums.ClassSessionType.THEORY, 340, 0, 340)));

        assertThatThrownBy(() -> service.approve(10L, COHORT_IDS, "admin", true, "  "))
            .isInstanceOf(IllegalArgumentException.class);

        verify(classScheduleRepository, never()).save(any());
    }

    @Test
    void shouldApproveWithIncompleteCoverageWhenOverridden() {
        ClassSchedule staffed = new ClassSchedule();
        staffed.setId(1L);
        staffed.setStatus(ClassScheduleStatus.DRAFT);
        staffed.setFaculty(faculty);
        stubCohortSchedules(List.of(staffed));

        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termWithStatus(10L, TermInstanceStatus.OPEN)));
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndIsActiveTrue(10L, ClassScheduleStatus.DRAFT))
            .thenReturn(List.of(staffed));
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));
        when(timetableConflictInspectorService.scanCohorts(eq(10L), eq(COHORT_IDS))).thenReturn(cleanScan());
        when(timetableCoverageService.findGaps(10L)).thenReturn(List.of(
            new TimetableCoverageGap(COHORT_ID, "BSc Nursing", com.cms.model.enums.ClassSessionType.THEORY, 340, 0, 340)));

        TimetableActionResponse response = service.approve(10L, COHORT_IDS, "admin", true, "Phased rollout, Theory starts next month");

        assertThat(response.affectedCount()).isEqualTo(1);
        assertThat(staffed.getStatus()).isEqualTo(ClassScheduleStatus.PUBLISHED);
        verify(auditLogService).record(eq("admin"), eq("TIMETABLE_APPROVED"), eq("TermInstance"), eq("10"),
            org.mockito.ArgumentMatchers.contains("Phased rollout, Theory starts next month"));
    }

    @Test
    void shouldThrowWhenApprovingWithNoDrafts() {
        stubCohortSchedules(List.of());
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termWithStatus(10L, TermInstanceStatus.OPEN)));
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndIsActiveTrue(10L, ClassScheduleStatus.DRAFT))
            .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.approve(10L, COHORT_IDS, "admin", false, null))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldRevertAllPublishedRowsToDraft() {
        ClassSchedule published1 = new ClassSchedule();
        published1.setId(1L);
        published1.setStatus(ClassScheduleStatus.PUBLISHED);
        ClassSchedule published2 = new ClassSchedule();
        published2.setId(2L);
        published2.setStatus(ClassScheduleStatus.PUBLISHED);
        stubCohortSchedules(List.of(published1, published2));

        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termWithStatus(10L, TermInstanceStatus.OPEN)));
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndIsActiveTrue(10L, ClassScheduleStatus.PUBLISHED))
            .thenReturn(List.of(published1, published2));
        when(labAttendanceRepository.existsByLabScheduleIdIn(anyList())).thenReturn(false);
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        TimetableActionResponse response = service.revertToDraft(10L, COHORT_IDS, "admin");

        assertThat(response.affectedCount()).isEqualTo(2);
        assertThat(published1.getStatus()).isEqualTo(ClassScheduleStatus.DRAFT);
        assertThat(published2.getStatus()).isEqualTo(ClassScheduleStatus.DRAFT);
        verify(auditLogService).record(eq("admin"), eq("TIMETABLE_REVERTED_TO_DRAFT"), eq("TermInstance"), eq("10"),
            org.mockito.ArgumentMatchers.contains("2 session(s) reverted to draft"));
    }

    @Test
    void shouldBlockRevertWhenTermIsLocked() {
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termWithStatus(10L, TermInstanceStatus.LOCKED)));

        assertThatThrownBy(() -> service.revertToDraft(10L, COHORT_IDS, "admin"))
            .isInstanceOf(LifecycleConflictException.class);

        verify(classScheduleRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenRevertingWithNoPublishedRows() {
        stubCohortSchedules(List.of());
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termWithStatus(10L, TermInstanceStatus.OPEN)));
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndIsActiveTrue(10L, ClassScheduleStatus.PUBLISHED))
            .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.revertToDraft(10L, COHORT_IDS, "admin"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldBlockRevertWhenAttendanceAlreadyRecorded() {
        ClassSchedule published1 = new ClassSchedule();
        published1.setId(1L);
        published1.setStatus(ClassScheduleStatus.PUBLISHED);
        stubCohortSchedules(List.of(published1));

        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termWithStatus(10L, TermInstanceStatus.OPEN)));
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndIsActiveTrue(10L, ClassScheduleStatus.PUBLISHED))
            .thenReturn(List.of(published1));
        when(labAttendanceRepository.existsByLabScheduleIdIn(anyList())).thenReturn(true);

        assertThatThrownBy(() -> service.revertToDraft(10L, COHORT_IDS, "admin"))
            .isInstanceOf(LifecycleConflictException.class);

        verify(classScheduleRepository, never()).save(any());
    }
}
