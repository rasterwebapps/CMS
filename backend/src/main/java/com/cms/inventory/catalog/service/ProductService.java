package com.cms.inventory.catalog.service;

import java.util.ArrayList;
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
import com.cms.inventory.catalog.model.Category;
import com.cms.inventory.catalog.model.CategoryAttribute;
import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.catalog.model.ProductAlias;
import com.cms.inventory.catalog.model.ProductAttributeValue;
import com.cms.inventory.catalog.model.Uom;
import com.cms.inventory.catalog.repository.CategoryAttributeRepository;
import com.cms.inventory.catalog.repository.ProductRepository;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryAttributeRepository attributeRepository;
    private final CategoryService categoryService;
    private final UomService uomService;

    public ProductService(ProductRepository productRepository,
                           CategoryAttributeRepository attributeRepository,
                           CategoryService categoryService,
                           UomService uomService) {
        this.productRepository = productRepository;
        this.attributeRepository = attributeRepository;
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
        product.setReorderLevel(request.reorderLevel());
        product.setReorderQty(request.reorderQty());
        if (request.isAsset() != null) product.setIsAsset(request.isAsset());
        if (request.isConsumable() != null) product.setIsConsumable(request.isConsumable());
        if (request.isService() != null) product.setIsService(request.isService());
        if (request.isLoanable() != null) product.setIsLoanable(request.isLoanable());
        product.setDepreciationRate(request.depreciationRate());
        product.setWarrantyPeriodMonths(request.warrantyPeriodMonths());
        product.setDescription(trim(request.description()));
        if (request.isActive() != null) product.setIsActive(request.isActive());

        applyAliases(product, request.aliases());
        applyAttributeValues(product, category, request.attributeValues());
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
            ProductAttributeValue pav = new ProductAttributeValue();
            pav.setProduct(product);
            pav.setAttribute(byId.get(entry.getKey()));
            pav.setValue(entry.getValue());
            product.getAttributeValues().add(pav);
        }
    }

    Product findOrThrow(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private ProductResponse toResponse(Product p) {
        Category category = p.getCategory();
        Uom uom = p.getBaseUom();
        List<String> aliases = p.getAliases().stream().map(ProductAlias::getAliasName).toList();
        List<ProductAttributeValueResponse> attrValues = new ArrayList<>();
        for (ProductAttributeValue pav : p.getAttributeValues()) {
            CategoryAttribute def = pav.getAttribute();
            attrValues.add(new ProductAttributeValueResponse(def.getId(), def.getName(), def.getDataType().name(), pav.getValue()));
        }
        return new ProductResponse(p.getId(), p.getProductCode(), p.getProductName(),
            category.getId(), category.getName(), uom.getId(), uom.getCode(), uom.getName(),
            p.getReorderLevel(), p.getReorderQty(), p.getIsAsset(), p.getIsConsumable(), p.getIsService(), p.getIsLoanable(),
            p.getDepreciationRate(), p.getWarrantyPeriodMonths(), p.getDescription(), p.getIsActive(),
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
