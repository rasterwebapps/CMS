package com.cms.inventory.procurement.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.procurement.dto.PurchaseOrderAddLineRequest;
import com.cms.inventory.procurement.dto.PurchaseOrderCreateRequest;
import com.cms.inventory.procurement.dto.PurchaseOrderResponse;
import com.cms.inventory.procurement.dto.PurchaseRequisitionItemResponse;
import com.cms.inventory.procurement.dto.QuotationRequestAddLineRequest;
import com.cms.inventory.procurement.dto.QuotationRequestAddSupplierRequest;
import com.cms.inventory.procurement.dto.QuotationRequestAwardRequest;
import com.cms.inventory.procurement.dto.QuotationRequestCreateRequest;
import com.cms.inventory.procurement.dto.QuotationRequestLineResponse;
import com.cms.inventory.procurement.dto.QuotationRequestResponse;
import com.cms.inventory.procurement.dto.QuotationRequestSupplierResponse;
import com.cms.inventory.procurement.dto.QuotationResponseLineRequest;
import com.cms.inventory.procurement.dto.QuotationResponseLineResponse;
import com.cms.inventory.procurement.model.PurchaseRequisitionItem;
import com.cms.inventory.procurement.model.QuotationRequest;
import com.cms.inventory.procurement.model.QuotationRequestLine;
import com.cms.inventory.procurement.model.QuotationRequestSupplier;
import com.cms.inventory.procurement.model.QuotationResponseLine;
import com.cms.inventory.procurement.model.Supplier;
import com.cms.inventory.procurement.model.enums.PurchaseRequisitionItemStatus;
import com.cms.inventory.procurement.model.enums.QuotationRequestLineStatus;
import com.cms.inventory.procurement.model.enums.QuotationRequestStatus;
import com.cms.inventory.procurement.repository.PurchaseRequisitionItemRepository;
import com.cms.inventory.procurement.repository.QuotationRequestLineRepository;
import com.cms.inventory.procurement.repository.QuotationRequestRepository;
import com.cms.inventory.procurement.repository.QuotationRequestSupplierRepository;
import com.cms.inventory.procurement.repository.QuotationResponseLineRepository;
import com.cms.inventory.procurement.repository.SupplierRepository;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.repository.InventoryLocationRepository;

/**
 * Owns the Quotation Request (RFQ) workflow — an OPTIONAL step between Purchase Requisition and
 * Purchase Order. A header invites one or more {@link Supplier}s to quote against a set of {@code
 * APPROVED} {@link PurchaseRequisitionItem} lines for one location; staff key in each supplier's
 * quoted price once received (no outbound email/portal exists anywhere in this module — same
 * staff-entry posture as the rest of Purchasing & Suppliers). Each line is awarded independently
 * (per-line, not one winner for the whole request), so {@link #convertAwardedLines} can spawn more
 * than one Purchase Order, one per winning supplier — done by reusing {@link
 * PurchaseOrderService#create} and {@link PurchaseOrderService#addLine} rather than duplicating
 * their tax/UOM/variant resolution logic, since the underlying requisition item is still
 * {@code APPROVED} at conversion time (its status only ever moves to {@code ORDERED} once a real
 * Purchase Order line exists, exactly as it already does for the direct-to-PO path). See the
 * "Quotation Request slice" decision-log entry.
 */
@Service
@Transactional(readOnly = true)
public class QuotationRequestService {

    private final QuotationRequestRepository requestRepository;
    private final QuotationRequestLineRepository lineRepository;
    private final QuotationRequestSupplierRepository supplierLinkRepository;
    private final QuotationResponseLineRepository responseRepository;
    private final PurchaseRequisitionItemRepository requisitionItemRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryLocationRepository locationRepository;
    private final PurchaseOrderService purchaseOrderService;

    public QuotationRequestService(QuotationRequestRepository requestRepository,
                                    QuotationRequestLineRepository lineRepository,
                                    QuotationRequestSupplierRepository supplierLinkRepository,
                                    QuotationResponseLineRepository responseRepository,
                                    PurchaseRequisitionItemRepository requisitionItemRepository,
                                    SupplierRepository supplierRepository,
                                    InventoryLocationRepository locationRepository,
                                    PurchaseOrderService purchaseOrderService) {
        this.requestRepository = requestRepository;
        this.lineRepository = lineRepository;
        this.supplierLinkRepository = supplierLinkRepository;
        this.responseRepository = responseRepository;
        this.requisitionItemRepository = requisitionItemRepository;
        this.supplierRepository = supplierRepository;
        this.locationRepository = locationRepository;
        this.purchaseOrderService = purchaseOrderService;
    }

    @Transactional
    public QuotationRequestResponse create(QuotationRequestCreateRequest request, String createdBy) {
        InventoryLocation location = locationRepository.findById(request.locationId())
            .orElseThrow(() -> new ResourceNotFoundException("Inventory location not found with id: " + request.locationId()));

        QuotationRequest quotationRequest = new QuotationRequest();
        quotationRequest.setLocation(location);
        quotationRequest.setStatus(QuotationRequestStatus.DRAFT);
        quotationRequest.setRequestDate(request.requestDate() != null ? request.requestDate() : LocalDate.now());
        quotationRequest.setNotes(trim(request.notes()));
        quotationRequest.setCreatedBy(createdBy);
        quotationRequest.setCreatedAt(Instant.now());
        quotationRequest.setUpdatedAt(Instant.now());
        return toResponse(requestRepository.save(quotationRequest));
    }

    public Page<QuotationRequestResponse> findPage(Long locationId, String status, Pageable pageable) {
        Specification<QuotationRequest> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (locationId != null) predicate = cb.and(predicate, cb.equal(root.get("location").get("id"), locationId));
            if (status != null && !status.isBlank()) predicate = cb.and(predicate, cb.equal(root.get("status"), parseStatus(status)));
            return predicate;
        };
        return requestRepository.findAll(spec, pageable).map(r -> toResponse(r, false));
    }

    public QuotationRequestResponse findById(Long id) {
        return toResponse(requireRequest(id));
    }

    /**
     * {@code APPROVED}, not-yet-ordered requisition lines for a location that also aren't
     * already live on another (non-{@code REJECTED}) Quotation Request — the same pool {@code
     * PurchaseOrderService.findAvailableRequisitionLines} exposes for the direct-to-PO path,
     * with the same exclusion applied from the other direction.
     */
    public List<PurchaseRequisitionItemResponse> findAvailableRequisitionLines(Long locationId) {
        return purchaseOrderService.findAvailableRequisitionLines(locationId);
    }

    @Transactional
    public QuotationRequestLineResponse addLine(Long requestId, QuotationRequestAddLineRequest request) {
        QuotationRequest quotationRequest = requireRequest(requestId);
        requireStatus(quotationRequest, QuotationRequestStatus.DRAFT, "add a line to");

        PurchaseRequisitionItem requisitionItem = requisitionItemRepository.findById(request.purchaseRequisitionItemId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Purchase requisition line not found with id: " + request.purchaseRequisitionItemId()));
        if (requisitionItem.getStatus() != PurchaseRequisitionItemStatus.APPROVED) {
            throw new IllegalArgumentException("This requisition line is not approved and available to quote");
        }
        if (!requisitionItem.getPurchaseRequisition().getLocation().getId().equals(quotationRequest.getLocation().getId())) {
            throw new IllegalArgumentException("This requisition line belongs to a different location than the request");
        }
        if (lineRepository.existsByPurchaseRequisitionItemIdAndStatusNot(requisitionItem.getId(), QuotationRequestLineStatus.REJECTED)) {
            throw new IllegalArgumentException("This requisition line is already on another live Quotation Request");
        }

        QuotationRequestLine line = new QuotationRequestLine();
        line.setQuotationRequest(quotationRequest);
        line.setProduct(requisitionItem.getProduct());
        line.setPurchaseRequisitionItem(requisitionItem);
        line.setRequestedQty(request.requestedQty() != null ? request.requestedQty() : requisitionItem.getRequestedQty());
        line.setStatus(QuotationRequestLineStatus.PENDING);
        return toLineResponse(lineRepository.save(line));
    }

    @Transactional
    public void removeLine(Long requestId, Long lineId) {
        QuotationRequest quotationRequest = requireRequest(requestId);
        requireStatus(quotationRequest, QuotationRequestStatus.DRAFT, "remove a line from");
        QuotationRequestLine line = requireLine(quotationRequest, lineId);
        lineRepository.delete(line);
    }

    @Transactional
    public QuotationRequestSupplierResponse addSupplier(Long requestId, QuotationRequestAddSupplierRequest request) {
        QuotationRequest quotationRequest = requireRequest(requestId);
        requireStatus(quotationRequest, QuotationRequestStatus.DRAFT, "invite a supplier to");
        if (supplierLinkRepository.existsByQuotationRequestIdAndSupplierId(requestId, request.supplierId())) {
            throw new IllegalArgumentException("This supplier has already been invited");
        }
        Supplier supplier = supplierRepository.findById(request.supplierId())
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + request.supplierId()));

        QuotationRequestSupplier link = new QuotationRequestSupplier();
        link.setQuotationRequest(quotationRequest);
        link.setSupplier(supplier);
        link.setInvitedAt(Instant.now());
        return toSupplierResponse(supplierLinkRepository.save(link));
    }

    @Transactional
    public void removeSupplier(Long requestId, Long supplierId) {
        QuotationRequest quotationRequest = requireRequest(requestId);
        requireStatus(quotationRequest, QuotationRequestStatus.DRAFT, "remove a supplier from");
        supplierLinkRepository.deleteByQuotationRequestIdAndSupplierId(requestId, supplierId);
    }

    @Transactional
    public QuotationRequestResponse submit(Long requestId, String submittedBy) {
        QuotationRequest quotationRequest = requireRequest(requestId);
        requireStatus(quotationRequest, QuotationRequestStatus.DRAFT, "submit");
        List<QuotationRequestLine> lines = lineRepository.findByQuotationRequestIdOrderByIdAsc(requestId);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Add at least one line to the request before submitting");
        }
        List<QuotationRequestSupplier> suppliers = supplierLinkRepository.findByQuotationRequestIdOrderByIdAsc(requestId);
        if (suppliers.isEmpty()) {
            throw new IllegalArgumentException("Invite at least one supplier before submitting");
        }
        quotationRequest.setStatus(QuotationRequestStatus.SUBMITTED);
        quotationRequest.setSubmittedBy(submittedBy);
        quotationRequest.setSubmittedAt(Instant.now());
        quotationRequest.setUpdatedAt(Instant.now());
        requestRepository.save(quotationRequest);
        return toResponse(quotationRequest);
    }

    /**
     * Records (or updates, while the line is still {@code PENDING}) one invited supplier's quote
     * for one line. No minimum number of responses is required before {@link #award} — staff can
     * award off a single quote if that's all that came back.
     */
    @Transactional
    public QuotationResponseLineResponse recordResponse(Long requestId, Long lineId, Long supplierId,
                                                          QuotationResponseLineRequest request, String recordedBy) {
        QuotationRequest quotationRequest = requireRequest(requestId);
        requireStatus(quotationRequest, QuotationRequestStatus.SUBMITTED, "record a quote on");
        QuotationRequestLine line = requireLine(quotationRequest, lineId);
        if (line.getStatus() != QuotationRequestLineStatus.PENDING) {
            throw new IllegalArgumentException("This line has already been resolved — no further quotes can be recorded");
        }
        if (!supplierLinkRepository.existsByQuotationRequestIdAndSupplierId(requestId, supplierId)) {
            throw new IllegalArgumentException("This supplier was not invited to quote on this request");
        }
        Supplier supplier = supplierRepository.findById(supplierId)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + supplierId));

        QuotationResponseLine response = responseRepository.findByQuotationRequestLineIdAndSupplierId(lineId, supplierId)
            .orElseGet(() -> {
                QuotationResponseLine created = new QuotationResponseLine();
                created.setQuotationRequestLine(line);
                created.setSupplier(supplier);
                created.setRecordedAt(Instant.now());
                return created;
            });
        response.setQuotedUnitPrice(request.quotedUnitPrice());
        response.setQuotedLeadTimeDays(request.quotedLeadTimeDays());
        response.setNotes(trim(request.notes()));
        response.setRecordedBy(recordedBy);
        response.setUpdatedAt(Instant.now());
        return toResponseLineResponse(responseRepository.save(response));
    }

    @Transactional
    public QuotationRequestLineResponse award(Long requestId, Long lineId, QuotationRequestAwardRequest request, String awardedBy) {
        QuotationRequest quotationRequest = requireRequest(requestId);
        requireStatus(quotationRequest, QuotationRequestStatus.SUBMITTED, "award a line on");
        QuotationRequestLine line = requireLine(quotationRequest, lineId);
        if (line.getStatus() != QuotationRequestLineStatus.PENDING) {
            throw new IllegalArgumentException("This line has already been resolved");
        }
        QuotationResponseLine response = responseRepository.findById(request.responseLineId())
            .orElseThrow(() -> new ResourceNotFoundException("Quotation response not found with id: " + request.responseLineId()));
        if (!response.getQuotationRequestLine().getId().equals(line.getId())) {
            throw new IllegalArgumentException("This response was not recorded against this line");
        }
        line.setStatus(QuotationRequestLineStatus.AWARDED);
        line.setAwardedResponseLine(response);
        line.setAwardedBy(awardedBy);
        line.setAwardedAt(Instant.now());
        lineRepository.save(line);
        return toLineResponse(line);
    }

    @Transactional
    public QuotationRequestLineResponse rejectLine(Long requestId, Long lineId) {
        QuotationRequest quotationRequest = requireRequest(requestId);
        requireStatus(quotationRequest, QuotationRequestStatus.SUBMITTED, "reject a line on");
        QuotationRequestLine line = requireLine(quotationRequest, lineId);
        if (line.getStatus() != QuotationRequestLineStatus.PENDING) {
            throw new IllegalArgumentException("This line has already been resolved");
        }
        line.setStatus(QuotationRequestLineStatus.REJECTED);
        lineRepository.save(line);
        completeIfResolved(quotationRequest);
        return toLineResponse(line);
    }

    /**
     * Converts every {@code AWARDED}, not-yet-{@code ORDERED} line into a Purchase Order — one
     * per distinct winning supplier, since a request's lines are awarded independently and can
     * land on different suppliers. Reuses {@code PurchaseOrderService.create}/{@code addLine}
     * rather than re-deriving tax/UOM/variant logic; a product with an active variant will
     * surface {@code addLine}'s existing "select one for this line" error rather than silently
     * succeeding — variant selection during quotation-award conversion isn't built in this
     * slice, same limitation noted in the decision log.
     */
    @Transactional
    public List<PurchaseOrderResponse> convertAwardedLines(Long requestId, String actor) {
        QuotationRequest quotationRequest = requireRequest(requestId);
        List<QuotationRequestLine> awarded = lineRepository.findAwardedNotYetOrdered(requestId);
        if (awarded.isEmpty()) {
            throw new IllegalArgumentException("No awarded lines are ready to convert into a Purchase Order");
        }

        Map<Long, List<QuotationRequestLine>> bySupplier = new LinkedHashMap<>();
        for (QuotationRequestLine line : awarded) {
            Long supplierId = line.getAwardedResponseLine().getSupplier().getId();
            bySupplier.computeIfAbsent(supplierId, k -> new ArrayList<>()).add(line);
        }

        List<PurchaseOrderResponse> created = new ArrayList<>();
        for (Map.Entry<Long, List<QuotationRequestLine>> entry : bySupplier.entrySet()) {
            PurchaseOrderResponse order = purchaseOrderService.create(
                new PurchaseOrderCreateRequest(entry.getKey(), quotationRequest.getLocation().getId(),
                    LocalDate.now(), null, null, null,
                    "Raised from Quotation Request #" + quotationRequest.getId()),
                actor);
            for (QuotationRequestLine line : entry.getValue()) {
                purchaseOrderService.addLine(order.id(), new PurchaseOrderAddLineRequest(
                    line.getPurchaseRequisitionItem().getId(), null, line.getRequestedQty(), null,
                    line.getAwardedResponseLine().getQuotedUnitPrice(), null, line.getId()));
            }
            created.add(purchaseOrderService.findById(order.id()));
        }

        completeIfResolved(quotationRequest);
        return created;
    }

    @Transactional
    public void cancel(Long requestId) {
        QuotationRequest quotationRequest = requireRequest(requestId);
        requireStatus(quotationRequest, QuotationRequestStatus.DRAFT, "cancel");
        quotationRequest.setStatus(QuotationRequestStatus.CANCELLED);
        quotationRequest.setUpdatedAt(Instant.now());
        requestRepository.save(quotationRequest);
    }

    private void completeIfResolved(QuotationRequest quotationRequest) {
        if (quotationRequest.getStatus() != QuotationRequestStatus.SUBMITTED) return;
        boolean anyUnresolved = lineRepository.existsByQuotationRequestIdAndStatus(quotationRequest.getId(), QuotationRequestLineStatus.PENDING)
            || lineRepository.existsByQuotationRequestIdAndStatus(quotationRequest.getId(), QuotationRequestLineStatus.AWARDED);
        if (!anyUnresolved) {
            quotationRequest.setStatus(QuotationRequestStatus.COMPLETED);
            quotationRequest.setCompletedAt(Instant.now());
            quotationRequest.setUpdatedAt(Instant.now());
            requestRepository.save(quotationRequest);
        }
    }

    private QuotationRequest requireRequest(Long id) {
        return requestRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Quotation request not found with id: " + id));
    }

    private QuotationRequestLine requireLine(QuotationRequest quotationRequest, Long lineId) {
        QuotationRequestLine line = lineRepository.findById(lineId)
            .orElseThrow(() -> new ResourceNotFoundException("Quotation request line not found with id: " + lineId));
        if (!line.getQuotationRequest().getId().equals(quotationRequest.getId())) {
            throw new ResourceNotFoundException("Quotation request line not found with id: " + lineId);
        }
        return line;
    }

    private void requireStatus(QuotationRequest quotationRequest, QuotationRequestStatus required, String action) {
        if (quotationRequest.getStatus() != required) {
            throw new IllegalArgumentException(
                "Cannot " + action + " a quotation request that is " + quotationRequest.getStatus().name().toLowerCase(Locale.ROOT)
                    + " — it must be " + required.name().toLowerCase(Locale.ROOT));
        }
    }

    private QuotationRequestStatus parseStatus(String value) {
        try {
            return QuotationRequestStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status '" + value + "'");
        }
    }

    private QuotationRequestResponse toResponse(QuotationRequest quotationRequest) {
        return toResponse(quotationRequest, true);
    }

    private QuotationRequestResponse toResponse(QuotationRequest quotationRequest, boolean includeDetail) {
        List<QuotationRequestLine> lines = lineRepository.findByQuotationRequestIdOrderByIdAsc(quotationRequest.getId());
        long pending = lines.stream().filter(l -> l.getStatus() == QuotationRequestLineStatus.PENDING).count();
        InventoryLocation location = quotationRequest.getLocation();
        return new QuotationRequestResponse(
            quotationRequest.getId(), location.getId(), location.getVirtualName(),
            quotationRequest.getStatus().name(), quotationRequest.getRequestDate(), quotationRequest.getNotes(),
            quotationRequest.getCreatedBy(), quotationRequest.getCreatedAt(),
            quotationRequest.getSubmittedBy(), quotationRequest.getSubmittedAt(), quotationRequest.getCompletedAt(),
            lines.size(), (int) pending,
            includeDetail ? supplierLinkRepository.findByQuotationRequestIdOrderByIdAsc(quotationRequest.getId())
                .stream().map(this::toSupplierResponse).toList() : null,
            includeDetail ? lines.stream().map(this::toLineResponse).toList() : null);
    }

    private QuotationRequestSupplierResponse toSupplierResponse(QuotationRequestSupplier link) {
        return new QuotationRequestSupplierResponse(
            link.getId(), link.getSupplier().getId(), link.getSupplier().getSupplierName(), link.getInvitedAt());
    }

    private QuotationRequestLineResponse toLineResponse(QuotationRequestLine line) {
        Product product = line.getProduct();
        QuotationResponseLine awarded = line.getAwardedResponseLine();
        List<QuotationResponseLineResponse> responses = responseRepository
            .findByQuotationRequestLineIdOrderByIdAsc(line.getId())
            .stream().map(this::toResponseLineResponse).toList();
        return new QuotationRequestLineResponse(
            line.getId(), product.getId(), product.getProductCode(), product.getProductName(),
            product.getBaseUom() != null ? product.getBaseUom().getCode() : null,
            line.getPurchaseRequisitionItem().getId(), line.getRequestedQty(), line.getStatus().name(),
            awarded != null ? awarded.getId() : null,
            awarded != null ? awarded.getSupplier().getId() : null,
            awarded != null ? awarded.getSupplier().getSupplierName() : null,
            awarded != null ? awarded.getQuotedUnitPrice() : null,
            line.getAwardedBy(), line.getAwardedAt(), responses);
    }

    private QuotationResponseLineResponse toResponseLineResponse(QuotationResponseLine response) {
        return new QuotationResponseLineResponse(
            response.getId(), response.getSupplier().getId(), response.getSupplier().getSupplierName(),
            response.getQuotedUnitPrice(), response.getQuotedLeadTimeDays(), response.getNotes(),
            response.getRecordedBy(), response.getRecordedAt());
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
