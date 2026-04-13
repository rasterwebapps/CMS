package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.LabSchedule;
import com.cms.model.enums.DayOfWeek;

public interface LabScheduleRepository extends JpaRepository<LabSchedule, Long> {

    List<LabSchedule> findByLabId(Long labId);

    List<LabSchedule> findByFacultyId(Long facultyId);

    List<LabSchedule> findByBatchName(String batchName);

    List<LabSchedule> findByDayOfWeek(DayOfWeek dayOfWeek);

    List<LabSchedule> findBySemesterId(Long semesterId);

    List<LabSchedule> findByLabIdAndDayOfWeek(Long labId, DayOfWeek dayOfWeek);

    List<LabSchedule> findByFacultyIdAndDayOfWeek(Long facultyId, DayOfWeek dayOfWeek);

    List<LabSchedule> findByBatchNameAndDayOfWeek(String batchName, DayOfWeek dayOfWeek);

    @Query("SELECT ls FROM LabSchedule ls WHERE ls.lab.id = :labId AND ls.dayOfWeek = :dayOfWeek " +
           "AND ls.labSlot.id = :labSlotId AND ls.isActive = true")
    List<LabSchedule> findConflictingLabSchedules(@Param("labId") Long labId,
                                                   @Param("dayOfWeek") DayOfWeek dayOfWeek,
                                                   @Param("labSlotId") Long labSlotId);

    @Query("SELECT ls FROM LabSchedule ls WHERE ls.faculty.id = :facultyId AND ls.dayOfWeek = :dayOfWeek " +
           "AND ls.labSlot.id = :labSlotId AND ls.isActive = true")
    List<LabSchedule> findConflictingFacultySchedules(@Param("facultyId") Long facultyId,
                                                        @Param("dayOfWeek") DayOfWeek dayOfWeek,
                                                        @Param("labSlotId") Long labSlotId);

    @Query("SELECT ls FROM LabSchedule ls WHERE ls.batchName = :batchName AND ls.dayOfWeek = :dayOfWeek " +
           "AND ls.labSlot.id = :labSlotId AND ls.isActive = true")
    List<LabSchedule> findConflictingBatchSchedules(@Param("batchName") String batchName,
                                                     @Param("dayOfWeek") DayOfWeek dayOfWeek,
                                                     @Param("labSlotId") Long labSlotId);
}
