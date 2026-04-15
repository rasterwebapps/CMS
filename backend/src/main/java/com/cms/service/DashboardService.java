package com.cms.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.DashboardSummaryResponse;
import com.cms.model.Attendance;
import com.cms.model.Equipment;
import com.cms.model.MaintenanceRequest;
import com.cms.model.Student;
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

/**
 * Provides aggregated KPI data for the main dashboard.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final ProgramRepository programRepository;
    private final LabRepository labRepository;
    private final EquipmentRepository equipmentRepository;
    private final ExaminationRepository examinationRepository;
    private final FeePaymentRepository feePaymentRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final AttendanceRepository attendanceRepository;

    public DashboardService(StudentRepository studentRepository,
                            FacultyRepository facultyRepository,
                            DepartmentRepository departmentRepository,
                            CourseRepository courseRepository,
                            ProgramRepository programRepository,
                            LabRepository labRepository,
                            EquipmentRepository equipmentRepository,
                            ExaminationRepository examinationRepository,
                            FeePaymentRepository feePaymentRepository,
                            MaintenanceRequestRepository maintenanceRequestRepository,
                            AttendanceRepository attendanceRepository) {
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.departmentRepository = departmentRepository;
        this.courseRepository = courseRepository;
        this.programRepository = programRepository;
        this.labRepository = labRepository;
        this.equipmentRepository = equipmentRepository;
        this.examinationRepository = examinationRepository;
        this.feePaymentRepository = feePaymentRepository;
        this.maintenanceRequestRepository = maintenanceRequestRepository;
        this.attendanceRepository = attendanceRepository;
    }

    /**
     * Collects counts and status breakdowns across all major entities.
     */
    public DashboardSummaryResponse getSummary() {
        long totalStudents = studentRepository.count();
        long totalFaculty = facultyRepository.count();
        long totalDepartments = departmentRepository.count();
        long totalCourses = courseRepository.count();
        long totalPrograms = programRepository.count();
        long totalLabs = labRepository.count();
        long totalEquipment = equipmentRepository.count();
        long totalExaminations = examinationRepository.count();
        long totalFeePayments = feePaymentRepository.count();
        long totalMaintenanceRequests = maintenanceRequestRepository.count();
        long totalAttendanceRecords = attendanceRepository.count();

        Map<String, Long> equipmentByStatus = buildEquipmentStatusMap();
        Map<String, Long> maintenanceByStatus = buildMaintenanceStatusMap();
        Map<String, Long> studentsByStatus = buildStudentStatusMap();
        Map<String, Long> attendanceByStatus = buildAttendanceStatusMap();

        return new DashboardSummaryResponse(
            totalStudents, totalFaculty, totalDepartments, totalCourses,
            totalPrograms, totalLabs, totalEquipment, totalExaminations,
            totalFeePayments, totalMaintenanceRequests, totalAttendanceRecords,
            equipmentByStatus, maintenanceByStatus, studentsByStatus, attendanceByStatus
        );
    }

    private Map<String, Long> buildEquipmentStatusMap() {
        List<Equipment> allEquipment = equipmentRepository.findAll();
        Map<String, Long> map = new LinkedHashMap<>();
        for (Equipment eq : allEquipment) {
            String status = eq.getStatus().name();
            map.merge(status, 1L, Long::sum);
        }
        return map;
    }

    private Map<String, Long> buildMaintenanceStatusMap() {
        List<MaintenanceRequest> allMaintenance = maintenanceRequestRepository.findAll();
        Map<String, Long> map = new LinkedHashMap<>();
        for (MaintenanceRequest mr : allMaintenance) {
            String status = mr.getStatus().name();
            map.merge(status, 1L, Long::sum);
        }
        return map;
    }

    private Map<String, Long> buildStudentStatusMap() {
        List<Student> allStudents = studentRepository.findAll();
        Map<String, Long> map = new LinkedHashMap<>();
        for (Student s : allStudents) {
            String status = s.getStatus().name();
            map.merge(status, 1L, Long::sum);
        }
        return map;
    }

    private Map<String, Long> buildAttendanceStatusMap() {
        List<Attendance> allAttendance = attendanceRepository.findAll();
        Map<String, Long> map = new LinkedHashMap<>();
        for (Attendance a : allAttendance) {
            String status = a.getStatus().name();
            map.merge(status, 1L, Long::sum);
        }
        return map;
    }
}

