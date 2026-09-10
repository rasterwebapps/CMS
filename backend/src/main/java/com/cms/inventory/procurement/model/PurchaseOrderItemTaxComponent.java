package com.cms.inventory.procurement.model;

import java.math.BigDecimal;
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
 * One applied {@link TaxSubType} component's amount on a {@link PurchaseOrderItem}, snapshotted
 * at line-creation time — {@code componentName}/{@code splitPercentApplied} are copied rather
 * than read live through {@code taxSubType} so this row stays self-explanatory (matches GST
 * filing needs — IGST/CGST/SGST reported separately) even if the referenced TaxSubType is edited
 * or deactivated later. {@code taxSubType} is nullable for the same reason
 * {@code PurchaseOrderItem.taxRule} tolerates a stale/deleted reference — the FK is kept for
 * traceability, not relied on for display. See the "GAP-02 pickup" decision-log entry.
 */
@Entity
@Table(name = "purchase_order_item_tax_components")
@EntityListeners(AuditingEntityListener.class)
public class PurchaseOrderItemTaxComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_item_id", nullable = false)
    private PurchaseOrderItem purchaseOrderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_sub_type_id")
    private TaxSubType taxSubType;

    @Column(name = "component_name", nullable = false, length = 50)
    private String componentName;

    @Column(name = "split_percent_applied", nullable = false, precision = 5, scale = 2)
    private BigDecimal splitPercentApplied;

    @Column(name = "component_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal componentAmount;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PurchaseOrderItem getPurchaseOrderItem() { return purchaseOrderItem; }
    public void setPurchaseOrderItem(PurchaseOrderItem purchaseOrderItem) { this.purchaseOrderItem = purchaseOrderItem; }

    public TaxSubType getTaxSubType() { return taxSubType; }
    public void setTaxSubType(TaxSubType taxSubType) { this.taxSubType = taxSubType; }

    public String getComponentName() { return componentName; }
    public void setComponentName(String componentName) { this.componentName = componentName; }

    public BigDecimal getSplitPercentApplied() { return splitPercentApplied; }
    public void setSplitPercentApplied(BigDecimal splitPercentApplied) { this.splitPercentApplied = splitPercentApplied; }

    public BigDecimal getComponentAmount() { return componentAmount; }
    public void setComponentAmount(BigDecimal componentAmount) { this.componentAmount = componentAmount; }

    public Instant getCreatedAt() { return createdAt; }
}
