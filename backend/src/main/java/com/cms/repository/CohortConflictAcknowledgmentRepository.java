package com.cms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.CohortConflictAcknowledgment;

public interface CohortConflictAcknowledgmentRepository extends JpaRepository<CohortConflictAcknowledgment, Long> {

    Optional<CohortConflictAcknowledgment> findByTermInstanceIdAndCohortId(Long termInstanceId, Long cohortId);

    void deleteByTermInstanceIdAndCohortId(Long termInstanceId, Long cohortId);
}
