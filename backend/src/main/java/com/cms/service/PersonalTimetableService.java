package com.cms.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.HolidayDayInfo;
import com.cms.dto.MyTimetableResponse;
import com.cms.dto.ProfileIdentity;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Batch;
import com.cms.model.ClassSchedule;
import com.cms.model.CourseRegistration;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.TermInstance;
import com.cms.model.enums.CalendarEventType;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.RegistrationStatus;
import com.cms.repository.BatchRepository;
import com.cms.repository.CalendarEventRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.CourseRegistrationRepository;
import com.cms.repository.StudentTermEnrollmentRepository;
import com.cms.repository.TermInstanceRepository;

/**
 * Resolves "my timetable" for whichever student/faculty is currently authenticated, and adds a
 * display-time-only holiday annotation for a specific viewed week. The schedule itself stays a
 * recurring weekly template (no calendar dates in ClassSchedule) — this service never lets
 * holiday-awareness leak into generation, only into what's shown for a given weekStart.
 */
@Service
@Transactional(readOnly = true)
public class PersonalTimetableService {

    private final ClassScheduleRepository classScheduleRepository;
    private final ClassScheduleService classScheduleService;
    private final StudentTermEnrollmentRepository studentTermEnrollmentRepository;
    private final CourseRegistrationRepository courseRegistrationRepository;
    private final BatchRepository batchRepository;
    private final TermInstanceRepository termInstanceRepository;
    private final CalendarEventRepository calendarEventRepository;

    public PersonalTimetableService(ClassScheduleRepository classScheduleRepository,
                                     ClassScheduleService classScheduleService,
                                     StudentTermEnrollmentRepository studentTermEnrollmentRepository,
                                     CourseRegistrationRepository courseRegistrationRepository,
                                     BatchRepository batchRepository,
                                     TermInstanceRepository termInstanceRepository,
                                     CalendarEventRepository calendarEventRepository) {
        this.classScheduleRepository = classScheduleRepository;
        this.classScheduleService = classScheduleService;
        this.studentTermEnrollmentRepository = studentTermEnrollmentRepository;
        this.courseRegistrationRepository = courseRegistrationRepository;
        this.batchRepository = batchRepository;
        this.termInstanceRepository = termInstanceRepository;
        this.calendarEventRepository = calendarEventRepository;
    }

    public MyTimetableResponse findMyTimetable(ProfileIdentity identity, Long termInstanceId, LocalDate weekStart) {
        List<ClassSchedule> rows = findPublishedSchedules(identity, termInstanceId);

        List<HolidayDayInfo> holidays = weekStart != null
            ? resolveHolidays(termInstanceId, weekStart)
            : List.of();

        return new MyTimetableResponse(classScheduleService.toResponseList(rows), holidays);
    }

    /** Published sessions visible to this identity for a term — the same resolution
     *  {@link #findMyTimetable} uses, exposed for other consumers (e.g. the occurrence-calendar
     *  endpoint's "scope=personal") that need the raw entities rather than the DTO envelope. */
    public List<ClassSchedule> findPublishedSchedules(ProfileIdentity identity, Long termInstanceId) {
        return switch (identity.entityType()) {
            case "STUDENT" -> findForStudent(identity.entityId(), termInstanceId);
            case "FACULTY" -> classScheduleRepository.findByTermInstanceIdAndStatusAndFacultyId(
                termInstanceId, ClassScheduleStatus.PUBLISHED, identity.entityId());
            default -> List.of();
        };
    }

    private List<ClassSchedule> findForStudent(Long studentId, Long termInstanceId) {
        StudentTermEnrollment enrollment = studentTermEnrollmentRepository
            .findByStudentIdAndTermInstanceId(studentId, termInstanceId).orElse(null);

        Set<Long> courseOfferingIds = new LinkedHashSet<>();
        if (enrollment != null) {
            for (CourseRegistration reg : courseRegistrationRepository.findByStudentTermEnrollmentId(enrollment.getId())) {
                if (reg.getStatus() == RegistrationStatus.REGISTERED) {
                    courseOfferingIds.add(reg.getCourseOffering().getId());
                }
            }
        }

        List<Batch> batches = batchRepository.findByTermInstanceIdAndStudentId(termInstanceId, studentId);
        List<Long> batchIds = batches.stream().map(Batch::getId).toList();

        // Whole-cohort THEORY rows only here -- a THEORY row scoped to one section (R3 Phase 3,
        // ClassSchedule.batch set) is picked up below via batchIds instead, same as LAB/CLINICAL,
        // so a student never sees another section's Theory schedule for the same subject.
        List<ClassSchedule> theoryRows = courseOfferingIds.isEmpty() ? List.of()
            : classScheduleRepository.findByTermInstanceIdAndStatusAndCourseOfferingIdIn(
                termInstanceId, ClassScheduleStatus.PUBLISHED, List.copyOf(courseOfferingIds))
              .stream()
              .filter(cs -> cs.getSessionType() == com.cms.model.enums.ClassSessionType.THEORY)
              .filter(cs -> cs.getBatch() == null)
              .toList();

        List<ClassSchedule> batchScopedRows = batchIds.isEmpty() ? List.of()
            : classScheduleRepository.findByTermInstanceIdAndStatusAndBatchIdIn(
                termInstanceId, ClassScheduleStatus.PUBLISHED, batchIds);

        List<ClassSchedule> merged = new ArrayList<>(theoryRows);
        merged.addAll(batchScopedRows);
        return merged;
    }

    private List<HolidayDayInfo> resolveHolidays(Long termInstanceId, LocalDate weekStart) {
        TermInstance termInstance = termInstanceRepository.findById(termInstanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + termInstanceId));
        Long academicYearId = termInstance.getAcademicYear().getId();

        List<HolidayDayInfo> holidays = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            LocalDate date = weekStart.plusDays(i);
            List<com.cms.model.CalendarEvent> events = calendarEventRepository.findOverlapping(
                academicYearId, CalendarEventType.HOLIDAY, date, date);
            if (!events.isEmpty()) {
                com.cms.model.CalendarEvent event = events.get(0);
                holidays.add(new HolidayDayInfo(i, event.getTitle(), event.getHolidayCategory()));
            }
        }
        return holidays;
    }
}
