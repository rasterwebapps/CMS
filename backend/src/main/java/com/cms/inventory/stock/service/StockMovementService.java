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
import com.cms.inventory.catalog.model.ProductVariant;
import com.cms.inventory.catalog.model.enums.StockTrackingMode;
import com.cms.inventory.catalog.repository.ProductRepository;
import com.cms.inventory.catalog.repository.ProductUomChainVersionRepository;
import com.cms.inventory.catalog.repository.ProductVariantRepository;
import com.cms.inventory.stock.dto.StockBalanceResponse;
import com.cms.inventory.stock.dto.StockMovementRequest;
import com.cms.inventory.stock.dto.StockMovementResponse;
import com.cms.inventory.stock.dto.VariantConvertRequest;
import com.cms.inventory.stock.model.InventoryBin;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.model.StockBalance;
import com.cms.inventory.stock.model.StockBatch;
import com.cms.inventory.stock.model.StockBinAllocation;
import com.cms.inventory.stock.model.StockLedger;
import com.cms.inventory.stock.model.enums.StockTxnType;
import com.cms.inventory.stock.repository.InventoryBinRepository;
import com.cms.inventory.stock.repository.InventoryLocationRepository;
import com.cms.inventory.stock.repository.StockBalanceRepository;
import com.cms.inventory.stock.repository.StockBatchRepository;
import com.cms.inventory.stock.repository.StockBinAllocationRepository;
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
    private final ProductVariantRepository variantRepository;
    private final InventoryBinRepository binRepository;
    private final StockBinAllocationRepository binAllocationRepository;

    public StockMovementService(ProductRepository productRepository,
                                 InventoryLocationRepository locationRepository,
                                 StockBatchRepository batchRepository,
                                 StockLedgerRepository ledgerRepository,
                                 StockBalanceRepository balanceRepository,
                                 ProductUomChainVersionRepository uomChainVersionRepository,
                                 ProductVariantRepository variantRepository,
                                 InventoryBinRepository binRepository,
                                 StockBinAllocationRepository binAllocationRepository) {
        this.productRepository = productRepository;
        this.locationRepository = locationRepository;
        this.batchRepository = batchRepository;
        this.ledgerRepository = ledgerRepository;
        this.balanceRepository = balanceRepository;
        this.uomChainVersionRepository = uomChainVersionRepository;
        this.variantRepository = variantRepository;
        this.binRepository = binRepository;
        this.binAllocationRepository = binAllocationRepository;
    }

    @Transactional
    public StockMovementResponse recordMovement(StockMovementRequest request, String performedBy) {
        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()));
        InventoryLocation location = locationRepository.findById(request.locationId())
            .orElseThrow(() -> new ResourceNotFoundException("Inventory location not found with id: " + request.locationId()));
        StockTxnType txnType = parseTxnType(request.txnType());
        ProductVariant variant = resolveVariant(product, request.variantId());
        requireTrackingModeCompliance(variant != null ? variant.getTrackingMode() : product.getTrackingMode(), product, request);

        StockBatch batch = resolveBatch(product, variant, request.batchOrSerialNo(), request.expiryDate());
        Long batchId = batch == null ? null : batch.getId();
        Long variantId = variant == null ? null : variant.getId();

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

        StockBalance existing = findBalance(product.getId(), variantId, location.getId(), batchId);
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
        ledger.setVariant(variant);
        ledger.setLocation(location);
        ledger.setBatch(batch);
        ledger.setTxnType(txnType);
        ledger.setQtyDelta(qtyDelta);
        ledger.setUnitCost(unitCostForLedger);
        ledger.setNotes(trim(request.notes()));
        ledger.setPerformedBy(performedBy);
        ledger.setTxnDate(Instant.now());
        ledger = ledgerRepository.save(ledger);

        balanceRepository.upsertBalance(product.getId(), variantId, location.getId(), batchId, qtyDelta, valueDelta);

        if (request.binId() != null) {
            applyBinAllocation(request.binId(), location, product.getId(), variantId, batchId, qtyDelta);
        }

        return new StockMovementResponse(ledger.getId(), product.getId(), variantId, location.getId(), batchId,
            txnType.name(), qtyDelta, currentQty.add(qtyDelta), currentValue.add(valueDelta), ledger.getTxnDate());
    }

    /**
     * Applies this movement's quantity delta to the per-bin breakdown, once a bin was supplied.
     * Only ever called after {@link StockBalanceRepository#upsertBalance} in the same transaction,
     * so the parent {@link StockBalance} row is guaranteed to exist by the time this re-fetches it.
     * Guards against a bin's own allocation going negative the same way {@link #recordMovement}
     * guards the balance itself — a decrease can only draw down what was actually allocated to
     * that specific bin, not just what's on hand anywhere in the location.
     */
    private void applyBinAllocation(Long binId, InventoryLocation location, Long productId, Long variantId, Long batchId, BigDecimal qtyDelta) {
        InventoryBin bin = binRepository.findById(binId)
            .orElseThrow(() -> new ResourceNotFoundException("Bin not found with id: " + binId));
        if (!bin.getRack().getLocation().getId().equals(location.getId())) {
            throw new IllegalArgumentException(
                "Bin '" + bin.getName() + "' does not belong to location '" + location.getVirtualName() + "'");
        }

        StockBalance balance = findBalance(productId, variantId, location.getId(), batchId);
        if (balance == null) {
            throw new IllegalStateException("Stock balance not found after upsert for product " + productId + " at location " + location.getId());
        }

        if (qtyDelta.signum() < 0) {
            BigDecimal currentBinQty = binAllocationRepository.findByStockBalanceIdAndBinId(balance.getId(), binId)
                .map(StockBinAllocation::getQty)
                .orElse(BigDecimal.ZERO);
            if (currentBinQty.add(qtyDelta).signum() < 0) {
                throw new IllegalArgumentException(
                    "This movement would leave a negative allocation in bin '" + bin.getName()
                        + "' (only " + currentBinQty + " is currently allocated there) — check the bin selected");
            }
        }

        binAllocationRepository.upsertAllocation(balance.getId(), binId, qtyDelta);
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

    /**
     * Resolves {@code request.variantId()} against the product, enforcing that it actually
     * belongs to that product, and — per the 2026-09-11 "Wire ProductVariant into Stock Movement"
     * decision-log entry — that a variant is given at all once the product has any active
     * variant. A product with no active variants keeps working exactly as before (variant stays
     * null, nothing required).
     */
    private ProductVariant resolveVariant(Product product, Long variantId) {
        if (variantId != null) {
            return variantRepository.findByIdAndProductId(variantId, product.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Variant " + variantId + " does not belong to '" + product.getProductName() + "'"));
        }
        if (variantRepository.existsByProductIdAndIsActiveTrue(product.getId())) {
            throw new IllegalArgumentException(
                "'" + product.getProductName() + "' has active variants — select one for this movement");
        }
        return null;
    }

    /**
     * Enforces the effective tracking mode — a selected variant's own {@code trackingMode}
     * overrides the parent product's (variants carry their own, per {@code ProductVariant}'s
     * copy-at-creation design), else the product's — see the 2026-09-11 "Serial/batch
     * tracking-mode flag" DECISION_LOG entry. {@code NONE} enforces nothing (today's pre-existing
     * behavior, {@code batchOrSerialNo} stays fully optional free text); {@code BATCH} requires a
     * batch number on every movement; {@code SERIAL} requires a serial number and restricts the
     * movement to exactly one unit.
     */
    private void requireTrackingModeCompliance(StockTrackingMode mode, Product product, StockMovementRequest request) {
        if (mode == StockTrackingMode.NONE) return;
        if (trim(request.batchOrSerialNo()) == null) {
            String label = mode == StockTrackingMode.SERIAL ? "a serial number" : "a batch number";
            throw new IllegalArgumentException(
                "'" + product.getProductName() + "' is tracked by " + mode.name().toLowerCase() + " — " + label + " is required for this movement");
        }
        if (mode == StockTrackingMode.SERIAL && request.quantity().compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalArgumentException(
                "'" + product.getProductName() + "' is serial-tracked — quantity must be exactly 1 per movement (one unit per serial number)");
        }
    }

    private StockBatch resolveBatch(Product product, ProductVariant variant, String batchOrSerialNo, java.time.LocalDate expiryDate) {
        String trimmed = trim(batchOrSerialNo);
        if (trimmed == null) return null;
        Long variantId = variant == null ? null : variant.getId();
        return batchRepository.findByProductAndVariantAndBatchOrSerialNo(product.getId(), variantId, trimmed)
            .orElseGet(() -> {
                StockBatch batch = new StockBatch();
                batch.setProduct(product);
                batch.setVariant(variant);
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

    private StockBalance findBalance(Long productId, Long variantId, Long locationId, Long batchId) {
        if (variantId != null) {
            return (batchId != null
                ? balanceRepository.findByProductIdAndVariantIdAndLocationIdAndBatchId(productId, variantId, locationId, batchId)
                : balanceRepository.findByProductIdAndVariantIdAndLocationIdAndBatchIsNull(productId, variantId, locationId))
                .orElse(null);
        }
        return (batchId != null
            ? balanceRepository.findByProductIdAndVariantIsNullAndLocationIdAndBatchId(productId, locationId, batchId)
            : balanceRepository.findByProductIdAndVariantIsNullAndLocationIdAndBatchIsNull(productId, locationId))
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
        ProductVariant variant = b.getVariant();
        return new StockBalanceResponse(b.getId(), product.getId(), product.getProductCode(), product.getProductName(),
            variant == null ? null : variant.getId(), variant == null ? null : variant.getVariantCode(), variant == null ? null : variant.getVariantName(),
            variant == null && variantRepository.existsByProductIdAndIsActiveTrue(product.getId()),
            location.getId(), location.getVirtualName(),
            batch == null ? null : batch.getId(), batch == null ? null : batch.getBatchOrSerialNo(), batch == null ? null : batch.getExpiryDate(),
            b.getQtyOnHand(), b.getValueOnHand(), b.getLastUpdated());
    }

    /**
     * Converts a stranded null-variant balance row onto a chosen active variant of the same
     * product — the fix for the gap {@code resolveVariant} itself created: once a product has any
     * active variant, no movement can ever again target {@code variantId = null} for it, so a
     * pre-existing null-variant balance can never be issued/transferred/adjusted again through the
     * normal API. Posts as two real, audit-logged {@code ADJUSTMENT} ledger entries (a decrease on
     * the null-variant bucket, an increase on the variant bucket) rather than an in-place UPDATE,
     * preserving the append-only-ledger invariant {@link StockBalance}'s own javadoc documents.
     * Always moves the row's *entire* remaining quantity/value in one call — see the 2026-09-15
     * specialist-round decision recorded in the DECISION_LOG "null-variant stock" entry. The
     * null-variant row is never deleted; it's left at zero as a permanent, addressable bucket in
     * case any further pre-existing history is later found to belong to it.
     */
    @Transactional
    public StockMovementResponse convertToVariant(Long balanceId, VariantConvertRequest request, String performedBy) {
        StockBalance balance = balanceRepository.findById(balanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Stock balance not found with id: " + balanceId));
        if (balance.getVariant() != null) {
            throw new IllegalArgumentException("This balance already belongs to a variant");
        }
        BigDecimal qty = balance.getQtyOnHand();
        if (qty.signum() == 0) {
            throw new IllegalArgumentException("This balance has no remaining quantity to convert");
        }
        Product product = balance.getProduct();
        ProductVariant variant = variantRepository.findByIdAndProductId(request.variantId(), product.getId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Variant " + request.variantId() + " does not belong to '" + product.getProductName() + "'"));
        if (!Boolean.TRUE.equals(variant.getIsActive())) {
            throw new IllegalArgumentException("Variant '" + variant.getVariantName() + "' is not active");
        }
        BigDecimal value = balance.getValueOnHand();
        BigDecimal unitCost = value.divide(qty, 2, RoundingMode.HALF_UP);

        InventoryLocation location = balance.getLocation();
        StockBatch sourceBatch = balance.getBatch();
        Long sourceBatchId = sourceBatch == null ? null : sourceBatch.getId();
        // Reuses the same per-variant batch resolution every ordinary movement already goes
        // through — StockBatch has been scoped by (product, variant, batchOrSerialNo) since V497.
        StockBatch targetBatch = sourceBatch == null ? null
            : resolveBatch(product, variant, sourceBatch.getBatchOrSerialNo(), sourceBatch.getExpiryDate());
        Long targetBatchId = targetBatch == null ? null : targetBatch.getId();

        Instant now = Instant.now();
        String note = "System: converted from unassigned to variant '" + variant.getVariantName() + "' (" + variant.getVariantCode() + ")";

        StockLedger outLedger = new StockLedger();
        outLedger.setProduct(product);
        outLedger.setVariant(null);
        outLedger.setLocation(location);
        outLedger.setBatch(sourceBatch);
        outLedger.setTxnType(StockTxnType.ADJUSTMENT);
        outLedger.setQtyDelta(qty.negate());
        outLedger.setUnitCost(unitCost);
        outLedger.setNotes(note);
        outLedger.setPerformedBy(performedBy);
        outLedger.setTxnDate(now);
        ledgerRepository.save(outLedger);

        StockLedger inLedger = new StockLedger();
        inLedger.setProduct(product);
        inLedger.setVariant(variant);
        inLedger.setLocation(location);
        inLedger.setBatch(targetBatch);
        inLedger.setTxnType(StockTxnType.ADJUSTMENT);
        inLedger.setQtyDelta(qty);
        inLedger.setUnitCost(unitCost);
        inLedger.setNotes(note);
        inLedger.setPerformedBy(performedBy);
        inLedger.setTxnDate(now);
        inLedger = ledgerRepository.save(inLedger);

        balanceRepository.upsertBalance(product.getId(), null, location.getId(), sourceBatchId, qty.negate(), value.negate());
        balanceRepository.upsertBalance(product.getId(), variant.getId(), location.getId(), targetBatchId, qty, value);

        return new StockMovementResponse(inLedger.getId(), product.getId(), variant.getId(), location.getId(), targetBatchId,
            StockTxnType.ADJUSTMENT.name(), qty, qty, value, now);
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
