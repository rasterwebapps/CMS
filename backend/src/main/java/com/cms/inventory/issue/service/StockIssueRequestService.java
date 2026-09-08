package com.cms.inventory.issue.service;

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
import com.cms.inventory.catalog.repository.ProductRepository;
import com.cms.inventory.issue.dto.StockIssueRequestAddLineRequest;
import com.cms.inventory.issue.dto.StockIssueRequestCreateRequest;
import com.cms.inventory.issue.dto.StockIssueRequestItemResponse;
import com.cms.inventory.issue.dto.StockIssueRequestResolutionRequest;
import com.cms.inventory.issue.dto.StockIssueRequestResponse;
import com.cms.inventory.issue.dto.StockIssueRequestReturnLineRequest;
import com.cms.inventory.issue.model.StockIssueRequest;
import com.cms.inventory.issue.model.StockIssueRequestItem;
import com.cms.inventory.issue.model.enums.StockIssueRequestItemStatus;
import com.cms.inventory.issue.model.enums.StockIssueRequestStatus;
import com.cms.inventory.issue.repository.StockIssueRequestItemRepository;
import com.cms.inventory.issue.repository.StockIssueRequestRepository;
import com.cms.inventory.stock.dto.StockMovementRequest;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.repository.InventoryLocationRepository;
import com.cms.inventory.stock.service.StockMovementService;

/**
 * Owns the Stock Issue Request workflow — Phase 4's ("Requests, Issues & Returns") first slice.
 * Mirrors {@code PurchaseRequisitionService}'s header/line shape (DRAFT -> SUBMITTED ->
 * COMPLETED/CANCELLED, per-line PENDING -> APPROVED/REJECTED, {@code CANCELLED} reachable only
 * from {@code DRAFT}) almost exactly — the closest in-repo precedent — except {@link
 * #approveLine} also posts a real {@code ISSUE} stock movement decreasing the issuing location's
 * balance through the existing {@code StockMovementService}, rather than only reaching a
 * terminal sign-off state with nothing downstream to pick it up. If the issuing location doesn't
 * have enough on hand, {@code recordMovement}'s existing negative-stock guard rejects the whole
 * approval — the line stays {@code PENDING}, no partial posting. See the "Stock Issue Request
 * slice" decision-log entry.
 */
@Service
@Transactional(readOnly = true)
public class StockIssueRequestService {

    private final StockIssueRequestRepository requestRepository;
    private final StockIssueRequestItemRepository lineRepository;
    private final InventoryLocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final StockMovementService stockMovementService;

    public StockIssueRequestService(StockIssueRequestRepository requestRepository,
                                     StockIssueRequestItemRepository lineRepository,
                                     InventoryLocationRepository locationRepository,
                                     ProductRepository productRepository,
                                     StockMovementService stockMovementService) {
        this.requestRepository = requestRepository;
        this.lineRepository = lineRepository;
        this.locationRepository = locationRepository;
        this.productRepository = productRepository;
        this.stockMovementService = stockMovementService;
    }

    @Transactional
    public StockIssueRequestResponse create(StockIssueRequestCreateRequest request, String createdBy) {
        if (request.requestingLocationId().equals(request.issuingLocationId())) {
            throw new IllegalArgumentException("Requesting and issuing locations must be different");
        }
        InventoryLocation requestingLocation = locationRepository.findById(request.requestingLocationId())
            .orElseThrow(() -> new ResourceNotFoundException("Inventory location not found with id: " + request.requestingLocationId()));
        InventoryLocation issuingLocation = locationRepository.findById(request.issuingLocationId())
            .orElseThrow(() -> new ResourceNotFoundException("Inventory location not found with id: " + request.issuingLocationId()));

        StockIssueRequest issueRequest = new StockIssueRequest();
        issueRequest.setRequestingLocation(requestingLocation);
        issueRequest.setIssuingLocation(issuingLocation);
        issueRequest.setStatus(StockIssueRequestStatus.DRAFT);
        issueRequest.setRequestDate(request.requestDate() != null ? request.requestDate() : LocalDate.now());
        issueRequest.setNotes(trim(request.notes()));
        issueRequest.setCreatedBy(createdBy);
        issueRequest.setCreatedAt(Instant.now());
        issueRequest.setUpdatedAt(Instant.now());
        return toResponse(requestRepository.save(issueRequest));
    }

    public Page<StockIssueRequestResponse> findPage(Long locationId, String status, Pageable pageable) {
        Specification<StockIssueRequest> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (locationId != null) {
                predicate = cb.and(predicate, cb.or(
                    cb.equal(root.get("requestingLocation").get("id"), locationId),
                    cb.equal(root.get("issuingLocation").get("id"), locationId)));
            }
            if (status != null && !status.isBlank()) predicate = cb.and(predicate, cb.equal(root.get("status"), parseStatus(status)));
            return predicate;
        };
        return requestRepository.findAll(spec, pageable).map(r -> toResponse(r, false));
    }

    public StockIssueRequestResponse findById(Long id) {
        return toResponse(requireRequest(id));
    }

    @Transactional
    public StockIssueRequestItemResponse addLine(Long requestId, StockIssueRequestAddLineRequest request) {
        StockIssueRequest issueRequest = requireRequest(requestId);
        requireStatus(issueRequest, StockIssueRequestStatus.DRAFT, "add a product to");
        if (lineRepository.existsByStockIssueRequestIdAndProductId(requestId, request.productId())) {
            throw new IllegalArgumentException("This product is already on the request");
        }
        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()));

        StockIssueRequestItem line = new StockIssueRequestItem();
        line.setStockIssueRequest(issueRequest);
        line.setProduct(product);
        line.setRequestedQty(request.requestedQty());
        line.setNotes(trim(request.notes()));
        return toLineResponse(lineRepository.save(line));
    }

    @Transactional
    public void removeLine(Long requestId, Long lineId) {
        StockIssueRequest issueRequest = requireRequest(requestId);
        requireStatus(issueRequest, StockIssueRequestStatus.DRAFT, "remove a line from");
        StockIssueRequestItem line = requireLine(issueRequest, lineId);
        lineRepository.delete(line);
    }

    @Transactional
    public StockIssueRequestResponse submit(Long requestId, String submittedBy) {
        StockIssueRequest issueRequest = requireRequest(requestId);
        requireStatus(issueRequest, StockIssueRequestStatus.DRAFT, "submit");
        List<StockIssueRequestItem> lines = lineRepository.findByStockIssueRequestIdOrderByIdAsc(requestId);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Add at least one product to the request before submitting");
        }
        issueRequest.setStatus(StockIssueRequestStatus.SUBMITTED);
        issueRequest.setSubmittedBy(submittedBy);
        issueRequest.setSubmittedAt(Instant.now());
        issueRequest.setUpdatedAt(Instant.now());
        requestRepository.save(issueRequest);
        return toResponse(issueRequest);
    }

    @Transactional
    public StockIssueRequestItemResponse approveLine(Long requestId, Long lineId, StockIssueRequestResolutionRequest request, String resolvedBy) {
        StockIssueRequest issueRequest = requireRequest(requestId);
        requireStatus(issueRequest, StockIssueRequestStatus.SUBMITTED, "approve a line on");
        StockIssueRequestItem line = requireLine(issueRequest, lineId);
        if (line.getStatus() != StockIssueRequestItemStatus.PENDING) {
            throw new IllegalArgumentException("This line has already been resolved");
        }

        stockMovementService.recordMovement(new StockMovementRequest(
            line.getProduct().getId(), issueRequest.getIssuingLocation().getId(), null, null,
            "ISSUE", null, line.getRequestedQty(), null,
            "Stock Issue Request #" + issueRequest.getId() + " to " + issueRequest.getRequestingLocation().getVirtualName()
        ), resolvedBy);

        line.setStatus(StockIssueRequestItemStatus.APPROVED);
        line.setResolvedBy(resolvedBy);
        line.setResolvedAt(Instant.now());
        line.setResolutionNotes(trim(request != null ? request.notes() : null));
        lineRepository.save(line);
        completeIfResolved(issueRequest);
        return toLineResponse(line);
    }

    @Transactional
    public StockIssueRequestItemResponse rejectLine(Long requestId, Long lineId, StockIssueRequestResolutionRequest request, String resolvedBy) {
        StockIssueRequest issueRequest = requireRequest(requestId);
        requireStatus(issueRequest, StockIssueRequestStatus.SUBMITTED, "reject a line on");
        StockIssueRequestItem line = requireLine(issueRequest, lineId);
        if (line.getStatus() != StockIssueRequestItemStatus.PENDING) {
            throw new IllegalArgumentException("This line has already been resolved");
        }
        line.setStatus(StockIssueRequestItemStatus.REJECTED);
        line.setResolvedBy(resolvedBy);
        line.setResolvedAt(Instant.now());
        line.setResolutionNotes(trim(request != null ? request.notes() : null));
        lineRepository.save(line);
        completeIfResolved(issueRequest);
        return toLineResponse(line);
    }

    @Transactional
    public void cancel(Long requestId) {
        StockIssueRequest issueRequest = requireRequest(requestId);
        requireStatus(issueRequest, StockIssueRequestStatus.DRAFT, "cancel");
        issueRequest.setStatus(StockIssueRequestStatus.CANCELLED);
        issueRequest.setUpdatedAt(Instant.now());
        requestRepository.save(issueRequest);
    }

    private void completeIfResolved(StockIssueRequest issueRequest) {
        if (issueRequest.getStatus() != StockIssueRequestStatus.SUBMITTED) return;
        boolean anyPending = lineRepository.existsByStockIssueRequestIdAndStatus(issueRequest.getId(), StockIssueRequestItemStatus.PENDING);
        if (!anyPending) {
            issueRequest.setStatus(StockIssueRequestStatus.COMPLETED);
            issueRequest.setCompletedAt(Instant.now());
            issueRequest.setUpdatedAt(Instant.now());
            requestRepository.save(issueRequest);
        }
    }

    private StockIssueRequest requireRequest(Long id) {
        return requestRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Stock issue request not found with id: " + id));
    }

    private StockIssueRequestItem requireLine(StockIssueRequest issueRequest, Long lineId) {
        StockIssueRequestItem line = lineRepository.findById(lineId)
            .orElseThrow(() -> new ResourceNotFoundException("Stock issue request line not found with id: " + lineId));
        if (!line.getStockIssueRequest().getId().equals(issueRequest.getId())) {
            throw new ResourceNotFoundException("Stock issue request line not found with id: " + lineId);
        }
        return line;
    }

    private void requireStatus(StockIssueRequest issueRequest, StockIssueRequestStatus required, String action) {
        if (issueRequest.getStatus() != required) {
            throw new IllegalArgumentException(
                "Cannot " + action + " a request that is " + issueRequest.getStatus().name().toLowerCase(Locale.ROOT)
                    + " — it must be " + required.name().toLowerCase(Locale.ROOT));
        }
    }

    private StockIssueRequestStatus parseStatus(String value) {
        try {
            return StockIssueRequestStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status '" + value + "'");
        }
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private StockIssueRequestResponse toResponse(StockIssueRequest issueRequest) {
        return toResponse(issueRequest, true);
    }

    private StockIssueRequestResponse toResponse(StockIssueRequest issueRequest, boolean includeLines) {
        List<StockIssueRequestItem> lines = lineRepository.findByStockIssueRequestIdOrderByIdAsc(issueRequest.getId());
        long pending = lines.stream().filter(l -> l.getStatus() == StockIssueRequestItemStatus.PENDING).count();
        InventoryLocation requestingLocation = issueRequest.getRequestingLocation();
        InventoryLocation issuingLocation = issueRequest.getIssuingLocation();
        return new StockIssueRequestResponse(
            issueRequest.getId(), requestingLocation.getId(), requestingLocation.getVirtualName(),
            issuingLocation.getId(), issuingLocation.getVirtualName(),
            issueRequest.getStatus().name(), issueRequest.getRequestDate(), issueRequest.getNotes(),
            issueRequest.getCreatedBy(), issueRequest.getCreatedAt(),
            issueRequest.getSubmittedBy(), issueRequest.getSubmittedAt(), issueRequest.getCompletedAt(),
            lines.size(), (int) pending,
            includeLines ? lines.stream().map(this::toLineResponse).toList() : null);
    }

    /**
     * Returns previously-issued stock back to this request's issuing location (Phase 4's
     * "Internal Return" concept) — only meaningful once the line is {@code APPROVED} (issued).
     * Posts an increasing {@code RETURN} movement (opposite direction from {@code
     * SupplierReturnService}'s own decreasing use of the same transaction type) and accumulates
     * the line's own {@code returnedQty} running total. See the "Internal Return slice"
     * decision-log entry.
     */
    @Transactional
    public StockIssueRequestItemResponse returnLine(Long requestId, Long lineId, StockIssueRequestReturnLineRequest request, String actor) {
        StockIssueRequest issueRequest = requireRequest(requestId);
        StockIssueRequestItem line = requireLine(issueRequest, lineId);
        if (line.getStatus() != StockIssueRequestItemStatus.APPROVED) {
            throw new IllegalArgumentException("Only an approved (issued) line can be returned");
        }
        BigDecimal openQty = line.getRequestedQty().subtract(line.getReturnedQty());
        if (request.returnedQty().compareTo(openQty) > 0) {
            throw new IllegalArgumentException(
                "Returned quantity (" + request.returnedQty() + ") exceeds what's still returnable on this line (" + openQty + ")");
        }

        stockMovementService.recordMovement(new StockMovementRequest(
            line.getProduct().getId(), issueRequest.getIssuingLocation().getId(), null, null,
            "RETURN", "INCREASE", request.returnedQty(), null,
            "Internal Return — Stock Issue Request #" + issueRequest.getId() + " line #" + line.getId()
                + (request.notes() != null ? " — " + request.notes() : "")
        ), actor);

        line.setReturnedQty(line.getReturnedQty().add(request.returnedQty()));
        lineRepository.save(line);
        return toLineResponse(line);
    }

    private StockIssueRequestItemResponse toLineResponse(StockIssueRequestItem line) {
        Product product = line.getProduct();
        return new StockIssueRequestItemResponse(
            line.getId(), product.getId(), product.getProductCode(), product.getProductName(),
            product.getBaseUom() != null ? product.getBaseUom().getCode() : null,
            line.getRequestedQty(), line.getStatus().name(),
            line.getResolvedBy(), line.getResolvedAt(), line.getResolutionNotes(),
            line.getReturnedQty(), line.getNotes());
    }
}
