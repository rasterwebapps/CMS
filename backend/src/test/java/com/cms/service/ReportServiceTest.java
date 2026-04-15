package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.cms.dto.AttendanceAnalyticsReportResponse;
import com.cms.dto.LabUtilizationReportResponse;
import com.cms.dto.StudentPerformanceReportResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Course;
import com.cms.model.ExamResult;
import com.cms.model.Examination;
import com.cms.model.Experiment;
import com.cms.model.LabContinuousEvaluation;
import com.cms.model.Semester;
import com.cms.model.Student;
import com.cms.model.enums.ExamResultStatus;
import com.cms.model.enums.ExamType;
import com.cms.model.enums.StudentStatus;
import com.cms.repository.AttendanceRepository;
import com.cms.repository.EquipmentRepository;
import com.cms.repository.ExamResultRepository;
import com.cms.repository.LabContinuousEvaluationRepository;
import com.cms.repository.LabRepository;
import com.cms.repository.LabScheduleRepository;
import com.cms.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private LabRepository labRepository;

    @Mock
    private LabScheduleRepository labScheduleRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private ExamResultRepository examResultRepository;

    @Mock
    private LabContinuousEvaluationRepository labContinuousEvaluationRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(labRepository, labScheduleRepository, studentRepository,
            attendanceRepository, examResultRepository, labContinuousEvaluationRepository,
            equipmentRepository);
    }

    @Test
    void shouldGetLabUtilizationReport() {
        when(labRepository.count()).thenReturn(5L);
        when(labScheduleRepository.count()).thenReturn(20L);
        when(equipmentRepository.count()).thenReturn(10L);
        when(equipmentRepository.findAll()).thenReturn(List.of());
        when(labRepository.findAll()).thenReturn(List.of());

        LabUtilizationReportResponse response = reportService.getLabUtilizationReport();

        assertThat(response.totalLabs()).isEqualTo(5L);
        assertThat(response.totalSchedules()).isEqualTo(20L);
        assertThat(response.averageSchedulesPerLab()).isEqualTo(4.0);
        assertThat(response.totalEquipment()).isEqualTo(10L);
        verify(labRepository).count();
        verify(labScheduleRepository).count();
    }

    @Test
    void shouldGetLabUtilizationReportWhenNoLabs() {
        when(labRepository.count()).thenReturn(0L);
        when(labScheduleRepository.count()).thenReturn(0L);
        when(equipmentRepository.count()).thenReturn(0L);
        when(equipmentRepository.findAll()).thenReturn(List.of());
        when(labRepository.findAll()).thenReturn(List.of());

        LabUtilizationReportResponse response = reportService.getLabUtilizationReport();

        assertThat(response.totalLabs()).isEqualTo(0L);
        assertThat(response.totalSchedules()).isEqualTo(0L);
        assertThat(response.averageSchedulesPerLab()).isEqualTo(0.0);
    }

    @Test
    void shouldGetStudentPerformanceReport() {
        Student student = createStudent();
        Examination examination = createExamination();
        Course course = createCourse();
        Experiment experiment = createExperiment(course);

        ExamResult examResult = new ExamResult(examination, student,
            new BigDecimal("85.50"), "A", ExamResultStatus.PUBLISHED);
        examResult.setId(1L);
        examResult.setCreatedAt(Instant.now());
        examResult.setUpdatedAt(Instant.now());

        LabContinuousEvaluation labEval = new LabContinuousEvaluation(
            experiment, student, 8, 7, 9, 24, LocalDate.of(2024, 6, 1), "Dr. Smith");
        labEval.setId(1L);
        labEval.setCreatedAt(Instant.now());
        labEval.setUpdatedAt(Instant.now());

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(examResultRepository.findByStudentId(1L)).thenReturn(List.of(examResult));
        when(labContinuousEvaluationRepository.findByStudentId(1L)).thenReturn(List.of(labEval));

        StudentPerformanceReportResponse response = reportService.getStudentPerformanceReport(1L);

        assertThat(response.studentId()).isEqualTo(1L);
        assertThat(response.studentName()).isEqualTo("John Doe");
        assertThat(response.examResults()).hasSize(1);
        assertThat(response.labEvaluations()).hasSize(1);
        verify(studentRepository).findById(1L);
        verify(examResultRepository).findByStudentId(1L);
        verify(labContinuousEvaluationRepository).findByStudentId(1L);
    }

    @Test
    void shouldThrowWhenStudentNotFoundForPerformanceReport() {
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.getStudentPerformanceReport(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Student not found with id: 999");
    }

    @Test
    void shouldGetAttendanceAnalyticsReport() {
        when(studentRepository.count()).thenReturn(100L);
        when(attendanceRepository.count()).thenReturn(3500L);
        when(attendanceRepository.findAll()).thenReturn(List.of());

        AttendanceAnalyticsReportResponse response = reportService.getAttendanceAnalyticsReport();

        assertThat(response.totalStudents()).isEqualTo(100L);
        assertThat(response.totalAttendanceRecords()).isEqualTo(3500L);
        verify(studentRepository).count();
        verify(attendanceRepository).count();
    }

    private Course createCourse() {
        Course course = new Course("Physics", "PHY101", 4, 3, 1, null, 1);
        course.setId(1L);
        return course;
    }

    private Semester createSemester() {
        Semester semester = new Semester("Semester 1", null, LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 6, 30), 1);
        semester.setId(1L);
        return semester;
    }

    private Examination createExamination() {
        Examination exam = new Examination("Midterm", createCourse(), ExamType.THEORY,
            LocalDate.of(2024, 6, 1), 120, 100, createSemester());
        exam.setId(1L);
        exam.setCreatedAt(Instant.now());
        exam.setUpdatedAt(Instant.now());
        return exam;
    }

    private Student createStudent() {
        Student student = new Student("ROLL001", "John", "Doe", "john@example.com",
            null, 1, LocalDate.of(2024, 1, 1), StudentStatus.ACTIVE);
        student.setId(1L);
        return student;
    }

    private Experiment createExperiment(Course course) {
        Experiment experiment = new Experiment(course, 1, "Ohm's Law",
            null, null, null, null, null, null, null, true);
        experiment.setId(1L);
        return experiment;
    }
}
