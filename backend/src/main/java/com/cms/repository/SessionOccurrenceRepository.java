package com.cms.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.SessionOccurrence;

public interface SessionOccurrenceRepository extends JpaRepository<SessionOccurrence, Long> {

    Optional<SessionOccurrence> findByClassScheduleIdAndOccurrenceDate(Long classScheduleId, LocalDate occurrenceDate);

    List<SessionOccurrence> findByClassSchedule_CourseOffering_Id(Long courseOfferingId);

    List<SessionOccurrence> findByClassSchedule_TermInstance_IdAndClassSchedule_Status(
        Long termInstanceId, com.cms.model.enums.ClassScheduleStatus status);
}
