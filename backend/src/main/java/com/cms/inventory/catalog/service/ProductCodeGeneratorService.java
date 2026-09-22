package com.cms.inventory.catalog.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.catalog.dto.ProductCodeChange;
import com.cms.inventory.catalog.dto.ProductCodeRegenerationResult;
import com.cms.inventory.catalog.model.Category;
import com.cms.inventory.catalog.model.CategoryProductSequence;
import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.catalog.repository.CategoryProductSequenceRepository;
import com.cms.inventory.catalog.repository.CategoryRepository;
import com.cms.inventory.catalog.repository.ProductRepository;

/**
 * Generates Product codes in the &lt;Category.shortCode&gt;-&lt;sequence&gt; pattern (e.g. STA-000001) —
 * the standard ERP "category-prefixed running number" scheme, replacing free-typed codes.
 * Per-category counters live in {@code category_product_sequences}, mirroring
 * {@code RollNumberGeneratorService}'s course-scoped counters.
 */
@Service
@Transactional(readOnly = true)
public class ProductCodeGeneratorService {

    private static final int SEQUENCE_WIDTH = 6;

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryProductSequenceRepository sequenceRepository;

    public ProductCodeGeneratorService(CategoryRepository categoryRepository,
                                        ProductRepository productRepository,
                                        CategoryProductSequenceRepository sequenceRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.sequenceRepository = sequenceRepository;
    }

    /** Increments the category's counter and returns the next code. Call only when actually
     *  creating a product — this commits the counter, it is not a preview. */
    @Transactional
    public String generateNextCode(Category category) {
        requireShortCode(category);
        CategoryProductSequence sequence = sequenceRepository.findByCategoryIdForUpdate(category.getId())
            .orElseGet(() -> new CategoryProductSequence(category.getId(), 0L));
        long next = sequence.getLastSequence() + 1;
        sequence.setLastSequence(next);
        sequenceRepository.save(sequence);
        return format(category.getShortCode(), next);
    }

    /** Read-only look-ahead for the Product form's live preview — does not touch the counter. */
    public String previewNextCode(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
        requireShortCode(category);
        long next = sequenceRepository.findByCategoryId(categoryId).map(s -> s.getLastSequence() + 1).orElse(1L);
        return format(category.getShortCode(), next);
    }

    /**
     * Reassigns every product's code to a fresh, gap-free per-category sequence under its
     * category's short code — oldest product first (createdAt, then id). {@code dryRun=true}
     * computes and returns the before/after mapping without writing anything.
     *
     * <p>Relies on {@code uq_products_code} being DEFERRABLE INITIALLY DEFERRED (V544) so writing
     * every product's new code inside this one transaction never trips a transient collision
     * against another, not-yet-updated product's old code — Postgres checks uniqueness once, at
     * commit, by which point every code in the table is already its final, mutually-unique value.
     */
    @Transactional
    public ProductCodeRegenerationResult regenerateAllCodes(boolean dryRun) {
        List<Category> missingShortCode = categoryRepository.findCategoriesWithProductsMissingShortCode();
        if (!missingShortCode.isEmpty()) {
            String names = missingShortCode.stream().map(Category::getName).collect(Collectors.joining(", "));
            throw new IllegalArgumentException(
                "Cannot regenerate product codes — these categories have products but no short code set: "
                    + names + ". Set a short code on each via Manage Categories first.");
        }

        List<Product> products = productRepository.findAllByOrderByCategoryIdAscCreatedAtAscIdAsc();
        Map<Long, Long> runningSequence = new HashMap<>();
        List<ProductCodeChange> changes = new ArrayList<>();

        for (Product product : products) {
            Category category = product.getCategory();
            long next = runningSequence.merge(category.getId(), 1L, Long::sum);
            String newCode = format(category.getShortCode(), next);
            if (!newCode.equals(product.getProductCode())) {
                changes.add(new ProductCodeChange(
                    product.getId(), product.getProductName(), category.getName(), product.getProductCode(), newCode));
                if (!dryRun) {
                    product.setProductCode(newCode);
                }
            }
        }

        if (!dryRun) {
            productRepository.saveAll(products);
            for (Map.Entry<Long, Long> entry : runningSequence.entrySet()) {
                CategoryProductSequence sequence = sequenceRepository.findByCategoryIdForUpdate(entry.getKey())
                    .orElseGet(() -> new CategoryProductSequence(entry.getKey(), 0L));
                sequence.setLastSequence(entry.getValue());
                sequenceRepository.save(sequence);
            }
        }

        return new ProductCodeRegenerationResult(changes.size(), changes);
    }

    private void requireShortCode(Category category) {
        if (category.getShortCode() == null || category.getShortCode().isBlank()) {
            throw new IllegalArgumentException(
                "Category '" + category.getName() + "' has no short code set — add one via Manage Categories before creating products in it.");
        }
    }

    private String format(String shortCode, long sequence) {
        return shortCode + "-" + String.format("%0" + SEQUENCE_WIDTH + "d", sequence);
    }
}
