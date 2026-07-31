package com.cms.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.FacultyAbsence;

public interface FacultyAbsenceRepository extends JpaRepository<FacultyAbsence, Long> {

    Optional<FacultyAbsence> findByFacultyIdAndAbsenceDate(Long facultyId, LocalDate absenceDate);
}
