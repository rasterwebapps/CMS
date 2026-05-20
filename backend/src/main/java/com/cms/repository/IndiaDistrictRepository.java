package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.IndiaDistrict;

public interface IndiaDistrictRepository extends JpaRepository<IndiaDistrict, Long> {
    List<IndiaDistrict> findByStateIdOrderByNameAsc(Long stateId);
    List<IndiaDistrict> findByStateIdAndIsActiveTrueOrderByNameAsc(Long stateId);
    boolean existsByStateIdAndNameIgnoreCase(Long stateId, String name);
    boolean existsByStateIdAndNameIgnoreCaseAndIdNot(Long stateId, String name, Long id);
}

