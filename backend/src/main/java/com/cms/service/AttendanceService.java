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
import com.cms.dto.BulkAttendanceRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Attendance;
import com.cms.model.Subject;
import com.cms.model.Student;
import com.cms.model.enums.AttendanceStatus;
import com.cms.repository.AttendanceRepository;
import com.cms.repository.SubjectRepository;
import com.cms.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class AttendanceService {

    private static final BigDecimal LOW_ATTENDANCE_THRESHOLD = new BigDecimal("75.00");

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;

    public AttendanceService(AttendanceRepository attendanceRepository,
                              StudentRepository studentRepository,
                              SubjectRepository subjectRepository) {
        this.attendanceRepository = attendanceRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
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

    public AttendanceReportResponse getAttendanceReport(Long studentId, Long subjectId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + subjectId));

        long totalClasses = attendanceRepository.countByStudentIdAndSubjectId(studentId, subjectId);
        long classesAttended = attendanceRepository.countByStudentIdAndSubjectIdAndStatus(
            studentId, subjectId, AttendanceStatus.PRESENT);

        BigDecimal attendancePercentage = BigDecimal.ZERO;
        if (totalClasses > 0) {
            attendancePercentage = BigDecimal.valueOf(classesAttended)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalClasses), 2, RoundingMode.HALF_UP);
        }

        boolean lowAttendance = attendancePercentage.compareTo(LOW_ATTENDANCE_THRESHOLD) < 0;

        return new AttendanceReportResponse(
            student.getId(),
            student.getFullName(),
            student.getRollNumber(),
            subject.getId(),
            subject.getName(),
            subject.getCode(),
            totalClasses,
            classesAttended,
            attendancePercentage,
            lowAttendance
        );
    }

    public List<AttendanceReportResponse> getLowAttendanceAlerts(Long subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + subjectId));

        List<Student> students = studentRepository.findByProgramId(subject.getCourse().getProgram().getId());

        List<AttendanceReportResponse> alerts = new ArrayList<>();
        for (Student student : students) {
            AttendanceReportResponse report = getAttendanceReport(student.getId(), subjectId);
            if (report.lowAttendance()) {
                alerts.add(report);
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
