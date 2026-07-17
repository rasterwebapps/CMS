package com.cms.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.AcademicYear;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long>, JpaSpecificationExecutor<AcademicYear> {

    Optional<AcademicYear> findByName(String name);

    Optional<AcademicYear> findByNameStartingWith(String prefix);

    boolean existsByName(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    Optional<AcademicYear> findByIsCurrentTrue();

    /** Used to resolve "the next academic year" when suggesting a promotion destination term. */
    List<AcademicYear> findByStartDateGreaterThanOrderByStartDateAsc(LocalDate startDate);

    @Modifying
    @Query("UPDATE AcademicYear a SET a.isCurrent = false WHERE a.isCurrent = true")
    void clearCurrentAcademicYear();

    /** True if any other academic year's date range intersects the given range. */
    @Query("SELECT COUNT(a) > 0 FROM AcademicYear a WHERE (:excludeId IS NULL OR a.id <> :excludeId) "
         + "AND a.startDate <= :endDate AND a.endDate >= :startDate")
    boolean existsOverlapping(@Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate,
                               @Param("excludeId") Long excludeId);
}
