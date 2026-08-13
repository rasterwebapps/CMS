package com.cms.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.BlockedPeriod;
import com.cms.model.enums.DayOfWeek;

public interface BlockedPeriodRepository extends JpaRepository<BlockedPeriod, Long> {

    List<BlockedPeriod> findAllByOrderByIdDesc();

    /** RECURRING blocks matching a day+time-range whose range overlaps a term's dates at all --
     *  used by Skeleton Builder's placement hard-block. Matches by actual clock-time overlap
     *  against the block's own {@code period.startTime/endTime} (same half-open-interval pattern
     *  as {@link com.cms.repository.ClassScheduleRepository#findOverlapping}), not period-id
     *  equality -- so a session sitting in a differently-shaped Period row (e.g. a combined
     *  double-period) that still clock-overlaps a blocked period (lunch, assembly) is correctly
     *  caught. Deliberately coarse on dates (any range overlap blocks the whole term for that
     *  day+time) since the recurring-template architecture can't represent "blocked some weeks,
     *  not others." */
    @Query("SELECT bp FROM BlockedPeriod bp WHERE bp.blockType = com.cms.model.enums.BlockType.RECURRING " +
           "AND bp.dayOfWeek = :dayOfWeek AND bp.period.startTime < :endTime AND bp.period.endTime > :startTime " +
           "AND bp.rangeStartDate <= :termEnd AND bp.rangeEndDate >= :termStart")
    List<BlockedPeriod> findOverlappingRecurringBlocks(@Param("dayOfWeek") DayOfWeek dayOfWeek,
                                                         @Param("startTime") LocalTime startTime,
                                                         @Param("endTime") LocalTime endTime,
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

    /** Same as {@link #findApplicableInRange}, scoped to one period -- used by
     *  {@link com.cms.service.ClassScheduleOccurrenceService} to resolve, per schedule, which
     *  candidate dates are actually cancelled (holiday-derived or manual block alike). */
    @Query("SELECT bp FROM BlockedPeriod bp WHERE bp.period.id = :periodId AND (" +
           "(bp.blockType = com.cms.model.enums.BlockType.ONE_OFF AND bp.specificDate BETWEEN :termStart AND :termEnd) " +
           "OR (bp.blockType = com.cms.model.enums.BlockType.RECURRING AND bp.rangeStartDate <= :termEnd AND bp.rangeEndDate >= :termStart))")
    List<BlockedPeriod> findApplicableForPeriodInRange(@Param("periodId") Long periodId,
                                                        @Param("termStart") LocalDate termStart,
                                                        @Param("termEnd") LocalDate termEnd);

    /** Holiday-derived ONE_OFF blocks only (see {@code source_calendar_event_id}), matched by
     *  actual clock-time overlap against the block's own {@code period.startTime/endTime} (not
     *  period-id equality -- see {@link #findOverlappingRecurringBlocks}), overlapping a term --
     *  used by {@link com.cms.service.TimetableSkeletonService}'s hard placement conflict.
     *  Deliberately excludes manually-created ONE_OFF blocks (e.g. "one date, staff meeting") so
     *  they keep their existing calendar-display-only behavior; only a holiday actually prevents
     *  new Skeleton Builder placement. */
    @Query("SELECT bp FROM BlockedPeriod bp WHERE bp.period.startTime < :endTime AND bp.period.endTime > :startTime " +
           "AND bp.blockType = com.cms.model.enums.BlockType.ONE_OFF " +
           "AND bp.sourceCalendarEvent IS NOT NULL " +
           "AND bp.specificDate BETWEEN :termStart AND :termEnd")
    List<BlockedPeriod> findHolidayOneOffBlocksInRange(@Param("startTime") LocalTime startTime,
                                                        @Param("endTime") LocalTime endTime,
                                                        @Param("termStart") LocalDate termStart,
                                                        @Param("termEnd") LocalDate termEnd);

    /** All blocks generated by one HOLIDAY {@link com.cms.model.CalendarEvent} -- the live set
     *  {@link com.cms.service.CalendarEventService#syncHolidayBlocks} diffs against on edit, and
     *  what a plain {@code delete} on the event cascades away (belt-and-braces alongside the FK's
     *  {@code ON DELETE CASCADE}). */
    List<BlockedPeriod> findBySourceCalendarEventId(Long sourceCalendarEventId);

    void deleteBySourceCalendarEventId(Long sourceCalendarEventId);

    boolean existsByPeriodIdAndBlockTypeAndSpecificDate(Long periodId, com.cms.model.enums.BlockType blockType,
                                                         LocalDate specificDate);
}