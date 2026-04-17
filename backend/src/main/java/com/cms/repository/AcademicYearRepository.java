package com.cms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.cms.model.AcademicYear;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {

    Optional<AcademicYear> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    Optional<AcademicYear> findByIsCurrentTrue();

    @Modifying
    @Query("UPDATE AcademicYear a SET a.isCurrent = false WHERE a.isCurrent = true")
    void clearCurrentAcademicYear();
}
