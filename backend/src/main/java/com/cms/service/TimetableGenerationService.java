package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.TimetableActionResponse;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.ClassSchedule;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.LabAttendanceRepository;
import com.cms.repository.TermInstanceRepository;

/**
 * Draft lifecycle actions (clear/approve/revert) for a term's {@link ClassSchedule} rows. R3.1
 * retired the one-shot auto-{@code generate()} this service used to offer — Skeleton Builder
 * (placement) → Staffing (faculty/room) is now the only path that creates DRAFT rows; this
 * service only ever acts on rows that already exist.
 */
@Service
@Transactional(readOnly = true)
public class TimetableGenerationService {

    private final ClassScheduleRepository classScheduleRepository;
    private final TermInstanceRepository termInstanceRepository;
    private final LabAttendanceRepository labAttendanceRepository;

    public TimetableGenerationService(ClassScheduleRepository classScheduleRepository,
                                       TermInstanceRepository termInstanceRepository,
                                       LabAttendanceRepository labAttendanceRepository) {
        this.classScheduleRepository = classScheduleRepository;
        this.termInstanceRepository = termInstanceRepository;
        this.labAttendanceRepository = labAttendanceRepository;
    }

    @Transactional
    public TimetableActionResponse clear(Long termInstanceId) {
        if (!termInstanceRepository.existsById(termInstanceId)) {
            throw new ResourceNotFoundException("Term instance not found with id: " + termInstanceId);
        }
        List<ClassSchedule> existing = classScheduleRepository.findByTermInstanceId(termInstanceId);
        classScheduleRepository.deleteByTermInstanceId(termInstanceId);
        return new TimetableActionResponse(existing.size());
    }

    @Transactional
    public TimetableActionResponse approve(Long termInstanceId) {
        List<ClassSchedule> drafts = classScheduleRepository.findByTermInstanceIdAndStatus(termInstanceId, ClassScheduleStatus.DRAFT);
        if (drafts.isEmpty()) {
            throw new ResourceNotFoundException("No draft timetable found for term instance id: " + termInstanceId);
        }
        // A skeleton cell with no faculty yet would otherwise fail with a raw
        // chk_class_schedule_session_shape violation the moment its status flips to PUBLISHED --
        // catch it here first with a message that actually tells the admin what to go do (visit
        // the Staffing screen) instead of a database error.
        long unstaffedCount = drafts.stream().filter(cs -> cs.getFaculty() == null).count();
        if (unstaffedCount > 0) {
            throw new LifecycleConflictException(
                unstaffedCount + " session(s) in this draft still need faculty/room assigned via the Staffing screen before it can be approved.",
                "TIMETABLE_UNSTAFFED_CELLS", "TermInstance", termInstanceId, (int) unstaffedCount);
        }
        for (ClassSchedule cs : drafts) {
            cs.setStatus(ClassScheduleStatus.PUBLISHED);
            classScheduleRepository.save(cs);
        }
        return new TimetableActionResponse(drafts.size());
    }

    /**
     * Un-publishes a live timetable back to DRAFT so it can be edited/swapped and re-approved,
     * without losing the placed sessions (unlike {@link #clear}, which deletes them outright).
     * Blocked once any {@code LabAttendance} has been recorded against the term's sessions —
     * {@code lab_attendances.lab_schedule_id} has no {@code ON DELETE}/status-transition handling,
     * so silently reverting attendance-backed sessions back to DRAFT would let a subsequent
     * clear/re-placement wipe out attendance history's session linkage.
     */
    @Transactional
    public TimetableActionResponse revertToDraft(Long termInstanceId) {
        List<ClassSchedule> published = classScheduleRepository.findByTermInstanceIdAndStatus(
            termInstanceId, ClassScheduleStatus.PUBLISHED);
        if (published.isEmpty()) {
            throw new ResourceNotFoundException("No published timetable found for term instance id: " + termInstanceId);
        }
        if (labAttendanceRepository.existsByLabScheduleTermInstanceId(termInstanceId)) {
            throw new LifecycleConflictException(
                "Attendance has already been recorded against this term's timetable. It can no longer be reverted to draft.",
                "TIMETABLE_ATTENDANCE_RECORDED", "TermInstance", termInstanceId, null);
        }
        for (ClassSchedule cs : published) {
            cs.setStatus(ClassScheduleStatus.DRAFT);
            classScheduleRepository.save(cs);
        }
        return new TimetableActionResponse(published.size());
    }
}
