package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.cms.config.RazorpayConfig;
import com.cms.dto.RazorpayOrderResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.RazorpayOrder;
import com.cms.model.Student;
import com.cms.repository.GuardianRepository;
import com.cms.repository.RazorpayOrderRepository;
import com.cms.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class RazorpayPaymentServiceTest {

    @Mock private RazorpayOrderGateway razorpayOrderGateway;
    @Mock private RazorpayOrderRepository razorpayOrderRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private GuardianRepository guardianRepository;
    @Mock private PaymentCollectionService paymentCollectionService;

    private RazorpayPaymentService service;
    private RazorpayConfig config;

    @BeforeEach
    void setUp() {
        config = new RazorpayConfig();
        config.setKeyId("rzp_test_key");
        config.setKeySecret("rzp_test_secret");
        config.setWebhookSecret("whsec");
        service = new RazorpayPaymentService(config, razorpayOrderGateway, razorpayOrderRepository,
            studentRepository, guardianRepository, paymentCollectionService);
    }

    private Student student(Long id) {
        Student s = new Student();
        s.setId(id);
        return s;
    }

    @Test
    void createOrder_succeedsWithinCollectibleOutstanding() {
        Student s = student(45L);
        when(studentRepository.findById(45L)).thenReturn(Optional.of(s));
        when(paymentCollectionService.getCollectibleOutstanding(s)).thenReturn(new BigDecimal("50000.00"));
        when(razorpayOrderGateway.createOrder(anyLong(), anyString(), anyString())).thenReturn("order_ABC123");

        RazorpayOrderResponse response = service.createOrder(45L, 1L, new BigDecimal("25000.00"));

        assertThat(response.razorpayOrderId()).isEqualTo("order_ABC123");
        assertThat(response.amount()).isEqualByComparingTo("25000.00");
        assertThat(response.razorpayKeyId()).isEqualTo("rzp_test_key");
        verify(razorpayOrderRepository).save(any(RazorpayOrder.class));
    }

    @Test
    void createOrder_rejectsAmountExceedingCollectibleOutstanding() {
        Student s = student(45L);
        when(studentRepository.findById(45L)).thenReturn(Optional.of(s));
        when(paymentCollectionService.getCollectibleOutstanding(s)).thenReturn(new BigDecimal("10000.00"));

        assertThatThrownBy(() -> service.createOrder(45L, 1L, new BigDecimal("25000.00")))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(razorpayOrderGateway, never()).createOrder(anyLong(), anyString(), anyString());
    }

    @Test
    void createOrder_rejectsWhenNothingCurrentlyDue() {
        Student s = student(45L);
        when(studentRepository.findById(45L)).thenReturn(Optional.of(s));
        when(paymentCollectionService.getCollectibleOutstanding(s)).thenReturn(BigDecimal.ZERO);

        assertThatThrownBy(() -> service.createOrder(45L, 1L, new BigDecimal("100.00")))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void createOrder_throwsWhenStudentNotFound() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createOrder(99L, 1L, new BigDecimal("100.00")))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createOrder_rejectsWhenGatewayNotConfigured() {
        config.setKeyId("");
        config.setKeySecret("");

        assertThatThrownBy(() -> service.createOrder(45L, 1L, new BigDecimal("100.00")))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        verify(studentRepository, never()).findById(anyLong());
    }
}
