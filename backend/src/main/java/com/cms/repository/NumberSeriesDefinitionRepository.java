package com.cms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.NumberSeriesDefinition;

public interface NumberSeriesDefinitionRepository extends JpaRepository<NumberSeriesDefinition, Long> {

    Optional<NumberSeriesDefinition> findBySeriesCode(String seriesCode);

    boolean existsBySeriesCode(String seriesCode);
}
