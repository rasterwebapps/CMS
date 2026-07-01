package com.cms.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.data.jpa.domain.Specification;

import com.cms.model.FeeRefund;

public final class FeeRefundSpecification {
    private FeeRefundSpecification() {}

    public static Specification<FeeRefund> byStatus(String status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<FeeRefund> byEntityType(String entityType) {
        return (root, query, cb) -> cb.equal(root.get("entityType"), entityType);
    }

    public static Specification<FeeRefund> bySearch(String q) {
        return (root, query, cb) -> {
            String pattern = "%" + q.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(cb.coalesce(root.get("studentName"), "")), pattern),
                cb.like(cb.lower(root.get("originalReceiptNumber")), pattern),
                cb.like(cb.lower(cb.coalesce(root.get("rollNumber"), "")), pattern)
            );
        };
    }

    public static Specification<FeeRefund> byDateFrom(LocalDate from) {
        Instant start = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("requestedAt"), start);
    }

    public static Specification<FeeRefund> byDateTo(LocalDate to) {
        Instant end = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return (root, query, cb) -> cb.lessThan(root.get("requestedAt"), end);
    }
}
