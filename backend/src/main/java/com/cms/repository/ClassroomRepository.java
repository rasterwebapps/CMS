package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.model.Classroom;
import com.cms.model.enums.RoomPurposeCategoryCode;

@Repository
public interface ClassroomRepository extends JpaRepository<Classroom, Long>, JpaSpecificationExecutor<Classroom> {

    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<Classroom> findAllByOrderByNameAsc();

    List<Classroom> findByIsActiveTrueOrderByNameAsc();

    /** Classrooms linked to a physical Room tagged with this Room Purpose Classification category
     *  — used to find Library-eligible classrooms for Run Automation's Library gap-fill pass
     *  ({@code TimetableGlobalAutoScheduleService#fillLibraryGaps}). A Classroom with no {@code room}
     *  link (legacy rows predate the link) is never returned here. */
    List<Classroom> findByIsActiveTrueAndRoom_PurposeCategory_CodeOrderByNameAsc(RoomPurposeCategoryCode code);
}
