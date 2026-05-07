package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.ScholarshipType;

public interface ScholarshipTypeRepository extends JpaRepository<ScholarshipType, Long> {
    Optional<ScholarshipType> findByCode(String code);
    List<ScholarshipType> findByActiveTrueOrderByNameAsc();
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
}

