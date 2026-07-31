package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.model.ClinicalVenue;

@Repository
public interface ClinicalVenueRepository extends JpaRepository<ClinicalVenue, Long>, JpaSpecificationExecutor<ClinicalVenue> {

    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<ClinicalVenue> findAllByOrderByNameAsc();

    List<ClinicalVenue> findByIsActiveTrueOrderByNameAsc();
}
