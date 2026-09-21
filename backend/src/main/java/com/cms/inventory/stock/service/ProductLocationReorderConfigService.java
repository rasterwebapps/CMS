package com.cms.inventory.stock.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.catalog.repository.ProductRepository;
import com.cms.inventory.stock.dto.ProductLocationReorderConfigRequest;
import com.cms.inventory.stock.dto.ProductLocationReorderConfigResponse;
import com.cms.inventory.stock.model.InventoryLocation;
import com.cms.inventory.stock.model.ProductLocationReorderConfig;
import com.cms.inventory.stock.model.enums.LocationRole;
import com.cms.inventory.stock.repository.InventoryLocationRepository;
import com.cms.inventory.stock.repository.ProductLocationReorderConfigRepository;

/**
 * Owns the per-(product, location) reorder config CRUD — Phase B of the Stock Indent auto-indent
 * feature (see the "OC-206 reopened" decision-log entry, 2026-09-21). Mirrors {@code
 * VendorProductMappingService}'s shape (at most one active row per key pair, {@code
 * ActiveStatusUpdateRequest}/{@code Response} reused for the status toggle) — the closest
 * in-repo precedent for a "product x other-entity, one active row" master.
 */
@Service
@Transactional(readOnly = true)
public class ProductLocationReorderConfigService {

    private final ProductLocationReorderConfigRepository configRepository;
    private final ProductRepository productRepository;
    private final InventoryLocationRepository locationRepository;

    public ProductLocationReorderConfigService(ProductLocationReorderConfigRepository configRepository,
                                                ProductRepository productRepository,
                                                InventoryLocationRepository locationRepository) {
        this.configRepository = configRepository;
        this.productRepository = productRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional
    public ProductLocationReorderConfigResponse create(ProductLocationReorderConfigRequest request) {
        ProductLocationReorderConfig config = new ProductLocationReorderConfig();
        applyRequest(config, request, null);
        return toResponse(configRepository.save(config));
    }

    public Page<ProductLocationReorderConfigResponse> findPage(Long productId, Long locationId, Boolean activeOnly, Pageable pageable) {
        Specification<ProductLocationReorderConfig> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (productId != null) predicate = cb.and(predicate, cb.equal(root.get("product").get("id"), productId));
            if (locationId != null) predicate = cb.and(predicate, cb.equal(root.get("location").get("id"), locationId));
            if (Boolean.TRUE.equals(activeOnly)) predicate = cb.and(predicate, cb.isTrue(root.get("isActive")));
            return predicate;
        };
        return configRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public ProductLocationReorderConfigResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public ProductLocationReorderConfigResponse update(Long id, ProductLocationReorderConfigRequest request) {
        ProductLocationReorderConfig config = findOrThrow(id);
        applyRequest(config, request, id);
        return toResponse(configRepository.save(config));
    }

    @Transactional
    public void delete(Long id) {
        if (!configRepository.existsById(id)) {
            throw new ResourceNotFoundException("Reorder configuration not found with id: " + id);
        }
        configRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        ProductLocationReorderConfig config = findOrThrow(id);
        boolean nextActive = Boolean.TRUE.equals(request.isActive());
        if (nextActive && pairExists(config.getProduct().getId(), config.getLocation().getId(), id)) {
            throw new IllegalArgumentException(
                "An active reorder configuration for '" + config.getProduct().getProductName() + "' at '"
                    + config.getLocation().getVirtualName() + "' already exists");
        }
        config.setIsActive(nextActive);
        ProductLocationReorderConfig saved = configRepository.save(config);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    public boolean pairExists(Long productId, Long locationId, Long excludeId) {
        if (productId == null || locationId == null) return false;
        return excludeId != null
            ? configRepository.existsByProductIdAndLocationIdAndIsActiveTrueAndIdNot(productId, locationId, excludeId)
            : configRepository.existsByProductIdAndLocationIdAndIsActiveTrue(productId, locationId);
    }

    private void applyRequest(ProductLocationReorderConfig config, ProductLocationReorderConfigRequest request, Long excludeId) {
        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()));
        InventoryLocation location = locationRepository.findById(request.locationId())
            .orElseThrow(() -> new ResourceNotFoundException("Inventory location not found with id: " + request.locationId()));

        if (location.getLocationRole() == LocationRole.STORE) {
            throw new IllegalArgumentException(
                "'" + location.getVirtualName() + "' is a store location — reorder configuration applies to requesting-point locations only, not the stores that supply them");
        }

        if (pairExists(product.getId(), location.getId(), excludeId)) {
            throw new IllegalArgumentException(
                "An active reorder configuration for '" + product.getProductName() + "' at '" + location.getVirtualName() + "' already exists");
        }

        if (request.maxStockQty() != null && request.maxStockQty().compareTo(request.reorderLevel()) < 0) {
            throw new IllegalArgumentException("Max stock quantity cannot be less than the reorder level");
        }

        boolean autoIndentEnabled = request.autoIndentEnabled() == null || request.autoIndentEnabled();
        if (autoIndentEnabled && location.getDefaultSupplyingLocation() == null) {
            throw new IllegalArgumentException(
                "'" + location.getVirtualName() + "' has no default supplying store configured yet — set one on the location before enabling auto-indent");
        }

        config.setProduct(product);
        config.setLocation(location);
        config.setReorderLevel(request.reorderLevel());
        config.setReorderQty(request.reorderQty());
        config.setMaxStockQty(request.maxStockQty());
        config.setAutoIndentEnabled(autoIndentEnabled);
        if (request.isActive() != null) config.setIsActive(request.isActive());
    }

    private ProductLocationReorderConfig findOrThrow(Long id) {
        return configRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reorder configuration not found with id: " + id));
    }

    private ProductLocationReorderConfigResponse toResponse(ProductLocationReorderConfig c) {
        Product product = c.getProduct();
        InventoryLocation location = c.getLocation();
        InventoryLocation supplying = location.getDefaultSupplyingLocation();
        return new ProductLocationReorderConfigResponse(
            c.getId(), product.getId(), product.getProductCode(), product.getProductName(),
            location.getId(), location.getVirtualName(),
            supplying != null ? supplying.getId() : null, supplying != null ? supplying.getVirtualName() : null,
            c.getReorderLevel(), c.getReorderQty(), c.getMaxStockQty(), c.getAutoIndentEnabled(), c.getIsActive(),
            c.getCreatedAt(), c.getUpdatedAt());
    }
}
