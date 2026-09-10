package com.cms.inventory.catalog.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.catalog.dto.ProductUomChainSaveRequest;
import com.cms.inventory.catalog.dto.ProductUomChainVersionResponse;
import com.cms.inventory.catalog.dto.ProductUomLevelRequest;
import com.cms.inventory.catalog.dto.ProductUomLevelResponse;
import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.catalog.model.ProductUomChainVersion;
import com.cms.inventory.catalog.model.ProductUomLevel;
import com.cms.inventory.catalog.model.Uom;
import com.cms.inventory.catalog.repository.ProductUomChainVersionRepository;
import com.cms.inventory.catalog.repository.ProductUomLevelRepository;

/**
 * Owns a {@link Product}'s unit-of-measure chain — the versioned set of levels (Tablet -> Strip
 * -> Box -> Carton, ml -> Bottle, etc.) a product can be purchased/received in above its base
 * unit. A version's levels are never edited once saved: {@link #saveVersion} either reactivates a
 * prior version whose levels exactly match what's submitted (a repack reverting to a pack size
 * used before) or creates a brand new one, flipping which single version is active for the
 * product. See the "Unit-of-Measure Hierarchy slice" decision-log entry (2026-09-10).
 */
@Service
@Transactional(readOnly = true)
public class ProductUomChainService {

    private final ProductUomChainVersionRepository versionRepository;
    private final ProductUomLevelRepository levelRepository;
    private final ProductService productService;
    private final UomService uomService;

    public ProductUomChainService(ProductUomChainVersionRepository versionRepository,
                                   ProductUomLevelRepository levelRepository,
                                   ProductService productService,
                                   UomService uomService) {
        this.versionRepository = versionRepository;
        this.levelRepository = levelRepository;
        this.productService = productService;
        this.uomService = uomService;
    }

    /** Null when the product has no chain configured yet (base unit only, no purchase-unit hierarchy). */
    public ProductUomChainVersionResponse getActiveVersion(Long productId) {
        return versionRepository.findByProductIdAndIsActiveTrue(productId).map(this::toResponse).orElse(null);
    }

    public List<ProductUomChainVersionResponse> listVersions(Long productId) {
        return versionRepository.findByProductIdOrderByVersionNoDesc(productId).stream().map(this::toResponse).toList();
    }

    /**
     * A level chosen for a Purchase Order/Goods Receipt line must belong to the product's
     * *currently active* chain version — a level from a deactivated version is historical-only,
     * readable through the transactions that already reference it but not selectable again. See
     * the "only the active version's units are selectable for new POs/GRNs" decision.
     */
    public ProductUomLevel requireActiveLevel(Long productId, Long uomLevelId) {
        ProductUomLevel level = levelRepository.findById(uomLevelId)
            .orElseThrow(() -> new ResourceNotFoundException("Unit-of-measure level not found with id: " + uomLevelId));
        ProductUomChainVersion version = level.getChainVersion();
        if (!version.getProduct().getId().equals(productId)) {
            throw new IllegalArgumentException("That unit does not belong to this product's unit hierarchy");
        }
        if (!Boolean.TRUE.equals(version.getIsActive())) {
            throw new IllegalArgumentException("That unit belongs to a retired pack configuration for this product — pick a current one");
        }
        return level;
    }

    @Transactional
    public ProductUomChainVersionResponse saveVersion(Long productId, ProductUomChainSaveRequest request, String actor) {
        Product product = productService.findOrThrow(productId);
        List<ProductUomLevelRequest> levels = request.levels();

        validateLevels(product, levels);

        // A configuration identical to one already on file (active or previously deactivated) is
        // reactivated rather than duplicated — see the "reuse a matching prior version" decision.
        List<ProductUomChainVersion> existing = versionRepository.findByProductIdOrderByVersionNoDesc(productId);
        for (ProductUomChainVersion candidate : existing) {
            if (matches(candidate, levels)) {
                return toResponse(activate(candidate, existing));
            }
        }

        int nextVersionNo = existing.stream().mapToInt(ProductUomChainVersion::getVersionNo).max().orElse(0) + 1;
        ProductUomChainVersion version = new ProductUomChainVersion();
        version.setProduct(product);
        version.setVersionNo(nextVersionNo);
        version.setIsActive(true);
        version.setCreatedBy(actor);
        for (ProductUomLevelRequest lr : levels) {
            ProductUomLevel level = new ProductUomLevel();
            level.setChainVersion(version);
            level.setUom(uomService.findOrThrow(lr.uomId()));
            level.setLevelRank(lr.levelRank());
            level.setFactorToBase(lr.factorToBase());
            level.setIsDefaultPurchase(Boolean.TRUE.equals(lr.isDefaultPurchase()));
            version.getLevels().add(level);
        }
        existing.forEach(v -> deactivateIfActive(v));
        version = versionRepository.save(version);
        return toResponse(version);
    }

    private void validateLevels(Product product, List<ProductUomLevelRequest> levels) {
        Set<Integer> ranks = new HashSet<>();
        Set<Long> uomIds = new HashSet<>();
        long defaultCount = 0;
        boolean hasBase = false;
        for (ProductUomLevelRequest lr : levels) {
            if (!ranks.add(lr.levelRank())) {
                throw new IllegalArgumentException("Each level must have a unique rank — rank " + lr.levelRank() + " is repeated");
            }
            if (!uomIds.add(lr.uomId())) {
                throw new IllegalArgumentException("Each level must use a different unit of measure");
            }
            if (lr.levelRank() == 0) {
                hasBase = true;
                if (!lr.uomId().equals(product.getBaseUom().getId())) {
                    throw new IllegalArgumentException(
                        "Level 0 must be the product's base unit ('" + product.getBaseUom().getCode() + "')");
                }
                if (lr.factorToBase().compareTo(BigDecimal.ONE) != 0) {
                    throw new IllegalArgumentException("The base level's conversion factor must be 1");
                }
            }
            if (Boolean.TRUE.equals(lr.isDefaultPurchase())) defaultCount++;
        }
        if (!hasBase) {
            throw new IllegalArgumentException("The chain must include level 0 (the product's base unit)");
        }
        if (defaultCount > 1) {
            throw new IllegalArgumentException("Only one level can be marked as the default purchase unit");
        }
    }

    /** True when {@code candidate}'s levels are exactly the set submitted (same unit, rank, factor, default flag). */
    private boolean matches(ProductUomChainVersion candidate, List<ProductUomLevelRequest> levels) {
        List<ProductUomLevel> existingLevels = levelRepository.findByChainVersionIdOrderByLevelRankAsc(candidate.getId());
        if (existingLevels.size() != levels.size()) return false;
        for (ProductUomLevelRequest lr : levels) {
            boolean found = existingLevels.stream().anyMatch(el ->
                el.getLevelRank().equals(lr.levelRank())
                    && el.getUom().getId().equals(lr.uomId())
                    && el.getFactorToBase().compareTo(lr.factorToBase()) == 0
                    && Boolean.TRUE.equals(el.getIsDefaultPurchase()) == Boolean.TRUE.equals(lr.isDefaultPurchase()));
            if (!found) return false;
        }
        return true;
    }

    private ProductUomChainVersion activate(ProductUomChainVersion target, List<ProductUomChainVersion> siblings) {
        siblings.forEach(this::deactivateIfActive);
        target.setIsActive(true);
        return versionRepository.save(target);
    }

    private void deactivateIfActive(ProductUomChainVersion version) {
        if (Boolean.TRUE.equals(version.getIsActive())) {
            version.setIsActive(false);
            versionRepository.save(version);
        }
    }

    private ProductUomChainVersionResponse toResponse(ProductUomChainVersion version) {
        List<ProductUomLevelResponse> levels = levelRepository.findByChainVersionIdOrderByLevelRankAsc(version.getId())
            .stream()
            .sorted(Comparator.comparing(ProductUomLevel::getLevelRank))
            .map(l -> {
                Uom uom = l.getUom();
                return new ProductUomLevelResponse(l.getId(), uom.getId(), uom.getCode(), uom.getName(),
                    l.getLevelRank(), l.getFactorToBase(), l.getIsDefaultPurchase());
            })
            .toList();
        return new ProductUomChainVersionResponse(version.getId(), version.getVersionNo(), version.getIsActive(),
            version.getCreatedBy(), version.getCreatedAt(), levels);
    }
}
