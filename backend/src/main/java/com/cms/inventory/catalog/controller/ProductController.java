package com.cms.inventory.catalog.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.inventory.catalog.dto.NextProductCodeResponse;
import com.cms.inventory.catalog.dto.ProductCodeRegenerationResult;
import com.cms.inventory.catalog.dto.ProductRequest;
import com.cms.inventory.catalog.dto.ProductResponse;
import com.cms.inventory.catalog.service.InventoryBarcodeService;
import com.cms.inventory.catalog.service.ProductCodeGeneratorService;
import com.cms.inventory.catalog.service.ProductService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/products")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;
    private final InventoryBarcodeService barcodeService;
    private final ProductCodeGeneratorService codeGeneratorService;

    public ProductController(ProductService productService, InventoryBarcodeService barcodeService,
                              ProductCodeGeneratorService codeGeneratorService) {
        this.productService = productService;
        this.barcodeService = barcodeService;
        this.codeGeneratorService = codeGeneratorService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_PRODUCT_MANAGE')")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.hasAny('INVENTORY_PRODUCT_VIEW', 'INVENTORY_PRODUCT_MANAGE')")
    public ResponseEntity<ProductResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_PRODUCT_MANAGE')")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('INVENTORY_PRODUCT_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('INVENTORY_PRODUCT_MANAGE')")
    public ResponseEntity<ActiveStatusUpdateResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ActiveStatusUpdateRequest request) {
        return ResponseEntity.ok(productService.updateStatus(id, request));
    }

    @GetMapping("/page")
    @PreAuthorize("@perm.hasAny('INVENTORY_PRODUCT_VIEW', 'INVENTORY_PRODUCT_MANAGE')")
    public ResponseEntity<Page<ProductResponse>> findPage(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 25, sort = "productName", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(productService.findPage(search, categoryId, pageable));
    }

    @GetMapping("/code-exists")
    @PreAuthorize("@perm.has('INVENTORY_PRODUCT_MANAGE')")
    public ResponseEntity<Boolean> codeExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(productService.codeExists(value, excludeId));
    }

    /** Live preview for the Add Product form — the code a save will actually be assigned, without
     *  committing the category's counter. */
    @GetMapping("/next-code")
    @PreAuthorize("@perm.has('INVENTORY_PRODUCT_MANAGE')")
    public ResponseEntity<NextProductCodeResponse> nextCode(@RequestParam Long categoryId) {
        return ResponseEntity.ok(new NextProductCodeResponse(codeGeneratorService.previewNextCode(categoryId)));
    }

    /** Preview of the "Regenerate Product Codes" admin action — computes the full before/after
     *  mapping without writing anything, for a confirm dialog. */
    @GetMapping("/regenerate-codes/preview")
    @PreAuthorize("@perm.has('INVENTORY_PRODUCT_REGENERATE_CODES')")
    public ResponseEntity<ProductCodeRegenerationResult> previewRegenerateCodes() {
        return ResponseEntity.ok(codeGeneratorService.regenerateAllCodes(true));
    }

    /** Executes the "Regenerate Product Codes" admin action — bulk-reassigns every product's code
     *  to a fresh, gap-free per-category sequence. See ProductCodeGeneratorService for the
     *  transactional-safety design; this is a mass rewrite of production data and should only be
     *  run after a backup, per the project's production-data-safety policy. */
    @PostMapping("/regenerate-codes")
    @PreAuthorize("@perm.has('INVENTORY_PRODUCT_REGENERATE_CODES')")
    public ResponseEntity<ProductCodeRegenerationResult> regenerateCodes() {
        return ResponseEntity.ok(codeGeneratorService.regenerateAllCodes(false));
    }

    @GetMapping("/name-exists")
    @PreAuthorize("@perm.has('INVENTORY_PRODUCT_MANAGE')")
    public ResponseEntity<Boolean> nameExists(
            @RequestParam String value,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(productService.nameExists(value, categoryId, excludeId));
    }

    @GetMapping("/barcode-exists")
    @PreAuthorize("@perm.has('INVENTORY_PRODUCT_MANAGE')")
    public ResponseEntity<Boolean> barcodeExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(productService.barcodeExists(value, excludeId));
    }

    /** The barcode-scan lookup workflow — find a product by its captured barcode/GTIN. */
    @GetMapping("/by-barcode")
    @PreAuthorize("@perm.hasAny('INVENTORY_PRODUCT_VIEW', 'INVENTORY_PRODUCT_MANAGE')")
    public ResponseEntity<ProductResponse> findByBarcode(@RequestParam String value) {
        return ResponseEntity.ok(productService.findByBarcode(value));
    }

    @GetMapping(value = "/{id}/barcode.png", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("@perm.hasAny('INVENTORY_PRODUCT_VIEW', 'INVENTORY_PRODUCT_MANAGE')")
    public ResponseEntity<byte[]> barcodePng(@PathVariable Long id) {
        ProductResponse product = productService.findById(id);
        // Prefer the captured real-world barcode/GTIN; every product can still get a printable
        // label from its own productCode when none has been captured yet.
        String code = product.barcode() != null && !product.barcode().isBlank() ? product.barcode() : product.productCode();
        try {
            byte[] png = barcodeService.generateBarcodePng(code, product.productName(), code);
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
        } catch (Exception e) {
            log.error("Failed to generate barcode PNG for product id={}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
