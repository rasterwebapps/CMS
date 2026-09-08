package com.cms.inventory.reporting.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.inventory.procurement.model.enums.PurchaseOrderStatus;
import com.cms.inventory.receiving.model.PurchaseOrderCycleTimeProjection;
import com.cms.inventory.receiving.model.enums.GoodsReceiptStatus;
import com.cms.inventory.receiving.repository.GoodsReceiptRepository;
import com.cms.inventory.reporting.dto.PurchaseOrderCycleTimeReportResponse;
import com.cms.inventory.reporting.dto.PurchaseOrderCycleTimeSupplierRow;

/**
 * A follow-on to the PO Aging Report (OC-222) — that slice's own decision log explicitly
 * deferred Cycle-Time as "a genuinely different metric... real, separately-scoped work," not
 * silently dropped. "PO Cycle Time" (average days from an order being raised to being fully
 * received) is itself a standard ERP procurement KPI, same category as PO Aging's day-bucket
 * scheme — grouped by supplier, so a deployment can see which suppliers fulfil fastest. Only
 * {@code COMPLETED} orders are counted (an order that's still open, or was {@code
 * FORCE_CLOSED} before every line arrived, was never actually "fully received" and has no real
 * cycle time to report). See the "Purchase Order Cycle-Time Report slice" decision-log entry.
 */
@Service
@Transactional(readOnly = true)
public class PurchaseOrderCycleTimeReportService {

    private final GoodsReceiptRepository goodsReceiptRepository;

    public PurchaseOrderCycleTimeReportService(GoodsReceiptRepository goodsReceiptRepository) {
        this.goodsReceiptRepository = goodsReceiptRepository;
    }

    public PurchaseOrderCycleTimeReportResponse get() {
        List<PurchaseOrderCycleTimeProjection> orders = goodsReceiptRepository.findCompletedOrdersForCycleTime(
            PurchaseOrderStatus.COMPLETED, GoodsReceiptStatus.CONFIRMED);

        Map<Long, String> supplierNames = new LinkedHashMap<>();
        Map<Long, Long> cycleDaysSumBySupplier = new LinkedHashMap<>();
        Map<Long, Long> orderCountBySupplier = new LinkedHashMap<>();

        long grandTotalCycleDays = 0;
        long grandTotalOrders = 0;

        for (PurchaseOrderCycleTimeProjection order : orders) {
            if (order.getLastConfirmedAt() == null) continue;
            LocalDate receivedDate = order.getLastConfirmedAt().atZone(ZoneId.systemDefault()).toLocalDate();
            long cycleDays = Math.max(0, ChronoUnit.DAYS.between(order.getPoDate(), receivedDate));

            supplierNames.putIfAbsent(order.getSupplierId(), order.getSupplierName());
            cycleDaysSumBySupplier.merge(order.getSupplierId(), cycleDays, Long::sum);
            orderCountBySupplier.merge(order.getSupplierId(), 1L, Long::sum);

            grandTotalCycleDays += cycleDays;
            grandTotalOrders++;
        }

        List<PurchaseOrderCycleTimeSupplierRow> suppliers = new ArrayList<>();
        for (Map.Entry<Long, String> entry : supplierNames.entrySet()) {
            Long supplierId = entry.getKey();
            long count = orderCountBySupplier.get(supplierId);
            double average = count > 0 ? (double) cycleDaysSumBySupplier.get(supplierId) / count : 0;
            suppliers.add(new PurchaseOrderCycleTimeSupplierRow(supplierId, entry.getValue(), count, round1(average)));
        }
        suppliers.sort((a, b) -> a.supplierName().compareToIgnoreCase(b.supplierName()));

        double overallAverage = grandTotalOrders > 0 ? (double) grandTotalCycleDays / grandTotalOrders : 0;

        return new PurchaseOrderCycleTimeReportResponse(suppliers, grandTotalOrders, round1(overallAverage), Instant.now());
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
