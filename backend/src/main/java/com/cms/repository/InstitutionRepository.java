package com.cms.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.model.Institution;

@Repository
public interface InstitutionRepository extends JpaRepository<Institution, Long> {

    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    Optional<Institution> findByCodeIgnoreCase(String code);

    List<Institution> findAllByOrderByNameAsc();

    List<Institution> findByIsActiveTrueOrderByNameAsc();
}
