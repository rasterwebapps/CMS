package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.cms.model.LibrarySubjectCategory;

public interface LibrarySubjectCategoryRepository extends JpaRepository<LibrarySubjectCategory, Long>, JpaSpecificationExecutor<LibrarySubjectCategory> {

    Optional<LibrarySubjectCategory> findByNameIgnoreCase(String name);

    Optional<LibrarySubjectCategory> findByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    List<LibrarySubjectCategory> findByIsActiveTrueOrderByNameAsc();

    List<LibrarySubjectCategory> findAllByOrderByNameAsc();
}
