package com.cms.inventory.procurement.model;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One supplier's quoted price for one {@link QuotationRequestLine} — keyed in by staff once
 * received (no outbound email/portal exists anywhere in this module, so the actual back-and-forth
 * with the supplier happens outside the system). At most one response per (line, supplier) pair;
 * editable in place while the line is still {@code PENDING}. No minimum number of responses is
 * required before {@link QuotationRequestLine#getAwardedResponseLine()} can point at one of
 * these.
 */
@Entity
@Table(name = "quotation_response_lines")
public class QuotationResponseLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_request_line_id", nullable = false)
    private QuotationRequestLine quotationRequestLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "quoted_unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal quotedUnitPrice;

    @Column(name = "quoted_lead_time_days")
    private Integer quotedLeadTimeDays;

    @Column(length = 500)
    private String notes;

    @Column(name = "recorded_by", length = 255)
    private String recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public QuotationRequestLine getQuotationRequestLine() { return quotationRequestLine; }
    public void setQuotationRequestLine(QuotationRequestLine quotationRequestLine) { this.quotationRequestLine = quotationRequestLine; }

    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }

    public BigDecimal getQuotedUnitPrice() { return quotedUnitPrice; }
    public void setQuotedUnitPrice(BigDecimal quotedUnitPrice) { this.quotedUnitPrice = quotedUnitPrice; }

    public Integer getQuotedLeadTimeDays() { return quotedLeadTimeDays; }
    public void setQuotedLeadTimeDays(Integer quotedLeadTimeDays) { this.quotedLeadTimeDays = quotedLeadTimeDays; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }

    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
