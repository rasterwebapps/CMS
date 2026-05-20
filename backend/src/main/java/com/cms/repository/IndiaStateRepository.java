package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.IndiaState;

public interface IndiaStateRepository extends JpaRepository<IndiaState, Long> {
    List<IndiaState> findAllByOrderByNameAsc();
    List<IndiaState> findByIsActiveTrueOrderByNameAsc();

    // Country-scoped queries (used for all-countries location master)
    List<IndiaState> findByCountryIdOrderByNameAsc(Long countryId);
    List<IndiaState> findByCountryIdAndIsActiveTrueOrderByNameAsc(Long countryId);

    // Country-scoped duplicate checks
    boolean existsByNameIgnoreCaseAndCountryId(String name, Long countryId);
    boolean existsByCodeIgnoreCaseAndCountryId(String code, Long countryId);
    boolean existsByNameIgnoreCaseAndCountryIdAndIdNot(String name, Long countryId, Long id);
    boolean existsByCodeIgnoreCaseAndCountryIdAndIdNot(String code, Long countryId, Long id);

    // Kept for backward compatibility (India-only screens)
    boolean existsByNameIgnoreCase(String name);
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    Optional<IndiaState> findByCode(String code);
}
