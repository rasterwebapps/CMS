package com.cms.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.Semester;

public interface SemesterRepository extends JpaRepository<Semester, Long> {

    List<Semester> findByAcademicYearId(Long academicYearId);

    List<Semester> findByAcademicYearIdOrderBySemesterNumber(Long academicYearId);

    boolean existsByNameAndAcademicYearIdAndIdNot(String name, Long academicYearId, Long id);

    /**
     * Returns true when any semester in the given academic year (excluding the semester identified
     * by {@code excludeId}) overlaps with the given date range.  Two ranges [s1,e1] and [s2,e2]
     * overlap when s1 <= e2 AND e1 >= s2.
     */
    @Query("""
        SELECT COUNT(s) > 0
        FROM Semester s
        WHERE s.academicYear.id = :academicYearId
          AND s.id <> :excludeId
          AND s.startDate <= :endDate
          AND s.endDate >= :startDate
        """)
    boolean existsOverlappingInAcademicYear(
        @Param("academicYearId") Long academicYearId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("excludeId") Long excludeId
    );
}
