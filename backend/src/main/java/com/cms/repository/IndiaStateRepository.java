package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.IndiaState;

public interface IndiaStateRepository extends JpaRepository<IndiaState, Long> {
    List<IndiaState> findAllByOrderByNameAsc();
    List<IndiaState> findByIsActiveTrueOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
    Optional<IndiaState> findByCode(String code);
}

