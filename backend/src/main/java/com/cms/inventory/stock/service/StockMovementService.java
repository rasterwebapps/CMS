package com.cms.inventory.stock.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.catalog.model.ProductUomChainVersion;
import com.cms.inventory.catalog.repository.ProductRepository;
import com.cms.inventory.catalog.repository.ProductUomChainVersionRepository;
import com.cms.inventory.stock.dto.StockBalanceResponse;
import com.cms.inventory.stock.dto.StockMovementRequest;
import com.cms.inventory.stock.dto.StockMovementResponse;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.model.StockBalance;
import com.cms.inventory.stock.model.StockBatch;
import com.cms.inventory.stock.model.StockLedger;
import com.cms.inventory.stock.model.enums.StockTxnType;
import com.cms.inventory.stock.repository.InventoryLocationRepository;
import com.cms.inventory.stock.repository.StockBalanceRepository;
import com.cms.inventory.stock.repository.StockBatchRepository;
import com.cms.inventory.stock.repository.StockLedgerRepository;

/**
 * Owns every write to {@link StockLedger} and {@link StockBalance} — the append-only ledger and
 * its materialized rollup are always written together, in the same transaction, and nowhere else
 * ({@code GoodsReceiptService} and {@code StockTransferService} both call {@link
 * #recordMovement} rather than writing the ledger/balance tables directly). See the 2026-09-07
 * "Stock Tracking slice" decision-log entry for the movement-type narrowing, negative-stock
 * guard, and weighted-average decrease-valuation decisions this class implements, and the
 * "Stock Transfer slice" entry for why {@code TRANSFER} was widened into {@link
 * #ALLOWED_TXN_TYPES} alongside {@code ADJUSTMENT}'s existing direction-based qty-delta handling.
 */
@Service
@Transactional(readOnly = true)
public class StockMovementService {

    /** Reachable through the API — see the decision log for why each was added, and when. */
    private static final Set<StockTxnType> ALLOWED_TXN_TYPES = EnumSet.of(
        StockTxnType.RECEIPT, StockTxnType.ADJUSTMENT, StockTxnType.DISPOSAL,
        StockTxnType.TRANSFER, StockTxnType.RETURN, StockTxnType.ISSUE);

    private final ProductRepository productRepository;
    private final InventoryLocationRepository locationRepository;
    private final StockBatchRepository batchRepository;
    private final StockLedgerRepository ledgerRepository;
    private final StockBalanceRepository balanceRepository;
    private final ProductUomChainVersionRepository uomChainVersionRepository;

    public StockMovementService(ProductRepository productRepository,
                                 InventoryLocationRepository locationRepository,
                                 StockBatchRepository batchRepository,
                                 StockLedgerRepository ledgerRepository,
                                 StockBalanceRepository balanceRepository,
                                 ProductUomChainVersionRepository uomChainVersionRepository) {
        this.productRepository = productRepository;
        this.locationRepository = locationRepository;
        this.batchRepository = batchRepository;
        this.ledgerRepository = ledgerRepository;
        this.balanceRepository = balanceRepository;
        this.uomChainVersionRepository = uomChainVersionRepository;
    }

    @Transactional
    public StockMovementResponse recordMovement(StockMovementRequest request, String performedBy) {
        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()));
        InventoryLocation location = locationRepository.findById(request.locationId())
            .orElseThrow(() -> new ResourceNotFoundException("Inventory location not found with id: " + request.locationId()));
        StockTxnType txnType = parseTxnType(request.txnType());

        StockBatch batch = resolveBatch(product, request.batchOrSerialNo(), request.expiryDate());
        Long batchId = batch == null ? null : batch.getId();

        BigDecimal magnitude = request.quantity();
        BigDecimal qtyDelta = switch (txnType) {
            case RECEIPT -> magnitude;
            // ISSUE (Phase 4: Stock Issue Request approval) is decrease-only — stock leaving the
            // issuing location for a requesting location, no matching increase posted here (the
            // requester isn't itself an InventoryLocation with its own tracked balance in this
            // slice's scope — see the "Stock Issue Request slice" decision-log entry).
            case DISPOSAL, ISSUE -> magnitude.negate();
            // RETURN is direction-based, same shape as ADJUSTMENT/TRANSFER — widened from an
            // original decrease-only meaning (goods leaving to a supplier, still SupplierReturn-
            // Service's own DECREASE call) once Phase 4's Internal Return needed the *opposite*
            // direction (previously-issued stock coming back to the issuing/store location, an
            // INCREASE). See the "Internal Return slice" decision-log entry.
            case ADJUSTMENT, TRANSFER, RETURN -> "DECREASE".equalsIgnoreCase(request.direction()) ? magnitude.negate() : magnitude;
            default -> throw new IllegalArgumentException(
                "Transaction type '" + txnType + "' is not yet available — only RECEIPT, ADJUSTMENT, DISPOSAL, TRANSFER, RETURN, and ISSUE can be recorded here");
        };

        StockBalance existing = findBalance(product.getId(), location.getId(), batchId);
        BigDecimal currentQty = existing != null ? existing.getQtyOnHand() : BigDecimal.ZERO;
        BigDecimal currentValue = existing != null ? existing.getValueOnHand() : BigDecimal.ZERO;

        BigDecimal newQty = currentQty.add(qtyDelta);
        if (newQty.signum() < 0) {
            throw new IllegalArgumentException(
                "This movement would leave a negative on-hand quantity (" + newQty + ") for this product/location — check the quantity entered");
        }

        BigDecimal unitCostForLedger;
        BigDecimal valueDelta;
        if (qtyDelta.signum() > 0) {
            unitCostForLedger = request.unitCost() != null ? request.unitCost() : BigDecimal.ZERO;
            valueDelta = qtyDelta.multiply(unitCostForLedger);
        } else {
            // Decrease: use the caller's unit cost if given, else the current weighted-average —
            // real FIFO/FEFO valuation is explicitly Phase 3 scope (see the decision log entry).
            unitCostForLedger = request.unitCost() != null
                ? request.unitCost()
                : (currentQty.signum() > 0 ? currentValue.divide(currentQty, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            valueDelta = qtyDelta.multiply(unitCostForLedger);
        }

        StockLedger ledger = new StockLedger();
        ledger.setProduct(product);
        ledger.setLocation(location);
        ledger.setBatch(batch);
        ledger.setTxnType(txnType);
        ledger.setQtyDelta(qtyDelta);
        ledger.setUnitCost(unitCostForLedger);
        ledger.setNotes(trim(request.notes()));
        ledger.setPerformedBy(performedBy);
        ledger.setTxnDate(Instant.now());
        ledger = ledgerRepository.save(ledger);

        balanceRepository.upsertBalance(product.getId(), location.getId(), batchId, qtyDelta, valueDelta);

        return new StockMovementResponse(ledger.getId(), product.getId(), location.getId(), batchId,
            txnType.name(), qtyDelta, currentQty.add(qtyDelta), currentValue.add(valueDelta), ledger.getTxnDate());
    }

    public Page<StockBalanceResponse> findBalancePage(Long productId, Long locationId, Pageable pageable) {
        Specification<StockBalance> spec = (root, query, cb) -> {
            var predicates = cb.conjunction();
            if (productId != null) predicates = cb.and(predicates, cb.equal(root.get("product").get("id"), productId));
            if (locationId != null) predicates = cb.and(predicates, cb.equal(root.get("location").get("id"), locationId));
            return predicates;
        };
        return balanceRepository.findAll(spec, pageable).map(this::toResponse);
    }

    private StockBatch resolveBatch(Product product, String batchOrSerialNo, java.time.LocalDate expiryDate) {
        String trimmed = trim(batchOrSerialNo);
        if (trimmed == null) return null;
        return batchRepository.findByProductAndBatchOrSerialNo(product.getId(), trimmed)
            .orElseGet(() -> {
                StockBatch batch = new StockBatch();
                batch.setProduct(product);
                batch.setBatchOrSerialNo(trimmed);
                batch.setExpiryDate(expiryDate);
                // Permanently stamped with whichever chain version is active right now — never
                // re-stamped later, so this batch keeps resolving through today's pack sizes even
                // after the product's active version changes (e.g. a future repack). Null when
                // the product has no chain configured yet.
                ProductUomChainVersion activeVersion =
                    uomChainVersionRepository.findByProductIdAndIsActiveTrue(product.getId()).orElse(null);
                batch.setChainVersion(activeVersion);
                return batchRepository.save(batch);
            });
    }

    private StockBalance findBalance(Long productId, Long locationId, Long batchId) {
        return (batchId != null
            ? balanceRepository.findByProductIdAndLocationIdAndBatchId(productId, locationId, batchId)
            : balanceRepository.findByProductIdAndLocationIdAndBatchIsNull(productId, locationId))
            .orElse(null);
    }

    private StockTxnType parseTxnType(String value) {
        StockTxnType type;
        try {
            type = StockTxnType.valueOf(value == null ? "" : value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid transaction type '" + value + "'");
        }
        if (!ALLOWED_TXN_TYPES.contains(type)) {
            throw new IllegalArgumentException(
                "Transaction type '" + type + "' is not yet available — only RECEIPT, ADJUSTMENT, and DISPOSAL can be recorded here");
        }
        return type;
    }

    private StockBalanceResponse toResponse(StockBalance b) {
        Product product = b.getProduct();
        InventoryLocation location = b.getLocation();
        StockBatch batch = b.getBatch();
        return new StockBalanceResponse(b.getId(), product.getId(), product.getProductCode(), product.getProductName(),
            location.getId(), location.getVirtualName(),
            batch == null ? null : batch.getId(), batch == null ? null : batch.getBatchOrSerialNo(), batch == null ? null : batch.getExpiryDate(),
            b.getQtyOnHand(), b.getValueOnHand(), b.getLastUpdated());
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
