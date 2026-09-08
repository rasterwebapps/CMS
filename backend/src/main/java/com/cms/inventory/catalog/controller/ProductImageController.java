package com.cms.inventory.catalog.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cms.dto.DocumentFileDownload;
import com.cms.inventory.catalog.dto.ProductImageResponse;
import com.cms.inventory.catalog.service.ProductImageService;

@RestController
@RequestMapping("/inventory/products/{productId}/images")
public class ProductImageController {

    private static final String VIEW_ANY = "@perm.hasAny('INVENTORY_PRODUCT_VIEW', 'INVENTORY_PRODUCT_MANAGE', 'INVENTORY_PRODUCT_IMAGE_MANAGE')";

    private final ProductImageService imageService;

    public ProductImageController(ProductImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<List<ProductImageResponse>> findByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(imageService.findByProduct(productId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@perm.has('INVENTORY_PRODUCT_IMAGE_MANAGE')")
    public ResponseEntity<ProductImageResponse> upload(
            @PathVariable Long productId,
            @RequestParam(required = false, defaultValue = "false") boolean setPrimary,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(imageService.upload(productId, file, setPrimary));
    }

    @PostMapping("/{id}/set-primary")
    @PreAuthorize("@perm.has('INVENTORY_PRODUCT_IMAGE_MANAGE')")
    public ResponseEntity<ProductImageResponse> setPrimary(@PathVariable Long productId, @PathVariable Long id) {
        return ResponseEntity.ok(imageService.setPrimary(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_PRODUCT_IMAGE_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long productId, @PathVariable Long id) {
        imageService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/download")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<Resource> download(@PathVariable Long productId, @PathVariable Long id) {
        DocumentFileDownload download = imageService.getFileForDownload(id);
        ByteArrayResource resource = new ByteArrayResource(download.data());

        String encoded = URLEncoder.encode(download.fileName(), StandardCharsets.UTF_8).replace("+", "%20");
        String contentDisposition = "inline; filename=\"" + sanitizeForHeader(download.fileName())
            + "\"; filename*=UTF-8''" + encoded;

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
            .contentType(MediaType.parseMediaType(download.contentType()))
            .contentLength(download.data().length)
            .body(resource);
    }

    private static String sanitizeForHeader(String name) {
        return name.replaceAll("[\\\\\"\\r\\n]", "_");
    }
}
