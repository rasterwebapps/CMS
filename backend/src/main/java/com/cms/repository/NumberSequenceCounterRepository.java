package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.NumberSequenceCounter;

import jakarta.persistence.LockModeType;

public interface NumberSequenceCounterRepository
        extends JpaRepository<NumberSequenceCounter, Long>, JpaSpecificationExecutor<NumberSequenceCounter> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM NumberSequenceCounter c WHERE c.seriesCode = :seriesCode AND c.scopeKey = :scopeKey")
    Optional<NumberSequenceCounter> findBySeriesCodeAndScopeKeyForUpdate(
            @Param("seriesCode") String seriesCode,
            @Param("scopeKey") String scopeKey);

    List<NumberSequenceCounter> findBySeriesCode(String seriesCode);
}
