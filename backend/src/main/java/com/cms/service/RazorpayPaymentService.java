package com.cms.service;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.cms.config.RazorpayConfig;
import com.cms.dto.RazorpayOrderResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.RazorpayOrder;
import com.cms.model.Student;
import com.cms.model.enums.RazorpayOrderStatus;
import com.cms.repository.GuardianRepository;
import com.cms.repository.RazorpayOrderRepository;
import com.cms.repository.StudentRepository;

/** Creates a Razorpay Order for a ward's fee payment (server-side only -- card/UPI details never
 *  touch this backend, per the hosted Checkout.js decision). The actual money movement is
 *  confirmed later, out of band, by {@link RazorpayWebhookService} once Razorpay calls back with a
 *  verified {@code payment.captured} event; this class never itself marks anything paid. */
@Service
public class RazorpayPaymentService {

    private final RazorpayConfig config;
    private final RazorpayOrderGateway razorpayOrderGateway;
    private final RazorpayOrderRepository razorpayOrderRepository;
    private final StudentRepository studentRepository;
    private final GuardianRepository guardianRepository;
    private final PaymentCollectionService paymentCollectionService;

    public RazorpayPaymentService(RazorpayConfig config,
                                   RazorpayOrderGateway razorpayOrderGateway,
                                   RazorpayOrderRepository razorpayOrderRepository,
                                   StudentRepository studentRepository,
                                   GuardianRepository guardianRepository,
                                   PaymentCollectionService paymentCollectionService) {
        this.config = config;
        this.razorpayOrderGateway = razorpayOrderGateway;
        this.razorpayOrderRepository = razorpayOrderRepository;
        this.studentRepository = studentRepository;
        this.guardianRepository = guardianRepository;
        this.paymentCollectionService = paymentCollectionService;
    }

    /** Caller must have already verified ward ownership (assertIsMyWard) before calling. Amount is
     *  capped at the ward's currently collectible outstanding -- the same figure
     *  {@link PaymentCollectionService#collectPayment} enforces for staff collection -- so a
     *  captured payment can never exceed what {@code collectPayment} will actually accept when the
     *  webhook later replays it. */
    @Transactional
    public RazorpayOrderResponse createOrder(Long studentId, Long guardianId, BigDecimal amount) {
        if (isBlank(config.getKeyId()) || isBlank(config.getKeySecret())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Online payment is not currently configured");
        }

        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        BigDecimal collectible = paymentCollectionService.getCollectibleOutstanding(student);
        if (collectible.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No fees are currently due for this ward");
        }
        if (amount.compareTo(collectible) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Payment amount (" + amount + ") exceeds the amount currently due: " + collectible);
        }

        long amountPaise = amount.multiply(BigDecimal.valueOf(100)).longValueExact();
        String receipt = "ward-" + studentId + "-" + System.currentTimeMillis();
        String razorpayOrderId = razorpayOrderGateway.createOrder(amountPaise, "INR", receipt);

        RazorpayOrder entity = new RazorpayOrder();
        entity.setRazorpayOrderId(razorpayOrderId);
        entity.setStudent(student);
        if (guardianId != null) {
            entity.setGuardian(guardianRepository.getReferenceById(guardianId));
        }
        entity.setAmount(amount);
        entity.setCurrency("INR");
        entity.setStatus(RazorpayOrderStatus.CREATED);
        razorpayOrderRepository.save(entity);

        return new RazorpayOrderResponse(razorpayOrderId, amount, "INR", config.getKeyId());
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
