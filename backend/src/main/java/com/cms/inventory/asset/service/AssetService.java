package com.cms.inventory.asset.service;

import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.exception.ResourceNotFoundException;
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
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.repository.InventoryLocationRepository;

/**
 * Owns the Asset register — Phase 5's ("Equipment & Asset Management") first slice. Status
 * changes are open-ended (no strict state machine) — real asset lifecycles aren't linear
 * (e.g. IN_USE ↔ UNDER_MAINTENANCE happens repeatedly before an eventual RETIRED/DISPOSED), so
 * {@link #updateStatus} only validates the target is a real {@code AssetStatus}, not a specific
 * transition graph. See the "Asset register slice" decision-log entry.
 */
@Service
@Transactional(readOnly = true)
public class AssetService {

    private final AssetRepository assetRepository;
    private final ProductRepository productRepository;
    private final InventoryLocationRepository locationRepository;
    private final GoodsReceiptLineRepository goodsReceiptLineRepository;

    public AssetService(AssetRepository assetRepository,
                         ProductRepository productRepository,
                         InventoryLocationRepository locationRepository,
                         GoodsReceiptLineRepository goodsReceiptLineRepository) {
        this.assetRepository = assetRepository;
        this.productRepository = productRepository;
        this.locationRepository = locationRepository;
        this.goodsReceiptLineRepository = goodsReceiptLineRepository;
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
        return new AssetResponse(
            asset.getId(), product.getId(), product.getProductCode(), product.getProductName(),
            location.getId(), location.getVirtualName(),
            asset.getAssetTag(), asset.getSerialNumber(), asset.getStatus().name(),
            asset.getGoodsReceiptLine() != null ? asset.getGoodsReceiptLine().getId() : null,
            asset.getPurchaseValue(), asset.getPurchaseDate(), asset.getUsefulLifeMonths(), asset.getSalvageValue(),
            asset.getNotes(), asset.getCreatedAt(), asset.getUpdatedAt());
    }
}
