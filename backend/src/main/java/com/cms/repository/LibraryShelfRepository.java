package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.model.LibraryShelf;

@Repository
public interface LibraryShelfRepository extends JpaRepository<LibraryShelf, Long>, JpaSpecificationExecutor<LibraryShelf> {

    boolean existsByNameIgnoreCaseAndRackId(String name, Long rackId);
    boolean existsByNameIgnoreCaseAndRackIdAndIdNot(String name, Long rackId, Long id);

    boolean existsByCodeIgnoreCaseAndRackId(String code, Long rackId);
    boolean existsByCodeIgnoreCaseAndRackIdAndIdNot(String code, Long rackId, Long id);

    Optional<LibraryShelf> findByNameIgnoreCaseAndRackId(String name, Long rackId);

    List<LibraryShelf> findByRackIdOrderByNameAsc(Long rackId);

    List<LibraryShelf> findByRackIdAndIsActiveTrueOrderByNameAsc(Long rackId);

    List<LibraryShelf> findByRackLibraryIdAndIsActiveTrueOrderByNameAsc(Long libraryId);

    List<LibraryShelf> findByIsActiveTrueOrderByNameAsc();
}
