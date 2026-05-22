package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.FeeState;

public interface FeeStateRepository extends JpaRepository<FeeState, Long> {

    List<FeeState> findByIsActiveTrueOrderBySortOrderAsc();

    Optional<FeeState> findByIsFallbackTrue();

    Optional<FeeState> findByCode(String code);
}
