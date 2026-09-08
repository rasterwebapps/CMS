package com.cms.inventory.catalog.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A photo attached to a {@link Product} — the {@code ProductImage} the "Product slice" decision-
 * log entry deliberately deferred, picked up now per the plan's "Also outstanding" checklist
 * item, following the `FloorPlan`/`MinioStorageService` precedent exactly (a {@code storageKey}
 * reference into MinIO plus original filename/content-type; the binary itself never touches this
 * table). Deliberately **not** a child collection wholesale-replaced on {@code Product}'s own
 * save (the pattern {@code aliases}/{@code attributeValues} already use) — a product's photos are
 * managed independently (upload one, delete one, change which is primary) rather than as a
 * single all-or-nothing list submitted with the rest of the product form. A product can have any
 * number of images; at most one is marked {@code isPrimary} (enforced in the service, not the
 * database, matching how single-row invariants are enforced elsewhere in this module). See the
 * "Product Image slice" decision-log entry.
 */
@Entity
@Table(name = "product_images")
@EntityListeners(AuditingEntityListener.class)
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(name = "original_content_type")
    private String originalContentType;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getOriginalContentType() { return originalContentType; }
    public void setOriginalContentType(String originalContentType) { this.originalContentType = originalContentType; }

    public Boolean getIsPrimary() { return isPrimary; }
    public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }

    public Instant getCreatedAt() { return createdAt; }
}
