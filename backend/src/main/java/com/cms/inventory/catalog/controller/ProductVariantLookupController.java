package com.cms.inventory.catalog.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.inventory.catalog.dto.ProductVariantResponse;
import com.cms.inventory.catalog.service.InventoryBarcodeService;
import com.cms.inventory.catalog.service.ProductVariantService;

/** Barcode-scan lookup and label printing for a {@code ProductVariant}, addressed without a
 *  parent productId — same reasoning as {@code ProductController}'s own /by-barcode and
 *  /{id}/barcode.png, just for the variant-per-barcode case ("barcode-per-variant"). */
@RestController
@RequestMapping("/inventory/product-variants")
public class ProductVariantLookupController {

    private static final Logger log = LoggerFactory.getLogger(ProductVariantLookupController.class);

    private final ProductVariantService variantService;
    private final InventoryBarcodeService barcodeService;

    public ProductVariantLookupController(ProductVariantService variantService, InventoryBarcodeService barcodeService) {
        this.variantService = variantService;
        this.barcodeService = barcodeService;
    }

    @GetMapping("/by-barcode")
    @PreAuthorize("@perm.hasAny('INVENTORY_PRODUCT_VARIANT_VIEW', 'INVENTORY_PRODUCT_VARIANT_MANAGE')")
    public ResponseEntity<ProductVariantResponse> findByBarcode(@RequestParam String value) {
        return ResponseEntity.ok(variantService.findByBarcode(value));
    }

    @GetMapping(value = "/{id}/barcode.png", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("@perm.hasAny('INVENTORY_PRODUCT_VARIANT_VIEW', 'INVENTORY_PRODUCT_VARIANT_MANAGE')")
    public ResponseEntity<byte[]> barcodePng(@PathVariable Long id) {
        ProductVariantResponse variant = variantService.findById(id);
        // Prefer the variant's own captured barcode/GTIN; falls back to its own variantCode when
        // none has been captured yet (mirrors ProductController.barcodePng's productCode fallback).
        String code = variant.barcode() != null && !variant.barcode().isBlank() ? variant.barcode() : variant.variantCode();
        try {
            byte[] png = barcodeService.generateBarcodePng(code, variant.productName() + " — " + variant.variantName(), code);
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
        } catch (Exception e) {
            log.error("Failed to generate barcode PNG for product variant id={}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
