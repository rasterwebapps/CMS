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
import com.cms.inventory.procurement.model.PurchaseOrderItem;
import com.cms.inventory.procurement.repository.PurchaseOrderItemRepository;
import com.cms.inventory.procurement.service.PurchaseOrderService;
import com.cms.inventory.receiving.dto.ReturnableGoodsReceiptLineResponse;
import com.cms.inventory.receiving.dto.SupplierReturnAddLineRequest;
import com.cms.inventory.receiving.dto.SupplierReturnCreateRequest;
import com.cms.inventory.receiving.dto.SupplierReturnLineResponse;
import com.cms.inventory.receiving.dto.SupplierReturnResponse;
import com.cms.inventory.receiving.model.GoodsReceipt;
import com.cms.inventory.receiving.model.GoodsReceiptLine;
import com.cms.inventory.receiving.model.SupplierReturn;
import com.cms.inventory.receiving.model.SupplierReturnLine;
import com.cms.inventory.receiving.model.enums.GoodsReceiptStatus;
import com.cms.inventory.receiving.model.enums.SupplierReturnReason;
import com.cms.inventory.receiving.model.enums.SupplierReturnStatus;
import com.cms.inventory.receiving.repository.GoodsReceiptLineRepository;
import com.cms.inventory.receiving.repository.GoodsReceiptRepository;
import com.cms.inventory.receiving.repository.SupplierReturnLineRepository;
import com.cms.inventory.receiving.repository.SupplierReturnRepository;
import com.cms.inventory.stock.dto.StockMovementRequest;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.service.StockMovementService;

/**
 * Owns the Return-to-Supplier workflow — Phase 3's ("Receiving & Stock Movement") third and
 * final slice. A return is raised against one {@code CONFIRMED} {@link GoodsReceipt} (only a
 * confirmed receipt has actually posted stock that can be returned), built up as {@code DRAFT}
 * lines against that receipt's still-returnable lines, then completed — which posts one
 * decreasing {@code RETURN} stock movement per line through {@code StockMovementService} (its
 * sole write path, untouched) and nets each line's quantity back out of its source {@code
 * PurchaseOrderItem.receivedQty}, calling {@code PurchaseOrderService.recalculateReceiptProgress}
 * so the parent order's own status reflects the true accepted quantity — including correctly
 * reverting a previously {@code COMPLETED} order to {@code PARTIALLY_COMPLETED}. See the "Return
 * to Supplier slice" decision-log entry.
 */
@Service
@Transactional(readOnly = true)
public class SupplierReturnService {

    private final SupplierReturnRepository returnRepository;
    private final SupplierReturnLineRepository lineRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final GoodsReceiptLineRepository goodsReceiptLineRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final PurchaseOrderService purchaseOrderService;
    private final StockMovementService stockMovementService;

    public SupplierReturnService(SupplierReturnRepository returnRepository,
                                  SupplierReturnLineRepository lineRepository,
                                  GoodsReceiptRepository goodsReceiptRepository,
                                  GoodsReceiptLineRepository goodsReceiptLineRepository,
                                  PurchaseOrderItemRepository purchaseOrderItemRepository,
                                  PurchaseOrderService purchaseOrderService,
                                  StockMovementService stockMovementService) {
        this.returnRepository = returnRepository;
        this.lineRepository = lineRepository;
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.goodsReceiptLineRepository = goodsReceiptLineRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.purchaseOrderService = purchaseOrderService;
        this.stockMovementService = stockMovementService;
    }

    @Transactional
    public SupplierReturnResponse create(SupplierReturnCreateRequest request, String createdBy) {
        GoodsReceipt receipt = goodsReceiptRepository.findById(request.goodsReceiptId())
            .orElseThrow(() -> new ResourceNotFoundException("Goods receipt not found with id: " + request.goodsReceiptId()));
        if (receipt.getStatus() != GoodsReceiptStatus.CONFIRMED) {
            throw new IllegalArgumentException("Cannot raise a return against a receipt that is not yet confirmed");
        }
        SupplierReturn ret = new SupplierReturn();
        ret.setGoodsReceipt(receipt);
        ret.setStatus(SupplierReturnStatus.DRAFT);
        ret.setReason(parseReasonOrNull(request.reason()));
        ret.setReturnDate(request.returnDate() != null ? request.returnDate() : LocalDate.now());
        ret.setNotes(trim(request.notes()));
        ret.setCreatedBy(createdBy);
        ret.setCreatedAt(Instant.now());
        ret.setUpdatedAt(Instant.now());
        return toResponse(returnRepository.save(ret));
    }

    public Page<SupplierReturnResponse> findPage(Long goodsReceiptId, String status, Pageable pageable) {
        Specification<SupplierReturn> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (goodsReceiptId != null) predicate = cb.and(predicate, cb.equal(root.get("goodsReceipt").get("id"), goodsReceiptId));
            if (status != null && !status.isBlank()) predicate = cb.and(predicate, cb.equal(root.get("status"), parseStatus(status)));
            return predicate;
        };
        return returnRepository.findAll(spec, pageable).map(r -> toResponse(r, false));
    }

    public SupplierReturnResponse findById(Long id) {
        return toResponse(requireReturn(id));
    }

    /** Confirmed receipt lines still holding a returnable quantity — the add-line picker's pool. */
    public List<ReturnableGoodsReceiptLineResponse> findReturnableLines(Long goodsReceiptId) {
        return goodsReceiptLineRepository.findByGoodsReceiptIdOrderByIdAsc(goodsReceiptId).stream()
            .map(line -> {
                BigDecimal alreadyReturned = lineRepository.sumReturnedQtyForReceiptLine(line.getId(), SupplierReturnStatus.COMPLETED);
                BigDecimal openQty = line.getReceivedQty().subtract(alreadyReturned);
                Product product = line.getPurchaseOrderItem().getProduct();
                return new ReturnableGoodsReceiptLineResponse(
                    line.getId(), product.getId(), product.getProductCode(), product.getProductName(),
                    product.getBaseUom() != null ? product.getBaseUom().getCode() : null,
                    line.getReceivedQty(), alreadyReturned, openQty);
            })
            .filter(l -> l.openQty().signum() > 0)
            .toList();
    }

    @Transactional
    public SupplierReturnLineResponse addLine(Long returnId, SupplierReturnAddLineRequest request) {
        SupplierReturn ret = requireReturn(returnId);
        requireStatus(ret, SupplierReturnStatus.DRAFT, "add a line to");

        GoodsReceiptLine receiptLine = goodsReceiptLineRepository.findById(request.goodsReceiptLineId())
            .orElseThrow(() -> new ResourceNotFoundException("Goods receipt line not found with id: " + request.goodsReceiptLineId()));
        if (!receiptLine.getGoodsReceipt().getId().equals(ret.getGoodsReceipt().getId())) {
            throw new IllegalArgumentException("This receipt line does not belong to this return's receipt");
        }

        BigDecimal alreadyReturned = lineRepository.sumReturnedQtyForReceiptLine(receiptLine.getId(), SupplierReturnStatus.COMPLETED);
        BigDecimal alreadyInThisDraft = lineRepository.sumReturnedQtyInReturnForLine(returnId, receiptLine.getId());
        BigDecimal openQty = receiptLine.getReceivedQty().subtract(alreadyReturned).subtract(alreadyInThisDraft);
        if (request.returnedQty().compareTo(openQty) > 0) {
            throw new IllegalArgumentException(
                "Returned quantity (" + request.returnedQty() + ") exceeds what's still returnable on this line (" + openQty + ")");
        }

        SupplierReturnLine line = new SupplierReturnLine();
        line.setSupplierReturn(ret);
        line.setGoodsReceiptLine(receiptLine);
        line.setReturnedQty(request.returnedQty());
        line.setNotes(trim(request.notes()));
        line = lineRepository.save(line);

        ret.setUpdatedAt(Instant.now());
        returnRepository.save(ret);
        return toLineResponse(line);
    }

    @Transactional
    public void removeLine(Long returnId, Long lineId) {
        SupplierReturn ret = requireReturn(returnId);
        requireStatus(ret, SupplierReturnStatus.DRAFT, "remove a line from");
        SupplierReturnLine line = requireLine(ret, lineId);
        lineRepository.delete(line);
        ret.setUpdatedAt(Instant.now());
        returnRepository.save(ret);
    }

    @Transactional
    public SupplierReturnResponse complete(Long returnId, String actor) {
        SupplierReturn ret = requireReturn(returnId);
        requireStatus(ret, SupplierReturnStatus.DRAFT, "complete");
        List<SupplierReturnLine> lines = lineRepository.findBySupplierReturnIdOrderByIdAsc(returnId);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Add at least one line before completing this return");
        }

        InventoryLocation location = ret.getGoodsReceipt().getPurchaseOrder().getLocation();
        for (SupplierReturnLine line : lines) {
            GoodsReceiptLine receiptLine = line.getGoodsReceiptLine();
            BigDecimal alreadyReturned = lineRepository.sumReturnedQtyForReceiptLine(receiptLine.getId(), SupplierReturnStatus.COMPLETED);
            BigDecimal openQty = receiptLine.getReceivedQty().subtract(alreadyReturned);
            if (line.getReturnedQty().compareTo(openQty) > 0) {
                throw new IllegalArgumentException(
                    "'" + receiptLine.getPurchaseOrderItem().getProduct().getProductName() + "' now has only " + openQty
                        + " still returnable (another return confirmed first) — reduce this line's quantity");
            }

            stockMovementService.recordMovement(new StockMovementRequest(
                receiptLine.getPurchaseOrderItem().getProduct().getId(), location.getId(), null, null,
                "RETURN", "DECREASE", line.getReturnedQty(), receiptLine.getUnitCost(),
                "Supplier Return #" + ret.getId() + (line.getNotes() != null ? " — " + line.getNotes() : "")
            ), actor);

            PurchaseOrderItem poItem = receiptLine.getPurchaseOrderItem();
            BigDecimal newReceivedQty = poItem.getReceivedQty().subtract(line.getReturnedQty());
            poItem.setReceivedQty(newReceivedQty.signum() < 0 ? BigDecimal.ZERO : newReceivedQty);
            purchaseOrderItemRepository.save(poItem);
        }

        ret.setStatus(SupplierReturnStatus.COMPLETED);
        ret.setCompletedBy(actor);
        ret.setCompletedAt(Instant.now());
        ret.setUpdatedAt(Instant.now());
        returnRepository.save(ret);

        purchaseOrderService.recalculateReceiptProgress(ret.getGoodsReceipt().getPurchaseOrder().getId());
        return toResponse(ret);
    }

    @Transactional
    public void cancel(Long returnId) {
        SupplierReturn ret = requireReturn(returnId);
        requireStatus(ret, SupplierReturnStatus.DRAFT, "cancel");
        ret.setStatus(SupplierReturnStatus.CANCELLED);
        ret.setUpdatedAt(Instant.now());
        returnRepository.save(ret);
    }

    private SupplierReturn requireReturn(Long id) {
        return returnRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier return not found with id: " + id));
    }

    private SupplierReturnLine requireLine(SupplierReturn ret, Long lineId) {
        SupplierReturnLine line = lineRepository.findById(lineId)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier return line not found with id: " + lineId));
        if (!line.getSupplierReturn().getId().equals(ret.getId())) {
            throw new ResourceNotFoundException("Supplier return line not found with id: " + lineId);
        }
        return line;
    }

    private void requireStatus(SupplierReturn ret, SupplierReturnStatus required, String action) {
        if (ret.getStatus() != required) {
            throw new IllegalArgumentException(
                "Cannot " + action + " a return that is " + ret.getStatus().name().toLowerCase(Locale.ROOT)
                    + " — it must be " + required.name().toLowerCase(Locale.ROOT));
        }
    }

    private SupplierReturnStatus parseStatus(String value) {
        try {
            return SupplierReturnStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status '" + value + "'");
        }
    }

    private SupplierReturnReason parseReasonOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return SupplierReturnReason.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid reason '" + value + "'");
        }
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private SupplierReturnResponse toResponse(SupplierReturn ret) {
        return toResponse(ret, true);
    }

    private SupplierReturnResponse toResponse(SupplierReturn ret, boolean includeLines) {
        List<SupplierReturnLine> lines = lineRepository.findBySupplierReturnIdOrderByIdAsc(ret.getId());
        GoodsReceipt receipt = ret.getGoodsReceipt();
        InventoryLocation location = receipt.getPurchaseOrder().getLocation();
        return new SupplierReturnResponse(
            ret.getId(), receipt.getId(), receipt.getPurchaseOrder().getSupplier().getSupplierName(),
            location.getId(), location.getVirtualName(),
            ret.getStatus().name(), ret.getReason() != null ? ret.getReason().name() : null,
            ret.getReturnDate(), ret.getNotes(),
            ret.getCreatedBy(), ret.getCreatedAt(), ret.getCompletedBy(), ret.getCompletedAt(),
            lines.size(), includeLines ? lines.stream().map(this::toLineResponse).toList() : null);
    }

    private SupplierReturnLineResponse toLineResponse(SupplierReturnLine line) {
        GoodsReceiptLine receiptLine = line.getGoodsReceiptLine();
        Product product = receiptLine.getPurchaseOrderItem().getProduct();
        BigDecimal alreadyReturned = lineRepository.sumReturnedQtyForReceiptLine(receiptLine.getId(), SupplierReturnStatus.COMPLETED);
        return new SupplierReturnLineResponse(
            line.getId(), receiptLine.getId(), product.getId(), product.getProductCode(), product.getProductName(),
            product.getBaseUom() != null ? product.getBaseUom().getCode() : null,
            receiptLine.getReceivedQty(), alreadyReturned, line.getReturnedQty(), line.getNotes());
    }
}
