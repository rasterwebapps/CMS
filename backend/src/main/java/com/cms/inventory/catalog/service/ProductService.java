package com.cms.inventory.catalog.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.catalog.dto.ProductAttributeValueRequest;
import com.cms.inventory.catalog.dto.ProductAttributeValueResponse;
import com.cms.inventory.catalog.dto.ProductRequest;
import com.cms.inventory.catalog.dto.ProductResponse;
import com.cms.inventory.catalog.model.Brand;
import com.cms.inventory.catalog.model.Category;
import com.cms.inventory.catalog.model.CategoryAttribute;
import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.catalog.model.ProductAlias;
import com.cms.inventory.catalog.model.ProductAttributeValue;
import com.cms.inventory.catalog.model.Uom;
import com.cms.inventory.catalog.model.enums.AttributeDataType;
import com.cms.inventory.catalog.model.enums.StockTrackingMode;
import com.cms.inventory.catalog.repository.CategoryAttributeRepository;
import com.cms.inventory.catalog.repository.ProductRepository;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryAttributeRepository attributeRepository;
    private final CategoryService categoryService;
    private final UomService uomService;
    private final BrandService brandService;

    public ProductService(ProductRepository productRepository,
                           CategoryAttributeRepository attributeRepository,
                           CategoryService categoryService,
                           UomService uomService,
                           BrandService brandService) {
        this.productRepository = productRepository;
        this.attributeRepository = attributeRepository;
        this.brandService = brandService;
        this.categoryService = categoryService;
        this.uomService = uomService;
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        applyRequest(product, request, null);
        return toResponse(productRepository.save(product));
    }

    public Page<ProductResponse> findPage(String search, Long categoryId, Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> {
            var predicates = cb.conjunction();
            if (categoryId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("category").get("id"), categoryId));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates = cb.and(predicates, cb.or(
                    cb.like(cb.lower(root.get("productName")), pattern),
                    cb.like(cb.lower(root.get("productCode")), pattern)
                ));
            }
            return predicates;
        };
        return productRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public ProductResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findOrThrow(id);
        applyRequest(product, request, id);
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        Product product = findOrThrow(id);
        product.setIsActive(Boolean.TRUE.equals(request.isActive()));
        Product saved = productRepository.save(product);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    public boolean codeExists(String code, Long excludeId) {
        String trimmed = code == null ? "" : code.trim();
        return excludeId != null
            ? productRepository.existsByProductCodeIgnoreCaseAndIdNot(trimmed, excludeId)
            : productRepository.existsByProductCodeIgnoreCase(trimmed);
    }

    public boolean nameExists(String name, Long categoryId, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        if (categoryId == null) return false;
        return excludeId != null
            ? productRepository.existsByProductNameIgnoreCaseAndCategoryIdAndIdNot(trimmed, categoryId, excludeId)
            : productRepository.existsByProductNameIgnoreCaseAndCategoryId(trimmed, categoryId);
    }

    private void applyRequest(Product product, ProductRequest request, Long excludeId) {
        String code = requireTrimmed(request.productCode(), "Product code is required");
        String name = requireTrimmed(request.productName(), "Product name is required");
        Category category = categoryService.findOrThrow(request.categoryId());
        Uom baseUom = uomService.findOrThrow(request.baseUomId());
        Brand brand = request.brandId() != null ? brandService.findOrThrow(request.brandId()) : null;

        boolean codeTaken = excludeId != null
            ? productRepository.existsByProductCodeIgnoreCaseAndIdNot(code, excludeId)
            : productRepository.existsByProductCodeIgnoreCase(code);
        if (codeTaken) {
            throw new IllegalArgumentException("A product with the code '" + code + "' already exists");
        }
        boolean nameTaken = excludeId != null
            ? productRepository.existsByProductNameIgnoreCaseAndCategoryIdAndIdNot(name, category.getId(), excludeId)
            : productRepository.existsByProductNameIgnoreCaseAndCategoryId(name, category.getId());
        if (nameTaken) {
            throw new IllegalArgumentException("A product named '" + name + "' already exists under '" + category.getName() + "'");
        }

        product.setProductCode(code);
        product.setProductName(name);
        product.setCategory(category);
        product.setBaseUom(baseUom);
        product.setBrand(brand);
        product.setReorderLevel(request.reorderLevel());
        product.setReorderQty(request.reorderQty());
        if (request.isAsset() != null) product.setIsAsset(request.isAsset());
        if (request.isConsumable() != null) product.setIsConsumable(request.isConsumable());
        if (request.isService() != null) product.setIsService(request.isService());
        if (request.isLoanable() != null) product.setIsLoanable(request.isLoanable());
        product.setTrackingMode(parseTrackingMode(request.trackingMode()));
        product.setDepreciationRate(request.depreciationRate());
        product.setWarrantyPeriodMonths(request.warrantyPeriodMonths());
        product.setLengthCm(request.lengthCm());
        product.setWidthCm(request.widthCm());
        product.setHeightCm(request.heightCm());
        product.setWeightKg(request.weightKg());
        product.setDescription(trim(request.description()));
        if (request.isActive() != null) product.setIsActive(request.isActive());

        applyAliases(product, request.aliases());
        applyAttributeValues(product, category, request.attributeValues());
    }

    /** Blank/null defaults to NONE — today's pre-existing free-text batch/serial behavior. */
    private StockTrackingMode parseTrackingMode(String value) {
        String trimmed = trim(value);
        if (trimmed == null) return StockTrackingMode.NONE;
        try {
            return StockTrackingMode.valueOf(trimmed.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid tracking mode '" + value + "' — must be NONE, BATCH, or SERIAL");
        }
    }

    private void applyAliases(Product product, List<String> aliasNames) {
        product.getAliases().clear();
        if (aliasNames == null) return;
        for (String raw : aliasNames) {
            String trimmed = trim(raw);
            if (trimmed == null) continue;
            ProductAlias alias = new ProductAlias();
            alias.setProduct(product);
            alias.setAliasName(trimmed);
            product.getAliases().add(alias);
        }
    }

    private void applyAttributeValues(Product product, Category category, List<ProductAttributeValueRequest> values) {
        List<CategoryAttribute> definitions = attributeRepository.findByCategoryIdOrderByDisplayOrderAscNameAsc(category.getId());
        Map<Long, CategoryAttribute> byId = new HashMap<>();
        for (CategoryAttribute def : definitions) byId.put(def.getId(), def);

        Map<Long, String> submitted = new HashMap<>();
        if (values != null) {
            for (ProductAttributeValueRequest v : values) {
                CategoryAttribute def = byId.get(v.attributeId());
                if (def == null) {
                    throw new IllegalArgumentException("Attribute id " + v.attributeId() + " is not defined on category '" + category.getName() + "'");
                }
                String trimmed = trim(v.value());
                if (trimmed != null) submitted.put(v.attributeId(), trimmed);
            }
        }

        for (CategoryAttribute def : definitions) {
            if (Boolean.TRUE.equals(def.getIsRequired()) && !submitted.containsKey(def.getId())) {
                throw new IllegalArgumentException("Attribute '" + def.getName() + "' is required for this category");
            }
        }

        product.getAttributeValues().clear();
        for (Map.Entry<Long, String> entry : submitted.entrySet()) {
            CategoryAttribute def = byId.get(entry.getKey());
            ProductAttributeValue pav = new ProductAttributeValue();
            pav.setProduct(product);
            pav.setAttribute(def);
            applyTypedValue(pav, def, entry.getValue());
            product.getAttributeValues().add(pav);
        }
    }

    /**
     * Parses {@code raw} (the plain string the form/API submits) per the attribute's declared
     * {@link AttributeDataType} and stores it in the matching typed column — the "typed EAV
     * storage" behavior; see the 2026-09-11 DECISION_LOG entry.
     */
    private void applyTypedValue(ProductAttributeValue pav, CategoryAttribute def, String raw) {
        switch (def.getDataType()) {
            case NUMBER -> {
                try {
                    pav.setNumberValue(new BigDecimal(raw));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Attribute '" + def.getName() + "' expects a number");
                }
            }
            case DATE -> {
                try {
                    pav.setDateValue(LocalDate.parse(raw));
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("Attribute '" + def.getName() + "' expects a date (yyyy-MM-dd)");
                }
            }
            case BOOLEAN -> {
                if (!"true".equalsIgnoreCase(raw) && !"false".equalsIgnoreCase(raw)) {
                    throw new IllegalArgumentException("Attribute '" + def.getName() + "' expects true or false");
                }
                pav.setBooleanValue(Boolean.parseBoolean(raw));
            }
            case ENUM -> {
                List<String> options = parseEnumOptions(def.getEnumOptions());
                if (!options.contains(raw)) {
                    throw new IllegalArgumentException("Attribute '" + def.getName() + "' must be one of: " + String.join(", ", options));
                }
                pav.setTextValue(raw);
            }
            case TEXT -> pav.setTextValue(raw);
        }
    }

    private static List<String> parseEnumOptions(String enumOptions) {
        if (enumOptions == null) return List.of();
        return Arrays.stream(enumOptions.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }

    Product findOrThrow(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private ProductResponse toResponse(Product p) {
        Category category = p.getCategory();
        Uom uom = p.getBaseUom();
        Brand brand = p.getBrand();
        List<String> aliases = p.getAliases().stream().map(ProductAlias::getAliasName).toList();
        List<ProductAttributeValueResponse> attrValues = new ArrayList<>();
        for (ProductAttributeValue pav : p.getAttributeValues()) {
            CategoryAttribute def = pav.getAttribute();
            attrValues.add(new ProductAttributeValueResponse(def.getId(), def.getName(), def.getDataType().name(), pav.renderValue()));
        }
        return new ProductResponse(p.getId(), p.getProductCode(), p.getProductName(),
            category.getId(), category.getName(), uom.getId(), uom.getCode(), uom.getName(),
            brand != null ? brand.getId() : null, brand != null ? brand.getName() : null,
            p.getReorderLevel(), p.getReorderQty(), p.getIsAsset(), p.getIsConsumable(), p.getIsService(), p.getIsLoanable(),
            p.getTrackingMode().name(),
            p.getDepreciationRate(), p.getWarrantyPeriodMonths(),
            p.getLengthCm(), p.getWidthCm(), p.getHeightCm(), p.getWeightKg(),
            p.getDescription(), p.getIsActive(),
            p.getCreatedAt(), p.getUpdatedAt(), aliases, attrValues);
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
