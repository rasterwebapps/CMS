package com.cms.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.DayMappingOverride;

public interface DayMappingOverrideRepository extends JpaRepository<DayMappingOverride, Long> {

    Optional<DayMappingOverride> findByMappedDate(LocalDate mappedDate);

    List<DayMappingOverride> findAllByOrderByMappedDateAsc();

    /** Batched window fetch for {@link com.cms.service.ClassScheduleOccurrenceService}'s
     *  per-window resolution -- one query per calendar window instead of one per schedule/date. */
    List<DayMappingOverride> findByMappedDateBetween(LocalDate from, LocalDate to);
}
