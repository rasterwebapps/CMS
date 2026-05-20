package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.LocationCountry;

public interface LocationCountryRepository extends JpaRepository<LocationCountry, Long> {
    List<LocationCountry> findAllByOrderByNameAsc();
    List<LocationCountry> findByIsActiveTrueOrderByNameAsc();
    Optional<LocationCountry> findByIsoCode(String isoCode);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByIsoCodeIgnoreCase(String isoCode);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    boolean existsByIsoCodeIgnoreCaseAndIdNot(String isoCode, Long id);
}

