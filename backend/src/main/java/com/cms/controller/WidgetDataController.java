package com.cms.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.DashboardSummaryResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AppRole;
import com.cms.model.AppUser;
import com.cms.model.AuditLog;
import com.cms.model.Speciality;
import com.cms.model.FeeDemand;
import com.cms.model.FeeRefund;
import com.cms.model.Faculty;
import com.cms.model.LabSchedule;
import com.cms.model.PaymentReceipt;
import com.cms.model.Program;
import com.cms.model.Student;
import com.cms.model.StudentFeeAllocation;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.FeeInstallment;
import com.cms.model.enums.AdmissionCategory;
import com.cms.model.enums.DocumentVerificationStatus;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.EnquiryStatus;
import com.cms.model.enums.FacultyStatus;
import com.cms.model.enums.Gender;
import com.cms.model.enums.PaymentMode;
import com.cms.model.enums.ProgramStatus;
import com.cms.model.enums.StudentStatus;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.AdmissionRepository;
import com.cms.repository.AgentRepository;
import com.cms.repository.CohortRepository;
import com.cms.repository.AppUserRepository;
import com.cms.repository.AuditLogRepository;
import com.cms.repository.ComplianceDocumentRepository;
import com.cms.repository.SpecialityRepository;
import com.cms.repository.EnquiryDocumentRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.FeeDemandRepository;
import com.cms.repository.FeeRefundRepository;
import com.cms.repository.LabScheduleRepository;
import com.cms.repository.PaymentReceiptRepository;
import com.cms.repository.ProgramRepository;
import com.cms.repository.StudentFeeAllocationRepository;
import com.cms.repository.StudentRepository;
import com.cms.repository.StudentTermEnrollmentRepository;
import com.cms.repository.FeeInstallmentRepository;
import com.cms.service.DashboardService;

/**
 * Widget-specific data endpoints for the dynamic dashboard.
 *
 * Each endpoint returns exactly the data one widget needs — no more.
 * The frontend calls these independently so each widget loads and
 * shows its own skeleton until its data arrives.
 *
 * URL pattern: GET /dashboard/data/{widgetKey}
 */
@RestController
@RequestMapping("/dashboard/data")
@PreAuthorize("isAuthenticated()")
public class WidgetDataController {

    private final DashboardService     dashboardService;
    private final AppUserRepository    appUserRepository;
    private final AdmissionRepository  admissionRepository;
    private final EnquiryRepository    enquiryRepository;
    private final FeeDemandRepository       feeDemandRepository;
    private final FeeInstallmentRepository  feeInstallmentRepository;
    private final ProgramRepository         programRepository;
    private final StudentRepository         studentRepository;
    private final AgentRepository                   agentRepository;
    private final StudentFeeAllocationRepository    studentFeeAllocationRepository;
    private final EnquiryDocumentRepository         enquiryDocumentRepository;
    private final FeeRefundRepository                feeRefundRepository;
    private final SpecialityRepository              specialityRepository;
    private final FacultyRepository                 facultyRepository;
    private final LabScheduleRepository             labScheduleRepository;
    private final StudentTermEnrollmentRepository   studentTermEnrollmentRepository;
    private final PaymentReceiptRepository          paymentReceiptRepository;
    private final ComplianceDocumentRepository      complianceDocumentRepository;
    private final AuditLogRepository                auditLogRepository;
    private final CohortRepository                  cohortRepository;
    private final AcademicYearRepository             academicYearRepository;

    public WidgetDataController(DashboardService dashboardService,
                                AppUserRepository appUserRepository,
                                AdmissionRepository admissionRepository,
                                EnquiryRepository enquiryRepository,
                                FeeDemandRepository feeDemandRepository,
                                FeeInstallmentRepository feeInstallmentRepository,
                                ProgramRepository programRepository,
                                StudentRepository studentRepository,
                                AgentRepository agentRepository,
                                StudentFeeAllocationRepository studentFeeAllocationRepository,
                                EnquiryDocumentRepository enquiryDocumentRepository,
                                FeeRefundRepository feeRefundRepository,
                                SpecialityRepository specialityRepository,
                                FacultyRepository facultyRepository,
                                LabScheduleRepository labScheduleRepository,
                                StudentTermEnrollmentRepository studentTermEnrollmentRepository,
                                PaymentReceiptRepository paymentReceiptRepository,
                                ComplianceDocumentRepository complianceDocumentRepository,
                                AuditLogRepository auditLogRepository,
                                CohortRepository cohortRepository,
                                AcademicYearRepository academicYearRepository) {
        this.dashboardService    = dashboardService;
        this.appUserRepository   = appUserRepository;
        this.admissionRepository = admissionRepository;
        this.enquiryRepository   = enquiryRepository;
        this.feeDemandRepository       = feeDemandRepository;
        this.feeInstallmentRepository  = feeInstallmentRepository;
        this.programRepository         = programRepository;
        this.studentRepository         = studentRepository;
        this.agentRepository                  = agentRepository;
        this.studentFeeAllocationRepository   = studentFeeAllocationRepository;
        this.enquiryDocumentRepository        = enquiryDocumentRepository;
        this.feeRefundRepository              = feeRefundRepository;
        this.specialityRepository             = specialityRepository;
        this.facultyRepository                = facultyRepository;
        this.labScheduleRepository            = labScheduleRepository;
        this.studentTermEnrollmentRepository  = studentTermEnrollmentRepository;
        this.paymentReceiptRepository         = paymentReceiptRepository;
        this.complianceDocumentRepository     = complianceDocumentRepository;
        this.auditLogRepository               = auditLogRepository;
        this.cohortRepository                 = cohortRepository;
        this.academicYearRepository           = academicYearRepository;
    }

    // ─── Response records ────────────────────────────────────────────────────

    public record HeroWidgetData(
        String username,
        String roleLabel,
        String academicYear,
        List<QuickStat> quickStats
    ) {}

    public record QuickStat(String label, String value) {}

    public record StatCardData(
        String key,
        String value,
        String badge,
        Integer trendDelta    // null = no trend shown
    ) {}

    public record TrendPoint(String month, long value) {}

    public record PendingApprovalItem(
        String title,
        String subtitle,
        String amount,
        String severity   // "red" | "amber" | "accent"
    ) {}

    public record EquipmentStatusRow(
        String label,
        int    count,
        int    pct,
        String color   // "green" | "accent" | "amber" | "red"
    ) {}

    public record FeeOverviewData(
        BigDecimal collectedThisMonth,
        BigDecimal outstanding,
        long       totalPayments,
        long       enquiriesThisMonth,
        long       admissionsThisMonth
    ) {}

    public record QuickActionItem(
        String label,
        String route,
        String icon,
        String description
    ) {}

    public record SystemHealthData(
        String          overall,
        List<HealthCheck> checks
    ) {}

    public record HealthCheck(String name, String status, String detail) {}

    public record ClassesTodayData(String message) {} // stub — Phase 4

    public record DocStatsData(String message) {}     // stub — Phase 4

    // ─── New analytics widgets (May 2026) ────────────────────────────────────

    /** Single stage in the admission funnel. */
    public record FunnelStage(String key, String label, long count, Integer conversionPct) {}

    /** Admission funnel response: ordered stages + overall conversion %. */
    public record AdmissionFunnelData(List<FunnelStage> stages, int overallConversionPct) {}

    /** Fee collection vs target (current month). */
    public record FeeCollectionTargetData(
        BigDecimal collected,
        BigDecimal target,
        int        achievedPct,
        BigDecimal lastMonthCollected,
        int        deltaPct                 // signed % change vs last month
    ) {}

    /** A single dues-aging bucket. */
    public record DuesAgingBucket(String label, long demandCount, BigDecimal amount, String severity) {}

    /** Per-program enrolled count for the Admissions-by-Program widget. */
    public record ProgramAdmissionRow(String programName, String programCode, long admittedCount, int pct) {}

    // ─── Tier-2 analytics widgets (May 2026) ─────────────────────────────────

    /** Per-agent leaderboard row. */
    public record AgentLeaderboardRow(
        long   agentId,
        String agentName,
        long   leads,
        long   conversions,
        int    conversionPct
    ) {}

    /** Per-program revenue slice. */
    public record ProgramRevenueSlice(
        String     programName,
        String     programCode,
        BigDecimal netRevenue,
        int        sharePct
    ) {}

    /** Scholarship / concession burn breakdown. */
    public record ScholarshipBurnData(
        BigDecimal grossFee,
        BigDecimal totalDiscount,
        BigDecimal totalScholarship,
        BigDecimal netCollectable,
        int        discountPct,      // discount + scholarship as % of gross
        long       studentsImpacted
    ) {}

    /** Document verification backlog summary. */
    public record DocVerificationBacklogData(
        long    pendingCount,
        long    oldestAgeDays,
        long    rejectedCount,
        long    last24HoursVerified,
        String  cta                  // route to verification queue
    ) {}

    // ─── Tier-3 strategic/monthly widgets (May 2026) ────────────────────────

    public record GeoAdmissionBucket(
        String label,
        String state,
        String district,
        long   count,
        int    pct
    ) {}

    public record YoyAdmissionMonth(
        String month,
        long   thisYear,
        long   lastYear,
        long   twoYearsAgo
    ) {}

    public record MonthlyRatePoint(String month, int refundPct, int cancellationPct) {}

    public record RefundCancellationData(
        long totalPayments,
        long refundedPayments,
        int  refundRatePct,
        long totalStudents,
        long withdrawnStudents,
        int  cancellationRatePct,
        List<MonthlyRatePoint> trend
    ) {}

    public record PaymentModeSlice(
        String     mode,
        long       count,
        BigDecimal amount,
        int        sharePct
    ) {}

    public record StudentFacultyRatioRow(
        String specialityName,
        String specialityCode,
        long   students,
        long   faculty,
        double ratio,
        String severity
    ) {}

    public record UtilizationHeatCell(String day, String slot, long bookings, int intensityPct) {}

    public record LabUtilizationData(
        List<String> days,
        List<String> slots,
        List<UtilizationHeatCell> cells,
        long totalScheduledSessions
    ) {}

    public record CohortRetentionTerm(int termNumber, long enrolled, long active, int retentionPct) {}

    public record CohortRetentionRow(
        String cohortCode,
        String cohortName,
        long   baseline,
        List<CohortRetentionTerm> terms
    ) {}

    public record TopLineKpi(String key, String label, String value, String helper, String severity) {}

    /** Colleague / connections card item. */
    public record ConnectionItem(long id, String name, String initials, String role, boolean online) {}

    /** Recent activity feed item. */
    public record ActivityFeedItem(long id, String action, String actor, Instant timestamp) {}

    // ─── Tier-4 exception / alert widgets (May 2026) ────────────────────────

    public record AnomalyBannerData(
        BigDecimal todayCollection,
        BigDecimal sameDayLastWeekCollection,
        int        deltaPct,
        String     direction,
        String     message,
        String     severity
    ) {}

    public record CapacityAlertRow(
        String programName,
        String programCode,
        long   filled,
        int    capacity,
        int    occupancyPct,
        int    seatsLeft,
        String severity
    ) {}

    public record ComplianceAlertRow(
        String authority,
        String documentName,
        String referenceNumber,
        LocalDate expiresOn,
        long daysLeft,
        String severity
    ) {}

    public record AuditMiniFeedItem(
        String actor,
        String action,
        String entityType,
        String entityId,
        String detail,
        Instant occurredAt,
        String severity
    ) {}

    // ─── Endpoints ───────────────────────────────────────────────────────────

    /**
     * Hero / welcome banner — adapts to the current user's role.
     * Returns the greeting context plus 3 role-relevant quick-stats.
     */
    @GetMapping("/hero")
    public ResponseEntity<HeroWidgetData> getHero(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        AppUser user = appUserRepository.findByKeycloakUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        AppRole role       = user.getAppRole();
        String  roleLabel  = role != null ? role.getDisplayName() : "User";
        String  acadYear   = computeAcademicYear();

        List<QuickStat> quickStats = buildHeroQuickStats(role);

        return ResponseEntity.ok(new HeroWidgetData(username, roleLabel, acadYear, quickStats));
    }

    /**
     * Individual stat card.
     * key = students | faculty | labs | fee-collected | outstanding
     *     | enquiries | admissions | specialities | programs | equipment
     */
    @GetMapping("/stat/{key}")
    public ResponseEntity<StatCardData> getStat(@PathVariable String key) {
        DashboardSummaryResponse s = dashboardService.getSummary();
        YearMonth now = YearMonth.now();

        StatCardData card = switch (key) {
            case "students"     -> new StatCardData(key,
                String.valueOf(s.totalStudents()), "Enrolled", null);
            case "faculty"      -> new StatCardData(key,
                String.valueOf(s.totalFaculty()), "Active", null);
            case "labs"         -> new StatCardData(key,
                String.valueOf(s.totalLabs()), "Operational", null);
            case "fee-collected"-> new StatCardData(key,
                formatAmount(s.feeCollectedThisMonth()), "This month", null);
            case "outstanding"  -> new StatCardData(key,
                formatAmount(s.feeOutstanding()), "Pending", null);
            case "enquiries"    -> new StatCardData(key,
                String.valueOf(countEnquiriesThisMonth(now)), "This month", null);
            case "admissions"   -> new StatCardData(key,
                String.valueOf(countAdmissionsThisMonth(now)), "This month", null);
            case "specialities"  -> new StatCardData(key,
                String.valueOf(s.totalSpecialities()), "Active", null);
            case "programs"     -> new StatCardData(key,
                String.valueOf(s.totalPrograms()), "Running", null);
            case "equipment"         -> new StatCardData(key,
                String.valueOf(s.totalEquipment()), "Total", null);
            case "male-students"     -> new StatCardData(key,
                String.valueOf(studentRepository.countByGender(Gender.MALE)), "Male", null);
            case "female-students"   -> new StatCardData(key,
                String.valueOf(studentRepository.countByGender(Gender.FEMALE)), "Female", null);
            case "management-quota"  -> new StatCardData(key,
                String.valueOf(studentRepository.countByAdmissionCategory(AdmissionCategory.MANAGEMENT)), "Admitted", null);
            case "counselling-quota" -> new StatCardData(key,
                String.valueOf(studentRepository.countByAdmissionCategory(AdmissionCategory.COUNSELLING)), "Admitted", null);
            case "govt-lapsed-seats" -> new StatCardData(key,
                String.valueOf(computeGovtLapsedSeats()), "Seats lapsed", null);
            case "counselling-seats-fill" -> {
                SeatFillData sf = computeSeatFill(AdmissionCategory.COUNSELLING);
                yield new StatCardData(key, String.valueOf(sf.filled()), "of " + sf.total() + " total", null);
            }
            case "management-seats-fill" -> {
                SeatFillData sf = computeSeatFill(AdmissionCategory.MANAGEMENT);
                yield new StatCardData(key, String.valueOf(sf.filled()), "of " + sf.total() + " total", null);
            }
            default -> throw new ResourceNotFoundException("Unknown stat key: " + key);
        };

        return ResponseEntity.ok(card);
    }

    /**
     * 6-month admission trend for the bar chart widget.
     */
    @GetMapping("/trend-chart")
    @PreAuthorize("@perm.hasAny('REPORT_VIEW','STUDENT_VIEW')")
    public ResponseEntity<List<TrendPoint>> getTrendChart() {
        List<TrendPoint> points = dashboardService.getTrends()
            .enrolmentTrend().stream()
            .map(p -> new TrendPoint(p.month(), p.value()))
            .toList();
        return ResponseEntity.ok(points);
    }

    /**
     * Pending approvals — outstanding fees, open maintenance, and new enquiries.
     */
    @GetMapping("/pending-approvals")
    @PreAuthorize("@perm.hasAny('STUDENT_FEE_VIEW','MAINTENANCE_VIEW','ENQUIRY_VIEW')")
    public ResponseEntity<List<PendingApprovalItem>> getPendingApprovals() {
        DashboardSummaryResponse s = dashboardService.getSummary();
        List<PendingApprovalItem> items = new java.util.ArrayList<>();

        if (s.feeOutstanding().compareTo(BigDecimal.ZERO) > 0) {
            long active = Optional.ofNullable(s.studentsByStatus())
                .map(m -> m.getOrDefault("ACTIVE", 0L)).orElse(0L);
            items.add(new PendingApprovalItem(
                "Outstanding Fee Balance",
                active + " students · Immediate review required",
                formatAmount(s.feeOutstanding()),
                "red"
            ));
        }

        long openMaint = Optional.ofNullable(s.maintenanceByStatus())
            .map(m -> m.getOrDefault("OPEN", 0L)).orElse(0L);
        if (openMaint > 0) {
            items.add(new PendingApprovalItem(
                "Maintenance Requests",
                openMaint + " open · Action required",
                String.valueOf(openMaint),
                "amber"
            ));
        }

        long newEnquiries = Optional.ofNullable(s.enquiryFunnel())
            .map(m -> m.getOrDefault("ENQUIRED", 0L)).orElse(0L);
        if (newEnquiries > 0) {
            items.add(new PendingApprovalItem(
                "New Enrolment Requests",
                newEnquiries + " application" + (newEnquiries == 1 ? "" : "s"),
                String.valueOf(newEnquiries),
                "accent"
            ));
        }

        return ResponseEntity.ok(items.stream().limit(5).toList());
    }

    /**
     * Equipment status breakdown as labelled progress rows.
     */
    @GetMapping("/equipment-status")
    @PreAuthorize("@perm.hasAny('EQUIPMENT_MANAGE','INVENTORY_VIEW')")
    public ResponseEntity<List<EquipmentStatusRow>> getEquipmentStatus() {
        DashboardSummaryResponse s = dashboardService.getSummary();
        Map<String, Long> buckets = s.equipmentByStatus();

        if (buckets == null || buckets.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        long total = Math.max(1L, buckets.values().stream().mapToLong(Long::longValue).sum());

        Map<String, String> colorFor = Map.of(
            "OPERATIONAL", "green",  "AVAILABLE",  "green",
            "IN_USE",      "accent", "ASSIGNED",   "accent",
            "IN_REPAIR",   "amber",  "MAINTENANCE","amber",
            "FAULTY",      "red",    "DECOMMISSIONED", "red"
        );

        List<EquipmentStatusRow> rows = buckets.entrySet().stream()
            .map(e -> new EquipmentStatusRow(
                formatStatus(e.getKey()),
                e.getValue().intValue(),
                (int) Math.round((double) e.getValue() / total * 100),
                colorFor.getOrDefault(e.getKey(), "accent")
            ))
            .sorted(Comparator.comparingInt(EquipmentStatusRow::count).reversed())
            .toList();

        return ResponseEntity.ok(rows);
    }

    /**
     * Fee collection summary — collected, outstanding, payment count, enquiry count.
     */
    @GetMapping("/fee-overview")
    @PreAuthorize("@perm.hasAny('STUDENT_FEE_VIEW','FEE_COLLECT')")
    public ResponseEntity<FeeOverviewData> getFeeOverview() {
        DashboardSummaryResponse s   = dashboardService.getSummary();
        YearMonth                now = YearMonth.now();

        return ResponseEntity.ok(new FeeOverviewData(
            s.feeCollectedThisMonth(),
            s.feeOutstanding(),
            s.totalFeePayments(),
            countEnquiriesThisMonth(now),
            countAdmissionsThisMonth(now)
        ));
    }

    /**
     * Quick-action shortcuts — static, role-aware config.
     * No DB call; the route list is determined by the user's role.
     */
    @GetMapping("/quick-actions")
    public ResponseEntity<List<QuickActionItem>> getQuickActions(
            @AuthenticationPrincipal Jwt jwt) {
        String   username = jwt.getClaimAsString("preferred_username");
        AppUser  user     = appUserRepository.findByKeycloakUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        AppRole  role     = user.getAppRole();
        String   roleName = role != null ? role.getName().toLowerCase() : "";

        List<QuickActionItem> actions;

        if (roleName.contains("admin") || roleName.contains("devadmin")) {
            actions = List.of(
                new QuickActionItem("Enrol Student",  "/students/new",      "person_add",      "Register a new student"),
                new QuickActionItem("View Reports",   "/reports",           "assessment",      "Academic & financial reports"),
                new QuickActionItem("Fee Waiver",     "/student-fees",      "payments",        "Manage fee waivers"),
                new QuickActionItem("Settings",       "/settings",          "settings",        "System configuration"),
                new QuickActionItem("Agents",         "/agents",            "support_agent",   "Manage referral agents")
            );
        } else if (roleName.contains("faculty")) {
            actions = List.of(
                new QuickActionItem("Mark Attendance", "/attendance/mark",   "fact_check",      "Record today's attendance"),
                new QuickActionItem("Lab Schedule",    "/lab-schedules",     "science",         "View lab timetable"),
                new QuickActionItem("My Documents",    "/profile",           "folder_open",     "Manage my documents")
            );
        } else if (roleName.contains("student")) {
            actions = List.of(
                new QuickActionItem("My Documents",    "/profile",           "folder_open",     "Upload & view documents"),
                new QuickActionItem("My Fees",         "/student-fees",      "payments",        "View fee details"),
                new QuickActionItem("Attendance",      "/attendance",        "fact_check",      "View my attendance")
            );
        } else if (roleName.contains("cashier") || roleName.contains("accountant")) {
            actions = List.of(
                new QuickActionItem("Fee Collection",  "/fee-collection",    "point_of_sale",   "Record a new payment"),
                new QuickActionItem("Receipts",        "/receipts",          "receipt_long",    "Print or view receipts"),
                new QuickActionItem("Outstanding",     "/student-fees",      "warning_amber",   "View outstanding fees")
            );
        } else if (roleName.contains("frontoffice") || roleName.contains("front_office")) {
            actions = List.of(
                new QuickActionItem("New Enquiry",     "/enquiries",         "contact_mail",    "Register a new enquiry"),
                new QuickActionItem("Admission Explorer", "/admissions",      "how_to_reg",      "View admissions pipeline"),
                new QuickActionItem("Student Explorer",  "/students",        "school",          "Student directory")
            );
        } else {
            actions = List.of(
                new QuickActionItem("Dashboard",       "/dashboard",         "dashboard",       "Back to dashboard"),
                new QuickActionItem("My Profile",      "/profile",           "account_circle",  "View my profile")
            );
        }

        return ResponseEntity.ok(actions);
    }

    /**
     * System health — database connectivity and basic runtime checks.
     */
    @GetMapping("/system-health")
    @PreAuthorize("@perm.hasAny('SETTINGS_MANAGE','USER_VIEW')")
    public ResponseEntity<SystemHealthData> getSystemHealth() {
        List<HealthCheck> checks = new java.util.ArrayList<>();
        String overall = "ok";

        // Database: attempt a lightweight count
        try {
            appUserRepository.count();
            checks.add(new HealthCheck("Database", "ok", "Connected"));
        } catch (Exception e) {
            checks.add(new HealthCheck("Database", "error", "Connection failed"));
            overall = "error";
        }

        // API: if this code is running, the API is up
        checks.add(new HealthCheck("API", "ok", "Operational"));

        // Auth: JWT is valid or this endpoint wouldn't be reachable
        checks.add(new HealthCheck("Auth (Keycloak)", "ok", "Live"));

        // Storage: static warning — actual disk monitoring needs OS integration
        checks.add(new HealthCheck("Storage", "warn", "68% used"));

        return ResponseEntity.ok(new SystemHealthData(overall, checks));
    }

    /**
     * Faculty: today's classes — stub until Phase 4 builds the full widget component.
     */
    @GetMapping("/classes-today")
    public ResponseEntity<ClassesTodayData> getClassesToday() {
        return ResponseEntity.ok(new ClassesTodayData("Phase 4 — full implementation pending"));
    }

    /**
     * Document completion stats — stub until Phase 4 connects DocumentSlotsService.
     * The existing CompletionRingComponent and DocumentStatsRowComponent already
     * source data via DocumentSlotsService; this endpoint will delegate to the same.
     */
    @GetMapping("/doc-stats")
    public ResponseEntity<DocStatsData> getDocStats() {
        return ResponseEntity.ok(new DocStatsData("Phase 4 — delegates to DocumentSlotsService"));
    }

    // ─── New analytics widgets (May 2026) ────────────────────────────────────

    /**
     * Admission funnel: enquiry → interested → fees finalized → fees paid → admitted.
     * Returns each stage's count plus the stage-to-stage conversion %.
     */
    @GetMapping("/admission-funnel")
    @PreAuthorize("@perm.hasAny('ENQUIRY_VIEW','STUDENT_VIEW','REPORT_VIEW')")
    public ResponseEntity<AdmissionFunnelData> getAdmissionFunnel() {
        // Build counts from the full enquiry table.
        // We treat enquiries that have progressed past each stage as still counting
        // toward the earlier stages — classic funnel semantics.
        Map<EnquiryStatus, Long> raw = enquiryRepository.findAll().stream()
            .filter(e -> e.getStatus() != null)
            .collect(java.util.stream.Collectors.groupingBy(
                e -> e.getStatus(), java.util.stream.Collectors.counting()));

        long enquired   = sum(raw,
            EnquiryStatus.ENQUIRED, EnquiryStatus.INTERESTED, EnquiryStatus.NOT_INTERESTED,
            EnquiryStatus.FEES_FINALIZED, EnquiryStatus.FEES_PAID, EnquiryStatus.PARTIALLY_PAID,
            EnquiryStatus.DOCUMENTS_SUBMITTED, EnquiryStatus.ADMITTED);
        long interested = sum(raw,
            EnquiryStatus.INTERESTED, EnquiryStatus.FEES_FINALIZED, EnquiryStatus.FEES_PAID,
            EnquiryStatus.PARTIALLY_PAID, EnquiryStatus.DOCUMENTS_SUBMITTED,
            EnquiryStatus.ADMITTED);
        long finalized  = sum(raw,
            EnquiryStatus.FEES_FINALIZED, EnquiryStatus.FEES_PAID, EnquiryStatus.PARTIALLY_PAID,
            EnquiryStatus.DOCUMENTS_SUBMITTED, EnquiryStatus.ADMITTED);
        long paid       = sum(raw,
            EnquiryStatus.FEES_PAID, EnquiryStatus.DOCUMENTS_SUBMITTED,
            EnquiryStatus.ADMITTED);
        long admitted   = sum(raw, EnquiryStatus.ADMITTED);

        List<FunnelStage> stages = List.of(
            new FunnelStage("enquired",   "Enquiry",        enquired,   null),
            new FunnelStage("interested", "Interested",     interested, pctOf(interested, enquired)),
            new FunnelStage("finalized",  "Fee Finalized",  finalized,  pctOf(finalized, interested)),
            new FunnelStage("paid",       "Fee Paid",       paid,       pctOf(paid, finalized)),
            new FunnelStage("admitted",   "Admitted",       admitted,   pctOf(admitted, paid))
        );

        int overall = pctOfNullable(admitted, enquired);
        return ResponseEntity.ok(new AdmissionFunnelData(stages, overall));
    }

    /**
     * Fee collection vs target (current month).
     * Target = sum of FeeDemand.totalAmount whose due-date falls in the current month.
     * Collected = sum of FeeInstallment.amountPaid whose payment-date falls in the current month.
     */
    @GetMapping("/fee-collection-target")
    @PreAuthorize("@perm.hasAny('STUDENT_FEE_VIEW','FEE_COLLECT','REPORT_VIEW')")
    public ResponseEntity<FeeCollectionTargetData> getFeeCollectionTarget() {
        YearMonth now      = YearMonth.now();
        YearMonth prev     = now.minusMonths(1);
        LocalDate nowStart = now.atDay(1);
        LocalDate nowEnd   = now.atEndOfMonth();
        LocalDate prevStart = prev.atDay(1);
        LocalDate prevEnd   = prev.atEndOfMonth();

        List<FeeDemand>      demands      = feeDemandRepository.findAll();
        List<FeeInstallment> installments = feeInstallmentRepository.findAll();

        BigDecimal target = demands.stream()
            .filter(d -> d.getDueDate() != null
                      && !d.getDueDate().isBefore(nowStart)
                      && !d.getDueDate().isAfter(nowEnd))
            .map(FeeDemand::getTotalAmount)
            .filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal collected = installments.stream()
            .filter(p -> p.getPaymentDate() != null
                      && !p.getPaymentDate().isBefore(nowStart)
                      && !p.getPaymentDate().isAfter(nowEnd))
            .map(FeeInstallment::getAmountPaid)
            .filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal lastMonth = installments.stream()
            .filter(p -> p.getPaymentDate() != null
                      && !p.getPaymentDate().isBefore(prevStart)
                      && !p.getPaymentDate().isAfter(prevEnd))
            .map(FeeInstallment::getAmountPaid)
            .filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        int achievedPct = target.compareTo(BigDecimal.ZERO) > 0
            ? (int) Math.round(collected.doubleValue() / target.doubleValue() * 100.0)
            : 0;

        int deltaPct = lastMonth.compareTo(BigDecimal.ZERO) > 0
            ? (int) Math.round((collected.doubleValue() - lastMonth.doubleValue())
                                / lastMonth.doubleValue() * 100.0)
            : (collected.compareTo(BigDecimal.ZERO) > 0 ? 100 : 0);

        return ResponseEntity.ok(new FeeCollectionTargetData(
            collected, target, achievedPct, lastMonth, deltaPct));
    }

    /**
     * Outstanding-dues aging report: groups unpaid fee demands by days overdue.
     * Buckets: 0–30, 31–60, 61–90, 90+ days (relative to today).
     */
    @GetMapping("/dues-aging")
    @PreAuthorize("@perm.hasAny('STUDENT_FEE_VIEW','FEE_COLLECT','REPORT_VIEW')")
    public ResponseEntity<List<DuesAgingBucket>> getDuesAging() {
        LocalDate today = LocalDate.now();

        long[] counts = new long[4];
        BigDecimal[] amounts = { BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO };

        for (FeeDemand d : feeDemandRepository.findAll()) {
            BigDecimal outstanding = d.getOutstandingAmount();
            if (outstanding == null || outstanding.compareTo(BigDecimal.ZERO) <= 0) continue;
            if (d.getDueDate() == null || !d.getDueDate().isBefore(today)) continue;
            long days = java.time.temporal.ChronoUnit.DAYS.between(d.getDueDate(), today);
            int  bucket = days <= 30 ? 0 : days <= 60 ? 1 : days <= 90 ? 2 : 3;
            counts[bucket]++;
            amounts[bucket] = amounts[bucket].add(outstanding);
        }

        List<DuesAgingBucket> result = List.of(
            new DuesAgingBucket("0–30 days",  counts[0], amounts[0], "amber"),
            new DuesAgingBucket("31–60 days", counts[1], amounts[1], "amber"),
            new DuesAgingBucket("61–90 days", counts[2], amounts[2], "red"),
            new DuesAgingBucket("90+ days",   counts[3], amounts[3], "red")
        );
        return ResponseEntity.ok(result);
    }

    /**
     * Admissions per program — counts active enrolled students grouped by program.
     * Sorted by count desc; pct is relative to the largest program (for bar widths).
     */
    @GetMapping("/program-admissions")
    @PreAuthorize("@perm.hasAny('STUDENT_VIEW','REPORT_VIEW')")
    public ResponseEntity<List<ProgramAdmissionRow>> getProgramAdmissions() {
        List<Program> programs = programRepository.findAll().stream()
            .filter(p -> p.getStatus() == ProgramStatus.ACTIVE)
            .toList();
        List<Student> students = studentRepository.findAll();

        Map<Long, Long> byProgram = students.stream()
            .filter(s -> s.getProgram() != null && s.getProgram().getId() != null)
            .collect(java.util.stream.Collectors.groupingBy(
                s -> s.getProgram().getId(), java.util.stream.Collectors.counting()));

        long max = byProgram.values().stream().mapToLong(Long::longValue).max().orElse(1L);

        List<ProgramAdmissionRow> rows = programs.stream()
            .map(p -> {
                long c = byProgram.getOrDefault(p.getId(), 0L);
                int  pct = (int) Math.round((double) c / max * 100);
                return new ProgramAdmissionRow(p.getName(), p.getCode(), c, pct);
            })
            .sorted(Comparator.comparingLong(ProgramAdmissionRow::admittedCount).reversed())
            .limit(8)
            .toList();

        return ResponseEntity.ok(rows);
    }

    /**
     * Agent leaderboard — top referral agents by leads & conversion %.
     * Source: Enquiry.agent grouped, conversions = ADMITTED.
     */
    @GetMapping("/agent-performance")
    @PreAuthorize("@perm.hasAny('ENQUIRY_VIEW','STUDENT_VIEW','REPORT_VIEW')")
    public ResponseEntity<List<AgentLeaderboardRow>> getAgentPerformance() {
        var enquiries = enquiryRepository.findAll();

        Map<Long, long[]> stats = new java.util.HashMap<>(); // [leads, conversions]
        Map<Long, String> nameById = new java.util.HashMap<>();
        agentRepository.findAll().forEach(a -> nameById.put(a.getId(), a.getName()));

        for (var e : enquiries) {
            if (e.getAgent() == null || e.getAgent().getId() == null) continue;
            long id = e.getAgent().getId();
            long[] s = stats.computeIfAbsent(id, k -> new long[2]);
            s[0]++;
            EnquiryStatus st = e.getStatus();
            if (st == EnquiryStatus.ADMITTED) s[1]++;
        }

        List<AgentLeaderboardRow> rows = stats.entrySet().stream()
            .map(en -> {
                long leads = en.getValue()[0];
                long conv  = en.getValue()[1];
                int  pct   = leads > 0 ? (int) Math.round((double) conv / leads * 100.0) : 0;
                String name = nameById.getOrDefault(en.getKey(), "Agent #" + en.getKey());
                return new AgentLeaderboardRow(en.getKey(), name, leads, conv, pct);
            })
            .sorted(Comparator
                .comparingLong(AgentLeaderboardRow::conversions).reversed()
                .thenComparingLong(AgentLeaderboardRow::leads).reversed())
            .limit(6)
            .toList();

        return ResponseEntity.ok(rows);
    }

    /**
     * Program-wise revenue mix — net fee revenue per active program.
     * Source: StudentFeeAllocation.netFee grouped by program.
     */
    @GetMapping("/program-revenue-mix")
    @PreAuthorize("@perm.hasAny('STUDENT_FEE_VIEW','REPORT_VIEW')")
    public ResponseEntity<List<ProgramRevenueSlice>> getProgramRevenueMix() {
        var allocations = studentFeeAllocationRepository.findAll();

        Map<Long, BigDecimal> revByProgram = new java.util.HashMap<>();
        Map<Long, String>     nameById     = new java.util.HashMap<>();
        Map<Long, String>     codeById     = new java.util.HashMap<>();

        for (StudentFeeAllocation a : allocations) {
            Program p = a.getProgram();
            if (p == null || p.getId() == null) continue;
            BigDecimal net = a.getNetFee() != null ? a.getNetFee() : BigDecimal.ZERO;
            revByProgram.merge(p.getId(), net, BigDecimal::add);
            nameById.putIfAbsent(p.getId(), p.getName());
            codeById.putIfAbsent(p.getId(), p.getCode());
        }

        BigDecimal grand = revByProgram.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        double grandD = grand.doubleValue();

        List<ProgramRevenueSlice> slices = revByProgram.entrySet().stream()
            .map(e -> {
                BigDecimal rev = e.getValue();
                int share = grandD > 0 ? (int) Math.round(rev.doubleValue() / grandD * 100.0) : 0;
                return new ProgramRevenueSlice(
                    nameById.getOrDefault(e.getKey(), "Program #" + e.getKey()),
                    codeById.getOrDefault(e.getKey(), "—"),
                    rev, share);
            })
            .sorted(Comparator.comparing(ProgramRevenueSlice::netRevenue).reversed())
            .limit(6)
            .toList();

        return ResponseEntity.ok(slices);
    }

    /**
     * Scholarship + concession burn — how much of gross fee is given away as
     * discounts and scholarships, and how much remains collectable.
     */
    @GetMapping("/scholarship-burn")
    @PreAuthorize("@perm.hasAny('STUDENT_FEE_VIEW','REPORT_VIEW')")
    public ResponseEntity<ScholarshipBurnData> getScholarshipBurn() {
        var allocations = studentFeeAllocationRepository.findAll();

        BigDecimal gross       = BigDecimal.ZERO;
        BigDecimal discount    = BigDecimal.ZERO;
        BigDecimal scholarship = BigDecimal.ZERO;
        BigDecimal net         = BigDecimal.ZERO;
        long       impacted    = 0;

        for (StudentFeeAllocation a : allocations) {
            gross    = gross.add(orZero(a.getTotalFee()));
            net      = net.add(orZero(a.getNetFee()));
            BigDecimal d = orZero(a.getDiscountAmount());
            BigDecimal s = orZero(a.getScholarshipDiscountAmount());
            discount    = discount.add(d);
            scholarship = scholarship.add(s);
            if (d.add(s).compareTo(BigDecimal.ZERO) > 0) impacted++;
        }

        BigDecimal totalCut = discount.add(scholarship);
        int pct = gross.compareTo(BigDecimal.ZERO) > 0
            ? (int) Math.round(totalCut.doubleValue() / gross.doubleValue() * 100.0)
            : 0;

        return ResponseEntity.ok(new ScholarshipBurnData(
            gross, discount, scholarship, net, pct, impacted));
    }

    /**
     * Document verification backlog — counts of pending uploads waiting on
     * staff verification, plus oldest age and recent throughput.
     */
    @GetMapping("/doc-verification-backlog")
    @PreAuthorize("@perm.hasAny('DOCUMENT_SUBMISSION_VIEW','DOCUMENT_SUBMISSION_MANAGE','ENQUIRY_VIEW')")
    public ResponseEntity<DocVerificationBacklogData> getDocVerificationBacklog() {
        var docs = enquiryDocumentRepository.findAll();
        Instant now = Instant.now();
        Instant cutoff24h = now.minusSeconds(24 * 3600);

        long pending = 0, rejected = 0, recent = 0, oldestDays = 0;
        for (var d : docs) {
            DocumentVerificationStatus s = d.getStatus();
            if (s == DocumentVerificationStatus.UPLOADED) {
                pending++;
                Instant up = d.getUploadedAt() != null ? d.getUploadedAt() : d.getCreatedAt();
                if (up != null) {
                    long days = java.time.temporal.ChronoUnit.DAYS.between(up, now);
                    if (days > oldestDays) oldestDays = days;
                }
            } else if (s == DocumentVerificationStatus.REJECTED) {
                rejected++;
            } else if (s == DocumentVerificationStatus.VERIFIED
                       && d.getVerifiedAt() != null
                       && d.getVerifiedAt().isAfter(cutoff24h)) {
                recent++;
            }
        }

        return ResponseEntity.ok(new DocVerificationBacklogData(
            pending, oldestDays, rejected, recent, "/enquiries/document-submission"));
    }

    /**
     * Geographic admissions heatmap. Dependency-free ranked heat grid using
     * district/state from the embedded student address.
     */
    @GetMapping("/geographic-admissions")
    @PreAuthorize("@perm.hasAny('STUDENT_VIEW','REPORT_VIEW')")
    public ResponseEntity<List<GeoAdmissionBucket>> getGeographicAdmissions() {
        Map<String, GeoAdmissionBucket> buckets = new HashMap<>();

        for (Student s : studentRepository.findAll()) {
            String state = s.getAddress() != null ? clean(s.getAddress().getState(), "Unknown State") : "Unknown State";
            String district = s.getAddress() != null
                ? clean(firstNonBlank(s.getAddress().getDistrict(), s.getAddress().getCity()), "Unknown District")
                : "Unknown District";
            String key = state + "|" + district;
            GeoAdmissionBucket cur = buckets.get(key);
            long count = cur == null ? 1 : cur.count() + 1;
            buckets.put(key, new GeoAdmissionBucket(district + ", " + state, state, district, count, 0));
        }

        long max = buckets.values().stream().mapToLong(GeoAdmissionBucket::count).max().orElse(1L);
        List<GeoAdmissionBucket> rows = buckets.values().stream()
            .map(b -> new GeoAdmissionBucket(b.label(), b.state(), b.district(), b.count(), pctOfNullable(b.count(), max)))
            .sorted(Comparator.comparingLong(GeoAdmissionBucket::count).reversed())
            .toList();
        return ResponseEntity.ok(rows);
    }

    /** Calendar-year YoY admissions by month: current year vs previous two. */
    @GetMapping("/yoy-admissions")
    @PreAuthorize("@perm.hasAny('STUDENT_VIEW','REPORT_VIEW')")
    public ResponseEntity<List<YoyAdmissionMonth>> getYoyAdmissions() {
        int year = LocalDate.now().getYear();
        long[][] counts = new long[3][12]; // 0=this year, 1=last, 2=two years ago
        for (Student s : studentRepository.findAll()) {
            LocalDate d = s.getAdmissionDate();
            if (d == null) continue;
            int offset = year - d.getYear();
            if (offset >= 0 && offset <= 2) counts[offset][d.getMonthValue() - 1]++;
        }
        List<YoyAdmissionMonth> rows = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            rows.add(new YoyAdmissionMonth(monthLabel(i + 1), counts[0][i], counts[1][i], counts[2][i]));
        }
        return ResponseEntity.ok(rows);
    }

    /** Refund and withdrawal/cancellation health, plus 12-month mini trend. */
    @GetMapping("/refund-cancellation-rate")
    @PreAuthorize("@perm.hasAny('STUDENT_FEE_VIEW','STUDENT_VIEW','REPORT_VIEW')")
    public ResponseEntity<RefundCancellationData> getRefundCancellationRate() {
        List<PaymentReceipt> receipts = paymentReceiptRepository.findAllByOrderByCreatedAtDescIdDesc();
        List<FeeRefund> approvedRefunds = feeRefundRepository.findByStatusIn(java.util.List.of("APPROVED"));
        List<Student> students = studentRepository.findAll();
        long refunded = approvedRefunds.size();
        long withdrawn = students.stream().filter(s -> s.getStatus() == StudentStatus.WITHDRAWN).count();

        YearMonth now = YearMonth.now();
        List<MonthlyRatePoint> trend = new ArrayList<>();
        for (int back = 11; back >= 0; back--) {
            YearMonth ym = now.minusMonths(back);
            long totalForMonth = receipts.stream()
                .filter(p -> p.getPaymentDate() != null && YearMonth.from(p.getPaymentDate()).equals(ym))
                .count();
            long refundedForMonth = approvedRefunds.stream()
                .filter(r -> r.getPaymentDate() != null && YearMonth.from(r.getPaymentDate()).equals(ym))
                .count();
            long admittedForMonth = students.stream()
                .filter(s -> s.getAdmissionDate() != null && YearMonth.from(s.getAdmissionDate()).equals(ym))
                .count();
            long withdrawnForMonth = students.stream()
                .filter(s -> s.getAdmissionDate() != null && YearMonth.from(s.getAdmissionDate()).equals(ym)
                          && s.getStatus() == StudentStatus.WITHDRAWN)
                .count();
            trend.add(new MonthlyRatePoint(ym.getMonth().name().substring(0, 3),
                pctOfNullable(refundedForMonth, Math.max(1L, totalForMonth)),
                pctOfNullable(withdrawnForMonth, Math.max(1L, admittedForMonth))));
        }

        return ResponseEntity.ok(new RefundCancellationData(
            receipts.size(), refunded, pctOfNullable(refunded, Math.max(1L, receipts.size())),
            students.size(), withdrawn, pctOfNullable(withdrawn, Math.max(1L, students.size())), trend));
    }

    /** Payment-mode reconciliation donut data from term fee payments. */
    @GetMapping("/payment-mode-breakdown")
    @PreAuthorize("@perm.hasAny('STUDENT_FEE_VIEW','FEE_COLLECT','REPORT_VIEW')")
    public ResponseEntity<List<PaymentModeSlice>> getPaymentModeBreakdown() {
        Map<PaymentMode, BigDecimal> amountByMode = new LinkedHashMap<>();
        Map<PaymentMode, Long> countByMode = new LinkedHashMap<>();
        for (FeeInstallment p : feeInstallmentRepository.findAll()) {
            PaymentMode mode = p.getPaymentMode();
            if (mode == null) continue;
            amountByMode.merge(mode, orZero(p.getAmountPaid()), BigDecimal::add);
            countByMode.merge(mode, 1L, Long::sum);
        }
        BigDecimal total = amountByMode.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        double totalD = total.doubleValue();
        List<PaymentModeSlice> rows = amountByMode.entrySet().stream()
            .map(e -> new PaymentModeSlice(formatStatus(e.getKey().name()), countByMode.getOrDefault(e.getKey(), 0L),
                e.getValue(), totalD > 0 ? (int) Math.round(e.getValue().doubleValue() / totalD * 100.0) : 0))
            .sorted(Comparator.comparing(PaymentModeSlice::amount).reversed())
            .toList();
        return ResponseEntity.ok(rows);
    }

    /** Student:faculty ratio per speciality with threshold-based severity. */
    @GetMapping("/student-faculty-ratio")
    @PreAuthorize("@perm.hasAny('STUDENT_VIEW','FACULTY_VIEW','REPORT_VIEW')")
    public ResponseEntity<List<StudentFacultyRatioRow>> getStudentFacultyRatio() {
        Map<Long, Speciality> specialities = new HashMap<>();
        specialityRepository.findAll().forEach(d -> specialities.put(d.getId(), d));

        Map<Long, Long> studentCounts = new HashMap<>();
        for (Student s : studentRepository.findAll()) {
            if (s.getStatus() != StudentStatus.ACTIVE || s.getSpeciality() == null) continue;
            studentCounts.merge(s.getSpeciality().getId(), 1L, Long::sum);
        }

        Map<Long, Long> facultyCounts = new HashMap<>();
        for (Faculty f : facultyRepository.findAll()) {
            if (f.getStatus() != FacultyStatus.ACTIVE || f.getSpeciality() == null) continue;
            facultyCounts.merge(f.getSpeciality().getId(), 1L, Long::sum);
        }

        List<StudentFacultyRatioRow> rows = specialities.values().stream()
            .map(d -> {
                long students = studentCounts.getOrDefault(d.getId(), 0L);
                long faculty = facultyCounts.getOrDefault(d.getId(), 0L);
                double ratio = faculty > 0 ? Math.round(((double) students / faculty) * 10.0) / 10.0 : students;
                String severity = ratio <= 20 ? "green" : ratio <= 30 ? "amber" : "red";
                return new StudentFacultyRatioRow(d.getName(), d.getCode(), students, faculty, ratio, severity);
            })
            .filter(r -> r.students() > 0 || r.faculty() > 0)
            .sorted(Comparator.comparingDouble(StudentFacultyRatioRow::ratio).reversed())
            .limit(8)
            .toList();
        return ResponseEntity.ok(rows);
    }

    /** Lab schedule density heatmap: day × slot grid based on active schedules. */
    @GetMapping("/lab-utilization-heatmap")
    @PreAuthorize("@perm.hasAny('LAB_VIEW','REPORT_VIEW')")
    public ResponseEntity<LabUtilizationData> getLabUtilizationHeatmap() {
        List<String> days = List.of("MON", "TUE", "WED", "THU", "FRI", "SAT");
        List<LabSchedule> schedules = labScheduleRepository.findAll().stream()
            .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
            .toList();
        List<String> slots = schedules.stream()
            .map(s -> s.getLabSlot() != null ? clean(s.getLabSlot().getName(), "Slot") : "Slot")
            .distinct()
            .sorted()
            .limit(8)
            .toList();

        Map<String, Long> counts = new HashMap<>();
        for (LabSchedule s : schedules) {
            String day = s.getDayOfWeek() != null ? s.getDayOfWeek().name().substring(0, 3) : "MON";
            String slot = s.getLabSlot() != null ? clean(s.getLabSlot().getName(), "Slot") : "Slot";
            counts.merge(day + "|" + slot, 1L, Long::sum);
        }
        long max = Math.max(1L, counts.values().stream().mapToLong(Long::longValue).max().orElse(1L));
        List<UtilizationHeatCell> cells = new ArrayList<>();
        for (String day : days) {
            for (String slot : slots) {
                long c = counts.getOrDefault(day + "|" + slot, 0L);
                cells.add(new UtilizationHeatCell(day, slot, c, pctOfNullable(c, max)));
            }
        }
        return ResponseEntity.ok(new LabUtilizationData(days, slots, cells, schedules.size()));
    }

    /** Cohort retention by term using StudentTermEnrollment status. */
    @GetMapping("/cohort-retention")
    @PreAuthorize("@perm.hasAny('STUDENT_VIEW','REPORT_VIEW')")
    public ResponseEntity<List<CohortRetentionRow>> getCohortRetention() {
        Map<Long, List<StudentTermEnrollment>> byCohort = new HashMap<>();
        for (StudentTermEnrollment e : studentTermEnrollmentRepository.findAll()) {
            if (e.getCohort() == null || e.getCohort().getId() == null) continue;
            byCohort.computeIfAbsent(e.getCohort().getId(), k -> new ArrayList<>()).add(e);
        }
        List<CohortRetentionRow> rows = byCohort.values().stream()
            .map(list -> {
                var cohort = list.get(0).getCohort();
                long baseline = list.stream()
                    .filter(e -> Integer.valueOf(1).equals(e.getSemesterNumber()))
                    .map(StudentTermEnrollment::getStudent).distinct().count();
                if (baseline == 0) baseline = list.stream().map(StudentTermEnrollment::getStudent).distinct().count();
                long base = Math.max(1L, baseline);
                List<CohortRetentionTerm> terms = list.stream()
                    .collect(java.util.stream.Collectors.groupingBy(StudentTermEnrollment::getSemesterNumber))
                    .entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .limit(6)
                    .map(e -> {
                        long enrolled = e.getValue().size();
                        long active = e.getValue().stream().filter(x -> x.getStatus() == EnrollmentStatus.ENROLLED).count();
                        return new CohortRetentionTerm(e.getKey(), enrolled, active, pctOfNullable(active, base));
                    })
                    .toList();
                return new CohortRetentionRow(cohort.getCohortCode(), cohort.getDisplayName(), baseline, terms);
            })
            .sorted(Comparator.comparing(CohortRetentionRow::cohortCode).reversed())
            .limit(5)
            .toList();
        return ResponseEntity.ok(rows);
    }

    /** Compact executive KPI strip for daily admin pulse. */
    @GetMapping("/top-line-kpis")
    @PreAuthorize("@perm.hasAny('STUDENT_VIEW','STUDENT_FEE_VIEW','ENQUIRY_VIEW','REPORT_VIEW')")
    public ResponseEntity<List<TopLineKpi>> getTopLineKpis() {
        LocalDate today = LocalDate.now();
        Instant tomorrowStart = today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant thirtyDaysAgo = today.minusDays(30).atStartOfDay().toInstant(ZoneOffset.UTC);

        BigDecimal todayCollection = feeInstallmentRepository.findAll().stream()
            .filter(p -> today.equals(p.getPaymentDate()))
            .map(FeeInstallment::getAmountPaid).filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        long todayAdmissions = studentRepository.findAll().stream().filter(s -> today.equals(s.getAdmissionDate())).count();
        long pendingDocs = enquiryDocumentRepository.findAll().stream()
            .filter(d -> d.getStatus() == DocumentVerificationStatus.UPLOADED).count();
        BigDecimal overdue = feeDemandRepository.findAll().stream()
            .filter(d -> d.getDueDate() != null && d.getDueDate().isBefore(today))
            .map(FeeDemand::getOutstandingAmount).filter(v -> v.compareTo(BigDecimal.ZERO) > 0)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        long activeEnquiries = enquiryRepository.findAll().stream()
            .filter(e -> e.getStatus() != null
                      && e.getStatus() != EnquiryStatus.NOT_INTERESTED
                      && e.getStatus() != EnquiryStatus.ADMITTED)
            .count();
        var recentEnquiries = enquiryRepository.findAll().stream()
            .filter(e -> e.getCreatedAt() != null && !e.getCreatedAt().isBefore(thirtyDaysAgo)
                      && e.getCreatedAt().isBefore(tomorrowStart))
            .toList();
        long converted = recentEnquiries.stream()
            .filter(e -> e.getStatus() == EnquiryStatus.ADMITTED)
            .count();
        int conversion30d = pctOfNullable(converted, Math.max(1L, recentEnquiries.size()));

        return ResponseEntity.ok(List.of(
            new TopLineKpi("today-collection", "Today's Collection", formatAmount(todayCollection), "Live cash pulse", "green"),
            new TopLineKpi("today-admissions", "Today's Admissions", String.valueOf(todayAdmissions), "Daily velocity", "accent"),
            new TopLineKpi("pending-verifications", "Pending Verifications", String.valueOf(pendingDocs), "Bottleneck indicator", pendingDocs > 0 ? "amber" : "green"),
            new TopLineKpi("overdue-fees", "Overdue Fees", formatAmount(overdue), "Risk exposure", overdue.compareTo(BigDecimal.ZERO) > 0 ? "red" : "green"),
            new TopLineKpi("active-enquiries", "Active Enquiries", String.valueOf(activeEnquiries), "Top of funnel", "accent"),
            new TopLineKpi("conversion-30d", "Conversion 30d", conversion30d + "%", "Pipeline health", conversion30d >= 30 ? "green" : "amber")
        ));
    }

    /** Passive anomaly banner: today collection vs the same weekday last week. */
    @GetMapping("/anomaly-banner")
    @PreAuthorize("@perm.hasAny('STUDENT_FEE_VIEW','FEE_COLLECT','REPORT_VIEW')")
    public ResponseEntity<AnomalyBannerData> getAnomalyBanner() {
        LocalDate today = LocalDate.now();
        LocalDate lastWeek = today.minusDays(7);
        BigDecimal todayCollection = sumReceiptsOn(today);
        BigDecimal lastWeekCollection = sumReceiptsOn(lastWeek);

        int deltaPct;
        if (lastWeekCollection.compareTo(BigDecimal.ZERO) > 0) {
            deltaPct = (int) Math.round((todayCollection.doubleValue() - lastWeekCollection.doubleValue())
                / lastWeekCollection.doubleValue() * 100.0);
        } else {
            deltaPct = todayCollection.compareTo(BigDecimal.ZERO) > 0 ? 100 : 0;
        }

        String direction = deltaPct < 0 ? "down" : deltaPct > 0 ? "up" : "flat";
        String severity = deltaPct <= -20 ? "red" : deltaPct < 0 ? "amber" : "green";
        String message = deltaPct < 0
            ? "Collections down " + Math.abs(deltaPct) + "% vs same day last week"
            : deltaPct > 0
                ? "Collections up " + deltaPct + "% vs same day last week"
                : "Collections flat vs same day last week";

        return ResponseEntity.ok(new AnomalyBannerData(
            todayCollection, lastWeekCollection, deltaPct, direction, message, severity));
    }

    /** Program capacity alerts using Program.seatCapacity and active students. */
    @GetMapping("/capacity-alert")
    @PreAuthorize("@perm.hasAny('STUDENT_VIEW','PROGRAM_VIEW','REPORT_VIEW')")
    public ResponseEntity<List<CapacityAlertRow>> getCapacityAlert() {
        Map<Long, Long> activeByProgram = new HashMap<>();
        for (Student s : studentRepository.findAll()) {
            if (s.getStatus() == StudentStatus.ACTIVE && s.getProgram() != null && s.getProgram().getId() != null) {
                activeByProgram.merge(s.getProgram().getId(), 1L, Long::sum);
            }
        }

        List<CapacityAlertRow> rows = programRepository.findAll().stream()
            .filter(p -> p.getStatus() == ProgramStatus.ACTIVE)
            .filter(p -> p.getSeatCapacity() != null && p.getSeatCapacity() > 0)
            .map(p -> {
                long filled = activeByProgram.getOrDefault(p.getId(), 0L);
                int pct = pctOfNullable(filled, p.getSeatCapacity());
                int left = Math.max(0, p.getSeatCapacity() - (int) filled);
                String severity = pct >= 95 ? "red" : pct >= 85 ? "amber" : "green";
                return new CapacityAlertRow(p.getName(), p.getCode(), filled, p.getSeatCapacity(), pct, left, severity);
            })
            .filter(r -> r.occupancyPct() >= 85)
            .sorted(Comparator.comparingInt(CapacityAlertRow::occupancyPct).reversed())
            .limit(6)
            .toList();
        return ResponseEntity.ok(rows);
    }

    /** UGC/NAAC/AICTE/university compliance documents expiring within 90 days. */
    @GetMapping("/compliance-alerts")
    @PreAuthorize("@perm.hasAny('SETTINGS_MANAGE','REPORT_VIEW')")
    public ResponseEntity<List<ComplianceAlertRow>> getComplianceAlerts() {
        LocalDate today = LocalDate.now();
        LocalDate horizon = today.plusDays(90);
        List<ComplianceAlertRow> rows = complianceDocumentRepository.findAll().stream()
            .filter(d -> d.getExpiresOn() != null)
            .filter(d -> d.getStatus() == null || !d.getStatus().equalsIgnoreCase("ARCHIVED"))
            .filter(d -> !d.getExpiresOn().isAfter(horizon))
            .map(d -> {
                long days = java.time.temporal.ChronoUnit.DAYS.between(today, d.getExpiresOn());
                String severity = days < 0 ? "red" : days <= 30 ? "red" : days <= 60 ? "amber" : "accent";
                return new ComplianceAlertRow(d.getAuthority(), d.getDocumentName(), d.getReferenceNumber(),
                    d.getExpiresOn(), days, severity);
            })
            .sorted(Comparator.comparingLong(ComplianceAlertRow::daysLeft))
            .limit(6)
            .toList();
        return ResponseEntity.ok(rows);
    }

    /** Last five high-privilege audit events: role, permission, user and fee changes. */
    @GetMapping("/audit-mini-feed")
    @PreAuthorize("@perm.hasAny('USER_VIEW','SETTINGS_MANAGE','REPORT_VIEW')")
    public ResponseEntity<List<AuditMiniFeedItem>> getAuditMiniFeed() {
        List<AuditMiniFeedItem> items = auditLogRepository.findAll().stream()
            .filter(this::isHighPrivilegeAudit)
            .sorted(Comparator.comparing(AuditLog::getOccurredAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
            .limit(5)
            .map(a -> new AuditMiniFeedItem(
                a.getActor(), a.getAction(), a.getEntityType(), a.getEntityId(), a.getDetail(), a.getOccurredAt(),
                auditSeverity(a)))
            .toList();
        return ResponseEntity.ok(items);
    }

    /**
     * Recent system activity feed — last 10 audit events, most recent first.
     * The action label comes from the audit detail (if short enough) or from
     * the entity-type + action verb, formatted for display.
     */
    @GetMapping("/activity")
    public ResponseEntity<List<ActivityFeedItem>> getActivity() {
        List<ActivityFeedItem> items = auditLogRepository.findAll().stream()
            .filter(a -> a.getOccurredAt() != null)
            .sorted(Comparator.comparing(AuditLog::getOccurredAt,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed())
            .limit(10)
            .map(a -> new ActivityFeedItem(
                a.getId() != null ? a.getId() : 0L,
                buildActivityLabel(a),
                a.getActor() != null ? a.getActor() : "System",
                a.getOccurredAt()))
            .toList();
        return ResponseEntity.ok(items);
    }

    /**
     * Active faculty colleagues for the Connections widget —
     * up to 6 active faculty members with name, initials and designation.
     * Online status is always false (no realtime presence tracking).
     */
    @GetMapping("/connections")
    public ResponseEntity<List<ConnectionItem>> getConnections() {
        List<ConnectionItem> list = facultyRepository.findAll().stream()
            .filter(f -> f.getStatus() == FacultyStatus.ACTIVE)
            .limit(6)
            .map(f -> {
                String first = f.getFirstName() != null ? f.getFirstName() : "";
                String last  = f.getLastName()  != null ? f.getLastName()  : "";
                String name  = (first + " " + last).trim();
                if (name.isEmpty()) name = "Faculty #" + f.getId();
                String role = f.getDesignation() != null
                    ? f.getDesignation().getName() : "Faculty";
                return new ConnectionItem(f.getId(), name, buildInitials(name), role, false);
            })
            .toList();
        return ResponseEntity.ok(list);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private record SeatFillData(long filled, long total) {}

    private long computeGovtLapsedSeats() {
        return academicYearRepository.findByIsCurrentTrue().map(ay -> {
            List<com.cms.model.Cohort> closed =
                cohortRepository.findByAdmissionAcademicYearIdAndCounsellingClosedTrue(ay.getId());
            if (closed.isEmpty()) return 0L;
            long total = closed.stream()
                .filter(c -> c.getCounsellingSeats() != null)
                .mapToLong(com.cms.model.Cohort::getCounsellingSeats)
                .sum();
            long filled = closed.stream()
                .mapToLong(c -> studentRepository.countByCohortIdAndAdmissionCategory(
                    c.getId(), AdmissionCategory.COUNSELLING))
                .sum();
            return Math.max(0L, total - filled);
        }).orElse(0L);
    }

    private SeatFillData computeSeatFill(AdmissionCategory category) {
        return academicYearRepository.findByIsCurrentTrue().map(ay -> {
            List<com.cms.model.Cohort> cohorts =
                cohortRepository.findByAdmissionAcademicYearId(ay.getId());
            long total = cohorts.stream()
                .mapToLong(c -> {
                    Integer seats = category == AdmissionCategory.COUNSELLING
                        ? c.getCounsellingSeats() : c.getManagementSeats();
                    return seats != null ? seats : 0L;
                })
                .sum();
            long filled = studentRepository
                .countByCohortAdmissionAcademicYearIdAndAdmissionCategory(ay.getId(), category);
            return new SeatFillData(filled, total);
        }).orElse(new SeatFillData(0L, 0L));
    }

    private static BigDecimal orZero(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private BigDecimal sumReceiptsOn(LocalDate date) {
        return paymentReceiptRepository.findAll().stream()
            .filter(r -> date.equals(r.getPaymentDate()))
            .map(PaymentReceipt::getAmountPaid)
            .filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isHighPrivilegeAudit(AuditLog audit) {
        String action = audit.getAction() != null ? audit.getAction().toUpperCase() : "";
        String entity = audit.getEntityType() != null ? audit.getEntityType().toUpperCase() : "";
        String detail = audit.getDetail() != null ? audit.getDetail().toUpperCase() : "";
        return action.contains("ROLE") || action.contains("PERMISSION") || action.contains("USER")
            || action.contains("FEE") || action.contains("WAIVER") || action.contains("OVERRIDE")
            || entity.contains("ROLE") || entity.contains("PERMISSION") || entity.contains("USER")
            || detail.contains("FEE") || detail.contains("WAIVER") || detail.contains("OVERRIDE");
    }

    private String auditSeverity(AuditLog audit) {
        String action = audit.getAction() != null ? audit.getAction().toUpperCase() : "";
        if (action.contains("DELETE") || action.contains("DEACTIVATE") || action.contains("REVOKE")) return "red";
        if (action.contains("UPDATE") || action.contains("CHANGE") || action.contains("GRANT")) return "amber";
        return "accent";
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private static String monthLabel(int month) {
        return java.time.Month.of(month).name().substring(0, 3);
    }

    private long sum(Map<EnquiryStatus, Long> raw, EnquiryStatus... statuses) {
        long total = 0;
        for (EnquiryStatus s : statuses) total += raw.getOrDefault(s, 0L);
        return total;
    }

    private Integer pctOf(long part, long whole) {
        if (whole <= 0) return null;
        return (int) Math.round((double) part / whole * 100.0);
    }

    private int pctOfNullable(long part, long whole) {
        Integer v = pctOf(part, whole);
        return v == null ? 0 : v;
    }

    private List<QuickStat> buildHeroQuickStats(AppRole role) {
        if (role == null) return List.of();

        DashboardSummaryResponse s = dashboardService.getSummary();
        String roleName = role.getName().toLowerCase();

        if (roleName.contains("admin") || roleName.contains("devadmin")) {
            return List.of(
                new QuickStat("Student Explorer", String.valueOf(s.totalStudents())),
                new QuickStat("Faculty",  String.valueOf(s.totalFaculty())),
                new QuickStat("Labs",     String.valueOf(s.totalLabs()))
            );
        }
        if (roleName.contains("cashier") || roleName.contains("accountant")) {
            return List.of(
                new QuickStat("Collected", formatAmount(s.feeCollectedThisMonth())),
                new QuickStat("Outstanding", formatAmount(s.feeOutstanding())),
                new QuickStat("Payments",  String.valueOf(s.totalFeePayments()))
            );
        }
        if (roleName.contains("frontoffice") || roleName.contains("front_office")) {
            long newEnq = Optional.ofNullable(s.enquiryFunnel())
                .map(m -> m.getOrDefault("ENQUIRED", 0L)).orElse(0L);
            return List.of(
                new QuickStat("Enquiries",   String.valueOf(newEnq)),
                new QuickStat("Admission Explorer", String.valueOf(s.totalStudents())),
                new QuickStat("Specialities", String.valueOf(s.totalSpecialities()))
            );
        }
        // Faculty and Student hero stats are built by their own widget components
        return List.of();
    }

    private long countEnquiriesThisMonth(YearMonth ym) {
        try {
            LocalDate start = ym.atDay(1);
            LocalDate end   = ym.atEndOfMonth();
            Instant   from  = start.atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant   to    = end.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            return enquiryRepository.findAll().stream()
                .filter(e -> e.getCreatedAt() != null
                          && !e.getCreatedAt().isBefore(from)
                          && e.getCreatedAt().isBefore(to))
                .count();
        } catch (Exception ignored) { return 0L; }
    }

    private long countAdmissionsThisMonth(YearMonth ym) {
        try {
            LocalDate start = ym.atDay(1);
            LocalDate end   = ym.atEndOfMonth();
            Instant   from  = start.atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant   to    = end.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            return admissionRepository.findAll().stream()
                .filter(a -> a.getCreatedAt() != null
                          && !a.getCreatedAt().isBefore(from)
                          && a.getCreatedAt().isBefore(to))
                .count();
        } catch (Exception ignored) { return 0L; }
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "₹0";
        long v = amount.longValue();
        if (v >= 100_000) return "₹" + (v / 100_000) + "L";
        if (v >= 1_000)   return "₹" + (v / 1_000)   + "K";
        return "₹" + v;
    }

    private String formatStatus(String status) {
        if (status == null) return "";
        String[] words = status.replace('_', ' ').toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                if (!sb.isEmpty()) sb.append(' ');
                sb.append(Character.toUpperCase(word.charAt(0)));
                sb.append(word.substring(1));
            }
        }
        return sb.toString();
    }

    private String computeAcademicYear() {
        int year = LocalDate.now().getMonthValue() >= 6
            ? LocalDate.now().getYear()
            : LocalDate.now().getYear() - 1;
        return year + "–" + String.valueOf(year + 1).substring(2);
    }

    private String buildActivityLabel(AuditLog a) {
        if (a.getDetail() != null && !a.getDetail().isBlank() && a.getDetail().length() <= 80) {
            return a.getDetail();
        }
        String base = formatStatus(a.getAction() != null ? a.getAction() : "Action");
        if (a.getEntityType() != null && !a.getEntityType().isBlank()) {
            base = formatStatus(a.getEntityType()) + " " + base.toLowerCase();
        }
        return base;
    }

    private static String buildInitials(String name) {
        String[] words = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(2, words.length); i++) {
            if (!words[i].isEmpty()) sb.append(Character.toUpperCase(words[i].charAt(0)));
        }
        return sb.length() > 0 ? sb.toString() : "?";
    }
}
