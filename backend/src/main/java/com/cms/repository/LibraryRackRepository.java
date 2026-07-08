package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.model.LibraryRack;

@Repository
public interface LibraryRackRepository extends JpaRepository<LibraryRack, Long>, JpaSpecificationExecutor<LibraryRack> {

    boolean existsByNameIgnoreCaseAndLibraryId(String name, Long libraryId);
    boolean existsByNameIgnoreCaseAndLibraryIdAndIdNot(String name, Long libraryId, Long id);

    boolean existsByCodeIgnoreCaseAndLibraryId(String code, Long libraryId);
    boolean existsByCodeIgnoreCaseAndLibraryIdAndIdNot(String code, Long libraryId, Long id);

    List<LibraryRack> findByLibraryIdOrderByNameAsc(Long libraryId);

    List<LibraryRack> findByLibraryIdAndIsActiveTrueOrderByNameAsc(Long libraryId);

    List<LibraryRack> findByIsActiveTrueOrderByNameAsc();
}
