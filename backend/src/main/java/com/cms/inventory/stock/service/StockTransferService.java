package com.cms.inventory.stock.service;

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
import com.cms.inventory.catalog.repository.ProductRepository;
import com.cms.inventory.stock.dto.StockMovementRequest;
import com.cms.inventory.stock.dto.StockTransferAddLineRequest;
import com.cms.inventory.stock.dto.StockTransferCreateRequest;
import com.cms.inventory.stock.dto.StockTransferLineResponse;
import com.cms.inventory.stock.dto.StockTransferResponse;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.model.StockTransfer;
import com.cms.inventory.stock.model.StockTransferLine;
import com.cms.inventory.stock.model.enums.StockTransferStatus;
import com.cms.inventory.stock.repository.InventoryLocationRepository;
import com.cms.inventory.stock.repository.StockBalanceRepository;
import com.cms.inventory.stock.repository.StockTransferLineRepository;
import com.cms.inventory.stock.repository.StockTransferRepository;

/**
 * Owns the Stock Transfer workflow — Phase 3's ("Receiving & Stock Movement") second slice,
 * giving the {@code TRANSFER} {@code StockTxnType} (reserved since Phase 1) a real screen. DRAFT
 * (build lines) -> COMPLETED posts, for every line, a matched decrease-at-source /
 * increase-at-destination movement pair through {@code StockMovementService.recordMovement} —
 * never written to the ledger/balance directly, preserving that class's "sole write path"
 * invariant. Both legs use the source's own current weighted-average unit cost (resolved here,
 * same formula {@code StockMovementService}'s own decrease-valuation already documents) so the
 * receiving location's value is never silently zeroed — see the "Stock Transfer slice"
 * decision-log entry. Unbatched stock only in this pass; the source location's own negative-stock
 * guard (already built into {@code recordMovement}) is what actually blocks a transfer that would
 * exceed what's on hand — no separate check duplicated here.
 */
@Service
@Transactional(readOnly = true)
public class StockTransferService {

    private final StockTransferRepository transferRepository;
    private final StockTransferLineRepository lineRepository;
    private final InventoryLocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final StockBalanceRepository balanceRepository;
    private final StockMovementService stockMovementService;

    public StockTransferService(StockTransferRepository transferRepository,
                                 StockTransferLineRepository lineRepository,
                                 InventoryLocationRepository locationRepository,
                                 ProductRepository productRepository,
                                 StockBalanceRepository balanceRepository,
                                 StockMovementService stockMovementService) {
        this.transferRepository = transferRepository;
        this.lineRepository = lineRepository;
        this.locationRepository = locationRepository;
        this.productRepository = productRepository;
        this.balanceRepository = balanceRepository;
        this.stockMovementService = stockMovementService;
    }

    @Transactional
    public StockTransferResponse create(StockTransferCreateRequest request, String createdBy) {
        if (request.sourceLocationId().equals(request.destinationLocationId())) {
            throw new IllegalArgumentException("Source and destination locations must be different");
        }
        InventoryLocation source = locationRepository.findById(request.sourceLocationId())
            .orElseThrow(() -> new ResourceNotFoundException("Inventory location not found with id: " + request.sourceLocationId()));
        InventoryLocation destination = locationRepository.findById(request.destinationLocationId())
            .orElseThrow(() -> new ResourceNotFoundException("Inventory location not found with id: " + request.destinationLocationId()));

        StockTransfer transfer = new StockTransfer();
        transfer.setSourceLocation(source);
        transfer.setDestinationLocation(destination);
        transfer.setStatus(StockTransferStatus.DRAFT);
        transfer.setTransferDate(request.transferDate() != null ? request.transferDate() : LocalDate.now());
        transfer.setNotes(trim(request.notes()));
        transfer.setCreatedBy(createdBy);
        transfer.setCreatedAt(Instant.now());
        transfer.setUpdatedAt(Instant.now());
        return toResponse(transferRepository.save(transfer));
    }

    public Page<StockTransferResponse> findPage(Long locationId, String status, Pageable pageable) {
        Specification<StockTransfer> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (locationId != null) {
                predicate = cb.and(predicate, cb.or(
                    cb.equal(root.get("sourceLocation").get("id"), locationId),
                    cb.equal(root.get("destinationLocation").get("id"), locationId)));
            }
            if (status != null && !status.isBlank()) predicate = cb.and(predicate, cb.equal(root.get("status"), parseStatus(status)));
            return predicate;
        };
        return transferRepository.findAll(spec, pageable).map(t -> toResponse(t, false));
    }

    public StockTransferResponse findById(Long id) {
        return toResponse(requireTransfer(id));
    }

    @Transactional
    public StockTransferLineResponse addLine(Long transferId, StockTransferAddLineRequest request) {
        StockTransfer transfer = requireTransfer(transferId);
        requireStatus(transfer, StockTransferStatus.DRAFT, "add a line to");
        if (lineRepository.existsByStockTransferIdAndProductId(transferId, request.productId())) {
            throw new IllegalArgumentException("This product is already on the transfer");
        }
        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()));

        StockTransferLine line = new StockTransferLine();
        line.setStockTransfer(transfer);
        line.setProduct(product);
        line.setQuantity(request.quantity());
        line.setNotes(trim(request.notes()));
        line = lineRepository.save(line);

        transfer.setUpdatedAt(Instant.now());
        transferRepository.save(transfer);
        return toLineResponse(line);
    }

    @Transactional
    public void removeLine(Long transferId, Long lineId) {
        StockTransfer transfer = requireTransfer(transferId);
        requireStatus(transfer, StockTransferStatus.DRAFT, "remove a line from");
        StockTransferLine line = requireLine(transfer, lineId);
        lineRepository.delete(line);
        transfer.setUpdatedAt(Instant.now());
        transferRepository.save(transfer);
    }

    @Transactional
    public StockTransferResponse complete(Long transferId, String actor) {
        StockTransfer transfer = requireTransfer(transferId);
        requireStatus(transfer, StockTransferStatus.DRAFT, "complete");
        List<StockTransferLine> lines = lineRepository.findByStockTransferIdOrderByIdAsc(transferId);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Add at least one line before completing this transfer");
        }

        InventoryLocation source = transfer.getSourceLocation();
        InventoryLocation destination = transfer.getDestinationLocation();
        for (StockTransferLine line : lines) {
            Long productId = line.getProduct().getId();
            BigDecimal unitCost = currentUnbatchedUnitCost(productId, source.getId());

            stockMovementService.recordMovement(new StockMovementRequest(
                productId, source.getId(), null, null,
                "TRANSFER", "DECREASE", line.getQuantity(), unitCost,
                "Stock Transfer #" + transfer.getId() + " to " + destination.getVirtualName()
            ), actor);

            stockMovementService.recordMovement(new StockMovementRequest(
                productId, destination.getId(), null, null,
                "TRANSFER", "INCREASE", line.getQuantity(), unitCost,
                "Stock Transfer #" + transfer.getId() + " from " + source.getVirtualName()
            ), actor);
        }

        transfer.setStatus(StockTransferStatus.COMPLETED);
        transfer.setCompletedBy(actor);
        transfer.setCompletedAt(Instant.now());
        transfer.setUpdatedAt(Instant.now());
        transferRepository.save(transfer);
        return toResponse(transfer);
    }

    @Transactional
    public void cancel(Long transferId) {
        StockTransfer transfer = requireTransfer(transferId);
        requireStatus(transfer, StockTransferStatus.DRAFT, "cancel");
        transfer.setStatus(StockTransferStatus.CANCELLED);
        transfer.setUpdatedAt(Instant.now());
        transferRepository.save(transfer);
    }

    /** Same weighted-average formula {@code StockMovementService}'s own decrease-valuation uses. */
    private BigDecimal currentUnbatchedUnitCost(Long productId, Long locationId) {
        return balanceRepository.findByProductIdAndLocationIdAndBatchIsNull(productId, locationId)
            .filter(b -> b.getQtyOnHand().signum() > 0)
            .map(b -> b.getValueOnHand().divide(b.getQtyOnHand(), 2, RoundingMode.HALF_UP))
            .orElse(BigDecimal.ZERO);
    }

    private StockTransfer requireTransfer(Long id) {
        return transferRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Stock transfer not found with id: " + id));
    }

    private StockTransferLine requireLine(StockTransfer transfer, Long lineId) {
        StockTransferLine line = lineRepository.findById(lineId)
            .orElseThrow(() -> new ResourceNotFoundException("Stock transfer line not found with id: " + lineId));
        if (!line.getStockTransfer().getId().equals(transfer.getId())) {
            throw new ResourceNotFoundException("Stock transfer line not found with id: " + lineId);
        }
        return line;
    }

    private void requireStatus(StockTransfer transfer, StockTransferStatus required, String action) {
        if (transfer.getStatus() != required) {
            throw new IllegalArgumentException(
                "Cannot " + action + " a transfer that is " + transfer.getStatus().name().toLowerCase(Locale.ROOT)
                    + " — it must be " + required.name().toLowerCase(Locale.ROOT));
        }
    }

    private StockTransferStatus parseStatus(String value) {
        try {
            return StockTransferStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status '" + value + "'");
        }
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private StockTransferResponse toResponse(StockTransfer transfer) {
        return toResponse(transfer, true);
    }

    private StockTransferResponse toResponse(StockTransfer transfer, boolean includeLines) {
        List<StockTransferLine> lines = lineRepository.findByStockTransferIdOrderByIdAsc(transfer.getId());
        InventoryLocation source = transfer.getSourceLocation();
        InventoryLocation destination = transfer.getDestinationLocation();
        return new StockTransferResponse(
            transfer.getId(), source.getId(), source.getVirtualName(),
            destination.getId(), destination.getVirtualName(),
            transfer.getStatus().name(), transfer.getTransferDate(), transfer.getNotes(),
            transfer.getCreatedBy(), transfer.getCreatedAt(), transfer.getCompletedBy(), transfer.getCompletedAt(),
            lines.size(), includeLines ? lines.stream().map(this::toLineResponse).toList() : null);
    }

    private StockTransferLineResponse toLineResponse(StockTransferLine line) {
        Product product = line.getProduct();
        return new StockTransferLineResponse(
            line.getId(), product.getId(), product.getProductCode(), product.getProductName(),
            product.getBaseUom() != null ? product.getBaseUom().getCode() : null,
            line.getQuantity(), line.getNotes());
    }
}
