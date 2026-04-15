package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.DashboardSummaryResponse;
import com.cms.repository.AttendanceRepository;
import com.cms.repository.CourseRepository;
import com.cms.repository.DepartmentRepository;
import com.cms.repository.EquipmentRepository;
import com.cms.repository.ExaminationRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.FeePaymentRepository;
import com.cms.repository.LabRepository;
import com.cms.repository.MaintenanceRequestRepository;
import com.cms.repository.ProgramRepository;
import com.cms.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private StudentRepository studentRepository;
    @Mock private FacultyRepository facultyRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private ProgramRepository programRepository;
    @Mock private LabRepository labRepository;
    @Mock private EquipmentRepository equipmentRepository;
    @Mock private ExaminationRepository examinationRepository;
    @Mock private FeePaymentRepository feePaymentRepository;
    @Mock private MaintenanceRequestRepository maintenanceRequestRepository;
    @Mock private AttendanceRepository attendanceRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
            studentRepository, facultyRepository, departmentRepository,
            courseRepository, programRepository, labRepository,
            equipmentRepository, examinationRepository, feePaymentRepository,
            maintenanceRequestRepository, attendanceRepository
        );
    }

    @Test
    void shouldReturnSummaryWithCounts() {
        when(studentRepository.count()).thenReturn(10L);
        when(facultyRepository.count()).thenReturn(10L);
        when(departmentRepository.count()).thenReturn(10L);
        when(courseRepository.count()).thenReturn(10L);
        when(programRepository.count()).thenReturn(10L);
        when(labRepository.count()).thenReturn(10L);
        when(equipmentRepository.count()).thenReturn(10L);
        when(examinationRepository.count()).thenReturn(10L);
        when(feePaymentRepository.count()).thenReturn(10L);
        when(maintenanceRequestRepository.count()).thenReturn(10L);
        when(attendanceRepository.count()).thenReturn(10L);
        when(equipmentRepository.findAll()).thenReturn(List.of());
        when(maintenanceRequestRepository.findAll()).thenReturn(List.of());
        when(studentRepository.findAll()).thenReturn(List.of());
        when(attendanceRepository.findAll()).thenReturn(List.of());

        DashboardSummaryResponse response = dashboardService.getSummary();

        assertThat(response.totalStudents()).isEqualTo(10L);
        assertThat(response.totalFaculty()).isEqualTo(10L);
        assertThat(response.totalDepartments()).isEqualTo(10L);
        assertThat(response.totalCourses()).isEqualTo(10L);
        assertThat(response.totalPrograms()).isEqualTo(10L);
        assertThat(response.totalLabs()).isEqualTo(10L);
        assertThat(response.totalEquipment()).isEqualTo(10L);
        assertThat(response.totalExaminations()).isEqualTo(10L);
        assertThat(response.totalFeePayments()).isEqualTo(10L);
        assertThat(response.totalMaintenanceRequests()).isEqualTo(10L);
        assertThat(response.totalAttendanceRecords()).isEqualTo(10L);
        assertThat(response.equipmentByStatus()).isEmpty();
        assertThat(response.maintenanceByStatus()).isEmpty();
        assertThat(response.studentsByStatus()).isEmpty();
        assertThat(response.attendanceByStatus()).isEmpty();
        verify(studentRepository).count();
        verify(facultyRepository).count();
        verify(departmentRepository).count();
    }

    @Test
    void shouldReturnZeroCounts() {
        when(studentRepository.count()).thenReturn(0L);
        when(facultyRepository.count()).thenReturn(0L);
        when(departmentRepository.count()).thenReturn(0L);
        when(courseRepository.count()).thenReturn(0L);
        when(programRepository.count()).thenReturn(0L);
        when(labRepository.count()).thenReturn(0L);
        when(equipmentRepository.count()).thenReturn(0L);
        when(examinationRepository.count()).thenReturn(0L);
        when(feePaymentRepository.count()).thenReturn(0L);
        when(maintenanceRequestRepository.count()).thenReturn(0L);
        when(attendanceRepository.count()).thenReturn(0L);
        when(equipmentRepository.findAll()).thenReturn(List.of());
        when(maintenanceRequestRepository.findAll()).thenReturn(List.of());
        when(studentRepository.findAll()).thenReturn(List.of());
        when(attendanceRepository.findAll()).thenReturn(List.of());

        DashboardSummaryResponse response = dashboardService.getSummary();

        assertThat(response.totalStudents()).isZero();
        assertThat(response.totalFaculty()).isZero();
        assertThat(response.totalDepartments()).isZero();
    }
}

