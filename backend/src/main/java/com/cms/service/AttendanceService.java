package com.cms.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AttendanceReportResponse;
import com.cms.dto.AttendanceRequest;
import com.cms.dto.AttendanceResponse;
import com.cms.dto.AvailableSubjectResponse;
import com.cms.dto.BulkAttendanceRequest;
import com.cms.dto.StudentRosterResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Attendance;
import com.cms.model.ClassSchedule;
import com.cms.model.Subject;
import com.cms.model.Student;
import com.cms.model.enums.AttendanceStatus;
import com.cms.model.enums.AttendanceType;
import com.cms.repository.AttendanceRepository;
import com.cms.repository.CourseRegistrationRepository;
import com.cms.repository.SubjectRepository;
import com.cms.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final CourseRegistrationRepository courseRegistrationRepository;
    private final AttendanceThresholdService thresholdService;
    private final ClassScheduleOccurrenceService classScheduleOccurrenceService;

    public AttendanceService(AttendanceRepository attendanceRepository,
                              StudentRepository studentRepository,
                              SubjectRepository subjectRepository,
                              CourseRegistrationRepository courseRegistrationRepository,
                              AttendanceThresholdService thresholdService,
                              ClassScheduleOccurrenceService classScheduleOccurrenceService) {
        this.attendanceRepository = attendanceRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.courseRegistrationRepository = courseRegistrationRepository;
        this.thresholdService = thresholdService;
        this.classScheduleOccurrenceService = classScheduleOccurrenceService;
    }

    /** Subjects a faculty member can mark attendance for on a specific date, resolved through
     *  {@link ClassScheduleOccurrenceService#schedulesEffectiveOn} -- day-mapping- and
     *  blocked-period-aware, so a compensatory working day correctly offers the borrowed weekday's
     *  subjects rather than the date's own (usually empty) actual-weekday list. */
    public List<AvailableSubjectResponse> findAvailableSubjects(Long facultyId, LocalDate date) {
        List<ClassSchedule> schedules = classScheduleOccurrenceService.schedulesEffectiveOn(date, facultyId);
        List<AvailableSubjectResponse> result = new ArrayList<>();
        for (ClassSchedule cs : schedules) {
            result.add(new AvailableSubjectResponse(
                cs.getId(), cs.getSubject().getId(), cs.getSubject().getName(), cs.getSubject().getCode(),
                cs.getBatchName(), cs.getPeriod() != null ? cs.getPeriod().getName() : null,
                cs.getPeriod() != null ? cs.getPeriod().getStartTime() : null,
                cs.getPeriod() != null ? cs.getPeriod().getEndTime() : null));
        }
        return result;
    }

    /** Students registered for a subject -- reuses the same roster query {@link
     *  #getLowAttendanceAlerts} already relies on, so the attendance-mark screen's roster
     *  matches exactly who low-attendance alerts are computed for. */
    public List<StudentRosterResponse> findRosterForSubject(Long subjectId) {
        if (!subjectRepository.existsById(subjectId)) {
            throw new ResourceNotFoundException("Subject not found with id: " + subjectId);
        }
        return courseRegistrationRepository.findRegisteredStudentsBySubjectId(subjectId).stream()
            .map(s -> new StudentRosterResponse(s.getId(), s.getFullName(), s.getRollNumber()))
            .toList();
    }

    @Transactional
    public AttendanceResponse markAttendance(AttendanceRequest request) {
        Student student = studentRepository.findById(request.studentId())
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + request.studentId()));

        Subject subject = subjectRepository.findById(request.subjectId())
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + request.subjectId()));

        Attendance attendance = new Attendance(
            student, subject, request.date(), request.status(), request.type()
        );
        attendance.setRemarks(request.remarks());

        Attendance saved = attendanceRepository.save(attendance);
        return toResponse(saved);
    }

    @Transactional
    public List<AttendanceResponse> markBulkAttendance(BulkAttendanceRequest request) {
        Subject subject = subjectRepository.findById(request.subjectId())
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + request.subjectId()));

        List<AttendanceResponse> responses = new ArrayList<>();

        for (BulkAttendanceRequest.StudentAttendance sa : request.studentAttendances()) {
            Student student = studentRepository.findById(sa.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + sa.studentId()));

            Attendance attendance = new Attendance(
                student, subject, request.date(), sa.status(), request.type()
            );
            attendance.setRemarks(sa.remarks());

            Attendance saved = attendanceRepository.save(attendance);
            responses.add(toResponse(saved));
        }

        return responses;
    }

    public List<AttendanceResponse> findByStudentId(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found with id: " + studentId);
        }
        return attendanceRepository.findByStudentId(studentId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<AttendanceResponse> findBySubjectId(Long subjectId) {
        if (!subjectRepository.existsById(subjectId)) {
            throw new ResourceNotFoundException("Subject not found with id: " + subjectId);
        }
        return attendanceRepository.findBySubjectId(subjectId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<AttendanceResponse> findByStudentIdAndSubjectId(Long studentId, Long subjectId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found with id: " + studentId);
        }
        if (!subjectRepository.existsById(subjectId)) {
            throw new ResourceNotFoundException("Subject not found with id: " + subjectId);
        }
        return attendanceRepository.findByStudentIdAndSubjectId(studentId, subjectId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<AttendanceResponse> findBySubjectIdAndDate(Long subjectId, LocalDate date) {
        if (!subjectRepository.existsById(subjectId)) {
            throw new ResourceNotFoundException("Subject not found with id: " + subjectId);
        }
        return attendanceRepository.findBySubjectIdAndDate(subjectId, date).stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Returns one report entry per attendance component (Theory/Lab/Clinical) that has at least
     * one recorded class for this student+subject, each checked against its own resolved
     * threshold (per-offering override, falling back to the 75% default) — a student can be
     * meeting an 80% Theory requirement while failing a 100% Clinical one, so a single blended
     * percentage across all types would hide that.
     */
    public List<AttendanceReportResponse> getAttendanceReport(Long studentId, Long subjectId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + subjectId));

        List<AttendanceReportResponse> reports = new ArrayList<>();
        for (AttendanceType type : AttendanceType.values()) {
            long totalClasses = attendanceRepository.countByStudentIdAndSubjectIdAndType(studentId, subjectId, type);
            if (totalClasses == 0) {
                continue;
            }
            long classesAttended = attendanceRepository.countByStudentIdAndSubjectIdAndTypeAndStatus(
                studentId, subjectId, type, AttendanceStatus.PRESENT);

            BigDecimal attendancePercentage = BigDecimal.valueOf(classesAttended)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalClasses), 2, RoundingMode.HALF_UP);

            BigDecimal threshold = thresholdService.resolveThreshold(studentId, subjectId, type);
            boolean lowAttendance = attendancePercentage.compareTo(threshold) < 0;

            reports.add(new AttendanceReportResponse(
                student.getId(),
                student.getFullName(),
                student.getRollNumber(),
                subject.getId(),
                subject.getName(),
                subject.getCode(),
                type,
                totalClasses,
                classesAttended,
                attendancePercentage,
                threshold,
                lowAttendance
            ));
        }

        return reports;
    }

    public List<AttendanceReportResponse> getLowAttendanceAlerts(Long subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + subjectId));

        List<Student> students = courseRegistrationRepository.findRegisteredStudentsBySubjectId(subjectId);

        List<AttendanceReportResponse> alerts = new ArrayList<>();
        for (Student student : students) {
            for (AttendanceReportResponse report : getAttendanceReport(student.getId(), subjectId)) {
                if (report.lowAttendance()) {
                    alerts.add(report);
                }
            }
        }

        return alerts;
    }

    @Transactional
    public AttendanceResponse update(Long id, AttendanceRequest request) {
        Attendance attendance = attendanceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Attendance not found with id: " + id));

        Student student = studentRepository.findById(request.studentId())
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + request.studentId()));

        Subject subject = subjectRepository.findById(request.subjectId())
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + request.subjectId()));

        attendance.setStudent(student);
        attendance.setSubject(subject);
        attendance.setDate(request.date());
        attendance.setStatus(request.status());
        attendance.setType(request.type());
        attendance.setRemarks(request.remarks());

        Attendance updated = attendanceRepository.save(attendance);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!attendanceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Attendance not found with id: " + id);
        }
        attendanceRepository.deleteById(id);
    }

    private AttendanceResponse toResponse(Attendance attendance) {
        return new AttendanceResponse(
            attendance.getId(),
            attendance.getStudent().getId(),
            attendance.getStudent().getFullName(),
            attendance.getStudent().getRollNumber(),
            attendance.getSubject().getId(),
            attendance.getSubject().getName(),
            attendance.getSubject().getCode(),
            attendance.getDate(),
            attendance.getStatus(),
            attendance.getType(),
            attendance.getRemarks(),
            attendance.getMarkedBy() != null ? attendance.getMarkedBy().getId() : null,
            attendance.getMarkedBy() != null ?
                attendance.getMarkedBy().getFirstName() + " " + attendance.getMarkedBy().getLastName() : null,
            attendance.getCreatedAt(),
            attendance.getUpdatedAt()
        );
    }
}
