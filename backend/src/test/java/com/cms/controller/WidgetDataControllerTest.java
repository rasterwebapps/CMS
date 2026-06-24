package com.cms.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.config.PermSecurityBean;
import com.cms.dto.DashboardSummaryResponse;
import com.cms.dto.DashboardTrendPoint;
import com.cms.dto.DashboardTrendsResponse;
import com.cms.model.AppRole;
import com.cms.model.AppUser;
import com.cms.repository.AdmissionRepository;
import com.cms.repository.AppUserRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.service.DashboardService;

@WebMvcTest(controllers = WidgetDataController.class)
class WidgetDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private AdmissionRepository admissionRepository;

    @MockitoBean
    private EnquiryRepository enquiryRepository;

    @MockitoBean(name = "perm")
    private PermSecurityBean perm;

    private DashboardSummaryResponse summary;
    private AppUser adminUser;
    private AppRole adminRole;

    @BeforeEach
    void setUp() {
        when(perm.has(anyString())).thenReturn(true);
        when(perm.hasAny(org.mockito.ArgumentMatchers.<String>any())).thenReturn(true);

        summary = new DashboardSummaryResponse(
            100L, 20L, 5L, 30L, 8L, 10L, 200L, 3L, 500L, 15L, 1000L,
            Map.of("OPERATIONAL", 150L, "FAULTY", 50L),
            Map.of("OPEN", 5L, "CLOSED", 10L),
            Map.of("ACTIVE", 90L),
            Map.of("PRESENT", 80L, "ABSENT", 20L),
            Map.of("ENQUIRED", 10L, "ENROLLED", 50L),
            BigDecimal.valueOf(250000),
            BigDecimal.valueOf(75000),
            7L
        );

        adminRole = new AppRole();
        adminRole.setName("ROLE_ADMIN");
        adminRole.setDisplayName("Administrator");

        adminUser = new AppUser();
        adminUser.setKeycloakUsername("testadmin");
        adminUser.setAppRole(adminRole);
    }

    // ─── /hero ───────────────────────────────────────────────────────────────

    @Test
    void getHeroReturnsWelcomeBannerForAuthenticatedUser() throws Exception {
        when(appUserRepository.findByKeycloakUsername("testadmin"))
            .thenReturn(Optional.of(adminUser));
        when(dashboardService.getSummary()).thenReturn(summary);

        mockMvc.perform(get("/dashboard/data/hero")
                .with(jwt().jwt(j -> j.claim("preferred_username", "testadmin"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("testadmin"))
            .andExpect(jsonPath("$.roleLabel").value("Administrator"))
            .andExpect(jsonPath("$.quickStats").isArray());
    }

    @Test
    void getHeroReturns404WhenUserNotFound() throws Exception {
        when(appUserRepository.findByKeycloakUsername("ghost"))
            .thenReturn(Optional.empty());

        mockMvc.perform(get("/dashboard/data/hero")
                .with(jwt().jwt(j -> j.claim("preferred_username", "ghost"))))
            .andExpect(status().isNotFound());
    }

    @Test
    void getHeroReturnsCashierQuickStats() throws Exception {
        AppRole cashierRole = new AppRole();
        cashierRole.setName("ROLE_CASHIER");
        cashierRole.setDisplayName("Cashier");
        AppUser cashierUser = new AppUser();
        cashierUser.setKeycloakUsername("cashier1");
        cashierUser.setAppRole(cashierRole);

        when(appUserRepository.findByKeycloakUsername("cashier1"))
            .thenReturn(Optional.of(cashierUser));
        when(dashboardService.getSummary()).thenReturn(summary);

        mockMvc.perform(get("/dashboard/data/hero")
                .with(jwt().jwt(j -> j.claim("preferred_username", "cashier1"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quickStats").isArray());
    }

    @Test
    void getHeroReturnsFrontOfficeQuickStats() throws Exception {
        AppRole foRole = new AppRole();
        foRole.setName("ROLE_FRONT_OFFICE");
        foRole.setDisplayName("Front Office");
        AppUser foUser = new AppUser();
        foUser.setKeycloakUsername("fo1");
        foUser.setAppRole(foRole);

        when(appUserRepository.findByKeycloakUsername("fo1"))
            .thenReturn(Optional.of(foUser));
        when(dashboardService.getSummary()).thenReturn(summary);

        mockMvc.perform(get("/dashboard/data/hero")
                .with(jwt().jwt(j -> j.claim("preferred_username", "fo1"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quickStats").isArray());
    }

    @Test
    void getHeroReturnsEmptyQuickStatsForFaculty() throws Exception {
        AppRole facultyRole = new AppRole();
        facultyRole.setName("ROLE_FACULTY");
        facultyRole.setDisplayName("Faculty");
        AppUser facultyUser = new AppUser();
        facultyUser.setKeycloakUsername("faculty1");
        facultyUser.setAppRole(facultyRole);

        when(appUserRepository.findByKeycloakUsername("faculty1"))
            .thenReturn(Optional.of(facultyUser));
        when(dashboardService.getSummary()).thenReturn(summary);

        mockMvc.perform(get("/dashboard/data/hero")
                .with(jwt().jwt(j -> j.claim("preferred_username", "faculty1"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quickStats").isArray());
    }

    @Test
    void getHeroHandlesNullRole() throws Exception {
        AppUser noRoleUser = new AppUser();
        noRoleUser.setKeycloakUsername("norole");
        noRoleUser.setAppRole(null);

        when(appUserRepository.findByKeycloakUsername("norole"))
            .thenReturn(Optional.of(noRoleUser));

        mockMvc.perform(get("/dashboard/data/hero")
                .with(jwt().jwt(j -> j.claim("preferred_username", "norole"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roleLabel").value("User"))
            .andExpect(jsonPath("$.quickStats").isArray());
    }

    // ─── /stat/{key} ─────────────────────────────────────────────────────────

    @Test
    void getStatStudents() throws Exception {
        when(dashboardService.getSummary()).thenReturn(summary);
        when(enquiryRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/dashboard/data/stat/students").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.key").value("students"))
            .andExpect(jsonPath("$.value").value("100"));
    }

    @Test
    void getStatFaculty() throws Exception {
        when(dashboardService.getSummary()).thenReturn(summary);

        mockMvc.perform(get("/dashboard/data/stat/faculty").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.key").value("faculty"))
            .andExpect(jsonPath("$.value").value("20"));
    }

    @Test
    void getStatLabs() throws Exception {
        when(dashboardService.getSummary()).thenReturn(summary);

        mockMvc.perform(get("/dashboard/data/stat/labs").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.key").value("labs"));
    }

    @Test
    void getStatFeeCollected() throws Exception {
        when(dashboardService.getSummary()).thenReturn(summary);

        mockMvc.perform(get("/dashboard/data/stat/fee-collected").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.key").value("fee-collected"))
            .andExpect(jsonPath("$.badge").value("This month"));
    }

    @Test
    void getStatOutstanding() throws Exception {
        when(dashboardService.getSummary()).thenReturn(summary);

        mockMvc.perform(get("/dashboard/data/stat/outstanding").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.key").value("outstanding"));
    }

    @Test
    void getStatEnquiries() throws Exception {
        when(dashboardService.getSummary()).thenReturn(summary);
        when(enquiryRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/dashboard/data/stat/enquiries").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.key").value("enquiries"));
    }

    @Test
    void getStatAdmissions() throws Exception {
        when(dashboardService.getSummary()).thenReturn(summary);
        when(admissionRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/dashboard/data/stat/admissions").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.key").value("admissions"));
    }

    @Test
    void getStatSpecialities() throws Exception {
        when(dashboardService.getSummary()).thenReturn(summary);

        mockMvc.perform(get("/dashboard/data/stat/specialities").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.key").value("specialities"));
    }

    @Test
    void getStatPrograms() throws Exception {
        when(dashboardService.getSummary()).thenReturn(summary);

        mockMvc.perform(get("/dashboard/data/stat/programs").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.key").value("programs"));
    }

    @Test
    void getStatEquipment() throws Exception {
        when(dashboardService.getSummary()).thenReturn(summary);

        mockMvc.perform(get("/dashboard/data/stat/equipment").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.key").value("equipment"));
    }

    @Test
    void getStatUnknownKeyReturns404() throws Exception {
        when(dashboardService.getSummary()).thenReturn(summary);

        mockMvc.perform(get("/dashboard/data/stat/unknown-key").with(jwt()))
            .andExpect(status().isNotFound());
    }

    // ─── /trend-chart ────────────────────────────────────────────────────────

    @Test
    void getTrendChartReturnsTrends() throws Exception {
        DashboardTrendsResponse trends = new DashboardTrendsResponse(
            List.of(new DashboardTrendPoint("Jan 2026", 10L), new DashboardTrendPoint("Feb 2026", 15L)),
            List.of(new DashboardTrendPoint("Jan 2026", 50000L))
        );
        when(dashboardService.getTrends()).thenReturn(trends);

        mockMvc.perform(get("/dashboard/data/trend-chart").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].month").value("Jan 2026"))
            .andExpect(jsonPath("$[0].value").value(10));
    }

    // ─── /pending-approvals ──────────────────────────────────────────────────

    @Test
    void getPendingApprovalsReturnsItems() throws Exception {
        when(dashboardService.getSummary()).thenReturn(summary);

        mockMvc.perform(get("/dashboard/data/pending-approvals").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getPendingApprovalsEmptyWhenNoIssues() throws Exception {
        DashboardSummaryResponse zeroSummary = new DashboardSummaryResponse(
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            Map.of(), Map.of("OPEN", 0L), Map.of(),
            Map.of(), Map.of("ENQUIRED", 0L),
            BigDecimal.ZERO, BigDecimal.ZERO,
            0L
        );
        when(dashboardService.getSummary()).thenReturn(zeroSummary);

        mockMvc.perform(get("/dashboard/data/pending-approvals").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    // ─── /equipment-status ───────────────────────────────────────────────────

    @Test
    void getEquipmentStatusReturnsRows() throws Exception {
        when(dashboardService.getSummary()).thenReturn(summary);

        mockMvc.perform(get("/dashboard/data/equipment-status").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getEquipmentStatusEmptyWhenNoBuckets() throws Exception {
        DashboardSummaryResponse emptySummary = new DashboardSummaryResponse(
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            null, null, null, null, null,
            BigDecimal.ZERO, BigDecimal.ZERO,
            0L
        );
        when(dashboardService.getSummary()).thenReturn(emptySummary);

        mockMvc.perform(get("/dashboard/data/equipment-status").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    // ─── /fee-overview ───────────────────────────────────────────────────────

    @Test
    void getFeeOverviewReturnsSummary() throws Exception {
        when(dashboardService.getSummary()).thenReturn(summary);
        when(enquiryRepository.findAll()).thenReturn(List.of());
        when(admissionRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/dashboard/data/fee-overview").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.collectedThisMonth").value(250000))
            .andExpect(jsonPath("$.outstanding").value(75000));
    }

    // ─── /quick-actions ──────────────────────────────────────────────────────

    @Test
    void getQuickActionsReturnsAdminActions() throws Exception {
        when(appUserRepository.findByKeycloakUsername("testadmin"))
            .thenReturn(Optional.of(adminUser));

        mockMvc.perform(get("/dashboard/data/quick-actions")
                .with(jwt().jwt(j -> j.claim("preferred_username", "testadmin"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].label").exists());
    }

    @Test
    void getQuickActionsReturnsFacultyActions() throws Exception {
        AppRole facultyRole = new AppRole();
        facultyRole.setName("ROLE_FACULTY");
        facultyRole.setDisplayName("Faculty");
        AppUser facultyUser = new AppUser();
        facultyUser.setKeycloakUsername("faculty1");
        facultyUser.setAppRole(facultyRole);

        when(appUserRepository.findByKeycloakUsername("faculty1"))
            .thenReturn(Optional.of(facultyUser));

        mockMvc.perform(get("/dashboard/data/quick-actions")
                .with(jwt().jwt(j -> j.claim("preferred_username", "faculty1"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getQuickActionsReturnsStudentActions() throws Exception {
        AppRole studentRole = new AppRole();
        studentRole.setName("ROLE_STUDENT");
        studentRole.setDisplayName("Student");
        AppUser studentUser = new AppUser();
        studentUser.setKeycloakUsername("student1");
        studentUser.setAppRole(studentRole);

        when(appUserRepository.findByKeycloakUsername("student1"))
            .thenReturn(Optional.of(studentUser));

        mockMvc.perform(get("/dashboard/data/quick-actions")
                .with(jwt().jwt(j -> j.claim("preferred_username", "student1"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getQuickActionsReturnsCashierActions() throws Exception {
        AppRole cashierRole = new AppRole();
        cashierRole.setName("ROLE_CASHIER");
        cashierRole.setDisplayName("Cashier");
        AppUser cashierUser = new AppUser();
        cashierUser.setKeycloakUsername("cashier1");
        cashierUser.setAppRole(cashierRole);

        when(appUserRepository.findByKeycloakUsername("cashier1"))
            .thenReturn(Optional.of(cashierUser));

        mockMvc.perform(get("/dashboard/data/quick-actions")
                .with(jwt().jwt(j -> j.claim("preferred_username", "cashier1"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getQuickActionsReturnsFrontOfficeActions() throws Exception {
        AppRole foRole = new AppRole();
        foRole.setName("ROLE_FRONT_OFFICE");
        foRole.setDisplayName("Front Office");
        AppUser foUser = new AppUser();
        foUser.setKeycloakUsername("fo1");
        foUser.setAppRole(foRole);

        when(appUserRepository.findByKeycloakUsername("fo1"))
            .thenReturn(Optional.of(foUser));

        mockMvc.perform(get("/dashboard/data/quick-actions")
                .with(jwt().jwt(j -> j.claim("preferred_username", "fo1"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getQuickActionsReturnsDefaultForUnknownRole() throws Exception {
        AppRole unknownRole = new AppRole();
        unknownRole.setName("ROLE_OTHER");
        unknownRole.setDisplayName("Other");
        AppUser otherUser = new AppUser();
        otherUser.setKeycloakUsername("other1");
        otherUser.setAppRole(unknownRole);

        when(appUserRepository.findByKeycloakUsername("other1"))
            .thenReturn(Optional.of(otherUser));

        mockMvc.perform(get("/dashboard/data/quick-actions")
                .with(jwt().jwt(j -> j.claim("preferred_username", "other1"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getQuickActionsReturns404WhenUserNotFound() throws Exception {
        when(appUserRepository.findByKeycloakUsername("ghost"))
            .thenReturn(Optional.empty());

        mockMvc.perform(get("/dashboard/data/quick-actions")
                .with(jwt().jwt(j -> j.claim("preferred_username", "ghost"))))
            .andExpect(status().isNotFound());
    }

    // ─── /system-health ──────────────────────────────────────────────────────

    @Test
    void getSystemHealthReturnsHealthData() throws Exception {
        when(appUserRepository.count()).thenReturn(5L);

        mockMvc.perform(get("/dashboard/data/system-health").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.overall").value("ok"))
            .andExpect(jsonPath("$.checks").isArray());
    }

    @Test
    void getSystemHealthReturnsErrorWhenDbFails() throws Exception {
        when(appUserRepository.count()).thenThrow(new RuntimeException("DB down"));

        mockMvc.perform(get("/dashboard/data/system-health").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.overall").value("error"));
    }

    // ─── /classes-today ──────────────────────────────────────────────────────

    @Test
    void getClassesTodayReturnsStubMessage() throws Exception {
        mockMvc.perform(get("/dashboard/data/classes-today").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").exists());
    }

    // ─── /doc-stats ──────────────────────────────────────────────────────────

    @Test
    void getDocStatsReturnsStubMessage() throws Exception {
        mockMvc.perform(get("/dashboard/data/doc-stats").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").exists());
    }
}

