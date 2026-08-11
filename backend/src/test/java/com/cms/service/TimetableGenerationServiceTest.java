package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.TimetableActionResponse;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
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

    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private TermInstanceRepository termInstanceRepository;
    @Mock private LabAttendanceRepository labAttendanceRepository;
    @Mock private AuditLogService auditLogService;

    private TimetableGenerationService service;

    private Faculty faculty;

    @BeforeEach
    void setUp() {
        service = new TimetableGenerationService(classScheduleRepository, termInstanceRepository,
            labAttendanceRepository, auditLogService);

        Speciality speciality = new Speciality("Nursing", "NUR", "Nursing Dept", null, null);
        speciality.setId(1L);

        DesignationMaster designation = new DesignationMaster("Assistant Professor", "ASSISTANT_PROFESSOR", null);
        designation.setId(1L);

        faculty = new Faculty("EMP001", "John", "Doe", "john@college.edu", "1234567890",
            speciality, designation, "Nursing", null, null, FacultyStatus.ACTIVE);
        faculty.setId(1L);
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
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termWithStatus(10L, TermInstanceStatus.OPEN)));
        when(labAttendanceRepository.existsByLabScheduleTermInstanceId(10L)).thenReturn(false);
        when(classScheduleRepository.findByTermInstanceId(10L)).thenReturn(List.of(row));

        TimetableActionResponse response = service.clear(10L, "admin");

        assertThat(response.affectedCount()).isEqualTo(1);
        verify(classScheduleRepository).deleteByTermInstanceId(10L);
        verify(auditLogService).record("admin", "TIMETABLE_DISCARDED", "TermInstance", "10", "1 session(s) discarded");
    }

    @Test
    void shouldThrowWhenClearingNonExistentTerm() {
        when(termInstanceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.clear(999L, "admin"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldBlockClearWhenTermIsLocked() {
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termWithStatus(10L, TermInstanceStatus.LOCKED)));

        assertThatThrownBy(() -> service.clear(10L, "admin"))
            .isInstanceOf(LifecycleConflictException.class);

        verify(classScheduleRepository, never()).deleteByTermInstanceId(any());
    }

    @Test
    void shouldBlockClearWhenAttendanceAlreadyRecorded() {
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termWithStatus(10L, TermInstanceStatus.OPEN)));
        when(labAttendanceRepository.existsByLabScheduleTermInstanceId(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.clear(10L, "admin"))
            .isInstanceOf(LifecycleConflictException.class);

        verify(classScheduleRepository, never()).deleteByTermInstanceId(any());
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

        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termWithStatus(10L, TermInstanceStatus.OPEN)));
        when(classScheduleRepository.findByTermInstanceIdAndStatus(10L, ClassScheduleStatus.DRAFT))
            .thenReturn(List.of(draft1, draft2));
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        TimetableActionResponse response = service.approve(10L, "admin");

        assertThat(response.affectedCount()).isEqualTo(2);
        assertThat(draft1.getStatus()).isEqualTo(ClassScheduleStatus.PUBLISHED);
        assertThat(draft2.getStatus()).isEqualTo(ClassScheduleStatus.PUBLISHED);
        verify(auditLogService).record("admin", "TIMETABLE_APPROVED", "TermInstance", "10", "2 session(s) approved");
    }

    @Test
    void shouldBlockApproveWhenTermIsLocked() {
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termWithStatus(10L, TermInstanceStatus.LOCKED)));

        assertThatThrownBy(() -> service.approve(10L, "admin"))
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

        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termWithStatus(10L, TermInstanceStatus.OPEN)));
        when(classScheduleRepository.findByTermInstanceIdAndStatus(10L, ClassScheduleStatus.DRAFT))
            .thenReturn(List.of(staffed, unstaffed));

        assertThatThrownBy(() -> service.approve(10L, "admin"))
            .isInstanceOf(LifecycleConflictException.class);

        verify(classScheduleRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenApprovingWithNoDrafts() {
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termWithStatus(10L, TermInstanceStatus.OPEN)));
        when(classScheduleRepository.findByTermInstanceIdAndStatus(10L, ClassScheduleStatus.DRAFT))
            .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.approve(10L, "admin"))
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

        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termWithStatus(10L, TermInstanceStatus.OPEN)));
        when(classScheduleRepository.findByTermInstanceIdAndStatus(10L, ClassScheduleStatus.PUBLISHED))
            .thenReturn(List.of(published1, published2));
        when(labAttendanceRepository.existsByLabScheduleTermInstanceId(10L)).thenReturn(false);
        when(classScheduleRepository.save(any(ClassSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        TimetableActionResponse response = service.revertToDraft(10L, "admin");

        assertThat(response.affectedCount()).isEqualTo(2);
        assertThat(published1.getStatus()).isEqualTo(ClassScheduleStatus.DRAFT);
        assertThat(published2.getStatus()).isEqualTo(ClassScheduleStatus.DRAFT);
        verify(auditLogService).record("admin", "TIMETABLE_REVERTED_TO_DRAFT", "TermInstance", "10", "2 session(s) reverted to draft");
    }

    @Test
    void shouldBlockRevertWhenTermIsLocked() {
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termWithStatus(10L, TermInstanceStatus.LOCKED)));

        assertThatThrownBy(() -> service.revertToDraft(10L, "admin"))
            .isInstanceOf(LifecycleConflictException.class);

        verify(classScheduleRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenRevertingWithNoPublishedRows() {
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termWithStatus(10L, TermInstanceStatus.OPEN)));
        when(classScheduleRepository.findByTermInstanceIdAndStatus(10L, ClassScheduleStatus.PUBLISHED))
            .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.revertToDraft(10L, "admin"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldBlockRevertWhenAttendanceAlreadyRecorded() {
        ClassSchedule published1 = new ClassSchedule();
        published1.setId(1L);
        published1.setStatus(ClassScheduleStatus.PUBLISHED);

        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termWithStatus(10L, TermInstanceStatus.OPEN)));
        when(classScheduleRepository.findByTermInstanceIdAndStatus(10L, ClassScheduleStatus.PUBLISHED))
            .thenReturn(List.of(published1));
        when(labAttendanceRepository.existsByLabScheduleTermInstanceId(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.revertToDraft(10L, "admin"))
            .isInstanceOf(LifecycleConflictException.class);

        verify(classScheduleRepository, never()).save(any());
    }
}
