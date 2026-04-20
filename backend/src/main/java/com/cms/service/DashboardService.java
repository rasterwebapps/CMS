package com.cms.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.DashboardSummaryResponse;
import com.cms.dto.DashboardTrendPoint;
import com.cms.dto.DashboardTrendsResponse;
import com.cms.model.Attendance;
import com.cms.model.EnquiryPayment;
import com.cms.model.Equipment;
import com.cms.model.FeePayment;
import com.cms.model.MaintenanceRequest;
import com.cms.model.Student;
import com.cms.repository.AttendanceRepository;
import com.cms.repository.DepartmentRepository;
import com.cms.repository.EnquiryPaymentRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.EquipmentRepository;
import com.cms.repository.ExaminationRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.FeePaymentRepository;
import com.cms.repository.LabRepository;
import com.cms.repository.MaintenanceRequestRepository;
import com.cms.repository.ProgramRepository;
import com.cms.repository.StudentRepository;
import com.cms.repository.SubjectRepository;

/**
 * Provides aggregated KPI data for the main dashboard.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final DepartmentRepository departmentRepository;
    private final SubjectRepository subjectRepository;
    private final ProgramRepository programRepository;
    private final LabRepository labRepository;
    private final EquipmentRepository equipmentRepository;
    private final ExaminationRepository examinationRepository;
    private final FeePaymentRepository feePaymentRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final AttendanceRepository attendanceRepository;
    private final EnquiryRepository enquiryRepository;
    private final EnquiryPaymentRepository enquiryPaymentRepository;

    public DashboardService(StudentRepository studentRepository,
                            FacultyRepository facultyRepository,
                            DepartmentRepository departmentRepository,
                            SubjectRepository subjectRepository,
                            ProgramRepository programRepository,
                            LabRepository labRepository,
                            EquipmentRepository equipmentRepository,
                            ExaminationRepository examinationRepository,
                            FeePaymentRepository feePaymentRepository,
                            MaintenanceRequestRepository maintenanceRequestRepository,
                            AttendanceRepository attendanceRepository,
                            EnquiryRepository enquiryRepository,
                            EnquiryPaymentRepository enquiryPaymentRepository) {
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.departmentRepository = departmentRepository;
        this.subjectRepository = subjectRepository;
        this.programRepository = programRepository;
        this.labRepository = labRepository;
        this.equipmentRepository = equipmentRepository;
        this.examinationRepository = examinationRepository;
        this.feePaymentRepository = feePaymentRepository;
        this.maintenanceRequestRepository = maintenanceRequestRepository;
        this.attendanceRepository = attendanceRepository;
        this.enquiryRepository = enquiryRepository;
        this.enquiryPaymentRepository = enquiryPaymentRepository;
    }

    /**
     * Collects counts and status breakdowns across all major entities.
     */
    public DashboardSummaryResponse getSummary() {
        long totalStudents = studentRepository.count();
        long totalFaculty = facultyRepository.count();
        long totalDepartments = departmentRepository.count();
        long totalSubjects = subjectRepository.count();
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
        Map<String, Long> enquiryFunnel = buildEnquiryFunnelMap();
        BigDecimal feeCollectedThisMonth = computeFeeCollectedThisMonth();
        BigDecimal feeOutstanding = computeFeeOutstanding();

        return new DashboardSummaryResponse(
            totalStudents, totalFaculty, totalDepartments, totalSubjects,
            totalPrograms, totalLabs, totalEquipment, totalExaminations,
            totalFeePayments, totalMaintenanceRequests, totalAttendanceRecords,
            equipmentByStatus, maintenanceByStatus, studentsByStatus, attendanceByStatus,
            enquiryFunnel, feeCollectedThisMonth, feeOutstanding
        );
    }

    /**
     * Returns 6-month trend data for enrolments and fee collection.
     */
    public DashboardTrendsResponse getTrends() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");
        YearMonth current = YearMonth.now();

        List<DashboardTrendPoint> enrolmentTrend = new ArrayList<>();
        List<DashboardTrendPoint> feeCollectionTrend = new ArrayList<>();

        List<Student> allStudents = studentRepository.findAll();

        for (int i = 5; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            String label = ym.format(formatter);

            long enrolled = allStudents.stream()
                .filter(s -> s.getAdmissionDate() != null
                    && YearMonth.from(s.getAdmissionDate()).equals(ym))
                .count();
            enrolmentTrend.add(new DashboardTrendPoint(label, enrolled));

            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            List<FeePayment> payments = feePaymentRepository.findByPaymentDateBetween(start, end);
            BigDecimal monthlyFees = payments.stream()
                .map(FeePayment::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            feeCollectionTrend.add(new DashboardTrendPoint(label, monthlyFees.longValue()));
        }

        return new DashboardTrendsResponse(enrolmentTrend, feeCollectionTrend);
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

    private Map<String, Long> buildEnquiryFunnelMap() {
        Map<String, Long> map = new LinkedHashMap<>();
        enquiryRepository.findAll().forEach(e -> {
            String status = e.getStatus().name();
            map.merge(status, 1L, Long::sum);
        });
        return map;
    }

    private BigDecimal computeFeeCollectedThisMonth() {
        YearMonth current = YearMonth.now();
        LocalDate start = current.atDay(1);
        LocalDate end = LocalDate.now();
        return feePaymentRepository.findByPaymentDateBetween(start, end).stream()
            .map(FeePayment::getAmountPaid)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal computeFeeOutstanding() {
        BigDecimal totalFinalized = enquiryRepository.findAll().stream()
            .map(e -> e.getFinalizedNetFee() != null ? e.getFinalizedNetFee() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = enquiryPaymentRepository.findAll().stream()
            .map(EnquiryPayment::getAmountPaid)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal outstanding = totalFinalized.subtract(totalPaid);
        return outstanding.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : outstanding;
    }
}

