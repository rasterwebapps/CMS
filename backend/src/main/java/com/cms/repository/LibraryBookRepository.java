package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.cms.model.LibraryBook;
import com.cms.model.enums.BookStatus;

public interface LibraryBookRepository extends JpaRepository<LibraryBook, Long>, JpaSpecificationExecutor<LibraryBook> {

    Optional<LibraryBook> findByAccessionNumber(String accessionNumber);

    boolean existsByAccessionNumber(String accessionNumber);

    boolean existsByAccessionNumberAndIdNot(String accessionNumber, Long id);

    List<LibraryBook> findByStatus(BookStatus status);

    List<LibraryBook> findBySubjectCategory(String subjectCategory);
}
