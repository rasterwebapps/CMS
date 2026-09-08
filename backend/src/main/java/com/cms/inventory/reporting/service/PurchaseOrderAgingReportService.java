package com.cms.inventory.reporting.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.inventory.procurement.model.PurchaseOrderAgingProjection;
import com.cms.inventory.procurement.model.enums.PurchaseOrderStatus;
import com.cms.inventory.procurement.repository.PurchaseOrderItemRepository;
import com.cms.inventory.reporting.dto.PurchaseOrderAgingBucketRow;
import com.cms.inventory.reporting.dto.PurchaseOrderAgingReportResponse;

/**
 * Phase 8's third Reporting slice — the standard ERP "PO Aging" report: every still-open
 * Purchase Order bucketed by how many days have passed since it was raised (0-30 / 31-60 /
 * 61-90 / 90+), the near-universal default bucket scheme every major procurement/ERP system
 * ships with. Computed live from {@code poDate} vs. today — no stored snapshot. See the
 * "Purchase Order Aging Report slice" decision-log entry for why this was judged buildable
 * without further product input, revising the earlier, more conservative call in the Phase 8
 * breakdown (the same reconsideration already made for the Stock Valuation Report).
 */
@Service
@Transactional(readOnly = true)
public class PurchaseOrderAgingReportService {

    /** Not yet fully received — the same "still open" set the Dashboard uses for its own
     *  "Open Purchase Orders" tile. */
    private static final List<PurchaseOrderStatus> OPEN_STATUSES = List.of(
        PurchaseOrderStatus.PENDING, PurchaseOrderStatus.ORDERED,
        PurchaseOrderStatus.IN_PROGRESS, PurchaseOrderStatus.PARTIALLY_COMPLETED);

    private static final String BUCKET_0_30 = "0–30 days";
    private static final String BUCKET_31_60 = "31–60 days";
    private static final String BUCKET_61_90 = "61–90 days";
    private static final String BUCKET_90_PLUS = "90+ days";
    private static final List<String> BUCKET_ORDER = List.of(BUCKET_0_30, BUCKET_31_60, BUCKET_61_90, BUCKET_90_PLUS);

    private final PurchaseOrderItemRepository purchaseOrderItemRepository;

    public PurchaseOrderAgingReportService(PurchaseOrderItemRepository purchaseOrderItemRepository) {
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
    }

    public PurchaseOrderAgingReportResponse get() {
        List<PurchaseOrderAgingProjection> orders = purchaseOrderItemRepository.findOpenOrdersForAging(OPEN_STATUSES);
        LocalDate today = LocalDate.now();

        Map<String, Long> countByBucket = new LinkedHashMap<>();
        Map<String, BigDecimal> valueByBucket = new LinkedHashMap<>();
        for (String bucket : BUCKET_ORDER) {
            countByBucket.put(bucket, 0L);
            valueByBucket.put(bucket, BigDecimal.ZERO);
        }

        for (PurchaseOrderAgingProjection order : orders) {
            String bucket = bucketFor(ChronoUnit.DAYS.between(order.getPoDate(), today));
            countByBucket.merge(bucket, 1L, Long::sum);
            valueByBucket.merge(bucket, order.getTotalValue(), BigDecimal::add);
        }

        List<PurchaseOrderAgingBucketRow> buckets = BUCKET_ORDER.stream()
            .map(b -> new PurchaseOrderAgingBucketRow(b, countByBucket.get(b), valueByBucket.get(b)))
            .toList();

        long grandTotalOrderCount = orders.size();
        BigDecimal grandTotalValue = orders.stream()
            .map(PurchaseOrderAgingProjection::getTotalValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PurchaseOrderAgingReportResponse(buckets, grandTotalOrderCount, grandTotalValue, Instant.now());
    }

    private String bucketFor(long daysOpen) {
        if (daysOpen <= 30) return BUCKET_0_30;
        if (daysOpen <= 60) return BUCKET_31_60;
        if (daysOpen <= 90) return BUCKET_61_90;
        return BUCKET_90_PLUS;
    }
}
