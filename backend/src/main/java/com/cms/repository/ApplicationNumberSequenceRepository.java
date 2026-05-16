package com.cms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.cms.model.ApplicationNumberSequence;

import jakarta.persistence.LockModeType;

public interface ApplicationNumberSequenceRepository extends JpaRepository<ApplicationNumberSequence, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		SELECT s FROM ApplicationNumberSequence s
		WHERE s.seriesCode = :seriesCode AND s.scopeKey = :scopeKey
		""")
	Optional<ApplicationNumberSequence> findBySeriesCodeAndScopeKeyForUpdate(String seriesCode, String scopeKey);

	Optional<ApplicationNumberSequence> findBySeriesCodeAndScopeKey(String seriesCode, String scopeKey);
}

