package com.cms.repository;

import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cms.model.FacultyAvailability;
import com.cms.model.enums.DayOfWeek;

@Repository
public interface FacultyAvailabilityRepository extends JpaRepository<FacultyAvailability, Long> {

    List<FacultyAvailability> findByFacultyIdOrderByDayOfWeekAscStartTimeAsc(Long facultyId);

    List<FacultyAvailability> findByFacultyIdInOrderByDayOfWeekAscStartTimeAsc(List<Long> facultyIds);

    @Query("""
        SELECT fa FROM FacultyAvailability fa
        WHERE fa.faculty.id = :facultyId
          AND fa.dayOfWeek = :dayOfWeek
          AND fa.startTime < :endTime AND fa.endTime > :startTime
        """)
    List<FacultyAvailability> findOverlapping(@Param("facultyId") Long facultyId,
                                               @Param("dayOfWeek") DayOfWeek dayOfWeek,
                                               @Param("startTime") LocalTime startTime,
                                               @Param("endTime") LocalTime endTime);
}
