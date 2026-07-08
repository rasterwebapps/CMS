package com.cms.model;

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

@Entity
@Table(name = "library_book_shelf_transfers")
public class LibraryBookShelfTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private LibraryBook book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_library_id")
    private Library oldLibrary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_rack_id")
    private LibraryRack oldRack;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_shelf_id")
    private LibraryShelf oldShelf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_library_id", nullable = false)
    private Library newLibrary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_rack_id")
    private LibraryRack newRack;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_shelf_id")
    private LibraryShelf newShelf;

    @Column(name = "transferred_at", nullable = false)
    private Instant transferredAt;

    @Column(name = "transferred_by")
    private String transferredBy;

    private String notes;

    public LibraryBookShelfTransfer() {}

    public Long getId() { return id; }

    public LibraryBook getBook() { return book; }
    public void setBook(LibraryBook book) { this.book = book; }

    public Library getOldLibrary() { return oldLibrary; }
    public void setOldLibrary(Library oldLibrary) { this.oldLibrary = oldLibrary; }

    public LibraryRack getOldRack() { return oldRack; }
    public void setOldRack(LibraryRack oldRack) { this.oldRack = oldRack; }

    public LibraryShelf getOldShelf() { return oldShelf; }
    public void setOldShelf(LibraryShelf oldShelf) { this.oldShelf = oldShelf; }

    public Library getNewLibrary() { return newLibrary; }
    public void setNewLibrary(Library newLibrary) { this.newLibrary = newLibrary; }

    public LibraryRack getNewRack() { return newRack; }
    public void setNewRack(LibraryRack newRack) { this.newRack = newRack; }

    public LibraryShelf getNewShelf() { return newShelf; }
    public void setNewShelf(LibraryShelf newShelf) { this.newShelf = newShelf; }

    public Instant getTransferredAt() { return transferredAt; }
    public void setTransferredAt(Instant transferredAt) { this.transferredAt = transferredAt; }

    public String getTransferredBy() { return transferredBy; }
    public void setTransferredBy(String transferredBy) { this.transferredBy = transferredBy; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
