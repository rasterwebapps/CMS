package com.cms.inventory.asset.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.asset.dto.AssetDisposalRequest;
import com.cms.inventory.asset.dto.AssetRequest;
import com.cms.inventory.asset.dto.AssetResponse;
import com.cms.inventory.asset.dto.AssetStatusUpdateRequest;
import com.cms.inventory.asset.model.Asset;
import com.cms.inventory.asset.model.enums.AssetStatus;
import com.cms.inventory.asset.repository.AssetRepository;
import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.catalog.repository.ProductRepository;
import com.cms.inventory.receiving.model.GoodsReceiptLine;
import com.cms.inventory.receiving.repository.GoodsReceiptLineRepository;
import com.cms.inventory.stock.dto.StockMovementRequest;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.model.StockBalance;
import com.cms.inventory.stock.repository.InventoryLocationRepository;
import com.cms.inventory.stock.repository.StockBalanceRepository;
import com.cms.inventory.stock.service.StockMovementService;

/**
 * Owns the Asset register — Phase 5's ("Equipment & Asset Management") first slice. Status
 * changes are open-ended (no strict state machine) — real asset lifecycles aren't linear
 * (e.g. IN_USE ↔ UNDER_MAINTENANCE happens repeatedly before an eventual RETIRED/DISPOSED), so
 * {@link #updateStatus} only validates the target is a real {@code AssetStatus}, not a specific
 * transition graph. See the "Asset register slice" decision-log entry.
 *
 * <p>{@link #toResponse} also computes standard straight-line depreciation (Phase 5's third
 * slice) directly from the asset's own {@code purchaseValue}/{@code purchaseDate}/{@code
 * usefulLifeMonths}/{@code salvageValue} — a read-only value, never stored, and never posted to
 * any ledger/connector (that's out of scope, per the already-deferred GL posting-connector
 * decision from Phase 1). {@code depreciationApplicable} is {@code false} whenever any of those
 * four inputs is missing, so the frontend can show "not enough data" rather than a misleading
 * zero. See the "Depreciation slice" decision-log entry.
 *
 * <p>{@link #dispose} (Phase 5's fourth and final slice) moves an asset to {@code DISPOSED}
 * with a reason/value/date, and — if the asset's own product still shows on-hand quantity at
 * the asset's location — posts a one-unit {@code DISPOSAL} stock movement through the existing
 * {@code StockMovementService} to write that unit off, since a Product can be both individually
 * asset-tracked and bulk stock-tracked at once. See the "Disposal slice" decision-log entry.
 */
@Service
@Transactional(readOnly = true)
public class AssetService {

    private final AssetRepository assetRepository;
    private final ProductRepository productRepository;
    private final InventoryLocationRepository locationRepository;
    private final GoodsReceiptLineRepository goodsReceiptLineRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StockMovementService stockMovementService;

    public AssetService(AssetRepository assetRepository,
                         ProductRepository productRepository,
                         InventoryLocationRepository locationRepository,
                         GoodsReceiptLineRepository goodsReceiptLineRepository,
                         StockBalanceRepository stockBalanceRepository,
                         StockMovementService stockMovementService) {
        this.assetRepository = assetRepository;
        this.productRepository = productRepository;
        this.locationRepository = locationRepository;
        this.goodsReceiptLineRepository = goodsReceiptLineRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.stockMovementService = stockMovementService;
    }

    @Transactional
    public AssetResponse create(AssetRequest request) {
        Asset asset = new Asset();
        applyRequest(asset, request, null);
        return toResponse(assetRepository.save(asset));
    }

    public Page<AssetResponse> findPage(Long locationId, String status, String search, Pageable pageable) {
        Specification<Asset> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (locationId != null) predicate = cb.and(predicate, cb.equal(root.get("location").get("id"), locationId));
            if (status != null && !status.isBlank()) predicate = cb.and(predicate, cb.equal(root.get("status"), parseStatus(status)));
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicate = cb.and(predicate, cb.or(
                    cb.like(cb.lower(root.get("assetTag")), like),
                    cb.like(cb.lower(root.get("serialNumber")), like)));
            }
            return predicate;
        };
        return assetRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public AssetResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public AssetResponse update(Long id, AssetRequest request) {
        Asset asset = findOrThrow(id);
        applyRequest(asset, request, id);
        return toResponse(assetRepository.save(asset));
    }

    @Transactional
    public AssetResponse updateStatus(Long id, AssetStatusUpdateRequest request) {
        Asset asset = findOrThrow(id);
        asset.setStatus(parseStatus(request.status()));
        if (request.notes() != null && !request.notes().isBlank()) {
            asset.setNotes(asset.getNotes() != null ? asset.getNotes() + " | " + request.notes().trim() : request.notes().trim());
        }
        return toResponse(assetRepository.save(asset));
    }

    /**
     * Disposes the asset: marks it {@code DISPOSED} with a reason/value/date, and writes off one
     * unit of on-hand stock for its product at its location if any is currently on hand
     * (unbatched balance only — same simplification precedent {@code CycleCount}'s own posting
     * step already established). A product with no on-hand balance there (or never bulk
     * stock-tracked at all) simply has nothing written off — not an error.
     */
    @Transactional
    public AssetResponse dispose(Long id, AssetDisposalRequest request, String actor) {
        Asset asset = findOrThrow(id);
        if (asset.getStatus() == AssetStatus.DISPOSED) {
            throw new IllegalArgumentException("This asset has already been disposed");
        }

        StockBalance balance = stockBalanceRepository
            .findByProductIdAndLocationIdAndBatchIsNull(asset.getProduct().getId(), asset.getLocation().getId())
            .orElse(null);
        if (balance != null && balance.getQtyOnHand().signum() > 0) {
            stockMovementService.recordMovement(new StockMovementRequest(
                asset.getProduct().getId(), asset.getLocation().getId(), null, null,
                "DISPOSAL", null, BigDecimal.ONE, null,
                "Asset disposal — " + asset.getAssetTag() + (request.reason() != null ? " — " + request.reason() : "")
            ), actor);
        }

        asset.setStatus(AssetStatus.DISPOSED);
        asset.setDisposalReason(request.reason().trim());
        asset.setDisposalValue(request.disposalValue());
        asset.setDisposalDate(request.disposalDate() != null ? request.disposalDate() : LocalDate.now());
        asset.setDisposedBy(actor);
        asset.setDisposedAt(Instant.now());
        return toResponse(assetRepository.save(asset));
    }

    public boolean assetTagExists(String value, Long excludeId) {
        if (value == null || value.isBlank()) return false;
        String trimmed = value.trim();
        return excludeId != null
            ? assetRepository.existsByAssetTagIgnoreCaseAndIdNot(trimmed, excludeId)
            : assetRepository.existsByAssetTagIgnoreCase(trimmed);
    }

    private void applyRequest(Asset asset, AssetRequest request, Long excludeId) {
        if (assetTagExists(request.assetTag(), excludeId)) {
            throw new IllegalArgumentException("Asset tag '" + request.assetTag() + "' is already in use");
        }
        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()));
        InventoryLocation location = locationRepository.findById(request.locationId())
            .orElseThrow(() -> new ResourceNotFoundException("Inventory location not found with id: " + request.locationId()));

        GoodsReceiptLine goodsReceiptLine = null;
        if (request.goodsReceiptLineId() != null) {
            goodsReceiptLine = goodsReceiptLineRepository.findById(request.goodsReceiptLineId())
                .orElseThrow(() -> new ResourceNotFoundException("Goods receipt line not found with id: " + request.goodsReceiptLineId()));
        }

        asset.setProduct(product);
        asset.setLocation(location);
        asset.setAssetTag(request.assetTag().trim());
        asset.setSerialNumber(trim(request.serialNumber()));
        asset.setGoodsReceiptLine(goodsReceiptLine);
        asset.setPurchaseValue(request.purchaseValue());
        asset.setPurchaseDate(request.purchaseDate());
        asset.setUsefulLifeMonths(request.usefulLifeMonths());
        asset.setSalvageValue(request.salvageValue());
        asset.setNotes(trim(request.notes()));
    }

    private Asset findOrThrow(Long id) {
        return assetRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + id));
    }

    private AssetStatus parseStatus(String value) {
        try {
            return AssetStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status '" + value + "'");
        }
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private AssetResponse toResponse(Asset asset) {
        Product product = asset.getProduct();
        InventoryLocation location = asset.getLocation();

        boolean depreciationApplicable = asset.getPurchaseValue() != null && asset.getPurchaseDate() != null
            && asset.getUsefulLifeMonths() != null && asset.getUsefulLifeMonths() > 0;
        BigDecimal accumulatedDepreciation = null;
        BigDecimal currentBookValue = null;
        if (depreciationApplicable) {
            BigDecimal salvage = asset.getSalvageValue() != null ? asset.getSalvageValue() : BigDecimal.ZERO;
            BigDecimal depreciableBase = asset.getPurchaseValue().subtract(salvage);
            int monthsElapsed = Math.max(0, Math.min(
                asset.getUsefulLifeMonths(),
                monthsBetween(asset.getPurchaseDate(), LocalDate.now())));
            BigDecimal monthlyDepreciation = depreciableBase
                .divide(BigDecimal.valueOf(asset.getUsefulLifeMonths()), 4, RoundingMode.HALF_UP);
            accumulatedDepreciation = monthlyDepreciation.multiply(BigDecimal.valueOf(monthsElapsed))
                .min(depreciableBase.max(BigDecimal.ZERO))
                .setScale(2, RoundingMode.HALF_UP);
            currentBookValue = asset.getPurchaseValue().subtract(accumulatedDepreciation).max(salvage)
                .setScale(2, RoundingMode.HALF_UP);
        }

        return new AssetResponse(
            asset.getId(), product.getId(), product.getProductCode(), product.getProductName(),
            location.getId(), location.getVirtualName(),
            asset.getAssetTag(), asset.getSerialNumber(), asset.getStatus().name(),
            asset.getGoodsReceiptLine() != null ? asset.getGoodsReceiptLine().getId() : null,
            asset.getPurchaseValue(), asset.getPurchaseDate(), asset.getUsefulLifeMonths(), asset.getSalvageValue(),
            depreciationApplicable, accumulatedDepreciation, currentBookValue,
            asset.getDisposalReason(), asset.getDisposalValue(), asset.getDisposalDate(), asset.getDisposedBy(), asset.getDisposedAt(),
            asset.getNotes(), asset.getCreatedAt(), asset.getUpdatedAt());
    }

    /** Whole calendar months elapsed from {@code start} to {@code end}, never negative. */
    private static int monthsBetween(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) return 0;
        return Period.between(start, end).getYears() * 12 + Period.between(start, end).getMonths();
    }
}
