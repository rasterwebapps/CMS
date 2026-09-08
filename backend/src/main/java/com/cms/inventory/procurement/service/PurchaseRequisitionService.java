package com.cms.inventory.procurement.service;

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
import com.cms.inventory.procurement.dto.PurchaseRequisitionAddLineRequest;
import com.cms.inventory.procurement.dto.PurchaseRequisitionCreateRequest;
import com.cms.inventory.procurement.dto.PurchaseRequisitionItemResponse;
import com.cms.inventory.procurement.dto.PurchaseRequisitionResolutionRequest;
import com.cms.inventory.procurement.dto.PurchaseRequisitionResponse;
import com.cms.inventory.procurement.model.PurchaseRequisition;
import com.cms.inventory.procurement.model.PurchaseRequisitionItem;
import com.cms.inventory.procurement.model.enums.PurchaseRequisitionItemStatus;
import com.cms.inventory.procurement.model.enums.PurchaseRequisitionStatus;
import com.cms.inventory.procurement.repository.PurchaseRequisitionItemRepository;
import com.cms.inventory.procurement.repository.PurchaseRequisitionRepository;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.repository.InventoryLocationRepository;

/**
 * Owns the Purchase Requisition workflow — a department/location's request to purchase specific
 * products, approved or rejected per line. Independent of any reorder-level computation (a
 * separate, not-yet-built "Wanted List" concept) and of Purchase Order itself (also not yet
 * built) — an approved line is simply ready to be picked up into a PO once that slice exists; this
 * service does not create one. Mirrors {@code CycleCountService}'s shape closely. See the
 * "Purchase Requisition slice" decision-log entry.
 */
@Service
@Transactional(readOnly = true)
public class PurchaseRequisitionService {

    private final PurchaseRequisitionRepository requisitionRepository;
    private final PurchaseRequisitionItemRepository lineRepository;
    private final InventoryLocationRepository locationRepository;
    private final ProductRepository productRepository;

    public PurchaseRequisitionService(PurchaseRequisitionRepository requisitionRepository,
                                       PurchaseRequisitionItemRepository lineRepository,
                                       InventoryLocationRepository locationRepository,
                                       ProductRepository productRepository) {
        this.requisitionRepository = requisitionRepository;
        this.lineRepository = lineRepository;
        this.locationRepository = locationRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public PurchaseRequisitionResponse create(PurchaseRequisitionCreateRequest request, String createdBy) {
        InventoryLocation location = locationRepository.findById(request.locationId())
            .orElseThrow(() -> new ResourceNotFoundException("Inventory location not found with id: " + request.locationId()));

        PurchaseRequisition requisition = new PurchaseRequisition();
        requisition.setLocation(location);
        requisition.setStatus(PurchaseRequisitionStatus.DRAFT);
        requisition.setRequisitionDate(request.requisitionDate() != null ? request.requisitionDate() : LocalDate.now());
        requisition.setNotes(trim(request.notes()));
        requisition.setCreatedBy(createdBy);
        requisition.setCreatedAt(Instant.now());
        requisition.setUpdatedAt(Instant.now());
        return toResponse(requisitionRepository.save(requisition));
    }

    public Page<PurchaseRequisitionResponse> findPage(Long locationId, String status, Pageable pageable) {
        Specification<PurchaseRequisition> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (locationId != null) predicate = cb.and(predicate, cb.equal(root.get("location").get("id"), locationId));
            if (status != null && !status.isBlank()) predicate = cb.and(predicate, cb.equal(root.get("status"), parseStatus(status)));
            return predicate;
        };
        return requisitionRepository.findAll(spec, pageable).map(r -> toResponse(r, false));
    }

    public PurchaseRequisitionResponse findById(Long id) {
        return toResponse(requireRequisition(id));
    }

    @Transactional
    public PurchaseRequisitionItemResponse addLine(Long requisitionId, PurchaseRequisitionAddLineRequest request) {
        PurchaseRequisition requisition = requireRequisition(requisitionId);
        requireStatus(requisition, PurchaseRequisitionStatus.DRAFT, "add a product to");
        if (lineRepository.existsByPurchaseRequisitionIdAndProductId(requisitionId, request.productId())) {
            throw new IllegalArgumentException("This product is already on the requisition");
        }
        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()));

        PurchaseRequisitionItem line = new PurchaseRequisitionItem();
        line.setPurchaseRequisition(requisition);
        line.setProduct(product);
        line.setRequestedQty(request.requestedQty());
        line.setNotes(trim(request.notes()));
        return toLineResponse(lineRepository.save(line));
    }

    @Transactional
    public void removeLine(Long requisitionId, Long lineId) {
        PurchaseRequisition requisition = requireRequisition(requisitionId);
        requireStatus(requisition, PurchaseRequisitionStatus.DRAFT, "remove a line from");
        PurchaseRequisitionItem line = requireLine(requisition, lineId);
        lineRepository.delete(line);
    }

    @Transactional
    public PurchaseRequisitionResponse submit(Long requisitionId, String submittedBy) {
        PurchaseRequisition requisition = requireRequisition(requisitionId);
        requireStatus(requisition, PurchaseRequisitionStatus.DRAFT, "submit");
        List<PurchaseRequisitionItem> lines = lineRepository.findByPurchaseRequisitionIdOrderByIdAsc(requisitionId);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Add at least one product to the requisition before submitting");
        }
        requisition.setStatus(PurchaseRequisitionStatus.SUBMITTED);
        requisition.setSubmittedBy(submittedBy);
        requisition.setSubmittedAt(Instant.now());
        requisition.setUpdatedAt(Instant.now());
        requisitionRepository.save(requisition);
        return toResponse(requisition);
    }

    @Transactional
    public PurchaseRequisitionItemResponse approveLine(Long requisitionId, Long lineId, PurchaseRequisitionResolutionRequest request, String resolvedBy) {
        PurchaseRequisition requisition = requireRequisition(requisitionId);
        requireStatus(requisition, PurchaseRequisitionStatus.SUBMITTED, "approve a line on");
        PurchaseRequisitionItem line = requireLine(requisition, lineId);
        if (line.getStatus() != PurchaseRequisitionItemStatus.PENDING) {
            throw new IllegalArgumentException("This line has already been resolved");
        }
        line.setStatus(PurchaseRequisitionItemStatus.APPROVED);
        line.setResolvedBy(resolvedBy);
        line.setResolvedAt(Instant.now());
        line.setResolutionNotes(trim(request.notes()));
        lineRepository.save(line);
        completeIfResolved(requisition);
        return toLineResponse(line);
    }

    @Transactional
    public PurchaseRequisitionItemResponse rejectLine(Long requisitionId, Long lineId, PurchaseRequisitionResolutionRequest request, String resolvedBy) {
        PurchaseRequisition requisition = requireRequisition(requisitionId);
        requireStatus(requisition, PurchaseRequisitionStatus.SUBMITTED, "reject a line on");
        PurchaseRequisitionItem line = requireLine(requisition, lineId);
        if (line.getStatus() != PurchaseRequisitionItemStatus.PENDING) {
            throw new IllegalArgumentException("This line has already been resolved");
        }
        line.setStatus(PurchaseRequisitionItemStatus.REJECTED);
        line.setResolvedBy(resolvedBy);
        line.setResolvedAt(Instant.now());
        line.setResolutionNotes(trim(request.notes()));
        lineRepository.save(line);
        completeIfResolved(requisition);
        return toLineResponse(line);
    }

    @Transactional
    public void cancel(Long requisitionId) {
        PurchaseRequisition requisition = requireRequisition(requisitionId);
        requireStatus(requisition, PurchaseRequisitionStatus.DRAFT, "cancel");
        requisition.setStatus(PurchaseRequisitionStatus.CANCELLED);
        requisition.setUpdatedAt(Instant.now());
        requisitionRepository.save(requisition);
    }

    private void completeIfResolved(PurchaseRequisition requisition) {
        if (requisition.getStatus() != PurchaseRequisitionStatus.SUBMITTED) return;
        boolean anyPending = lineRepository.existsByPurchaseRequisitionIdAndStatus(requisition.getId(), PurchaseRequisitionItemStatus.PENDING);
        if (!anyPending) {
            requisition.setStatus(PurchaseRequisitionStatus.COMPLETED);
            requisition.setCompletedAt(Instant.now());
            requisition.setUpdatedAt(Instant.now());
            requisitionRepository.save(requisition);
        }
    }

    private PurchaseRequisition requireRequisition(Long id) {
        return requisitionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Purchase requisition not found with id: " + id));
    }

    private PurchaseRequisitionItem requireLine(PurchaseRequisition requisition, Long lineId) {
        PurchaseRequisitionItem line = lineRepository.findById(lineId)
            .orElseThrow(() -> new ResourceNotFoundException("Purchase requisition line not found with id: " + lineId));
        if (!line.getPurchaseRequisition().getId().equals(requisition.getId())) {
            throw new ResourceNotFoundException("Purchase requisition line not found with id: " + lineId);
        }
        return line;
    }

    private void requireStatus(PurchaseRequisition requisition, PurchaseRequisitionStatus required, String action) {
        if (requisition.getStatus() != required) {
            throw new IllegalArgumentException(
                "Cannot " + action + " a requisition that is " + requisition.getStatus().name().toLowerCase(Locale.ROOT)
                    + " — it must be " + required.name().toLowerCase(Locale.ROOT));
        }
    }

    private PurchaseRequisitionStatus parseStatus(String value) {
        try {
            return PurchaseRequisitionStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status '" + value + "'");
        }
    }

    private PurchaseRequisitionResponse toResponse(PurchaseRequisition requisition) {
        return toResponse(requisition, true);
    }

    private PurchaseRequisitionResponse toResponse(PurchaseRequisition requisition, boolean includeLines) {
        List<PurchaseRequisitionItem> lines = lineRepository.findByPurchaseRequisitionIdOrderByIdAsc(requisition.getId());
        long pending = lines.stream().filter(l -> l.getStatus() == PurchaseRequisitionItemStatus.PENDING).count();
        InventoryLocation location = requisition.getLocation();
        return new PurchaseRequisitionResponse(
            requisition.getId(), location.getId(), location.getVirtualName(),
            requisition.getStatus().name(), requisition.getRequisitionDate(), requisition.getNotes(),
            requisition.getCreatedBy(), requisition.getCreatedAt(),
            requisition.getSubmittedBy(), requisition.getSubmittedAt(), requisition.getCompletedAt(),
            lines.size(), (int) pending,
            includeLines ? lines.stream().map(this::toLineResponse).toList() : null);
    }

    private PurchaseRequisitionItemResponse toLineResponse(PurchaseRequisitionItem line) {
        Product product = line.getProduct();
        return new PurchaseRequisitionItemResponse(
            line.getId(), product.getId(), product.getProductCode(), product.getProductName(),
            product.getBaseUom() != null ? product.getBaseUom().getCode() : null,
            line.getRequestedQty(), line.getStatus().name(),
            line.getResolvedBy(), line.getResolvedAt(), line.getResolutionNotes(), line.getNotes());
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
