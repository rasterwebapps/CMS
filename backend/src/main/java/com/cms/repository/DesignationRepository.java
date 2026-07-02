package com.cms.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.model.DesignationMaster;

@Repository
public interface DesignationRepository extends JpaRepository<DesignationMaster, Long>, JpaSpecificationExecutor<DesignationMaster> {

    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    Optional<DesignationMaster> findByCodeIgnoreCase(String code);

    List<DesignationMaster> findAllByOrderByNameAsc();

    List<DesignationMaster> findByIsActiveTrueOrderByNameAsc();
}
