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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.AttendanceReportResponse;
import com.cms.dto.AttendanceRequest;
import com.cms.dto.AttendanceResponse;
import com.cms.dto.BulkAttendanceRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Attendance;
import com.cms.model.Course;
import com.cms.model.Department;
import com.cms.model.Program;
import com.cms.model.Student;
import com.cms.model.enums.AttendanceStatus;
import com.cms.model.enums.AttendanceType;
import com.cms.model.enums.StudentStatus;
import com.cms.repository.AttendanceRepository;
import com.cms.repository.CourseRepository;
import com.cms.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private CourseRepository courseRepository;

    private AttendanceService attendanceService;

    private Student testStudent;
    private Course testCourse;
    private Program testProgram;

    @BeforeEach
    void setUp() {
        attendanceService = new AttendanceService(attendanceRepository, studentRepository, courseRepository);

        Department department = new Department("Computer Science", "CS", "CS Dept", "Dr. Smith");
        department.setId(1L);

        testProgram = new Program();
        testProgram.setId(1L);
        testProgram.setName("B.Tech CS");

        testStudent = new Student(
            "CS2024001", "John", "Doe", "john@college.edu",
            testProgram, 1, LocalDate.of(2024, 6, 1), StudentStatus.ACTIVE
        );
        testStudent.setId(1L);

        testCourse = new Course("Data Structures", "CS201", 3, 2, 1, testProgram, 3);
        testCourse.setId(1L);
    }

    @Test
    void shouldMarkAttendance() {
        AttendanceRequest request = new AttendanceRequest(
            1L, 1L, LocalDate.now(), AttendanceStatus.PRESENT, AttendanceType.THEORY, null
        );

        Attendance savedAttendance = createAttendance(1L, testStudent, testCourse,
            LocalDate.now(), AttendanceStatus.PRESENT, AttendanceType.THEORY);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(savedAttendance);

        AttendanceResponse response = attendanceService.markAttendance(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(AttendanceStatus.PRESENT);
    }

    @Test
    void shouldThrowExceptionWhenStudentNotFoundForMarkAttendance() {
        AttendanceRequest request = new AttendanceRequest(
            999L, 1L, LocalDate.now(), AttendanceStatus.PRESENT, AttendanceType.THEORY, null
        );

        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.markAttendance(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Student not found with id: 999");
    }

    @Test
    void shouldThrowExceptionWhenCourseNotFoundForMarkAttendance() {
        AttendanceRequest request = new AttendanceRequest(
            1L, 999L, LocalDate.now(), AttendanceStatus.PRESENT, AttendanceType.THEORY, null
        );

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.markAttendance(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Course not found with id: 999");
    }

    @Test
    void shouldMarkBulkAttendance() {
        BulkAttendanceRequest.StudentAttendance sa1 = new BulkAttendanceRequest.StudentAttendance(
            1L, AttendanceStatus.PRESENT, null
        );

        BulkAttendanceRequest request = new BulkAttendanceRequest(
            1L, LocalDate.now(), AttendanceType.THEORY, List.of(sa1)
        );

        Attendance savedAttendance = createAttendance(1L, testStudent, testCourse,
            LocalDate.now(), AttendanceStatus.PRESENT, AttendanceType.THEORY);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(savedAttendance);

        List<AttendanceResponse> responses = attendanceService.markBulkAttendance(request);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).status()).isEqualTo(AttendanceStatus.PRESENT);
    }

    @Test
    void shouldFindByStudentId() {
        Attendance attendance = createAttendance(1L, testStudent, testCourse,
            LocalDate.now(), AttendanceStatus.PRESENT, AttendanceType.THEORY);

        when(studentRepository.existsById(1L)).thenReturn(true);
        when(attendanceRepository.findByStudentId(1L)).thenReturn(List.of(attendance));

        List<AttendanceResponse> responses = attendanceService.findByStudentId(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).studentId()).isEqualTo(1L);
    }

    @Test
    void shouldThrowExceptionWhenStudentNotFoundForFindByStudentId() {
        when(studentRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> attendanceService.findByStudentId(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Student not found with id: 999");
    }

    @Test
    void shouldFindByCourseId() {
        Attendance attendance = createAttendance(1L, testStudent, testCourse,
            LocalDate.now(), AttendanceStatus.PRESENT, AttendanceType.THEORY);

        when(courseRepository.existsById(1L)).thenReturn(true);
        when(attendanceRepository.findByCourseId(1L)).thenReturn(List.of(attendance));

        List<AttendanceResponse> responses = attendanceService.findByCourseId(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).courseId()).isEqualTo(1L);
    }

    @Test
    void shouldFindByStudentIdAndCourseId() {
        Attendance attendance = createAttendance(1L, testStudent, testCourse,
            LocalDate.now(), AttendanceStatus.PRESENT, AttendanceType.THEORY);

        when(studentRepository.existsById(1L)).thenReturn(true);
        when(courseRepository.existsById(1L)).thenReturn(true);
        when(attendanceRepository.findByStudentIdAndCourseId(1L, 1L)).thenReturn(List.of(attendance));

        List<AttendanceResponse> responses = attendanceService.findByStudentIdAndCourseId(1L, 1L);

        assertThat(responses).hasSize(1);
    }

    @Test
    void shouldThrowExceptionWhenStudentNotFoundForFindByStudentIdAndCourseId() {
        when(studentRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> attendanceService.findByStudentIdAndCourseId(999L, 1L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Student not found with id: 999");
    }

    @Test
    void shouldThrowExceptionWhenCourseNotFoundForFindByStudentIdAndCourseId() {
        when(studentRepository.existsById(1L)).thenReturn(true);
        when(courseRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> attendanceService.findByStudentIdAndCourseId(1L, 999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Course not found with id: 999");
    }

    @Test
    void shouldThrowExceptionWhenCourseNotFoundForFindByCourseId() {
        when(courseRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> attendanceService.findByCourseId(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Course not found with id: 999");
    }

    @Test
    void shouldThrowExceptionWhenCourseNotFoundForFindByCourseIdAndDate() {
        when(courseRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> attendanceService.findByCourseIdAndDate(999L, LocalDate.now()))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Course not found with id: 999");
    }

    @Test
    void shouldFindByCourseIdAndDate() {
        Attendance attendance = createAttendance(1L, testStudent, testCourse,
            LocalDate.now(), AttendanceStatus.PRESENT, AttendanceType.THEORY);

        when(courseRepository.existsById(1L)).thenReturn(true);
        when(attendanceRepository.findByCourseIdAndDate(1L, LocalDate.now())).thenReturn(List.of(attendance));

        List<AttendanceResponse> responses = attendanceService.findByCourseIdAndDate(1L, LocalDate.now());

        assertThat(responses).hasSize(1);
    }

    @Test
    void shouldGetAttendanceReport() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(attendanceRepository.countByStudentIdAndCourseId(1L, 1L)).thenReturn(10L);
        when(attendanceRepository.countByStudentIdAndCourseIdAndStatus(1L, 1L, AttendanceStatus.PRESENT))
            .thenReturn(8L);

        AttendanceReportResponse report = attendanceService.getAttendanceReport(1L, 1L);

        assertThat(report.totalClasses()).isEqualTo(10);
        assertThat(report.classesAttended()).isEqualTo(8);
        assertThat(report.attendancePercentage()).isEqualTo(new BigDecimal("80.00"));
        assertThat(report.lowAttendance()).isFalse();
    }

    @Test
    void shouldReportLowAttendance() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(attendanceRepository.countByStudentIdAndCourseId(1L, 1L)).thenReturn(10L);
        when(attendanceRepository.countByStudentIdAndCourseIdAndStatus(1L, 1L, AttendanceStatus.PRESENT))
            .thenReturn(6L);

        AttendanceReportResponse report = attendanceService.getAttendanceReport(1L, 1L);

        assertThat(report.attendancePercentage()).isEqualTo(new BigDecimal("60.00"));
        assertThat(report.lowAttendance()).isTrue();
    }

    @Test
    void shouldReportZeroAttendanceWhenNoClasses() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(attendanceRepository.countByStudentIdAndCourseId(1L, 1L)).thenReturn(0L);
        when(attendanceRepository.countByStudentIdAndCourseIdAndStatus(1L, 1L, AttendanceStatus.PRESENT))
            .thenReturn(0L);

        AttendanceReportResponse report = attendanceService.getAttendanceReport(1L, 1L);

        assertThat(report.attendancePercentage()).isEqualTo(BigDecimal.ZERO);
        assertThat(report.lowAttendance()).isTrue();
    }

    @Test
    void shouldGetLowAttendanceAlerts() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(studentRepository.findByProgramId(1L)).thenReturn(List.of(testStudent));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(attendanceRepository.countByStudentIdAndCourseId(1L, 1L)).thenReturn(10L);
        when(attendanceRepository.countByStudentIdAndCourseIdAndStatus(1L, 1L, AttendanceStatus.PRESENT))
            .thenReturn(6L);

        List<AttendanceReportResponse> alerts = attendanceService.getLowAttendanceAlerts(1L);

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).lowAttendance()).isTrue();
    }

    @Test
    void shouldThrowExceptionWhenCourseNotFoundForLowAttendanceAlerts() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.getLowAttendanceAlerts(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Course not found with id: 999");
    }

    @Test
    void shouldUpdateAttendance() {
        Attendance existingAttendance = createAttendance(1L, testStudent, testCourse,
            LocalDate.now(), AttendanceStatus.PRESENT, AttendanceType.THEORY);

        AttendanceRequest updateRequest = new AttendanceRequest(
            1L, 1L, LocalDate.now(), AttendanceStatus.ABSENT, AttendanceType.THEORY, "Was sick"
        );

        Attendance updatedAttendance = createAttendance(1L, testStudent, testCourse,
            LocalDate.now(), AttendanceStatus.ABSENT, AttendanceType.THEORY);
        updatedAttendance.setRemarks("Was sick");

        when(attendanceRepository.findById(1L)).thenReturn(Optional.of(existingAttendance));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(updatedAttendance);

        AttendanceResponse response = attendanceService.update(1L, updateRequest);

        assertThat(response.status()).isEqualTo(AttendanceStatus.ABSENT);
    }

    @Test
    void shouldDeleteAttendance() {
        when(attendanceRepository.existsById(1L)).thenReturn(true);

        attendanceService.delete(1L);

        verify(attendanceRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentAttendance() {
        when(attendanceRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> attendanceService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Attendance not found with id: 999");

        verify(attendanceRepository, never()).deleteById(any());
    }

    private Attendance createAttendance(Long id, Student student, Course course,
                                         LocalDate date, AttendanceStatus status, AttendanceType type) {
        Attendance attendance = new Attendance(student, course, date, status, type);
        attendance.setId(id);
        Instant now = Instant.now();
        attendance.setCreatedAt(now);
        attendance.setUpdatedAt(now);
        return attendance;
    }
}
