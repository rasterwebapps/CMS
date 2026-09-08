package com.cms.inventory.catalog.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cms.dto.DocumentFileDownload;
import com.cms.exception.ResourceNotFoundException;
import com.cms.inventory.catalog.dto.ProductImageResponse;
import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.catalog.model.ProductImage;
import com.cms.inventory.catalog.repository.ProductImageRepository;
import com.cms.inventory.catalog.repository.ProductRepository;
import com.cms.service.StorageService;

/**
 * Owns Product photo upload/management — the {@code ProductImage} the "Product slice" decision-
 * log entry deliberately deferred, picked up per the plan's "Also outstanding" checklist item,
 * following the {@code FloorPlanService}/{@code MinioStorageService} precedent exactly. See the
 * {@link ProductImage} class docs and the "Product Image slice" decision-log entry.
 */
@Service
@Transactional(readOnly = true)
public class ProductImageService {

    public static final long MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final String STORAGE_FOLDER = "product_images";

    private final ProductImageRepository imageRepository;
    private final ProductRepository productRepository;
    private final StorageService storageService;

    public ProductImageService(ProductImageRepository imageRepository,
                                ProductRepository productRepository,
                                StorageService storageService) {
        this.imageRepository = imageRepository;
        this.productRepository = productRepository;
        this.storageService = storageService;
    }

    public List<ProductImageResponse> findByProduct(Long productId) {
        return imageRepository.findByProductIdOrderByIsPrimaryDescCreatedAtAsc(productId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public ProductImageResponse upload(Long productId, MultipartFile file, boolean setPrimary) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        validateFile(file);

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read uploaded file", ex);
        }

        String sanitizedName = sanitizeFileName(file.getOriginalFilename());
        String contentType = file.getContentType();
        String objectKey = STORAGE_FOLDER + "/" + productId + "-" + UUID.randomUUID() + "-" + sanitizedName;
        storageService.upload(objectKey, new ByteArrayInputStream(bytes), bytes.length, contentType);

        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setStorageKey(objectKey);
        image.setOriginalFileName(sanitizedName);
        image.setOriginalContentType(contentType);
        // The first image ever uploaded for a product automatically becomes primary — a caller
        // never has to remember to flag it, and a product is never left with zero primary image
        // once it has at least one.
        boolean noPrimaryYet = imageRepository.findFirstByProductIdAndIsPrimaryTrue(productId).isEmpty();
        image.setIsPrimary(setPrimary || noPrimaryYet);
        image = imageRepository.save(image);

        if (image.getIsPrimary()) {
            demoteOtherPrimaries(productId, image.getId());
        }
        return toResponse(image);
    }

    @Transactional
    public ProductImageResponse setPrimary(Long id) {
        ProductImage image = getOrThrow(id);
        image.setIsPrimary(true);
        imageRepository.save(image);
        demoteOtherPrimaries(image.getProduct().getId(), image.getId());
        return toResponse(image);
    }

    @Transactional
    public void delete(Long id) {
        ProductImage image = getOrThrow(id);
        Long productId = image.getProduct().getId();
        boolean wasPrimary = Boolean.TRUE.equals(image.getIsPrimary());
        storageService.delete(image.getStorageKey());
        imageRepository.delete(image);

        // Never leave a product with images but no primary — promote the oldest remaining one.
        if (wasPrimary) {
            List<ProductImage> remaining = imageRepository.findByProductIdOrderByIsPrimaryDescCreatedAtAsc(productId);
            if (!remaining.isEmpty()) {
                ProductImage next = remaining.get(0);
                next.setIsPrimary(true);
                imageRepository.save(next);
            }
        }
    }

    public DocumentFileDownload getFileForDownload(Long id) {
        ProductImage image = getOrThrow(id);
        String fileName = image.getOriginalFileName() != null ? image.getOriginalFileName() : "product-image";
        String contentType = image.getOriginalContentType() != null ? image.getOriginalContentType() : "application/octet-stream";
        return new DocumentFileDownload(fileName, contentType, storageService.downloadBytes(image.getStorageKey()));
    }

    private void demoteOtherPrimaries(Long productId, Long keepImageId) {
        imageRepository.findByProductIdOrderByIsPrimaryDescCreatedAtAsc(productId).stream()
            .filter(i -> Boolean.TRUE.equals(i.getIsPrimary()) && !i.getId().equals(keepImageId))
            .forEach(i -> {
                i.setIsPrimary(false);
                imageRepository.save(i);
            });
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                "File exceeds maximum allowed size of " + MAX_FILE_SIZE_BYTES + " bytes");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Product image file must be an image (got " + contentType + ")");
        }
    }

    private String sanitizeFileName(String original) {
        if (original == null) return "image";
        String name = original.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) name = name.substring(slash + 1);
        return name.isBlank() ? "image" : name;
    }

    private ProductImage getOrThrow(Long id) {
        return imageRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product image not found with id: " + id));
    }

    private ProductImageResponse toResponse(ProductImage image) {
        return new ProductImageResponse(
            image.getId(), image.getProduct().getId(), image.getOriginalFileName(),
            image.getOriginalContentType(), Boolean.TRUE.equals(image.getIsPrimary()), image.getCreatedAt());
    }
}
