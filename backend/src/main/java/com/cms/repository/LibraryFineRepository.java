package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.cms.model.LibraryFine;
import com.cms.model.enums.FineStatus;

public interface LibraryFineRepository extends JpaRepository<LibraryFine, Long>, JpaSpecificationExecutor<LibraryFine> {

    Optional<LibraryFine> findByIssueId(Long issueId);

    List<LibraryFine> findByIssueIdIn(List<Long> issueIds);

    boolean existsByIssueId(Long issueId);

    List<LibraryFine> findByStatus(FineStatus status);
}
