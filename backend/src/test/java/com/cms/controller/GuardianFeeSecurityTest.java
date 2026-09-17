package com.cms.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.RazorpayOrderCreateRequest;
import com.cms.dto.RazorpayOrderResponse;
import com.cms.service.GuardianFeeSelfServiceService;
import com.cms.service.GuardianService;
import com.cms.service.RazorpayPaymentService;
import com.cms.service.UserPermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Full-context proof (real {@code SecurityConfig}, real {@code @PreAuthorize} enforcement) that
 *  {@code MY_WARD_FEE_VIEW} and {@code MY_WARD_FEE_PAY} stay properly separated -- per this
 *  project's operation-wise permission mapping hard gate, a guardian who can only view a ward's
 *  fee status must never be able to initiate a payment, and vice versa. Same pattern as
 *  {@link StudentFeeSelfServiceSecurityTest}: a {@code @WebMvcTest(addFilters=false)} slice never
 *  actually exercises {@code @PreAuthorize}, so only a real {@code @SpringBootTest} can catch a
 *  regression here. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(GuardianFeeSecurityTest.TestConfig.class)
class GuardianFeeSecurityTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserPermissionService userPermissionService;

    @MockBean
    private GuardianService guardianService;

    @MockBean
    private GuardianFeeSelfServiceService guardianFeeSelfServiceService;

    @MockBean
    private RazorpayPaymentService razorpayPaymentService;

    @Test
    void viewOnlyCanReadWardFeeDataButNotInitiatePayment() throws Exception {
        when(userPermissionService.getPermissions("parent1")).thenReturn(Set.of("MY_WARD_FEE_VIEW"));
        when(guardianFeeSelfServiceService.findWardSummary(45L)).thenReturn(Optional.empty());
        when(guardianFeeSelfServiceService.findWardReceipts(45L)).thenReturn(List.of());
        when(guardianFeeSelfServiceService.findWardPenalties(45L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/guardian/wards/45/fee-summary")
                .with(jwt().jwt(j -> j.claim("preferred_username", "parent1"))))
            .andExpect(status().isNoContent());

        mockMvc.perform(post("/guardian/wards/45/fee-payments/orders")
                .with(jwt().jwt(j -> j.claim("preferred_username", "parent1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RazorpayOrderCreateRequest(new BigDecimal("100.00")))))
            .andExpect(status().isForbidden());
    }

    @Test
    void payOnlyCanInitiatePaymentButNotReadWardFeeData() throws Exception {
        when(userPermissionService.getPermissions("parent2")).thenReturn(Set.of("MY_WARD_FEE_PAY"));
        when(guardianService.currentGuardianId("parent2")).thenReturn(3L);
        when(razorpayPaymentService.createOrder(45L, 3L, new BigDecimal("100.00")))
            .thenReturn(new RazorpayOrderResponse("order_XYZ", new BigDecimal("100.00"), "INR", "rzp_test_key"));

        mockMvc.perform(post("/guardian/wards/45/fee-payments/orders")
                .with(jwt().jwt(j -> j.claim("preferred_username", "parent2")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RazorpayOrderCreateRequest(new BigDecimal("100.00")))))
            .andExpect(status().isOk());

        mockMvc.perform(get("/guardian/wards/45/fee-summary")
                .with(jwt().jwt(j -> j.claim("preferred_username", "parent2"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void noPermissionAtAllIsForbiddenEverywhere() throws Exception {
        when(userPermissionService.getPermissions("nobody")).thenReturn(Set.of());

        mockMvc.perform(get("/guardian/wards/45/fee-summary")
                .with(jwt().jwt(j -> j.claim("preferred_username", "nobody"))))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/guardian/wards/45/fee-payments/orders")
                .with(jwt().jwt(j -> j.claim("preferred_username", "nobody")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RazorpayOrderCreateRequest(new BigDecimal("100.00")))))
            .andExpect(status().isForbidden());
    }
}
