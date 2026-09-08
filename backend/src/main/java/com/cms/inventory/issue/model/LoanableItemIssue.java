package com.cms.inventory.issue.model;

import java.time.Instant;
import java.time.LocalDate;

import com.cms.inventory.catalog.model.Product;
import com.cms.inventory.issue.model.enums.LoanableItemIssueStatus;
import com.cms.inventory.stock.model.InventoryLocation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Phase 4's ("Requests, Issues & Returns") fourth and final slice — a generic "borrow and
 * return" record for a {@link Product} flagged {@code isLoanable} (sports equipment, hostel
 * items, and similar — never consumed, always expected back). Deliberately **not** integrated
 * with {@code StockLedger}/{@code StockBalance} in this pass: the item isn't consumed, so
 * treating a loan as an {@code ISSUE}/{@code RETURN} stock movement would be wrong (it would
 * either permanently remove it from on-hand stock, or need a whole new "on-loan quantity"
 * concept alongside on-hand — real, separately-scoped work, not something to half-build here).
 * This is a standalone tracking record: who has it, since when, expected back when, and its
 * condition at each end. No borrower entity exists in this generic module (per the "no vertical
 * branding" rule — a college's students and a hospital's staff are both just "a borrower"), so
 * the borrower is captured as plain text. "Overdue" is derived at read time from {@code
 * expectedReturnDate}, never stored. See the "Loanable Item Issue slice" decision-log entry.
 */
@Entity
@Table(name = "loanable_item_issues")
public class LoanableItemIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private InventoryLocation location;

    @Column(name = "borrower_name", nullable = false, length = 200)
    private String borrowerName;

    @Column(name = "borrower_contact", length = 100)
    private String borrowerContact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanableItemIssueStatus status = LoanableItemIssueStatus.ISSUED;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "expected_return_date", nullable = false)
    private LocalDate expectedReturnDate;

    @Column(name = "actual_return_date")
    private LocalDate actualReturnDate;

    @Column(name = "condition_on_issue", length = 500)
    private String conditionOnIssue;

    @Column(name = "condition_on_return", length = 500)
    private String conditionOnReturn;

    @Column(length = 500)
    private String notes;

    @Column(name = "issued_by", length = 255)
    private String issuedBy;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "returned_by", length = 255)
    private String returnedBy;

    @Column(name = "returned_at")
    private Instant returnedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public InventoryLocation getLocation() { return location; }
    public void setLocation(InventoryLocation location) { this.location = location; }

    public String getBorrowerName() { return borrowerName; }
    public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }

    public String getBorrowerContact() { return borrowerContact; }
    public void setBorrowerContact(String borrowerContact) { this.borrowerContact = borrowerContact; }

    public LoanableItemIssueStatus getStatus() { return status; }
    public void setStatus(LoanableItemIssueStatus status) { this.status = status; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public LocalDate getExpectedReturnDate() { return expectedReturnDate; }
    public void setExpectedReturnDate(LocalDate expectedReturnDate) { this.expectedReturnDate = expectedReturnDate; }

    public LocalDate getActualReturnDate() { return actualReturnDate; }
    public void setActualReturnDate(LocalDate actualReturnDate) { this.actualReturnDate = actualReturnDate; }

    public String getConditionOnIssue() { return conditionOnIssue; }
    public void setConditionOnIssue(String conditionOnIssue) { this.conditionOnIssue = conditionOnIssue; }

    public String getConditionOnReturn() { return conditionOnReturn; }
    public void setConditionOnReturn(String conditionOnReturn) { this.conditionOnReturn = conditionOnReturn; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getIssuedBy() { return issuedBy; }
    public void setIssuedBy(String issuedBy) { this.issuedBy = issuedBy; }

    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }

    public String getReturnedBy() { return returnedBy; }
    public void setReturnedBy(String returnedBy) { this.returnedBy = returnedBy; }

    public Instant getReturnedAt() { return returnedAt; }
    public void setReturnedAt(Instant returnedAt) { this.returnedAt = returnedAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
