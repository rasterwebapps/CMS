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
import com.cms.model.Course;
import com.cms.model.Student;
import com.cms.model.enums.AttendanceStatus;
import com.cms.repository.AttendanceRepository;
import com.cms.repository.CourseRepository;
import com.cms.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class AttendanceService {

    private static final BigDecimal LOW_ATTENDANCE_THRESHOLD = new BigDecimal("75.00");

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    public AttendanceService(AttendanceRepository attendanceRepository,
                              StudentRepository studentRepository,
                              CourseRepository courseRepository) {
        this.attendanceRepository = attendanceRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional
    public AttendanceResponse markAttendance(AttendanceRequest request) {
        Student student = studentRepository.findById(request.studentId())
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + request.studentId()));

        Course course = courseRepository.findById(request.courseId())
            .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.courseId()));

        Attendance attendance = new Attendance(
            student, course, request.date(), request.status(), request.type()
        );
        attendance.setRemarks(request.remarks());

        Attendance saved = attendanceRepository.save(attendance);
        return toResponse(saved);
    }

    @Transactional
    public List<AttendanceResponse> markBulkAttendance(BulkAttendanceRequest request) {
        Course course = courseRepository.findById(request.courseId())
            .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.courseId()));

        List<AttendanceResponse> responses = new ArrayList<>();

        for (BulkAttendanceRequest.StudentAttendance sa : request.studentAttendances()) {
            Student student = studentRepository.findById(sa.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + sa.studentId()));

            Attendance attendance = new Attendance(
                student, course, request.date(), sa.status(), request.type()
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

    public List<AttendanceResponse> findByCourseId(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Course not found with id: " + courseId);
        }
        return attendanceRepository.findByCourseId(courseId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<AttendanceResponse> findByStudentIdAndCourseId(Long studentId, Long courseId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found with id: " + studentId);
        }
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Course not found with id: " + courseId);
        }
        return attendanceRepository.findByStudentIdAndCourseId(studentId, courseId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<AttendanceResponse> findByCourseIdAndDate(Long courseId, LocalDate date) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Course not found with id: " + courseId);
        }
        return attendanceRepository.findByCourseIdAndDate(courseId, date).stream()
            .map(this::toResponse)
            .toList();
    }

    public AttendanceReportResponse getAttendanceReport(Long studentId, Long courseId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        long totalClasses = attendanceRepository.countByStudentIdAndCourseId(studentId, courseId);
        long classesAttended = attendanceRepository.countByStudentIdAndCourseIdAndStatus(
            studentId, courseId, AttendanceStatus.PRESENT);

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
            course.getId(),
            course.getName(),
            course.getCode(),
            totalClasses,
            classesAttended,
            attendancePercentage,
            lowAttendance
        );
    }

    public List<AttendanceReportResponse> getLowAttendanceAlerts(Long courseId) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        List<Student> students = studentRepository.findByProgramId(course.getProgram().getId());

        List<AttendanceReportResponse> alerts = new ArrayList<>();
        for (Student student : students) {
            AttendanceReportResponse report = getAttendanceReport(student.getId(), courseId);
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

        Course course = courseRepository.findById(request.courseId())
            .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.courseId()));

        attendance.setStudent(student);
        attendance.setCourse(course);
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
            attendance.getCourse().getId(),
            attendance.getCourse().getName(),
            attendance.getCourse().getCode(),
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
