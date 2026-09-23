package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.cms.dto.PenaltyResponse;
import com.cms.dto.RazorpayOrderCreateRequest;
import com.cms.dto.RazorpayOrderResponse;
import com.cms.dto.StudentFeeAllocationResponse;
import com.cms.service.GuardianFeeSelfServiceService;
import com.cms.service.GuardianService;
import com.cms.service.RazorpayPaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = GuardianFeeController.class)
@AutoConfigureMockMvc(addFilters = false)
class GuardianFeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GuardianService guardianService;

    @MockitoBean
    private GuardianFeeSelfServiceService guardianFeeSelfServiceService;

    @MockitoBean
    private RazorpayPaymentService razorpayPaymentService;

    @Test
    void feeSummary_assertsWardOwnershipBeforeReturningData() throws Exception {
        StudentFeeAllocationResponse response = allocationResponse();
        when(guardianFeeSelfServiceService.findWardSummary(45L)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/guardian/wards/45/fee-summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.studentId").value(45));

        verify(guardianService).assertIsMyWard(eq(""), eq(45L));
    }

    @Test
    void feeSummary_returnsNoContentWhenNoAllocationYet() throws Exception {
        when(guardianFeeSelfServiceService.findWardSummary(45L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/guardian/wards/45/fee-summary"))
            .andExpect(status().isNoContent());
    }

    @Test
    void feeSummary_propagatesForbiddenWhenNotAWard() throws Exception {
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Student 99 is not one of your wards"))
            .when(guardianService).assertIsMyWard(eq(""), eq(99L));

        mockMvc.perform(get("/guardian/wards/99/fee-summary"))
            .andExpect(status().isForbidden());
    }

    @Test
    void feeReceipts_delegatesToService() throws Exception {
        when(guardianFeeSelfServiceService.findWardReceipts(45L)).thenReturn(List.of());

        mockMvc.perform(get("/guardian/wards/45/fee-receipts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());

        verify(guardianService).assertIsMyWard(eq(""), eq(45L));
    }

    @Test
    void feePenalties_returnsNoContentWhenEmpty() throws Exception {
        when(guardianFeeSelfServiceService.findWardPenalties(45L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/guardian/wards/45/fee-penalties"))
            .andExpect(status().isNoContent());
    }

    @Test
    void createFeePaymentOrder_resolvesCallersGuardianIdAndDelegates() throws Exception {
        when(guardianService.currentGuardianId("")).thenReturn(7L);
        RazorpayOrderResponse response = new RazorpayOrderResponse("order_ABC", new BigDecimal("25000.00"), "INR", "rzp_test_key");
        when(razorpayPaymentService.createOrder(45L, 7L, new BigDecimal("25000.00"))).thenReturn(response);

        mockMvc.perform(post("/guardian/wards/45/fee-payments/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RazorpayOrderCreateRequest(new BigDecimal("25000.00")))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.razorpayOrderId").value("order_ABC"));

        verify(guardianService).assertIsMyWard(eq(""), eq(45L));
    }

    @Test
    void createFeePaymentOrder_rejectsNonPositiveAmount() throws Exception {
        mockMvc.perform(post("/guardian/wards/45/fee-payments/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RazorpayOrderCreateRequest(BigDecimal.ZERO))))
            .andExpect(status().isBadRequest());
    }

    private StudentFeeAllocationResponse allocationResponse() {
        Instant now = Instant.now();
        return new StudentFeeAllocationResponse(
            1L, 45L, "Oviya Thangam", "GNM4-015", 1L, "GNM",
            new BigDecimal("200000.00"), new BigDecimal("10000.00"), null,
            BigDecimal.ZERO, new BigDecimal("190000.00"), "FINALIZED",
            now, "admin", List.of(), now, now
        );
    }
}
