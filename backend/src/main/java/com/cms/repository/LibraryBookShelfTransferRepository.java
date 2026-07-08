package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.model.LibraryBookShelfTransfer;

@Repository
public interface LibraryBookShelfTransferRepository extends JpaRepository<LibraryBookShelfTransfer, Long> {

    List<LibraryBookShelfTransfer> findByBookIdOrderByTransferredAtDesc(Long bookId);
}
