package com.cms.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.cms.model.Speciality;

public interface SpecialityRepository extends JpaRepository<Speciality, Long>, JpaSpecificationExecutor<Speciality> {

    Optional<Speciality> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    List<Speciality> findAllByOrderByNameAsc();

    List<Speciality> findByIsActiveTrueOrderByNameAsc();
}
