package com.cms.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.domain.Specification;

import com.cms.model.PaymentReceipt;

public final class PaymentReceiptSpecification {
    private PaymentReceiptSpecification() {}

    public static Specification<PaymentReceipt> bySearch(String q) {
        return (root, query, cb) -> {
            String pattern = "%" + q.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("payerName")), pattern),
                cb.like(cb.lower(root.get("receiptNumber")), pattern),
                cb.like(cb.lower(cb.coalesce(root.get("payerIdentifier"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(root.get("admissionNumber"), "")), pattern)
            );
        };
    }

    public static Specification<PaymentReceipt> byPaymentMode(String mode) {
        return (root, query, cb) -> cb.equal(root.get("paymentMode"), mode);
    }

    public static Specification<PaymentReceipt> byPayerType(String payerType) {
        return (root, query, cb) -> cb.equal(root.get("payerType"), payerType);
    }

    public static Specification<PaymentReceipt> byDateFrom(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("paymentDate"), from);
    }

    public static Specification<PaymentReceipt> byDateTo(LocalDate to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("paymentDate"), to);
    }
}
