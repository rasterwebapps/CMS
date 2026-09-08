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
import com.cms.inventory.procurement.dto.PurchaseOrderAddLineRequest;
import com.cms.inventory.procurement.dto.PurchaseOrderCreateRequest;
import com.cms.inventory.procurement.dto.PurchaseOrderForceCloseRequest;
import com.cms.inventory.procurement.dto.PurchaseOrderItemResponse;
import com.cms.inventory.procurement.dto.PurchaseOrderResponse;
import com.cms.inventory.procurement.dto.PurchaseRequisitionItemResponse;
import com.cms.inventory.procurement.model.PurchaseOrder;
import com.cms.inventory.procurement.model.PurchaseOrderItem;
import com.cms.inventory.procurement.model.PurchaseRequisitionItem;
import com.cms.inventory.procurement.model.Supplier;
import com.cms.inventory.procurement.model.TaxRule;
import com.cms.inventory.procurement.model.enums.PurchaseOrderStatus;
import com.cms.inventory.procurement.model.enums.PurchaseRequisitionItemStatus;
import com.cms.inventory.procurement.repository.PurchaseOrderItemRepository;
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

    public PurchaseOrderService(PurchaseOrderRepository orderRepository,
                                 PurchaseOrderItemRepository itemRepository,
                                 PurchaseRequisitionItemRepository requisitionItemRepository,
                                 SupplierRepository supplierRepository,
                                 InventoryLocationRepository locationRepository,
                                 TaxRuleRepository taxRuleRepository,
                                 VendorProductMappingService vendorProductMappingService) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.requisitionItemRepository = requisitionItemRepository;
        this.supplierRepository = supplierRepository;
        this.locationRepository = locationRepository;
        this.taxRuleRepository = taxRuleRepository;
        this.vendorProductMappingService = vendorProductMappingService;
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
        BigDecimal orderedQty = request.orderedQty() != null ? request.orderedQty() : requisitionItem.getRequestedQty();
        if (orderedQty == null || orderedQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ordered quantity must be greater than zero");
        }

        BigDecimal unitPrice = request.unitPrice();
        if (unitPrice == null) {
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

        BigDecimal subtotal = unitPrice.multiply(orderedQty).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = taxRule != null
            ? subtotal.multiply(taxRule.getRatePercent()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setPurchaseOrder(order);
        item.setProduct(product);
        item.setPurchaseRequisitionItem(requisitionItem);
        item.setOrderedQty(orderedQty);
        item.setUnitPrice(unitPrice);
        item.setTaxRule(taxRule);
        item.setTaxAmount(taxAmount);
        item.setLineTotal(subtotal.add(taxAmount));
        item = itemRepository.save(item);

        requisitionItem.setStatus(PurchaseRequisitionItemStatus.ORDERED);
        requisitionItemRepository.save(requisitionItem);

        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
        return toItemResponse(item);
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
        return new PurchaseOrderItemResponse(
            item.getId(), product.getId(), product.getProductCode(), product.getProductName(),
            product.getBaseUom() != null ? product.getBaseUom().getCode() : null,
            item.getPurchaseRequisitionItem() != null ? item.getPurchaseRequisitionItem().getId() : null,
            item.getOrderedQty(), item.getUnitPrice(),
            taxRule != null ? taxRule.getId() : null, taxRule != null ? taxRule.getName() : null,
            item.getTaxAmount(), item.getLineTotal(), item.getReceivedQty());
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
