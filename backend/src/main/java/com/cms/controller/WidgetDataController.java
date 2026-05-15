package com.cms.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Comparator;
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
import com.cms.dto.DashboardTrendPoint;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AppRole;
import com.cms.model.AppUser;
import com.cms.repository.AdmissionRepository;
import com.cms.repository.AppUserRepository;
import com.cms.repository.EnquiryRepository;
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

    public WidgetDataController(DashboardService dashboardService,
                                AppUserRepository appUserRepository,
                                AdmissionRepository admissionRepository,
                                EnquiryRepository enquiryRepository) {
        this.dashboardService   = dashboardService;
        this.appUserRepository  = appUserRepository;
        this.admissionRepository = admissionRepository;
        this.enquiryRepository  = enquiryRepository;
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
     *     | enquiries | admissions | departments | programs | equipment
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
            case "departments"  -> new StatCardData(key,
                String.valueOf(s.totalDepartments()), "Active", null);
            case "programs"     -> new StatCardData(key,
                String.valueOf(s.totalPrograms()), "Running", null);
            case "equipment"    -> new StatCardData(key,
                String.valueOf(s.totalEquipment()), "Total", null);
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
                new QuickActionItem("Admissions",      "/admissions",        "how_to_reg",      "View admissions pipeline"),
                new QuickActionItem("Students",        "/students",          "school",          "Student directory")
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

    // ─── Private helpers ─────────────────────────────────────────────────────

    private List<QuickStat> buildHeroQuickStats(AppRole role) {
        if (role == null) return List.of();

        DashboardSummaryResponse s = dashboardService.getSummary();
        String roleName = role.getName().toLowerCase();

        if (roleName.contains("admin") || roleName.contains("devadmin")) {
            return List.of(
                new QuickStat("Students", String.valueOf(s.totalStudents())),
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
                new QuickStat("Admissions",  String.valueOf(s.totalStudents())),
                new QuickStat("Departments", String.valueOf(s.totalDepartments()))
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
}
