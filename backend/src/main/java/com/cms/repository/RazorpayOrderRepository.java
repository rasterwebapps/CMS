package com.cms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.RazorpayOrder;

public interface RazorpayOrderRepository extends JpaRepository<RazorpayOrder, Long> {

    Optional<RazorpayOrder> findByRazorpayOrderId(String razorpayOrderId);

    boolean existsByRazorpayPaymentId(String razorpayPaymentId);
}
