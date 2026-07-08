package com.cms.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.DashboardSummaryResponse;
import com.cms.dto.DashboardTrendPoint;
import com.cms.dto.DashboardTrendsResponse;
import com.cms.dto.FrontOfficeDashboardResponse;
import com.cms.dto.FrontOfficeEnquiryItem;
import com.cms.model.EnquiryPayment;
import com.cms.model.Enquiry;
import com.cms.model.Student;
import com.cms.model.enums.EnquiryStatus;
import com.cms.repository.AdmissionRepository;
import com.cms.repository.AttendanceRepository;
import com.cms.repository.SpecialityRepository;
import com.cms.repository.EnquiryPaymentRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.EquipmentRepository;
import com.cms.repository.ExaminationRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.LabRepository;
import com.cms.repository.MaintenanceRequestRepository;
import com.cms.repository.PaymentReceiptRepository;
import com.cms.repository.ProgramRepository;
import com.cms.repository.StudentFeeAllocationRepository;
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
    private final SpecialityRepository specialityRepository;
    private final SubjectRepository subjectRepository;
    private final ProgramRepository programRepository;
    private final LabRepository labRepository;
    private final EquipmentRepository equipmentRepository;
    private final ExaminationRepository examinationRepository;
    private final PaymentReceiptRepository paymentReceiptRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final AttendanceRepository attendanceRepository;
    private final EnquiryRepository enquiryRepository;
    private final EnquiryPaymentRepository enquiryPaymentRepository;
    private final AdmissionRepository admissionRepository;
    private final StudentFeeAllocationRepository allocationRepository;
    private final EnquiryPaymentService enquiryPaymentService;
    private final FeeFinalizationService feeFinalizationService;
    private final PaymentCollectionService paymentCollectionService;

    public DashboardService(StudentRepository studentRepository,
                            FacultyRepository facultyRepository,
                            SpecialityRepository specialityRepository,
                            SubjectRepository subjectRepository,
                            ProgramRepository programRepository,
                            LabRepository labRepository,
                            EquipmentRepository equipmentRepository,
                            ExaminationRepository examinationRepository,
                            PaymentReceiptRepository paymentReceiptRepository,
                            MaintenanceRequestRepository maintenanceRequestRepository,
                            AttendanceRepository attendanceRepository,
                            EnquiryRepository enquiryRepository,
                            EnquiryPaymentRepository enquiryPaymentRepository,
                            AdmissionRepository admissionRepository,
                            StudentFeeAllocationRepository allocationRepository,
                            EnquiryPaymentService enquiryPaymentService,
                            FeeFinalizationService feeFinalizationService,
                            PaymentCollectionService paymentCollectionService) {
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.specialityRepository = specialityRepository;
        this.subjectRepository = subjectRepository;
        this.programRepository = programRepository;
        this.labRepository = labRepository;
        this.equipmentRepository = equipmentRepository;
        this.examinationRepository = examinationRepository;
        this.paymentReceiptRepository = paymentReceiptRepository;
        this.maintenanceRequestRepository = maintenanceRequestRepository;
        this.attendanceRepository = attendanceRepository;
        this.enquiryRepository = enquiryRepository;
        this.enquiryPaymentRepository = enquiryPaymentRepository;
        this.admissionRepository = admissionRepository;
        this.allocationRepository = allocationRepository;
        this.enquiryPaymentService = enquiryPaymentService;
        this.feeFinalizationService = feeFinalizationService;
        this.paymentCollectionService = paymentCollectionService;
    }

    /**
     * Collects counts and status breakdowns across all major entities.
     */
    public DashboardSummaryResponse getSummary() {
        long totalStudents = studentRepository.count();
        long totalFaculty = facultyRepository.count();
        long totalSpecialities = specialityRepository.count();
        long totalSubjects = subjectRepository.count();
        long totalPrograms = programRepository.count();
        long totalLabs = labRepository.count();
        long totalEquipment = equipmentRepository.count();
        long totalExaminations = examinationRepository.count();
        long totalFeePayments = paymentReceiptRepository.count();
        long totalMaintenanceRequests = maintenanceRequestRepository.count();
        long totalAttendanceRecords = attendanceRepository.count();

        Map<String, Long> equipmentByStatus = buildEquipmentStatusMap();
        Map<String, Long> maintenanceByStatus = buildMaintenanceStatusMap();
        Map<String, Long> studentsByStatus = buildStudentStatusMap();
        Map<String, Long> attendanceByStatus = buildAttendanceStatusMap();
        Map<String, Long> enquiryFunnel = buildEnquiryFunnelMap();
        BigDecimal feeCollectedThisMonth = computeFeeCollectedThisMonth();
        BigDecimal feeOutstanding = computeFeeOutstanding();
        long enquiryCollectPaymentEligibleCount = computeEnquiryCollectPaymentCount();
        long collectPaymentEligibleCount = enquiryCollectPaymentEligibleCount + computeStudentCollectPaymentCount();

        return new DashboardSummaryResponse(
            totalStudents, totalFaculty, totalSpecialities, totalSubjects,
            totalPrograms, totalLabs, totalEquipment, totalExaminations,
            totalFeePayments, totalMaintenanceRequests, totalAttendanceRecords,
            equipmentByStatus, maintenanceByStatus, studentsByStatus, attendanceByStatus,
            enquiryFunnel, feeCollectedThisMonth, feeOutstanding,
            collectPaymentEligibleCount, enquiryCollectPaymentEligibleCount
        );
    }

    /** Exposed for the lightweight fee-collection count endpoint — same logic as the badge. */
    public long getFeeCollectionEligibleCount() {
        return computeCollectPaymentEligibleCount();
    }

    private long computeEnquiryCollectPaymentCount() {
        long count = 0;
        List<Long> eligibleEnquiryIds = enquiryRepository.findPaymentEligibleEnquiryIds();
        if (!eligibleEnquiryIds.isEmpty()) {
            List<Enquiry> eligibleEnquiries = enquiryRepository.findAllById(eligibleEnquiryIds);
            for (Enquiry enquiry : eligibleEnquiries) {
                BigDecimal totalPaid = Optional.ofNullable(
                    enquiryPaymentRepository.sumAmountPaidByEnquiryId(enquiry.getId()))
                    .orElse(BigDecimal.ZERO);
                if (enquiryPaymentService.getCollectibleOutstanding(enquiry, totalPaid)
                        .compareTo(BigDecimal.ZERO) > 0) {
                    count++;
                }
            }
        }
        return count;
    }

    private long computeStudentCollectPaymentCount() {
        long count = 0;
        List<Long> finalizedStudentIds = allocationRepository.findFinalizedStudentIds();
        final int batchSize = 200;
        for (int i = 0; i < finalizedStudentIds.size(); i += batchSize) {
            int end = Math.min(i + batchSize, finalizedStudentIds.size());
            List<Long> batchIds = finalizedStudentIds.subList(i, end);
            List<Student> students = studentRepository.findByIdInWithRelations(batchIds);
            count += students.stream()
                .filter(s -> paymentCollectionService.getCollectibleOutstanding(s).compareTo(BigDecimal.ZERO) > 0)
                .count();
        }
        return count;
    }

    private long computeCollectPaymentEligibleCount() {
        return computeEnquiryCollectPaymentCount() + computeStudentCollectPaymentCount();
    }

    /**
     * Returns 6-month trend data for enrolments and fee collection.
     * Uses two aggregated DB queries (instead of findAll + 6 per-month queries).
     */
    public DashboardTrendsResponse getTrends() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");
        YearMonth current = YearMonth.now();
        YearMonth fromMonth = current.minusMonths(5);

        LocalDate fromDate = fromMonth.atDay(1);
        LocalDate toDate = current.atEndOfMonth();

        Map<YearMonth, Long> enrollmentByMonth = new LinkedHashMap<>();
        for (Object[] row : studentRepository.countAdmissionsByYearMonth(fromDate, toDate)) {
            enrollmentByMonth.put(
                YearMonth.of(((Number) row[0]).intValue(), ((Number) row[1]).intValue()),
                ((Number) row[2]).longValue());
        }

        Map<YearMonth, Long> feesByMonth = new LinkedHashMap<>();
        for (Object[] row : paymentReceiptRepository.sumAmountByYearMonth(fromDate, toDate)) {
            feesByMonth.put(
                YearMonth.of(((Number) row[0]).intValue(), ((Number) row[1]).intValue()),
                ((Number) row[2]).longValue());
        }

        List<DashboardTrendPoint> enrolmentTrend = new ArrayList<>();
        List<DashboardTrendPoint> feeCollectionTrend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            String label = ym.format(formatter);
            enrolmentTrend.add(new DashboardTrendPoint(label, enrollmentByMonth.getOrDefault(ym, 0L)));
            feeCollectionTrend.add(new DashboardTrendPoint(label, feesByMonth.getOrDefault(ym, 0L)));
        }

        return new DashboardTrendsResponse(enrolmentTrend, feeCollectionTrend);
    }

    private Map<String, Long> buildEquipmentStatusMap() {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : equipmentRepository.countByStatusGrouped()) {
            map.put(row[0].toString(), (Long) row[1]);
        }
        return map;
    }

    private Map<String, Long> buildMaintenanceStatusMap() {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : maintenanceRequestRepository.countByStatusGrouped()) {
            map.put(row[0].toString(), (Long) row[1]);
        }
        return map;
    }

    private Map<String, Long> buildStudentStatusMap() {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : studentRepository.countByStatusGrouped()) {
            map.put(row[0].toString(), (Long) row[1]);
        }
        return map;
    }

    private Map<String, Long> buildAttendanceStatusMap() {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : attendanceRepository.countByStatusGrouped()) {
            map.put(row[0].toString(), (Long) row[1]);
        }
        return map;
    }

    private Map<String, Long> buildEnquiryFunnelMap() {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : enquiryRepository.countByStatusGrouped()) {
            map.put(row[0].toString(), (Long) row[1]);
        }
        return map;
    }

    private BigDecimal computeFeeCollectedThisMonth() {
        YearMonth current = YearMonth.now();
        LocalDate start = current.atDay(1);
        LocalDate end = LocalDate.now();
        return paymentReceiptRepository.findByPaymentDateBetween(start, end).stream()
            .map(com.cms.model.PaymentReceipt::getAmountPaid)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal computeFeeOutstanding() {
        BigDecimal totalFinalized = enquiryRepository.sumFinalizedNetFee();
        BigDecimal totalPaid = enquiryPaymentRepository.sumAllAmountPaid();
        BigDecimal outstanding = totalFinalized.subtract(totalPaid);
        return outstanding.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : outstanding;
    }

    /**
     * Collects Front Office-specific KPI data for the front office dashboard.
     */
    public FrontOfficeDashboardResponse getFrontOfficeDashboard() {
        LocalDate today = LocalDate.now();

        // Today's enquiries
        List<com.cms.model.Enquiry> todayEnquiries =
            enquiryRepository.findByEnquiryDateBetween(today, today);
        long todayEnquiryCount = todayEnquiries.size();

        // Total enquiry count
        long totalEnquiryCount = enquiryRepository.count();

        // Total admissions (enrollment records)
        long totalAdmissions = admissionRepository.count();

        // Fee collected today from enquiry payments
        BigDecimal feeCollectedToday = enquiryPaymentRepository.findByPaymentDate(today).stream()
            .map(EnquiryPayment::getAmountPaid)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Conversions this week (ISO week: Monday–Sunday)
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd   = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        long conversionsThisWeek = enquiryRepository
            .findByEnquiryDateBetweenAndStatus(weekStart, weekEnd, EnquiryStatus.ADMITTED)
            .size();

        // Conversion rate: conversions this week relative to all-time enquiries (per spec).
        // This shows what fraction of the total pipeline converted this week.
        double conversionRate = (double) conversionsThisWeek / Math.max(totalEnquiryCount, 1) * 100;

        // Enquiry funnel — compute once and reuse below
        Map<String, Long> enquiryFunnel = buildEnquiryFunnelMap();

        // Today's enquiries (up to 10), mapped to lightweight items
        List<FrontOfficeEnquiryItem> todaysEnquiries = todayEnquiries.stream()
            .limit(10)
            .map(e -> new FrontOfficeEnquiryItem(
                e.getId(),
                e.getName(),
                e.getProgram() != null ? e.getProgram().getName() : null,
                e.getReferralType() != null ? e.getReferralType().getName() : null,
                e.getStatus().name(),
                e.getEnquiryDate()
            ))
            .toList();

        // Pending action items (human-readable strings) — pass already-computed funnel
        List<String> pendingActionItems = buildPendingActionItems(totalAdmissions, feeCollectedToday, enquiryFunnel);

        return new FrontOfficeDashboardResponse(
            todayEnquiryCount,
            totalEnquiryCount,
            totalAdmissions,
            feeCollectedToday,
            conversionsThisWeek,
            conversionRate,
            enquiryFunnel,
            todaysEnquiries,
            pendingActionItems
        );
    }

    private List<String> buildPendingActionItems(long pendingAdmissionsCount, BigDecimal feeCollectedToday, Map<String, Long> funnel) {
        List<String> items = new ArrayList<>();
        if (pendingAdmissionsCount > 0) {
            items.add(pendingAdmissionsCount + " " + pluralize(pendingAdmissionsCount, "admission", "admissions") + " pending review");
        }
        long docsSubmitted = funnel.getOrDefault(EnquiryStatus.DOCUMENTS_SUBMITTED.name(), 0L);
        if (docsSubmitted > 0) {
            items.add(docsSubmitted + " " + pluralize(docsSubmitted, "application", "applications") + " with documents submitted — awaiting conversion");
        }
        long feesFinalized = funnel.getOrDefault(EnquiryStatus.FEES_FINALIZED.name(), 0L);
        if (feesFinalized > 0) {
            items.add(feesFinalized + " " + pluralize(feesFinalized, "enquiry", "enquiries") + " with fees finalized — awaiting payment");
        }
        return items;
    }

    private static String pluralize(long count, String singular, String plural) {
        return count == 1 ? singular : plural;
    }
}
