package com.cms.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.StudentFeeAllocationResponse;
import com.cms.service.FeeExplorerService;
import com.cms.service.FeeExportService;
import com.cms.service.FeeFinalizationService;
import com.cms.service.FeeRefundExportService;
import com.cms.service.FeeRefundService;
import com.cms.service.OneBookIntegrationService;
import com.cms.service.PaymentCollectionService;
import com.cms.service.PenaltyCalculationService;
import com.cms.service.StudentFeeSelfServiceService;
import com.cms.service.UserPermissionService;

/**
 * Full-context proof (real {@code SecurityConfig}, real {@code @PreAuthorize} enforcement) that
 * the two fee permissions stay properly separated: {@code MY_FEE_VIEW} unlocks only the
 * self-scoped {@code /student-fees/my/*} endpoints, never the by-studentId staff endpoints those
 * were missing {@code @PreAuthorize} on before this change (the real gap this test class exists
 * to prevent regressing) -- and {@code STUDENT_FEE_VIEW} keeps working for existing staff callers,
 * completely unaffected by the new self-service permission's introduction. A
 * {@code @WebMvcTest(addFilters=false)} slice (as {@link StudentFeeControllerTest} uses) never
 * actually exercises {@code @PreAuthorize}, so only a real {@code @SpringBootTest} like this one
 * can catch a regression here -- same pattern as {@link TimetableSelfServiceScopeSecurityTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(StudentFeeSelfServiceSecurityTest.TestConfig.class)
class StudentFeeSelfServiceSecurityTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserPermissionService userPermissionService;

    @MockBean
    private FeeFinalizationService feeFinalizationService;

    @MockBean
    private PaymentCollectionService paymentCollectionService;

    @MockBean
    private PenaltyCalculationService penaltyCalculationService;

    @MockBean
    private FeeExplorerService feeExplorerService;

    @MockBean
    private FeeExportService feeExportService;

    @MockBean
    private FeeRefundService feeRefundService;

    @MockBean
    private FeeRefundExportService feeRefundExportService;

    @MockBean
    private OneBookIntegrationService oneBookIntegrationService;

    @MockBean
    private StudentFeeSelfServiceService studentFeeSelfServiceService;

    @Test
    void myFeeViewOnlyCanReadOwnSummaryReceiptsAndPenalties() throws Exception {
        when(userPermissionService.getPermissions("teststudent45")).thenReturn(Set.of("MY_FEE_VIEW"));
        when(studentFeeSelfServiceService.findMySummary("teststudent45")).thenReturn(Optional.empty());
        when(studentFeeSelfServiceService.findMyReceipts("teststudent45")).thenReturn(List.of());
        when(studentFeeSelfServiceService.findMyPenalties("teststudent45")).thenReturn(Optional.empty());

        mockMvc.perform(get("/student-fees/my/summary")
                .with(jwt().jwt(j -> j.claim("preferred_username", "teststudent45"))))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/student-fees/my/receipts")
                .with(jwt().jwt(j -> j.claim("preferred_username", "teststudent45"))))
            .andExpect(status().isOk());
        mockMvc.perform(get("/student-fees/my/penalties")
                .with(jwt().jwt(j -> j.claim("preferred_username", "teststudent45"))))
            .andExpect(status().isNoContent());
    }

    @Test
    void myFeeViewOnlyCannotReadAnotherStudentsFeeDataByIdEndpoints() throws Exception {
        when(userPermissionService.getPermissions("teststudent45")).thenReturn(Set.of("MY_FEE_VIEW"));

        mockMvc.perform(get("/student-fees/1/semester-breakdown")
                .with(jwt().jwt(j -> j.claim("preferred_username", "teststudent45"))))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/student-fees/1/receipts")
                .with(jwt().jwt(j -> j.claim("preferred_username", "teststudent45"))))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/student-fees/1/penalties")
                .with(jwt().jwt(j -> j.claim("preferred_username", "teststudent45"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void studentFeeViewStillReadsByIdEndpointsUnaffected() throws Exception {
        // Regression check: existing staff callers with only the broader STUDENT_FEE_VIEW must be
        // completely unaffected by MY_FEE_VIEW's introduction.
        when(userPermissionService.getPermissions("feeclerk")).thenReturn(Set.of("STUDENT_FEE_VIEW"));
        Instant now = Instant.now();
        when(feeFinalizationService.getByStudentId(1L)).thenReturn(new StudentFeeAllocationResponse(
            1L, 1L, "John Doe", "CS2024001", 1L, "B.Tech Computer Science",
            new BigDecimal("200000.00"), new BigDecimal("10000.00"), "Merit scholarship",
            new BigDecimal("5000.00"), new BigDecimal("185000.00"), "FINALIZED",
            now, "admin", List.of(), now, now
        ));

        mockMvc.perform(get("/student-fees/1/semester-breakdown")
                .with(jwt().jwt(j -> j.claim("preferred_username", "feeclerk"))))
            .andExpect(status().isOk());
    }

    @Test
    void studentFeeViewOnlyCannotReadSelfServiceEndpoints() throws Exception {
        when(userPermissionService.getPermissions("feeclerk")).thenReturn(Set.of("STUDENT_FEE_VIEW"));

        mockMvc.perform(get("/student-fees/my/summary")
                .with(jwt().jwt(j -> j.claim("preferred_username", "feeclerk"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void noPermissionAtAllIsForbiddenEverywhere() throws Exception {
        when(userPermissionService.getPermissions("nobody")).thenReturn(Set.of());

        mockMvc.perform(get("/student-fees/my/summary")
                .with(jwt().jwt(j -> j.claim("preferred_username", "nobody"))))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/student-fees/1/semester-breakdown")
                .with(jwt().jwt(j -> j.claim("preferred_username", "nobody"))))
            .andExpect(status().isForbidden());
    }
}
