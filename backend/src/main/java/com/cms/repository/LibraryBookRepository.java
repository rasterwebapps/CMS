package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.LibraryBook;
import com.cms.model.enums.BookStatus;

public interface LibraryBookRepository extends JpaRepository<LibraryBook, Long> {

    Optional<LibraryBook> findByAccessionNumber(String accessionNumber);

    boolean existsByAccessionNumber(String accessionNumber);

    boolean existsByAccessionNumberAndIdNot(String accessionNumber, Long id);

    List<LibraryBook> findByStatus(BookStatus status);

    List<LibraryBook> findBySubjectCategory(String subjectCategory);

    List<LibraryBook> findByShelfLocation(String shelfLocation);
}
