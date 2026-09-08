package com.cms.inventory.procurement.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A vendor that products can be purchased from. {@code taxRegistrationId}/{@code
 * legalRegistrationNo} generalize GSTIN/PAN (closes GAP-02's naming concern); the bank fields and
 * both registration ids are masked to their last 4 characters in {@code SupplierResponse} for a
 * caller holding only {@code INVENTORY_SUPPLIER_VIEW} (not {@code _MANAGE}) — this module's
 * decision log already committed to that masking principle. {@code isApproved} is set only
 * through the dedicated approve endpoint ({@code INVENTORY_SUPPLIER_APPROVE}), never via the
 * regular create/update path, per the operation-wise permission mapping rule. {@code
 * portalAccessEnabled} is a reserved flag for a future actual vendor-portal login capability — no
 * portal/auth is built against it yet, same "flag now, build later" precedent {@code
 * Product.isLoanable} set before {@code LoanableItemIssue} existed. See the 2026-09-08 "Phase 2
 * kickoff" decision-log entry.
 */
@Entity
@Table(name = "suppliers")
@EntityListeners(AuditingEntityListener.class)
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_code", nullable = false, length = 50)
    private String supplierCode;

    @Column(name = "supplier_name", nullable = false, length = 200)
    private String supplierName;

    @Column(name = "tax_registration_id", length = 50)
    private String taxRegistrationId;

    @Column(name = "legal_registration_no", length = 50)
    private String legalRegistrationNo;

    @Column(name = "bank_account_number", length = 40)
    private String bankAccountNumber;

    @Column(name = "bank_ifsc_code", length = 20)
    private String bankIfscCode;

    @Column(name = "bank_name", length = 150)
    private String bankName;

    @Column(name = "bank_account_holder", length = 150)
    private String bankAccountHolder;

    @Column(name = "contact_person", length = 150)
    private String contactPerson;

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(name = "is_approved", nullable = false)
    private Boolean isApproved = false;

    @Column(name = "approval_date")
    private Instant approvalDate;

    @Column(name = "portal_access_enabled", nullable = false)
    private Boolean portalAccessEnabled = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSupplierCode() { return supplierCode; }
    public void setSupplierCode(String supplierCode) { this.supplierCode = supplierCode; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getTaxRegistrationId() { return taxRegistrationId; }
    public void setTaxRegistrationId(String taxRegistrationId) { this.taxRegistrationId = taxRegistrationId; }

    public String getLegalRegistrationNo() { return legalRegistrationNo; }
    public void setLegalRegistrationNo(String legalRegistrationNo) { this.legalRegistrationNo = legalRegistrationNo; }

    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }

    public String getBankIfscCode() { return bankIfscCode; }
    public void setBankIfscCode(String bankIfscCode) { this.bankIfscCode = bankIfscCode; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getBankAccountHolder() { return bankAccountHolder; }
    public void setBankAccountHolder(String bankAccountHolder) { this.bankAccountHolder = bankAccountHolder; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Boolean getIsApproved() { return isApproved; }
    public void setIsApproved(Boolean isApproved) { this.isApproved = isApproved; }

    public Instant getApprovalDate() { return approvalDate; }
    public void setApprovalDate(Instant approvalDate) { this.approvalDate = approvalDate; }

    public Boolean getPortalAccessEnabled() { return portalAccessEnabled; }
    public void setPortalAccessEnabled(Boolean portalAccessEnabled) { this.portalAccessEnabled = portalAccessEnabled; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
