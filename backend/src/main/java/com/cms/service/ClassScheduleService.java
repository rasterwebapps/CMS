package com.cms.service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ClassScheduleRequest;
import com.cms.dto.ClassScheduleResponse;
import com.cms.dto.ScheduleConflictResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Batch;
import com.cms.model.ClassSchedule;
import com.cms.model.Classroom;
import com.cms.model.CourseOffering;
import com.cms.model.Faculty;
import com.cms.model.Lab;
import com.cms.model.LabSlot;
import com.cms.model.Period;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.repository.BatchRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.LabRepository;
import com.cms.repository.LabSlotRepository;
import com.cms.repository.PeriodRepository;
import com.cms.repository.SubjectRepository;
import com.cms.repository.TermInstanceRepository;

@Service
@Transactional(readOnly = true)
public class ClassScheduleService {

    private final ClassScheduleRepository classScheduleRepository;
    private final LabRepository labRepository;
    private final SubjectRepository subjectRepository;
    private final FacultyRepository facultyRepository;
    private final LabSlotRepository labSlotRepository;
    private final TermInstanceRepository termInstanceRepository;
    private final BatchRepository batchRepository;
    private final ClassroomRepository classroomRepository;
    private final PeriodRepository periodRepository;
    private final CourseOfferingRepository courseOfferingRepository;

    public ClassScheduleService(ClassScheduleRepository classScheduleRepository,
                               LabRepository labRepository,
                               SubjectRepository subjectRepository,
                               FacultyRepository facultyRepository,
                               LabSlotRepository labSlotRepository,
                               TermInstanceRepository termInstanceRepository,
                               BatchRepository batchRepository,
                               ClassroomRepository classroomRepository,
                               PeriodRepository periodRepository,
                               CourseOfferingRepository courseOfferingRepository) {
        this.classScheduleRepository = classScheduleRepository;
        this.labRepository = labRepository;
        this.subjectRepository = subjectRepository;
        this.facultyRepository = facultyRepository;
        this.labSlotRepository = labSlotRepository;
        this.termInstanceRepository = termInstanceRepository;
        this.batchRepository = batchRepository;
        this.classroomRepository = classroomRepository;
        this.periodRepository = periodRepository;
        this.courseOfferingRepository = courseOfferingRepository;
    }

    @Transactional
    public ClassScheduleResponse create(ClassScheduleRequest request) {
        ClassSchedule cs = new ClassSchedule();
        applyRequest(cs, request);
        cs.setIsActive(request.isActive() != null ? request.isActive() : true);
        cs.setStatus(ClassScheduleStatus.PUBLISHED);
        return toResponse(classScheduleRepository.save(cs));
    }

    private void applyRequest(ClassSchedule cs, ClassScheduleRequest request) {
        Subject subject = subjectRepository.findById(request.subjectId())
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + request.subjectId()));
        Faculty faculty = facultyRepository.findById(request.facultyId())
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + request.facultyId()));
        requireEligibleFaculty(subject, faculty, cs.getFaculty());
        TermInstance termInstance = termInstanceRepository.findById(request.termInstanceId())
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + request.termInstanceId()));

        cs.setSessionType(request.sessionType());
        cs.setSubject(subject);
        cs.setFaculty(faculty);
        cs.setDayOfWeek(request.dayOfWeek());
        cs.setTermInstance(termInstance);
        cs.setCourseOffering(resolveCourseOffering(request.courseOfferingId()));

        if (request.sessionType() == ClassSessionType.LAB) {
            if (request.labId() == null || request.labSlotId() == null || request.batchName() == null || request.batchName().isBlank()) {
                throw new IllegalArgumentException("Lab, lab slot, and batch name are required for a LAB session");
            }
            Lab lab = labRepository.findById(request.labId())
                .orElseThrow(() -> new ResourceNotFoundException("Lab not found with id: " + request.labId()));
            LabSlot labSlot = labSlotRepository.findById(request.labSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Lab slot not found with id: " + request.labSlotId()));
            cs.setLab(lab);
            cs.setLabSlot(labSlot);
            cs.setBatchName(request.batchName());
            cs.setBatch(resolveBatch(request.batchId()));
            cs.setClassroom(null);
            cs.setPeriod(null);
        } else {
            if (request.classroomId() == null || request.periodId() == null) {
                throw new IllegalArgumentException("Classroom and period are required for a THEORY session");
            }
            Classroom classroom = classroomRepository.findById(request.classroomId())
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found with id: " + request.classroomId()));
            Period period = periodRepository.findById(request.periodId())
                .orElseThrow(() -> new ResourceNotFoundException("Period not found with id: " + request.periodId()));
            cs.setClassroom(classroom);
            cs.setPeriod(period);
            cs.setLab(null);
            cs.setLabSlot(null);
            cs.setBatchName(null);
            cs.setBatch(null);
        }
    }

    /**
     * Department-level (Speciality) eligibility gate: a faculty can only be assigned to teach a
     * subject from their own department. Skipped when the subject has no speciality set (not
     * every subject is department-scoped) and grandfathered when the requested faculty is the
     * same one already on the row — this blocks new/changed mismatched assignments without
     * retroactively breaking rows saved before this rule existed on an otherwise-unrelated edit.
     */
    private void requireEligibleFaculty(Subject subject, Faculty faculty, Faculty previousFaculty) {
        if (subject.getSpeciality() == null) {
            return;
        }
        if (previousFaculty != null && previousFaculty.getId().equals(faculty.getId())) {
            return;
        }
        if (!subject.getSpeciality().getId().equals(faculty.getSpeciality().getId())) {
            throw new IllegalArgumentException("Faculty '" + faculty.getFullName() + "' belongs to the "
                + faculty.getSpeciality().getName() + " department and is not eligible to teach '"
                + subject.getName() + "' (" + subject.getSpeciality().getName() + ")");
        }
    }

    private Batch resolveBatch(Long batchId) {
        if (batchId == null) {
            return null;
        }
        return batchRepository.findById(batchId)
            .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + batchId));
    }

    private CourseOffering resolveCourseOffering(Long courseOfferingId) {
        if (courseOfferingId == null) {
            return null;
        }
        return courseOfferingRepository.findById(courseOfferingId)
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + courseOfferingId));
    }

    public List<ClassScheduleResponse> findAll() {
        return classScheduleRepository.findAll().stream().map(this::toResponse).toList();
    }

    public ClassScheduleResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    public List<ClassScheduleResponse> findByLabId(Long labId) {
        if (!labRepository.existsById(labId)) {
            throw new ResourceNotFoundException("Lab not found with id: " + labId);
        }
        return classScheduleRepository.findByLabId(labId).stream().map(this::toResponse).toList();
    }

    public List<ClassScheduleResponse> findByFacultyId(Long facultyId) {
        if (!facultyRepository.existsById(facultyId)) {
            throw new ResourceNotFoundException("Faculty not found with id: " + facultyId);
        }
        return classScheduleRepository.findByFacultyId(facultyId).stream().map(this::toResponse).toList();
    }

    public List<ClassScheduleResponse> findByBatchName(String batchName) {
        return classScheduleRepository.findByBatchName(batchName).stream().map(this::toResponse).toList();
    }

    public List<ClassScheduleResponse> findByDayOfWeek(DayOfWeek dayOfWeek) {
        return classScheduleRepository.findByDayOfWeek(dayOfWeek).stream().map(this::toResponse).toList();
    }

    public List<ClassScheduleResponse> findByTermInstanceId(Long termInstanceId) {
        if (!termInstanceRepository.existsById(termInstanceId)) {
            throw new ResourceNotFoundException("Term instance not found with id: " + termInstanceId);
        }
        return classScheduleRepository.findByTermInstanceId(termInstanceId).stream().map(this::toResponse).toList();
    }

    public List<ClassScheduleResponse> findByTermInstanceIdAndStatus(Long termInstanceId, ClassScheduleStatus status) {
        return classScheduleRepository.findByTermInstanceIdAndStatus(termInstanceId, status).stream()
            .map(this::toResponse).toList();
    }

    public List<ClassScheduleResponse> toResponseList(List<ClassSchedule> schedules) {
        return schedules.stream().map(this::toResponse).toList();
    }

    public ScheduleConflictResponse checkConflicts(ClassScheduleRequest request) {
        return checkConflicts(request, null);
    }

    public ScheduleConflictResponse checkConflicts(ClassScheduleRequest request, Long excludeId) {
        LocalTime startTime;
        LocalTime endTime;
        if (request.sessionType() == ClassSessionType.LAB) {
            LabSlot labSlot = labSlotRepository.findById(request.labSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Lab slot not found with id: " + request.labSlotId()));
            startTime = labSlot.getStartTime();
            endTime = labSlot.getEndTime();
        } else {
            Period period = periodRepository.findById(request.periodId())
                .orElseThrow(() -> new ResourceNotFoundException("Period not found with id: " + request.periodId()));
            startTime = period.getStartTime();
            endTime = period.getEndTime();
        }

        List<ClassSchedule> overlapping = classScheduleRepository.findOverlapping(
            request.dayOfWeek(), request.termInstanceId(), startTime, endTime,
            ClassScheduleStatus.PUBLISHED, excludeId);

        List<String> roomConflicts = new ArrayList<>();
        List<String> facultyConflicts = new ArrayList<>();
        List<String> audienceConflicts = new ArrayList<>();

        for (ClassSchedule c : overlapping) {
            boolean sameRoom = request.sessionType() == ClassSessionType.LAB
                ? c.getSessionType() == ClassSessionType.LAB && sameId(c.getLab() != null ? c.getLab().getId() : null, request.labId())
                : c.getSessionType() == ClassSessionType.THEORY && sameId(c.getClassroom() != null ? c.getClassroom().getId() : null, request.classroomId());
            if (sameRoom) {
                roomConflicts.add(String.format("Room is already scheduled for %s on %s",
                    c.getSubject().getName(), c.getDayOfWeek()));
            }

            if (sameId(c.getFaculty().getId(), request.facultyId())) {
                facultyConflicts.add(String.format("Faculty is already scheduled for %s on %s",
                    c.getSubject().getName(), c.getDayOfWeek()));
            }

            boolean sameAudience = request.sessionType() == ClassSessionType.LAB
                ? c.getSessionType() == ClassSessionType.LAB && sameStr(c.getBatchName(), request.batchName())
                : c.getSessionType() == ClassSessionType.THEORY && sameId(
                    c.getCourseOffering() != null ? c.getCourseOffering().getId() : null, request.courseOfferingId());
            if (sameAudience) {
                audienceConflicts.add(String.format("Audience is already scheduled for %s on %s",
                    c.getSubject().getName(), c.getDayOfWeek()));
            }
        }

        boolean hasConflict = !roomConflicts.isEmpty() || !facultyConflicts.isEmpty() || !audienceConflicts.isEmpty();
        return new ScheduleConflictResponse(hasConflict, roomConflicts, facultyConflicts, audienceConflicts);
    }

    private static boolean sameId(Long a, Long b) {
        return a != null && a.equals(b);
    }

    private static boolean sameStr(String a, String b) {
        return a != null && a.equalsIgnoreCase(b);
    }

    @Transactional
    public ClassScheduleResponse update(Long id, ClassScheduleRequest request) {
        ClassSchedule cs = findOrThrow(id);
        applyRequest(cs, request);
        if (request.isActive() != null) {
            cs.setIsActive(request.isActive());
        }
        return toResponse(classScheduleRepository.save(cs));
    }

    @Transactional
    public void delete(Long id) {
        if (!classScheduleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Class schedule not found with id: " + id);
        }
        classScheduleRepository.deleteById(id);
    }

    private ClassSchedule findOrThrow(Long id) {
        return classScheduleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with id: " + id));
    }

    private ClassScheduleResponse toResponse(ClassSchedule cs) {
        TermInstance ti = cs.getTermInstance();
        String label = ti.getAcademicYear().getName() + " " + ti.getTermType();

        boolean isLab = cs.getSessionType() == ClassSessionType.LAB;
        String slotName = isLab ? (cs.getLabSlot() != null ? cs.getLabSlot().getName() : null) : (cs.getPeriod() != null ? cs.getPeriod().getName() : null);
        LocalTime startTime = isLab ? (cs.getLabSlot() != null ? cs.getLabSlot().getStartTime() : null) : (cs.getPeriod() != null ? cs.getPeriod().getStartTime() : null);
        LocalTime endTime = isLab ? (cs.getLabSlot() != null ? cs.getLabSlot().getEndTime() : null) : (cs.getPeriod() != null ? cs.getPeriod().getEndTime() : null);
        String roomName = isLab ? (cs.getLab() != null ? cs.getLab().getName() : null) : (cs.getClassroom() != null ? cs.getClassroom().getName() : null);

        return new ClassScheduleResponse(
            cs.getId(),
            cs.getSessionType(),
            cs.getStatus(),
            cs.getLab() != null ? cs.getLab().getId() : null,
            isLab ? roomName : null, // labName, kept for LAB-row display/backward-compat
            cs.getSubject().getId(),
            cs.getSubject().getName(),
            cs.getSubject().getCode(),
            cs.getFaculty().getId(),
            cs.getFaculty().getFirstName() + " " + cs.getFaculty().getLastName(),
            cs.getLabSlot() != null ? cs.getLabSlot().getId() : null,
            cs.getPeriod() != null ? cs.getPeriod().getId() : null,
            slotName,
            startTime,
            endTime,
            cs.getBatchName(),
            cs.getBatch() != null ? cs.getBatch().getId() : null,
            cs.getClassroom() != null ? cs.getClassroom().getId() : null,
            roomName, // session-type-neutral: resolves to classroom or lab name regardless of type
            cs.getCourseOffering() != null ? cs.getCourseOffering().getId() : null,
            cs.getDayOfWeek(),
            ti.getId(),
            label,
            cs.getIsActive(),
            cs.getCreatedAt(),
            cs.getUpdatedAt()
        );
    }
}
