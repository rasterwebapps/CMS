package com.cms.inventory.catalog.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.catalog.dto.ProductAttributeValueRequest;
import com.cms.inventory.catalog.dto.ProductAttributeValueResponse;
import com.cms.inventory.catalog.dto.ProductVariantRequest;
import com.cms.inventory.catalog.dto.ProductVariantResponse;
import com.cms.inventory.catalog.model.CategoryAttribute;
import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.catalog.model.ProductVariant;
import com.cms.inventory.catalog.model.ProductVariantAttributeValue;
import com.cms.inventory.catalog.model.enums.StockTrackingMode;
import com.cms.inventory.catalog.repository.CategoryAttributeRepository;
import com.cms.inventory.catalog.repository.ProductVariantRepository;

/**
 * Owns a {@link Product}'s {@link ProductVariant} rows — the "parent/child SKU matrix." A
 * variant's typed attribute values are validated the same way {@code ProductService} validates a
 * product's own (shared logic in {@link TypedAttributeValueSupport}), scoped to whichever {@code
 * CategoryAttribute}s are defined on the *parent* product's category — a variant has no category
 * of its own. See the 2026-09-11 "ProductVariant" decision-log entry.
 */
@Service
@Transactional(readOnly = true)
public class ProductVariantService {

    private final ProductVariantRepository variantRepository;
    private final CategoryAttributeRepository attributeRepository;
    private final ProductService productService;

    public ProductVariantService(ProductVariantRepository variantRepository,
                                  CategoryAttributeRepository attributeRepository,
                                  ProductService productService) {
        this.variantRepository = variantRepository;
        this.attributeRepository = attributeRepository;
        this.productService = productService;
    }

    @Transactional
    public ProductVariantResponse create(Long productId, ProductVariantRequest request) {
        Product product = productService.findOrThrow(productId);
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        applyRequest(variant, product, request, null);
        return toResponse(variantRepository.save(variant));
    }

    public List<ProductVariantResponse> findByProduct(Long productId) {
        productService.findOrThrow(productId);
        return variantRepository.findByProductIdOrderByVariantNameAsc(productId).stream().map(this::toResponse).toList();
    }

    public ProductVariantResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public ProductVariantResponse update(Long productId, Long id, ProductVariantRequest request) {
        ProductVariant variant = findOrThrow(id);
        requireBelongsToProduct(variant, productId);
        applyRequest(variant, variant.getProduct(), request, id);
        return toResponse(variantRepository.save(variant));
    }

    @Transactional
    public void delete(Long id) {
        if (!variantRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product variant not found with id: " + id);
        }
        variantRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        ProductVariant variant = findOrThrow(id);
        variant.setIsActive(Boolean.TRUE.equals(request.isActive()));
        ProductVariant saved = variantRepository.save(variant);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    public boolean codeExists(String variantCode, Long excludeId) {
        String trimmed = variantCode == null ? "" : variantCode.trim();
        if (trimmed.isEmpty()) return false;
        return excludeId != null
            ? variantRepository.existsByVariantCodeIgnoreCaseAndIdNot(trimmed, excludeId)
            : variantRepository.existsByVariantCodeIgnoreCase(trimmed);
    }

    public boolean barcodeExists(String barcode, Long excludeId) {
        String trimmed = barcode == null ? "" : barcode.trim();
        if (trimmed.isEmpty()) return false;
        return excludeId != null
            ? variantRepository.existsByBarcodeIgnoreCaseAndIdNot(trimmed, excludeId)
            : variantRepository.existsByBarcodeIgnoreCase(trimmed);
    }

    /** The barcode-scan lookup workflow — see ProductVariantController's /by-barcode endpoint. */
    public ProductVariantResponse findByBarcode(String barcode) {
        String trimmed = trim(barcode);
        if (trimmed == null) {
            throw new ResourceNotFoundException("No product variant found for barcode: " + barcode);
        }
        ProductVariant variant = variantRepository.findByBarcodeIgnoreCase(trimmed)
            .orElseThrow(() -> new ResourceNotFoundException("No product variant found for barcode: " + barcode));
        return toResponse(variant);
    }

    private void applyRequest(ProductVariant variant, Product product, ProductVariantRequest request, Long excludeId) {
        String code = requireTrimmed(request.variantCode(), "Variant code is required");
        String name = requireTrimmed(request.variantName(), "Variant name is required");
        String barcode = trim(request.barcode());

        if (codeExists(code, excludeId)) {
            throw new IllegalArgumentException("A variant with the code '" + code + "' already exists");
        }
        if (barcode != null && barcodeExists(barcode, excludeId)) {
            throw new IllegalArgumentException("A variant with the barcode '" + barcode + "' already exists");
        }

        variant.setVariantCode(code);
        variant.setVariantName(name);
        variant.setBarcode(barcode);
        variant.setTrackingMode(parseTrackingMode(request.trackingMode()));
        variant.setStandardCost(request.standardCost());
        variant.setListPrice(request.listPrice());
        if (request.isActive() != null) variant.setIsActive(request.isActive());

        applyAttributeValues(variant, product, request.attributeValues());
    }

    /** Blank/null defaults to NONE — mirrors ProductService.parseTrackingMode. */
    private StockTrackingMode parseTrackingMode(String value) {
        String trimmed = trim(value);
        if (trimmed == null) return StockTrackingMode.NONE;
        try {
            return StockTrackingMode.valueOf(trimmed.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid tracking mode '" + value + "' — must be NONE, BATCH, or SERIAL");
        }
    }

    /**
     * Unlike {@code ProductService.applyAttributeValues}, a variant's submitted values aren't
     * required to cover every one of the parent category's attribute definitions — only the ones
     * actually being overridden at the variant level (e.g. "Size"/"Color"); the rest stay the
     * parent product's own values, read from there by callers rather than duplicated per variant.
     * A definition's own {@code isRequired} flag is therefore not enforced here (it's already
     * enforced once, on the parent product, by {@code ProductService}).
     */
    private void applyAttributeValues(ProductVariant variant, Product product, List<ProductAttributeValueRequest> values) {
        List<CategoryAttribute> definitions = attributeRepository.findByCategoryIdOrderByDisplayOrderAscNameAsc(product.getCategory().getId());
        Map<Long, CategoryAttribute> byId = new HashMap<>();
        for (CategoryAttribute def : definitions) byId.put(def.getId(), def);

        Map<Long, String> submitted = new HashMap<>();
        if (values != null) {
            for (ProductAttributeValueRequest v : values) {
                CategoryAttribute def = byId.get(v.attributeId());
                if (def == null) {
                    throw new IllegalArgumentException(
                        "Attribute id " + v.attributeId() + " is not defined on category '" + product.getCategory().getName() + "'");
                }
                String trimmed = trim(v.value());
                if (trimmed != null) submitted.put(v.attributeId(), trimmed);
            }
        }

        variant.getAttributeValues().clear();
        for (Map.Entry<Long, String> entry : submitted.entrySet()) {
            CategoryAttribute def = byId.get(entry.getKey());
            ProductVariantAttributeValue vav = new ProductVariantAttributeValue();
            vav.setVariant(variant);
            vav.setAttribute(def);
            TypedAttributeValueSupport.applyTypedValue(vav, def, entry.getValue());
            variant.getAttributeValues().add(vav);
        }
    }

    private void requireBelongsToProduct(ProductVariant variant, Long productId) {
        if (!variant.getProduct().getId().equals(productId)) {
            throw new IllegalArgumentException("That variant does not belong to this product");
        }
    }

    ProductVariant findOrThrow(Long id) {
        return variantRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product variant not found with id: " + id));
    }

    private ProductVariantResponse toResponse(ProductVariant v) {
        Product product = v.getProduct();
        List<ProductAttributeValueResponse> attrValues = new ArrayList<>();
        for (ProductVariantAttributeValue vav : v.getAttributeValues()) {
            CategoryAttribute def = vav.getAttribute();
            attrValues.add(new ProductAttributeValueResponse(def.getId(), def.getName(), def.getDataType().name(), vav.renderValue()));
        }
        return new ProductVariantResponse(v.getId(), product.getId(), product.getProductCode(), product.getProductName(),
            v.getVariantCode(), v.getVariantName(), v.getBarcode(), v.getTrackingMode().name(),
            v.getStandardCost(), v.getListPrice(), v.getIsActive(), v.getCreatedAt(), v.getUpdatedAt(), attrValues);
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String requireTrimmed(String s, String message) {
        String t = trim(s);
        if (t == null) throw new IllegalArgumentException(message);
        return t;
    }
}
