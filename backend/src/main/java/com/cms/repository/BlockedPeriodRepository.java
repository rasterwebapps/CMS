package com.cms.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.BlockedPeriod;
import com.cms.model.enums.DayOfWeek;

public interface BlockedPeriodRepository extends JpaRepository<BlockedPeriod, Long> {

    List<BlockedPeriod> findAllByOrderByIdDesc();

    /** RECURRING blocks matching a day+period whose range overlaps a term's dates at all --
     *  used by Skeleton Builder's placement hard-block. Deliberately coarse (any overlap blocks
     *  the whole term for that day+period) since the recurring-template architecture can't
     *  represent "blocked some weeks, not others." */
    @Query("SELECT bp FROM BlockedPeriod bp WHERE bp.blockType = com.cms.model.enums.BlockType.RECURRING " +
           "AND bp.dayOfWeek = :dayOfWeek AND bp.period.id = :periodId " +
           "AND bp.rangeStartDate <= :termEnd AND bp.rangeEndDate >= :termStart")
    List<BlockedPeriod> findOverlappingRecurringBlocks(@Param("dayOfWeek") DayOfWeek dayOfWeek,
                                                         @Param("periodId") Long periodId,
                                                         @Param("termStart") LocalDate termStart,
                                                         @Param("termEnd") LocalDate termEnd);

    /** Every block (either type) that could possibly apply within a term's date range -- ONE_OFF
     *  dated inside it, or RECURRING whose range overlaps it -- used by the Capacity Planner's
     *  buffer-hours calculation. */
    @Query("SELECT bp FROM BlockedPeriod bp WHERE " +
           "(bp.blockType = com.cms.model.enums.BlockType.ONE_OFF AND bp.specificDate BETWEEN :termStart AND :termEnd) " +
           "OR (bp.blockType = com.cms.model.enums.BlockType.RECURRING AND bp.rangeStartDate <= :termEnd AND bp.rangeEndDate >= :termStart)")
    List<BlockedPeriod> findApplicableInRange(@Param("termStart") LocalDate termStart,
                                               @Param("termEnd") LocalDate termEnd);
}