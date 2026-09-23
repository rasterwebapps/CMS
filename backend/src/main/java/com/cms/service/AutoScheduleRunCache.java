package com.cms.service;

import com.cms.dto.ClinicalShiftWindow;
import com.cms.model.ClassSchedule;
import com.cms.model.Faculty;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.DayOfWeek;
import com.cms.repository.ClassScheduleRepository;

import org.hibernate.Hibernate;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Opt-in, per-run in-memory mirror of the handful of {@link ClassSchedule} repository queries the
 * auto-schedule hot loop ({@link TimetableGlobalAutoScheduleService}) re-runs on every single
 * placement <em>attempt</em> — {@code checkAlreadyPlaced}, {@code checkCohortExclusivity},
 * {@code checkWithinWorkloadCaps}, {@code checkFacultyFree}/{@code scanRoomAndAudience} all used to
 * hit the database fresh every time, even though within one run the only thing that actually
 * changes between attempts is what THIS run itself just placed/removed/staffed. With thousands of
 * attempts on real data, that was the dominant cost behind an 8-10 minute "Global Auto-Schedule"
 * run, not any single slow query.
 *
 * <p>Bound to the calling thread via a {@link ThreadLocal} so every other caller of
 * {@link TimetableSkeletonService}/{@link TimetableStaffingService} (manual single-cell placement,
 * {@code moveCell}, {@code swapCells}) is completely unaffected — they never activate this cache,
 * so they keep hitting the database fresh every time, which is correct and cheap for a single
 * interactive click. Every {@code check*} call site this backs falls back to its original direct
 * repository call whenever no cache is active, so behavior is identical either way — this is a
 * pure performance path, not a new code path.
 *
 * <p>Holds every {@link ClassSchedule} row for the run's one {@code TermInstance} (auto-schedule
 * always runs against exactly one term), loaded once via
 * {@link ClassScheduleRepository#findByTermInstanceId}, and kept in sync with this run's own writes
 * via {@link #recordPlacement}/{@link #recordRemoval}/{@link #recordStaffing} — a plain read-once
 * snapshot would go stale the moment the run placed its own first cell.
 */
public final class AutoScheduleRunCache {

    private static final ThreadLocal<AutoScheduleRunCache> ACTIVE = new ThreadLocal<>();

    private final List<ClassSchedule> cells;
    private final Map<String, Optional<String>> blockReasonMemo = new HashMap<>();
    private final Map<String, List<ClinicalShiftWindow>> shiftWindowMemo = new HashMap<>();

    private AutoScheduleRunCache(List<ClassSchedule> cells) {
        this.cells = new ArrayList<>(cells);
    }

    public static Optional<AutoScheduleRunCache> current() {
        return Optional.ofNullable(ACTIVE.get());
    }

    /** Runs {@code body} with a fresh cache for {@code termInstanceId} bound to this thread,
     *  guaranteed cleared afterward even if {@code body} throws. Callers must never nest two runs
     *  on the same thread — asserted defensively since a nested run silently reusing the outer
     *  run's stale snapshot would be a subtle, hard-to-notice correctness bug. */
    public static <T> T run(Long termInstanceId, ClassScheduleRepository repository, Supplier<T> body) {
        if (ACTIVE.get() != null) {
            throw new IllegalStateException("AutoScheduleRunCache is already active on this thread");
        }
        // isActive=false rows are ghosts orphaned by a since-reverted CohortRoomAllocation (see
        // TimetableSkeletonService#getCohortSkeleton's own filter) -- excluded at load so every
        // mirror method below (byCourseOfferingId, byCourseOfferingIdIn, etc.) never has to filter
        // it individually, and a ghost invisible in the grid can never phantom-block a real slot.
        List<ClassSchedule> loaded = repository.findByTermInstanceId(termInstanceId).stream()
            .filter(cs -> Boolean.TRUE.equals(cs.getIsActive()))
            .toList();
        loaded.forEach(AutoScheduleRunCache::initializeRoomAssociations);
        ACTIVE.set(new AutoScheduleRunCache(loaded));
        try {
            return body.get();
        } finally {
            ACTIVE.remove();
        }
    }

    /** Called right after a real {@code classScheduleRepository.save(...)} of a newly-placed cell
     *  — the saved entity already carries every association ({@code courseOffering}, {@code
     *  period}, etc.) placeCell set before saving, so it's usable as-is for every filter below. */
    public void recordPlacement(ClassSchedule cell) {
        initializeRoomAssociations(cell);
        cells.add(cell);
    }

    /** {@code removed} is the entity as it stood immediately before deletion. If it belonged to a
     *  periodSpan group (non-null {@code sessionGroupId}), every sibling sharing that group is
     *  dropped too, mirroring {@code TimetableSkeletonService#removeCell}'s own group-wide delete. */
    public void recordRemoval(ClassSchedule removed) {
        if (removed.getSessionGroupId() != null) {
            cells.removeIf(cs -> removed.getSessionGroupId().equals(cs.getSessionGroupId()));
        } else {
            cells.removeIf(cs -> cs.getId().equals(removed.getId()));
        }
    }

    /** Called right after a real staffing save — mutates the cached copy of this cell (a different
     *  Java instance than {@code staffCell}'s own REQUIRES_NEW-transaction-scoped entity, since
     *  each call opens a fresh persistence context) so later workload/conflict checks in the same
     *  run see the new faculty immediately.
     *
     *  <p>The venue is copied too, not just the faculty: staffing is also the moment a placed cell
     *  gets its committed Classroom/Lab/ClinicalVenue. Copying only the faculty left every cached
     *  LAB cell venue-less for the rest of the run, so the room-free check saw an empty lab and
     *  stacked the next batch into it at the same slot -- local dev had four 30-seat batches in
     *  the one Computer lab at Monday Periods 7-8, invisible until the Conflict Inspector re-read
     *  the real rows from the database. */
    public void recordStaffing(ClassSchedule staffed) {
        initializeRoomAssociations(staffed);
        cells.stream()
            .filter(cs -> cs.getId().equals(staffed.getId()))
            .findFirst()
            .ifPresent(cs -> {
                cs.setFaculty(staffed.getFaculty());
                cs.setClassroom(staffed.getClassroom());
                cs.setLab(staffed.getLab());
                cs.setClinicalVenue(staffed.getClinicalVenue());
            });
    }

    /** Force-loads a cell's classroom/lab/clinicalVenue -- and each one's own nested physical
     *  {@code Room} -- before the cell enters or is updated in this run-long cache. Necessary
     *  because staffing runs each in its own {@code REQUIRES_NEW} transaction (see
     *  {@code TimetableStaffingService#staffCell}): an still-uninitialized Hibernate proxy is
     *  usable only by the exact session that created it, and later code reading this cell out of
     *  the cache (a room-conflict check against another cell, run from a *different* REQUIRES_NEW
     *  transaction, or from the outer transaction once resumed) can never initialize it there --
     *  Hibernate requires the owning session to be the one currently active on the thread, and a
     *  suspended/different session throws {@code LazyInitializationException} regardless of
     *  whether the owning session is technically still open. Initializing here, while each
     *  association's own session is still the active one, resolves it once so every later
     *  cross-transaction read (e.g. {@code TimetableStaffingService#physicalRoomOf}) is safe. */
    private static void initializeRoomAssociations(ClassSchedule cs) {
        if (cs.getClassroom() != null) {
            Hibernate.initialize(cs.getClassroom());
            Hibernate.initialize(cs.getClassroom().getRoom());
        }
        if (cs.getLab() != null) {
            Hibernate.initialize(cs.getLab());
            Hibernate.initialize(cs.getLab().getRoom());
        }
        if (cs.getClinicalVenue() != null) {
            Hibernate.initialize(cs.getClinicalVenue());
            Hibernate.initialize(cs.getClinicalVenue().getRoom());
        }
    }

    /** Memoizes {@code TimetableBlockedPeriodChecker#blockReason}'s two-repository-call result by
     *  {@code key} for the lifetime of this run — the term's recurring/holiday blocks are
     *  institutional configuration, never created or changed mid-run, so the same (day, start, end,
     *  term) input always produces the same result within one run. Computes and caches on first
     *  request only. */
    public Optional<String> memoizedBlockReason(String key, Supplier<Optional<String>> compute) {
        return blockReasonMemo.computeIfAbsent(key, k -> compute.get());
    }

    /** Memoizes {@code ClinicalShiftGroupService#resolveActiveWindowsForCohort}'s result by
     *  {@code key} for the lifetime of this run — same idiom as {@link #memoizedBlockReason},
     *  since {@code tryPlaceAndStaff} calls it on every placement attempt and a cohort's shift
     *  assignments never change mid-run. */
    public List<ClinicalShiftWindow> memoizedShiftWindows(String key, Supplier<List<ClinicalShiftWindow>> compute) {
        return shiftWindowMemo.computeIfAbsent(key, k -> compute.get());
    }

    /** Every active cell in this run's term, exactly as loaded at {@link #run} time plus this
     *  run's own {@link #recordPlacement}/{@link #recordRemoval} writes -- for a caller (e.g.
     *  {@link TimetableConflictInspectorService#scanTerm}) that needs the whole-term cell list
     *  itself rather than one of the narrower mirror queries below. Avoids that caller running its
     *  own separate {@code findByTermInstanceIdAndIsActiveTrue} on top of the fetch this cache
     *  already did -- on a term with several past Global Auto-Schedule reruns, a second *unfiltered*
     *  {@code findByTermInstanceId} (what {@link #run} itself loads from) would otherwise be the
     *  larger of the two queries, not a cheap extra. */
    public List<ClassSchedule> allCells() {
        return List.copyOf(cells);
    }

    /** Mirrors {@code ClassScheduleRepository#findByCourseOfferingId}. */
    public List<ClassSchedule> byCourseOfferingId(Long courseOfferingId) {
        return cells.stream()
            .filter(cs -> cs.getCourseOffering() != null && cs.getCourseOffering().getId().equals(courseOfferingId))
            .toList();
    }

    /** Mirrors {@code ClassScheduleRepository#findByTermInstanceIdAndCourseOfferingIdIn} — the
     *  termInstanceId filter is implicit since this whole cache is scoped to one term. */
    public List<ClassSchedule> byCourseOfferingIdIn(Collection<Long> courseOfferingIds) {
        return cells.stream()
            .filter(cs -> cs.getCourseOffering() != null && courseOfferingIds.contains(cs.getCourseOffering().getId()))
            .toList();
    }

    /** Mirrors {@code ClassScheduleRepository#findByCohortSectionIdInAndIsActiveTrue} — a
     *  cohortSection always belongs to exactly one TermInstance, so no separate term filter is
     *  needed once the caller has already resolved section ids scoped to the run's term. */
    public List<ClassSchedule> byCohortSectionIdIn(Collection<Long> cohortSectionIds) {
        return cells.stream()
            .filter(cs -> cs.getCohortSection() != null && cohortSectionIds.contains(cs.getCohortSection().getId()))
            .toList();
    }

    /** Mirrors {@code ClassScheduleRepository#findByTermInstanceIdAndStatusAndFacultyId}. */
    public List<ClassSchedule> byStatusAndFacultyId(ClassScheduleStatus status, Long facultyId) {
        return cells.stream()
            .filter(cs -> cs.getStatus() == status && cs.getFaculty() != null && cs.getFaculty().getId().equals(facultyId))
            .toList();
    }

    /** Mirrors {@code ClassScheduleRepository#findOverlapping} exactly: same day, active, matching
     *  status, excluding one id, real clock-time overlap on the period's start/end. */
    public List<ClassSchedule> overlapping(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
                                            ClassScheduleStatus status, Long excludeId) {
        return cells.stream()
            .filter(cs -> cs.getDayOfWeek() == dayOfWeek
                && Boolean.TRUE.equals(cs.getIsActive())
                && cs.getStatus() == status
                && (excludeId == null || !cs.getId().equals(excludeId))
                && cs.getPeriod() != null
                && cs.getPeriod().getStartTime().isBefore(endTime)
                && cs.getPeriod().getEndTime().isAfter(startTime))
            .toList();
    }
}
