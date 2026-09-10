package com.cms.inventory.procurement.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import com.cms.inventory.catalog.model.ProductUomLevel;
import com.cms.inventory.catalog.service.ProductUomChainService;
import com.cms.inventory.procurement.dto.PurchaseOrderAddLineRequest;
import com.cms.inventory.procurement.dto.PurchaseOrderCreateRequest;
import com.cms.inventory.procurement.dto.PurchaseOrderForceCloseRequest;
import com.cms.inventory.procurement.dto.PurchaseOrderItemResponse;
import com.cms.inventory.procurement.dto.PurchaseOrderResponse;
import com.cms.inventory.procurement.dto.PurchaseRequisitionItemResponse;
import com.cms.inventory.procurement.dto.TaxComponentResponse;
import com.cms.inventory.procurement.model.PurchaseOrder;
import com.cms.inventory.procurement.model.PurchaseOrderItem;
import com.cms.inventory.procurement.model.PurchaseOrderItemTaxComponent;
import com.cms.inventory.procurement.model.PurchaseRequisitionItem;
import com.cms.inventory.procurement.model.Supplier;
import com.cms.inventory.procurement.model.TaxRule;
import com.cms.inventory.procurement.model.TaxSubType;
import com.cms.inventory.procurement.model.enums.JurisdictionMode;
import com.cms.inventory.procurement.model.enums.PurchaseOrderStatus;
import com.cms.inventory.procurement.model.enums.PurchaseRequisitionItemStatus;
import com.cms.inventory.procurement.repository.PurchaseOrderItemRepository;
import com.cms.inventory.procurement.repository.PurchaseOrderItemTaxComponentRepository;
import com.cms.inventory.procurement.repository.PurchaseOrderRepository;
import com.cms.inventory.procurement.repository.PurchaseRequisitionItemRepository;
import com.cms.inventory.procurement.repository.SupplierRepository;
import com.cms.inventory.procurement.repository.TaxRuleRepository;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.repository.InventoryLocationRepository;

/**
 * Owns the Purchase Order workflow — the last step of this phase's procurement chain (Wanted List
 * → Purchase Requisition → Purchase Order). A PO's lines are picked up from {@code APPROVED}
 * {@link PurchaseRequisitionItem} rows for the chosen supplier's location, mirroring {@code
 * WantedListService.convert}'s "collective conversion" shape rather than duplicating it (the
 * source entity differs, so the logic isn't directly reusable). No approval gate — real
 * multi-level/parallel approval routing is Phase 6 scope, built once. {@code
 * IN_PROGRESS}/{@code PARTIALLY_COMPLETED}/{@code COMPLETED} are not driven from here yet — they
 * activate once Phase 3's Goods Receipt slice starts posting against {@code
 * PurchaseOrderItem.receivedQty}; until then a PO only ever reaches {@code ORDERED} or {@code
 * FORCE_CLOSED} from this service. See the "Purchase Order slice" decision-log entry.
 */
@Service
@Transactional(readOnly = true)
public class PurchaseOrderService {

    private final PurchaseOrderRepository orderRepository;
    private final PurchaseOrderItemRepository itemRepository;
    private final PurchaseRequisitionItemRepository requisitionItemRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryLocationRepository locationRepository;
    private final TaxRuleRepository taxRuleRepository;
    private final VendorProductMappingService vendorProductMappingService;
    private final JurisdictionService jurisdictionService;
    private final TaxSubTypeService taxSubTypeService;
    private final PurchaseOrderItemTaxComponentRepository taxComponentRepository;
    private final ProductUomChainService uomChainService;

    public PurchaseOrderService(PurchaseOrderRepository orderRepository,
                                 PurchaseOrderItemRepository itemRepository,
                                 PurchaseRequisitionItemRepository requisitionItemRepository,
                                 SupplierRepository supplierRepository,
                                 InventoryLocationRepository locationRepository,
                                 TaxRuleRepository taxRuleRepository,
                                 VendorProductMappingService vendorProductMappingService,
                                 JurisdictionService jurisdictionService,
                                 TaxSubTypeService taxSubTypeService,
                                 PurchaseOrderItemTaxComponentRepository taxComponentRepository,
                                 ProductUomChainService uomChainService) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.requisitionItemRepository = requisitionItemRepository;
        this.supplierRepository = supplierRepository;
        this.locationRepository = locationRepository;
        this.taxRuleRepository = taxRuleRepository;
        this.vendorProductMappingService = vendorProductMappingService;
        this.jurisdictionService = jurisdictionService;
        this.taxSubTypeService = taxSubTypeService;
        this.taxComponentRepository = taxComponentRepository;
        this.uomChainService = uomChainService;
    }

    @Transactional
    public PurchaseOrderResponse create(PurchaseOrderCreateRequest request, String createdBy) {
        Supplier supplier = supplierRepository.findById(request.supplierId())
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + request.supplierId()));
        InventoryLocation location = locationRepository.findById(request.locationId())
            .orElseThrow(() -> new ResourceNotFoundException("Inventory location not found with id: " + request.locationId()));

        PurchaseOrder order = new PurchaseOrder();
        order.setSupplier(supplier);
        order.setLocation(location);
        order.setStatus(PurchaseOrderStatus.PENDING);
        order.setPoDate(request.poDate() != null ? request.poDate() : LocalDate.now());
        order.setExpectedDeliveryDate(request.expectedDeliveryDate());
        order.setCurrencyCode(request.currencyCode() != null && !request.currencyCode().isBlank()
            ? request.currencyCode().trim().toUpperCase(Locale.ROOT) : "INR");
        order.setExchangeRate(request.exchangeRate());
        order.setNotes(trim(request.notes()));
        order.setCreatedBy(createdBy);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        return toResponse(orderRepository.save(order));
    }

    public Page<PurchaseOrderResponse> findPage(Long supplierId, Long locationId, String status, Pageable pageable) {
        Specification<PurchaseOrder> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (supplierId != null) predicate = cb.and(predicate, cb.equal(root.get("supplier").get("id"), supplierId));
            if (locationId != null) predicate = cb.and(predicate, cb.equal(root.get("location").get("id"), locationId));
            if (status != null && !status.isBlank()) predicate = cb.and(predicate, cb.equal(root.get("status"), parseStatus(status)));
            return predicate;
        };
        return orderRepository.findAll(spec, pageable).map(o -> toResponse(o, false));
    }

    public PurchaseOrderResponse findById(Long id) {
        return toResponse(requireOrder(id));
    }

    /** {@code APPROVED}, not-yet-ordered requisition lines for a location — the PO line picker's pool. */
    public List<PurchaseRequisitionItemResponse> findAvailableRequisitionLines(Long locationId) {
        return requisitionItemRepository
            .findByStatusAndPurchaseRequisition_Location_IdOrderByIdAsc(PurchaseRequisitionItemStatus.APPROVED, locationId)
            .stream()
            .map(this::toRequisitionLineResponse)
            .toList();
    }

    @Transactional
    public PurchaseOrderItemResponse addLine(Long orderId, PurchaseOrderAddLineRequest request) {
        PurchaseOrder order = requireOrder(orderId);
        requireStatus(order, PurchaseOrderStatus.PENDING, "add a line to");

        PurchaseRequisitionItem requisitionItem = requisitionItemRepository.findById(request.purchaseRequisitionItemId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Purchase requisition line not found with id: " + request.purchaseRequisitionItemId()));
        if (requisitionItem.getStatus() != PurchaseRequisitionItemStatus.APPROVED) {
            throw new IllegalArgumentException("This requisition line is not approved and available to order");
        }
        if (!requisitionItem.getPurchaseRequisition().getLocation().getId().equals(order.getLocation().getId())) {
            throw new IllegalArgumentException("This requisition line belongs to a different location than the order");
        }

        Product product = requisitionItem.getProduct();
        BigDecimal enteredQty = request.orderedQty() != null ? request.orderedQty() : requisitionItem.getRequestedQty();
        if (enteredQty == null || enteredQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ordered quantity must be greater than zero");
        }

        // uomLevel is the unit this line is actually being ordered in (must be on the product's
        // *active* chain — see ProductUomChainService). orderedQty always stays base-unit-only
        // (every open-qty/received-progress comparison elsewhere depends on that); enteredQty is
        // the raw as-typed number, kept for display/audit. No level chosen -> unchanged from
        // before this slice: the entered quantity already *is* the base-unit quantity.
        ProductUomLevel uomLevel = request.uomLevelId() != null
            ? uomChainService.requireActiveLevel(product.getId(), request.uomLevelId())
            : null;
        BigDecimal orderedQty = uomLevel != null
            ? enteredQty.multiply(uomLevel.getFactorToBase()).setScale(3, RoundingMode.HALF_UP)
            : enteredQty;

        BigDecimal unitPrice = request.unitPrice();
        if (unitPrice == null) {
            // Note: the resolved rate isn't unit-aware yet (VendorProductMapping.uomId is still a
            // display-only label, not wired into conversion) — it's assumed quoted for whichever
            // unit was actually picked here. Fine while a supplier only ever quotes one way; a
            // future slice should make the rate lookup unit-specific once mappings need it.
            VendorProductMappingService.EffectiveRate rate =
                vendorProductMappingService.resolveEffectiveRate(order.getSupplier().getId(), product.getId());
            unitPrice = rate != null ? rate.unitPrice() : null;
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                "No vendor rate is on file for '" + product.getProductName() + "' from this supplier — enter a unit price");
        }

        TaxRule taxRule = null;
        if (request.taxRuleId() != null) {
            taxRule = taxRuleRepository.findById(request.taxRuleId())
                .orElseThrow(() -> new ResourceNotFoundException("Tax rule not found with id: " + request.taxRuleId()));
        }

        // unitPrice prices one of whatever unit was entered (a carton's price, not one base-unit
        // item's) — so cost math uses enteredQty, never the base-converted orderedQty.
        BigDecimal subtotal = unitPrice.multiply(enteredQty).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = taxRule != null
            ? subtotal.multiply(taxRule.getRatePercent()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // Resolving jurisdiction and splitting into components only matters once a tax is
        // actually selected — a no-tax line stays exactly as before.
        JurisdictionMode jurisdictionMode = null;
        List<TaxSubType> components = List.of();
        if (taxRule != null) {
            jurisdictionMode = jurisdictionService.resolve(order.getSupplier());
            components = taxSubTypeService.requireCompleteSplit(taxRule.getId(), jurisdictionMode);
        }

        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setPurchaseOrder(order);
        item.setProduct(product);
        item.setPurchaseRequisitionItem(requisitionItem);
        item.setOrderedQty(orderedQty);
        item.setUomLevel(uomLevel);
        item.setEnteredQty(uomLevel != null ? enteredQty : null);
        item.setUnitPrice(unitPrice);
        item.setTaxRule(taxRule);
        item.setTaxAmount(taxAmount);
        item.setJurisdictionMode(jurisdictionMode);
        item.setLineTotal(subtotal.add(taxAmount));
        item = itemRepository.save(item);

        saveTaxComponents(item, components, taxAmount);

        requisitionItem.setStatus(PurchaseRequisitionItemStatus.ORDERED);
        requisitionItemRepository.save(requisitionItem);

        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
        return toItemResponse(item);
    }

    /**
     * Splits {@code taxAmount} across {@code components} in proportion to each one's {@code
     * splitPercent}, rounding every component but the last to 2dp and letting the last one absorb
     * the rounding remainder — guarantees the snapshotted component amounts always sum to exactly
     * {@code taxAmount}, never a cent more or less.
     */
    private void saveTaxComponents(PurchaseOrderItem item, List<TaxSubType> components, BigDecimal taxAmount) {
        if (components.isEmpty()) return;
        BigDecimal remaining = taxAmount;
        for (int i = 0; i < components.size(); i++) {
            TaxSubType subType = components.get(i);
            boolean isLast = i == components.size() - 1;
            BigDecimal amount = isLast
                ? remaining
                : taxAmount.multiply(subType.getSplitPercent()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            remaining = remaining.subtract(amount);

            PurchaseOrderItemTaxComponent component = new PurchaseOrderItemTaxComponent();
            component.setPurchaseOrderItem(item);
            component.setTaxSubType(subType);
            component.setComponentName(subType.getComponentName());
            component.setSplitPercentApplied(subType.getSplitPercent());
            component.setComponentAmount(amount);
            taxComponentRepository.save(component);
        }
    }

    @Transactional
    public void removeLine(Long orderId, Long lineId) {
        PurchaseOrder order = requireOrder(orderId);
        requireStatus(order, PurchaseOrderStatus.PENDING, "remove a line from");
        PurchaseOrderItem item = requireItem(order, lineId);

        PurchaseRequisitionItem requisitionItem = item.getPurchaseRequisitionItem();
        if (requisitionItem != null && requisitionItem.getStatus() == PurchaseRequisitionItemStatus.ORDERED) {
            requisitionItem.setStatus(PurchaseRequisitionItemStatus.APPROVED);
            requisitionItemRepository.save(requisitionItem);
        }
        itemRepository.delete(item);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
    }

    @Transactional
    public PurchaseOrderResponse order(Long orderId, String actor) {
        PurchaseOrder order = requireOrder(orderId);
        requireStatus(order, PurchaseOrderStatus.PENDING, "send");
        List<PurchaseOrderItem> lines = itemRepository.findByPurchaseOrderIdOrderByIdAsc(orderId);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Add at least one line to the order before sending it");
        }
        order.setStatus(PurchaseOrderStatus.ORDERED);
        order.setOrderedBy(actor);
        order.setOrderedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
        return toResponse(order);
    }

    /**
     * Closes the order before every line is fully received — reachable from any non-terminal
     * state, including {@code PENDING} (never sent), since this lifecycle has no separate
     * "cancel a not-yet-sent order" state of its own (see the decision-log entry).
     */
    @Transactional
    public PurchaseOrderResponse forceClose(Long orderId, PurchaseOrderForceCloseRequest request, String actor) {
        PurchaseOrder order = requireOrder(orderId);
        if (order.getStatus() == PurchaseOrderStatus.COMPLETED || order.getStatus() == PurchaseOrderStatus.FORCE_CLOSED) {
            throw new IllegalArgumentException("This order is already closed");
        }
        order.setStatus(PurchaseOrderStatus.FORCE_CLOSED);
        order.setForceClosedBy(actor);
        order.setForceClosedAt(Instant.now());
        order.setForceCloseReason(trim(request.reason()));
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
        return toResponse(order);
    }

    /**
     * Recomputes {@code IN_PROGRESS}/{@code PARTIALLY_COMPLETED}/{@code COMPLETED} from every
     * line's {@code receivedQty} vs. {@code orderedQty} — called by {@code GoodsReceiptService}
     * after it posts a confirmed receipt's stock movements and updates each line's received
     * quantity, keeping "PurchaseOrderService owns every write to PurchaseOrder" true the same
     * way {@code StockMovementService} owns every write to the stock ledger. A no-op once the
     * order is {@code FORCE_CLOSED} (the one genuinely manual, terminal state) — but {@code
     * COMPLETED} is NOT treated as terminal here: a confirmed {@code SupplierReturnService}
     * return lowers a line's {@code receivedQty} after the fact, and this method is called again
     * to let a previously-COMPLETED order correctly revert to {@code PARTIALLY_COMPLETED}/
     * {@code IN_PROGRESS} once its true accepted quantity drops. Leaves the order at {@code
     * ORDERED} if nothing has been received yet. See the "Goods Receipt slice" and "Return to
     * Supplier slice" decision-log entries.
     */
    @Transactional
    public void recalculateReceiptProgress(Long orderId) {
        PurchaseOrder order = requireOrder(orderId);
        if (order.getStatus() == PurchaseOrderStatus.FORCE_CLOSED) {
            return;
        }
        List<PurchaseOrderItem> lines = itemRepository.findByPurchaseOrderIdOrderByIdAsc(orderId);
        if (lines.isEmpty()) return;

        long fullyReceived = lines.stream().filter(l -> l.getReceivedQty().compareTo(l.getOrderedQty()) >= 0).count();
        long anyReceived = lines.stream().filter(l -> l.getReceivedQty().signum() > 0).count();

        PurchaseOrderStatus next;
        if (fullyReceived == lines.size()) {
            next = PurchaseOrderStatus.COMPLETED;
        } else if (fullyReceived > 0) {
            next = PurchaseOrderStatus.PARTIALLY_COMPLETED;
        } else if (anyReceived > 0) {
            next = PurchaseOrderStatus.IN_PROGRESS;
        } else {
            // Nothing received (or a return brought every line back to zero) — revert to ORDERED
            // rather than leaving a stale COMPLETED/IN_PROGRESS/PARTIALLY_COMPLETED status behind.
            next = PurchaseOrderStatus.ORDERED;
        }
        if (order.getStatus() != next) {
            order.setStatus(next);
            order.setUpdatedAt(Instant.now());
            orderRepository.save(order);
        }
    }

    /** Read-only access for {@code GoodsReceiptService} — validating a receipt is raised against a real, receivable order. */
    public PurchaseOrder requireOrderEntity(Long id) {
        return requireOrder(id);
    }

    private PurchaseOrder requireOrder(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id: " + id));
    }

    private PurchaseOrderItem requireItem(PurchaseOrder order, Long lineId) {
        PurchaseOrderItem item = itemRepository.findById(lineId)
            .orElseThrow(() -> new ResourceNotFoundException("Purchase order line not found with id: " + lineId));
        if (!item.getPurchaseOrder().getId().equals(order.getId())) {
            throw new ResourceNotFoundException("Purchase order line not found with id: " + lineId);
        }
        return item;
    }

    private void requireStatus(PurchaseOrder order, PurchaseOrderStatus required, String action) {
        if (order.getStatus() != required) {
            throw new IllegalArgumentException(
                "Cannot " + action + " an order that is " + order.getStatus().name().toLowerCase(Locale.ROOT)
                    + " — it must be " + required.name().toLowerCase(Locale.ROOT));
        }
    }

    private PurchaseOrderStatus parseStatus(String value) {
        try {
            return PurchaseOrderStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status '" + value + "'");
        }
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private PurchaseOrderResponse toResponse(PurchaseOrder order) {
        return toResponse(order, true);
    }

    private PurchaseOrderResponse toResponse(PurchaseOrder order, boolean includeLines) {
        List<PurchaseOrderItem> lines = itemRepository.findByPurchaseOrderIdOrderByIdAsc(order.getId());
        BigDecimal total = lines.stream().map(PurchaseOrderItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        Supplier supplier = order.getSupplier();
        InventoryLocation location = order.getLocation();
        return new PurchaseOrderResponse(
            order.getId(), supplier.getId(), supplier.getSupplierName(),
            location.getId(), location.getVirtualName(),
            order.getStatus().name(), order.getPoDate(), order.getExpectedDeliveryDate(),
            order.getCurrencyCode(), order.getExchangeRate(), order.getNotes(),
            order.getCreatedBy(), order.getCreatedAt(),
            order.getOrderedBy(), order.getOrderedAt(),
            order.getForceClosedBy(), order.getForceClosedAt(), order.getForceCloseReason(),
            lines.size(), total,
            includeLines ? lines.stream().map(this::toItemResponse).toList() : null);
    }

    private PurchaseOrderItemResponse toItemResponse(PurchaseOrderItem item) {
        Product product = item.getProduct();
        TaxRule taxRule = item.getTaxRule();
        List<TaxComponentResponse> components = taxComponentRepository
            .findByPurchaseOrderItem_IdOrderByIdAsc(item.getId())
            .stream()
            .map(c -> new TaxComponentResponse(c.getComponentName(), c.getSplitPercentApplied(), c.getComponentAmount()))
            .toList();
        ProductUomLevel uomLevel = item.getUomLevel();
        return new PurchaseOrderItemResponse(
            item.getId(), product.getId(), product.getProductCode(), product.getProductName(),
            product.getBaseUom() != null ? product.getBaseUom().getCode() : null,
            item.getPurchaseRequisitionItem() != null ? item.getPurchaseRequisitionItem().getId() : null,
            item.getOrderedQty(),
            uomLevel != null ? uomLevel.getId() : null,
            uomLevel != null ? uomLevel.getUom().getCode() : null,
            item.getEnteredQty(),
            item.getUnitPrice(),
            taxRule != null ? taxRule.getId() : null, taxRule != null ? taxRule.getName() : null,
            item.getTaxAmount(), item.getJurisdictionMode() != null ? item.getJurisdictionMode().name() : null,
            components, item.getLineTotal(), item.getReceivedQty());
    }

    private PurchaseRequisitionItemResponse toRequisitionLineResponse(PurchaseRequisitionItem line) {
        Product product = line.getProduct();
        return new PurchaseRequisitionItemResponse(
            line.getId(), product.getId(), product.getProductCode(), product.getProductName(),
            product.getBaseUom() != null ? product.getBaseUom().getCode() : null,
            line.getRequestedQty(), line.getStatus().name(),
            line.getResolvedBy(), line.getResolvedAt(), line.getResolutionNotes(), line.getNotes());
    }
}
