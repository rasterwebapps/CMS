package com.cms.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.model.enums.BookSourceOfSupply;
import com.cms.model.enums.BookStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "library_books")
@EntityListeners(AuditingEntityListener.class)
public class LibraryBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "accession_number", nullable = false, unique = true, length = 30)
    private String accessionNumber;

    @Column(name = "barcode", unique = true, length = 30)
    private String barcode;

    @Column(name = "entry_date")
    private LocalDate entryDate;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 500)
    private String authors;

    @Column(length = 300)
    private String publisher;

    @Column(name = "year_of_publication", length = 20)
    private String yearOfPublication;

    @Column(length = 100)
    private String edition;

    @Column(length = 30)
    private String isbn;

    @Column(name = "book_collation", length = 200)
    private String collation;

    @Column(length = 200)
    private String series;

    @Column(name = "call_number", length = 50)
    private String callNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "library_id", nullable = false)
    private Library library;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shelf_id")
    private LibraryShelf shelf;

    @Column(name = "subject_category", length = 100)
    private String subjectCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_of_supply", length = 20)
    private BookSourceOfSupply sourceOfSupply;

    @Column(name = "vendor_donor_name", length = 200)
    private String vendorDonorName;

    @Column(name = "bill_number", length = 50)
    private String billNumber;

    @Column(name = "bill_date")
    private LocalDate billDate;

    @Column(name = "price_rs", precision = 10, scale = 2)
    private BigDecimal priceRs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookStatus status = BookStatus.AVAILABLE;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAccessionNumber() { return accessionNumber; }
    public void setAccessionNumber(String accessionNumber) { this.accessionNumber = accessionNumber; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthors() { return authors; }
    public void setAuthors(String authors) { this.authors = authors; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public String getYearOfPublication() { return yearOfPublication; }
    public void setYearOfPublication(String yearOfPublication) { this.yearOfPublication = yearOfPublication; }

    public String getEdition() { return edition; }
    public void setEdition(String edition) { this.edition = edition; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getCollation() { return collation; }
    public void setCollation(String collation) { this.collation = collation; }

    public String getSeries() { return series; }
    public void setSeries(String series) { this.series = series; }

    public String getCallNumber() { return callNumber; }
    public void setCallNumber(String callNumber) { this.callNumber = callNumber; }

    public Library getLibrary() { return library; }
    public void setLibrary(Library library) { this.library = library; }

    public LibraryShelf getShelf() { return shelf; }
    public void setShelf(LibraryShelf shelf) { this.shelf = shelf; }

    public String getSubjectCategory() { return subjectCategory; }
    public void setSubjectCategory(String subjectCategory) { this.subjectCategory = subjectCategory; }

    public BookSourceOfSupply getSourceOfSupply() { return sourceOfSupply; }
    public void setSourceOfSupply(BookSourceOfSupply sourceOfSupply) { this.sourceOfSupply = sourceOfSupply; }

    public String getVendorDonorName() { return vendorDonorName; }
    public void setVendorDonorName(String vendorDonorName) { this.vendorDonorName = vendorDonorName; }

    public String getBillNumber() { return billNumber; }
    public void setBillNumber(String billNumber) { this.billNumber = billNumber; }

    public LocalDate getBillDate() { return billDate; }
    public void setBillDate(LocalDate billDate) { this.billDate = billDate; }

    public BigDecimal getPriceRs() { return priceRs; }
    public void setPriceRs(BigDecimal priceRs) { this.priceRs = priceRs; }

    public BookStatus getStatus() { return status; }
    public void setStatus(BookStatus status) { this.status = status; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
