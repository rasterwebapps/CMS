package com.cms.inventory.procurement.model;

import java.math.BigDecimal;

import com.cms.inventory.catalog.model.Product;

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
 * A negotiated rate for one {@link Product} under a {@link RateContract}. Managed as a child
 * collection of its RateContract, replaced wholesale on every RateContract save — no lifecycle of
 * its own, same pattern as {@code ProductAlias} on {@code Product}. A {@link VendorProductMapping}
 * that links this contract and is currently within its active date window shows this rate in place
 * of its own {@code unitPrice}; see the "VendorProductMapping slice" decision-log entry.
 */
@Entity
@Table(name = "rate_contract_lines")
public class RateContractLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rate_contract_id", nullable = false)
    private RateContract rateContract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "negotiated_rate", nullable = false, precision = 14, scale = 2)
    private BigDecimal negotiatedRate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public RateContract getRateContract() { return rateContract; }
    public void setRateContract(RateContract rateContract) { this.rateContract = rateContract; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public BigDecimal getNegotiatedRate() { return negotiatedRate; }
    public void setNegotiatedRate(BigDecimal negotiatedRate) { this.negotiatedRate = negotiatedRate; }
}
