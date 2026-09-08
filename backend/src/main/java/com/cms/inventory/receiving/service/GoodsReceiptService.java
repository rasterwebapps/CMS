package com.cms.inventory.receiving.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.procurement.model.PurchaseOrder;
import com.cms.inventory.procurement.model.PurchaseOrderItem;
import com.cms.inventory.procurement.model.enums.PurchaseOrderStatus;
import com.cms.inventory.procurement.repository.PurchaseOrderItemRepository;
import com.cms.inventory.procurement.service.PurchaseOrderService;
import com.cms.inventory.receiving.dto.GoodsReceiptAddLineRequest;
import com.cms.inventory.receiving.dto.GoodsReceiptCreateRequest;
import com.cms.inventory.receiving.dto.GoodsReceiptLineResponse;
import com.cms.inventory.receiving.dto.GoodsReceiptResponse;
import com.cms.inventory.receiving.dto.ReceivablePurchaseOrderLineResponse;
import com.cms.inventory.receiving.model.GoodsReceipt;
import com.cms.inventory.receiving.model.GoodsReceiptLine;
import com.cms.inventory.receiving.model.enums.GoodsReceiptStatus;
import com.cms.inventory.receiving.repository.GoodsReceiptLineRepository;
import com.cms.inventory.receiving.repository.GoodsReceiptRepository;
import com.cms.inventory.stock.dto.StockMovementRequest;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.service.StockMovementService;

/**
 * Owns the Goods Receipt workflow — Phase 3's ("Receiving & Stock Movement") first slice. A
 * receipt is raised against one {@link PurchaseOrder} that has been sent to the supplier
 * ({@code ORDERED}/{@code IN_PROGRESS}/{@code PARTIALLY_COMPLETED}), built up as {@code DRAFT}
 * lines against that order's still-open lines, then confirmed — which posts one {@code RECEIPT}
 * stock movement per line through the existing {@code StockMovementService} (the sole owner of
 * every stock-ledger write) and rolls the parent order's status forward via {@code
 * PurchaseOrderService.recalculateReceiptProgress}. Over-receipt is blocked outright (0%
 * tolerance) rather than allowed with a warning — see the "Goods Receipt slice" decision-log
 * entry for why, and for the documented limitation around two DRAFT receipts open against the
 * same PO line at once.
 */
@Service
@Transactional(readOnly = true)
public class GoodsReceiptService {

    private static final List<PurchaseOrderStatus> RECEIVABLE_ORDER_STATUSES =
        List.of(PurchaseOrderStatus.ORDERED, PurchaseOrderStatus.IN_PROGRESS, PurchaseOrderStatus.PARTIALLY_COMPLETED);

    private final GoodsReceiptRepository receiptRepository;
    private final GoodsReceiptLineRepository lineRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final PurchaseOrderService purchaseOrderService;
    private final StockMovementService stockMovementService;

    public GoodsReceiptService(GoodsReceiptRepository receiptRepository,
                                GoodsReceiptLineRepository lineRepository,
                                PurchaseOrderItemRepository purchaseOrderItemRepository,
                                PurchaseOrderService purchaseOrderService,
                                StockMovementService stockMovementService) {
        this.receiptRepository = receiptRepository;
        this.lineRepository = lineRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.purchaseOrderService = purchaseOrderService;
        this.stockMovementService = stockMovementService;
    }

    @Transactional
    public GoodsReceiptResponse create(GoodsReceiptCreateRequest request, String createdBy) {
        PurchaseOrder order = purchaseOrderService.requireOrderEntity(request.purchaseOrderId());
        if (!RECEIVABLE_ORDER_STATUSES.contains(order.getStatus())) {
            throw new IllegalArgumentException(
                "Cannot raise a goods receipt against an order that is " + order.getStatus().name().toLowerCase(Locale.ROOT)
                    + " — it must have been sent to the supplier and not yet fully received or closed");
        }
        GoodsReceipt receipt = new GoodsReceipt();
        receipt.setPurchaseOrder(order);
        receipt.setStatus(GoodsReceiptStatus.DRAFT);
        receipt.setReceiptDate(request.receiptDate() != null ? request.receiptDate() : LocalDate.now());
        receipt.setNotes(trim(request.notes()));
        receipt.setCreatedBy(createdBy);
        receipt.setCreatedAt(Instant.now());
        receipt.setUpdatedAt(Instant.now());
        return toResponse(receiptRepository.save(receipt));
    }

    public Page<GoodsReceiptResponse> findPage(Long purchaseOrderId, String status, Pageable pageable) {
        Specification<GoodsReceipt> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (purchaseOrderId != null) predicate = cb.and(predicate, cb.equal(root.get("purchaseOrder").get("id"), purchaseOrderId));
            if (status != null && !status.isBlank()) predicate = cb.and(predicate, cb.equal(root.get("status"), parseStatus(status)));
            return predicate;
        };
        return receiptRepository.findAll(spec, pageable).map(r -> toResponse(r, false));
    }

    public GoodsReceiptResponse findById(Long id) {
        return toResponse(requireReceipt(id));
    }

    /** PO lines still open to receive against (orderedQty > receivedQty) — the add-line picker's pool. */
    public List<ReceivablePurchaseOrderLineResponse> findReceivableLines(Long purchaseOrderId) {
        return purchaseOrderItemRepository.findByPurchaseOrderIdOrderByIdAsc(purchaseOrderId).stream()
            .filter(item -> item.getReceivedQty().compareTo(item.getOrderedQty()) < 0)
            .map(item -> {
                Product product = item.getProduct();
                return new ReceivablePurchaseOrderLineResponse(
                    item.getId(), product.getId(), product.getProductCode(), product.getProductName(),
                    product.getBaseUom() != null ? product.getBaseUom().getCode() : null,
                    item.getOrderedQty(), item.getReceivedQty(), item.getOrderedQty().subtract(item.getReceivedQty()),
                    item.getUnitPrice());
            })
            .toList();
    }

    @Transactional
    public GoodsReceiptLineResponse addLine(Long receiptId, GoodsReceiptAddLineRequest request) {
        GoodsReceipt receipt = requireReceipt(receiptId);
        requireStatus(receipt, GoodsReceiptStatus.DRAFT, "add a line to");

        PurchaseOrderItem poItem = purchaseOrderItemRepository.findById(request.purchaseOrderItemId())
            .orElseThrow(() -> new ResourceNotFoundException("Purchase order line not found with id: " + request.purchaseOrderItemId()));
        if (!poItem.getPurchaseOrder().getId().equals(receipt.getPurchaseOrder().getId())) {
            throw new IllegalArgumentException("This purchase order line does not belong to this receipt's order");
        }

        BigDecimal alreadyInThisDraft = lineRepository.sumReceivedQtyInReceiptForItem(receiptId, poItem.getId());
        BigDecimal openQty = poItem.getOrderedQty().subtract(poItem.getReceivedQty()).subtract(alreadyInThisDraft);
        if (request.receivedQty().compareTo(openQty) > 0) {
            throw new IllegalArgumentException(
                "Received quantity (" + request.receivedQty() + ") exceeds what's still open on this order line (" + openQty + ")");
        }

        GoodsReceiptLine line = new GoodsReceiptLine();
        line.setGoodsReceipt(receipt);
        line.setPurchaseOrderItem(poItem);
        line.setReceivedQty(request.receivedQty());
        line.setUnitCost(request.unitCost() != null ? request.unitCost() : poItem.getUnitPrice());
        line.setBatchOrSerialNo(trim(request.batchOrSerialNo()));
        line.setExpiryDate(request.expiryDate());
        line.setNotes(trim(request.notes()));
        line = lineRepository.save(line);

        receipt.setUpdatedAt(Instant.now());
        receiptRepository.save(receipt);
        return toLineResponse(line);
    }

    @Transactional
    public void removeLine(Long receiptId, Long lineId) {
        GoodsReceipt receipt = requireReceipt(receiptId);
        requireStatus(receipt, GoodsReceiptStatus.DRAFT, "remove a line from");
        GoodsReceiptLine line = requireLine(receipt, lineId);
        lineRepository.delete(line);
        receipt.setUpdatedAt(Instant.now());
        receiptRepository.save(receipt);
    }

    @Transactional
    public GoodsReceiptResponse confirm(Long receiptId, String actor) {
        GoodsReceipt receipt = requireReceipt(receiptId);
        requireStatus(receipt, GoodsReceiptStatus.DRAFT, "confirm");
        List<GoodsReceiptLine> lines = lineRepository.findByGoodsReceiptIdOrderByIdAsc(receiptId);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Add at least one line before confirming this receipt");
        }

        InventoryLocation location = receipt.getPurchaseOrder().getLocation();
        for (GoodsReceiptLine line : lines) {
            PurchaseOrderItem poItem = line.getPurchaseOrderItem();
            // Fresh re-check at confirm time — the real guard against a race with another DRAFT
            // receipt against the same PO line that confirmed first (see the decision-log entry).
            BigDecimal openQty = poItem.getOrderedQty().subtract(poItem.getReceivedQty());
            if (line.getReceivedQty().compareTo(openQty) > 0) {
                throw new IllegalArgumentException(
                    "'" + poItem.getProduct().getProductName() + "' now has only " + openQty
                        + " still open on its order line (another receipt confirmed first) — reduce this line's quantity");
            }

            stockMovementService.recordMovement(new StockMovementRequest(
                poItem.getProduct().getId(), location.getId(),
                line.getBatchOrSerialNo(), line.getExpiryDate(),
                "RECEIPT", null, line.getReceivedQty(), line.getUnitCost(),
                "Goods Receipt #" + receipt.getId() + (line.getNotes() != null ? " — " + line.getNotes() : "")
            ), actor);

            poItem.setReceivedQty(poItem.getReceivedQty().add(line.getReceivedQty()));
            purchaseOrderItemRepository.save(poItem);
        }

        receipt.setStatus(GoodsReceiptStatus.CONFIRMED);
        receipt.setConfirmedBy(actor);
        receipt.setConfirmedAt(Instant.now());
        receipt.setUpdatedAt(Instant.now());
        receiptRepository.save(receipt);

        purchaseOrderService.recalculateReceiptProgress(receipt.getPurchaseOrder().getId());
        return toResponse(receipt);
    }

    private GoodsReceipt requireReceipt(Long id) {
        return receiptRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Goods receipt not found with id: " + id));
    }

    private GoodsReceiptLine requireLine(GoodsReceipt receipt, Long lineId) {
        GoodsReceiptLine line = lineRepository.findById(lineId)
            .orElseThrow(() -> new ResourceNotFoundException("Goods receipt line not found with id: " + lineId));
        if (!line.getGoodsReceipt().getId().equals(receipt.getId())) {
            throw new ResourceNotFoundException("Goods receipt line not found with id: " + lineId);
        }
        return line;
    }

    private void requireStatus(GoodsReceipt receipt, GoodsReceiptStatus required, String action) {
        if (receipt.getStatus() != required) {
            throw new IllegalArgumentException(
                "Cannot " + action + " a receipt that is " + receipt.getStatus().name().toLowerCase(Locale.ROOT)
                    + " — it must be " + required.name().toLowerCase(Locale.ROOT));
        }
    }

    private GoodsReceiptStatus parseStatus(String value) {
        try {
            return GoodsReceiptStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status '" + value + "'");
        }
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private GoodsReceiptResponse toResponse(GoodsReceipt receipt) {
        return toResponse(receipt, true);
    }

    private GoodsReceiptResponse toResponse(GoodsReceipt receipt, boolean includeLines) {
        List<GoodsReceiptLine> lines = lineRepository.findByGoodsReceiptIdOrderByIdAsc(receipt.getId());
        PurchaseOrder order = receipt.getPurchaseOrder();
        InventoryLocation location = order.getLocation();
        return new GoodsReceiptResponse(
            receipt.getId(), order.getId(), order.getSupplier().getSupplierName(),
            location.getId(), location.getVirtualName(),
            receipt.getStatus().name(), receipt.getReceiptDate(), receipt.getNotes(),
            receipt.getCreatedBy(), receipt.getCreatedAt(), receipt.getConfirmedBy(), receipt.getConfirmedAt(),
            lines.size(), includeLines ? lines.stream().map(this::toLineResponse).toList() : null);
    }

    private GoodsReceiptLineResponse toLineResponse(GoodsReceiptLine line) {
        PurchaseOrderItem poItem = line.getPurchaseOrderItem();
        Product product = poItem.getProduct();
        return new GoodsReceiptLineResponse(
            line.getId(), poItem.getId(), product.getId(), product.getProductCode(), product.getProductName(),
            product.getBaseUom() != null ? product.getBaseUom().getCode() : null,
            poItem.getOrderedQty(), poItem.getReceivedQty(), line.getReceivedQty(),
            line.getUnitCost(), line.getBatchOrSerialNo(), line.getExpiryDate(), line.getNotes());
    }
}
