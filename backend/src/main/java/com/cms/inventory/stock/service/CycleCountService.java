package com.cms.inventory.stock.service;

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
import com.cms.inventory.stock.dto.CycleCountAddLineRequest;
import com.cms.inventory.stock.dto.CycleCountCreateRequest;
import com.cms.inventory.stock.dto.CycleCountEnterCountRequest;
import com.cms.inventory.stock.dto.CycleCountLineResponse;
import com.cms.inventory.stock.dto.CycleCountResolutionRequest;
import com.cms.inventory.stock.dto.CycleCountResponse;
import com.cms.inventory.stock.dto.StockMovementRequest;
import com.cms.inventory.stock.model.CycleCount;
import com.cms.inventory.stock.model.CycleCountLine;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.model.enums.CycleCountLineStatus;
import com.cms.inventory.stock.model.enums.CycleCountScope;
import com.cms.inventory.stock.model.enums.CycleCountStatus;
import com.cms.inventory.stock.repository.CycleCountLineRepository;
import com.cms.inventory.stock.repository.CycleCountRepository;
import com.cms.inventory.stock.repository.InventoryLocationRepository;
import com.cms.inventory.stock.repository.ProductQtyProjection;
import com.cms.inventory.stock.repository.StockBalanceRepository;

/**
 * Owns the Cycle Count (physical stock count / reconciliation) workflow — creating a count sheet,
 * blind count entry, submission (variance computation), and approve/reject of each nonzero
 * variance, which posts through {@link StockMovementService} exactly like a manual adjustment
 * would. See the 2026-09-08 "Cycle Count slice" decision-log entry for the full design, including
 * why posting only ever touches a product's unbatched balance.
 */
@Service
@Transactional(readOnly = true)
public class CycleCountService {

    private final CycleCountRepository cycleCountRepository;
    private final CycleCountLineRepository lineRepository;
    private final InventoryLocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final StockBalanceRepository balanceRepository;
    private final StockMovementService stockMovementService;

    public CycleCountService(CycleCountRepository cycleCountRepository,
                              CycleCountLineRepository lineRepository,
                              InventoryLocationRepository locationRepository,
                              ProductRepository productRepository,
                              StockBalanceRepository balanceRepository,
                              StockMovementService stockMovementService) {
        this.cycleCountRepository = cycleCountRepository;
        this.lineRepository = lineRepository;
        this.locationRepository = locationRepository;
        this.productRepository = productRepository;
        this.balanceRepository = balanceRepository;
        this.stockMovementService = stockMovementService;
    }

    @Transactional
    public CycleCountResponse create(CycleCountCreateRequest request, String createdBy) {
        InventoryLocation location = locationRepository.findById(request.locationId())
            .orElseThrow(() -> new ResourceNotFoundException("Inventory location not found with id: " + request.locationId()));
        CycleCountScope scope = parseScope(request.scope());

        CycleCount count = new CycleCount();
        count.setLocation(location);
        count.setScope(scope);
        count.setStatus(CycleCountStatus.DRAFT);
        count.setCountDate(request.countDate() != null ? request.countDate() : LocalDate.now());
        count.setNotes(trim(request.notes()));
        count.setCreatedBy(createdBy);
        count.setCreatedAt(Instant.now());
        count.setUpdatedAt(Instant.now());
        count = cycleCountRepository.save(count);

        if (scope == CycleCountScope.FULL_LOCATION) {
            for (ProductQtyProjection snap : balanceRepository.sumQtyByProductForLocation(location.getId())) {
                Product product = productRepository.findById(snap.getProductId()).orElse(null);
                if (product == null) continue; // defensive — shouldn't happen, balance FKs products
                CycleCountLine line = new CycleCountLine();
                line.setCycleCount(count);
                line.setProduct(product);
                line.setSystemQtySnapshot(snap.getQty());
                lineRepository.save(line);
            }
        }
        return toResponse(count);
    }

    public Page<CycleCountResponse> findPage(Long locationId, String status, Pageable pageable) {
        Specification<CycleCount> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (locationId != null) predicate = cb.and(predicate, cb.equal(root.get("location").get("id"), locationId));
            if (status != null && !status.isBlank()) predicate = cb.and(predicate, cb.equal(root.get("status"), parseStatus(status)));
            return predicate;
        };
        return cycleCountRepository.findAll(spec, pageable).map(c -> toResponse(c, false));
    }

    public CycleCountResponse findById(Long id) {
        return toResponse(requireCount(id));
    }

    @Transactional
    public CycleCountLineResponse addLine(Long countId, CycleCountAddLineRequest request) {
        CycleCount count = requireCount(countId);
        requireStatus(count, CycleCountStatus.DRAFT, "add a product to");
        if (lineRepository.existsByCycleCountIdAndProductId(countId, request.productId())) {
            throw new IllegalArgumentException("This product is already on the count sheet");
        }
        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()));

        BigDecimal systemQty = balanceRepository.sumQtyForProductAndLocation(product.getId(), count.getLocation().getId());
        CycleCountLine line = new CycleCountLine();
        line.setCycleCount(count);
        line.setProduct(product);
        line.setSystemQtySnapshot(systemQty != null ? systemQty : BigDecimal.ZERO);
        line.setNotes(trim(request.notes()));
        return toLineResponse(lineRepository.save(line), false);
    }

    @Transactional
    public void removeLine(Long countId, Long lineId) {
        CycleCount count = requireCount(countId);
        requireStatus(count, CycleCountStatus.DRAFT, "remove a line from");
        CycleCountLine line = requireLine(count, lineId);
        lineRepository.delete(line);
    }

    @Transactional
    public CycleCountLineResponse enterCount(Long countId, Long lineId, CycleCountEnterCountRequest request, String countedBy) {
        CycleCount count = requireCount(countId);
        requireStatus(count, CycleCountStatus.DRAFT, "enter a count for");
        CycleCountLine line = requireLine(count, lineId);
        line.setCountedQty(request.countedQty());
        if (request.notes() != null) line.setNotes(trim(request.notes()));
        line.setCountedBy(countedBy);
        line.setCountedAt(Instant.now());
        return toLineResponse(lineRepository.save(line), false);
    }

    @Transactional
    public CycleCountResponse submit(Long countId, String submittedBy) {
        CycleCount count = requireCount(countId);
        requireStatus(count, CycleCountStatus.DRAFT, "submit");
        List<CycleCountLine> lines = lineRepository.findByCycleCountIdOrderByIdAsc(countId);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Add at least one product to the count sheet before submitting");
        }
        for (CycleCountLine line : lines) {
            if (line.getCountedQty() == null) {
                throw new IllegalArgumentException(
                    "\"" + line.getProduct().getProductName() + "\" has not been counted yet — count every line, or remove it, before submitting");
            }
            BigDecimal variance = line.getCountedQty().subtract(line.getSystemQtySnapshot());
            line.setVarianceQty(variance);
            line.setStatus(variance.signum() == 0 ? CycleCountLineStatus.MATCHED : CycleCountLineStatus.PENDING_REVIEW);
            lineRepository.save(line);
        }
        count.setStatus(CycleCountStatus.SUBMITTED);
        count.setSubmittedBy(submittedBy);
        count.setSubmittedAt(Instant.now());
        count.setUpdatedAt(Instant.now());
        cycleCountRepository.save(count);
        completeIfResolved(count);
        return toResponse(count);
    }

    @Transactional
    public CycleCountLineResponse approveLine(Long countId, Long lineId, CycleCountResolutionRequest request, String resolvedBy) {
        CycleCount count = requireCount(countId);
        requireStatus(count, CycleCountStatus.SUBMITTED, "approve a variance on");
        CycleCountLine line = requireLine(count, lineId);
        if (line.getStatus() != CycleCountLineStatus.PENDING_REVIEW) {
            throw new IllegalArgumentException("This line has no pending variance to approve");
        }

        BigDecimal variance = line.getVarianceQty();
        StockMovementRequest movementRequest = new StockMovementRequest(
            line.getProduct().getId(), count.getLocation().getId(), null, null,
            "ADJUSTMENT", variance.signum() > 0 ? "INCREASE" : "DECREASE", variance.abs(), null,
            "Cycle Count #" + count.getId() + " variance");
        try {
            var movement = stockMovementService.recordMovement(movementRequest, resolvedBy);
            line.setLedgerRefId(movement.ledgerId());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Could not post this variance automatically (" + e.getMessage() + "). This can happen when the product's "
                    + "stock is tracked in specific batches — resolve it manually via Record Stock Movement against the "
                    + "correct batch instead, then reject this line here.");
        }

        line.setStatus(CycleCountLineStatus.APPROVED);
        line.setResolvedBy(resolvedBy);
        line.setResolvedAt(Instant.now());
        line.setResolutionNotes(trim(request.notes()));
        lineRepository.save(line);
        completeIfResolved(count);
        return toLineResponse(line, true);
    }

    @Transactional
    public CycleCountLineResponse rejectLine(Long countId, Long lineId, CycleCountResolutionRequest request, String resolvedBy) {
        CycleCount count = requireCount(countId);
        requireStatus(count, CycleCountStatus.SUBMITTED, "reject a variance on");
        CycleCountLine line = requireLine(count, lineId);
        if (line.getStatus() != CycleCountLineStatus.PENDING_REVIEW) {
            throw new IllegalArgumentException("This line has no pending variance to reject");
        }
        line.setStatus(CycleCountLineStatus.REJECTED);
        line.setResolvedBy(resolvedBy);
        line.setResolvedAt(Instant.now());
        line.setResolutionNotes(trim(request.notes()));
        lineRepository.save(line);
        completeIfResolved(count);
        return toLineResponse(line, true);
    }

    @Transactional
    public void cancel(Long countId) {
        CycleCount count = requireCount(countId);
        requireStatus(count, CycleCountStatus.DRAFT, "cancel");
        count.setStatus(CycleCountStatus.CANCELLED);
        count.setUpdatedAt(Instant.now());
        cycleCountRepository.save(count);
    }

    private void completeIfResolved(CycleCount count) {
        if (count.getStatus() != CycleCountStatus.SUBMITTED) return;
        boolean anyPending = lineRepository.existsByCycleCountIdAndStatus(count.getId(), CycleCountLineStatus.PENDING_REVIEW);
        if (!anyPending) {
            count.setStatus(CycleCountStatus.COMPLETED);
            count.setCompletedAt(Instant.now());
            count.setUpdatedAt(Instant.now());
            cycleCountRepository.save(count);
        }
    }

    private CycleCount requireCount(Long id) {
        return cycleCountRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cycle count not found with id: " + id));
    }

    private CycleCountLine requireLine(CycleCount count, Long lineId) {
        CycleCountLine line = lineRepository.findById(lineId)
            .orElseThrow(() -> new ResourceNotFoundException("Cycle count line not found with id: " + lineId));
        if (!line.getCycleCount().getId().equals(count.getId())) {
            throw new ResourceNotFoundException("Cycle count line not found with id: " + lineId);
        }
        return line;
    }

    private void requireStatus(CycleCount count, CycleCountStatus required, String action) {
        if (count.getStatus() != required) {
            throw new IllegalArgumentException(
                "Cannot " + action + " a count that is " + count.getStatus().name().toLowerCase(Locale.ROOT)
                    + " — it must be " + required.name().toLowerCase(Locale.ROOT));
        }
    }

    private CycleCountScope parseScope(String value) {
        try {
            return CycleCountScope.valueOf(value == null ? "" : value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid scope '" + value + "'");
        }
    }

    private CycleCountStatus parseStatus(String value) {
        try {
            return CycleCountStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status '" + value + "'");
        }
    }

    private CycleCountResponse toResponse(CycleCount count) {
        return toResponse(count, true);
    }

    private CycleCountResponse toResponse(CycleCount count, boolean includeLines) {
        List<CycleCountLine> lines = lineRepository.findByCycleCountIdOrderByIdAsc(count.getId());
        boolean blind = count.getStatus() == CycleCountStatus.DRAFT;
        long pendingReview = lines.stream().filter(l -> l.getStatus() == CycleCountLineStatus.PENDING_REVIEW).count();
        InventoryLocation location = count.getLocation();
        return new CycleCountResponse(
            count.getId(), location.getId(), location.getVirtualName(),
            count.getScope().name(), count.getStatus().name(), count.getCountDate(), count.getNotes(),
            count.getCreatedBy(), count.getCreatedAt(), count.getSubmittedBy(), count.getSubmittedAt(), count.getCompletedAt(),
            lines.size(), (int) pendingReview,
            includeLines ? lines.stream().map(l -> toLineResponse(l, !blind)).toList() : null);
    }

    private CycleCountLineResponse toLineResponse(CycleCountLine line, boolean revealVariance) {
        Product product = line.getProduct();
        return new CycleCountLineResponse(
            line.getId(), product.getId(), product.getProductCode(), product.getProductName(),
            product.getBaseUom() != null ? product.getBaseUom().getCode() : null,
            revealVariance ? line.getSystemQtySnapshot() : null,
            line.getCountedQty(),
            revealVariance ? line.getVarianceQty() : null,
            line.getStatus().name(), line.getCountedBy(), line.getCountedAt(),
            line.getResolvedBy(), line.getResolvedAt(), line.getResolutionNotes(),
            line.getLedgerRefId(), line.getNotes());
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
