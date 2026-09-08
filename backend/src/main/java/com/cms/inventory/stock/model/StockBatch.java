package com.cms.inventory.stock.model;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.inventory.catalog.model.Product;

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
 * A batch or serial number a {@link Product}'s stock is tracked under (expiry date where
 * relevant). Created on the fly the first time a given batch/serial number is used for a product
 * on a stock movement — no dedicated CRUD screen, see the 2026-09-07 "Stock Tracking slice"
 * decision-log entry.
 */
@Entity
@Table(name = "stock_batches")
@EntityListeners(AuditingEntityListener.class)
public class StockBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "batch_or_serial_no", nullable = false, length = 100)
    private String batchOrSerialNo;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "is_consignment", nullable = false)
    private Boolean isConsignment = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public String getBatchOrSerialNo() { return batchOrSerialNo; }
    public void setBatchOrSerialNo(String batchOrSerialNo) { this.batchOrSerialNo = batchOrSerialNo; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public Boolean getIsConsignment() { return isConsignment; }
    public void setIsConsignment(Boolean isConsignment) { this.isConsignment = isConsignment; }

    public Instant getCreatedAt() { return createdAt; }
}
