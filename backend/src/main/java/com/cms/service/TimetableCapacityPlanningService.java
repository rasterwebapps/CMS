package com.cms.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.CapacityPlanResponse;
import com.cms.dto.VenueOptionResponse;
import com.cms.dto.VenueUtilizationResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.BlockedPeriod;
import com.cms.model.CalendarEvent;
import com.cms.model.ClassSchedule;
import com.cms.model.Classroom;
import com.cms.model.ClinicalVenue;
import com.cms.model.Cohort;
import com.cms.model.CourseOffering;
import com.cms.model.Lab;
import com.cms.model.Period;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.TermInstance;
import com.cms.model.enums.BlockType;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.LabStatus;
import com.cms.model.enums.PlanningBasis;
import com.cms.repository.BlockedPeriodRepository;
import com.cms.repository.CalendarEventRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.ClinicalVenueRepository;
import com.cms.repository.CohortRepository;
import com.cms.repository.CohortSectionRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.LabRepository;
import com.cms.repository.PeriodRepository;
import com.cms.repository.StudentTermEnrollmentRepository;
import com.cms.repository.TermInstanceRepository;

/**
 * Answers "how many classrooms/lab-batches do I need, and what's already free" for a Cohort in a
 * TermInstance, ahead of building a Skeleton Builder / Staffing pass — the visibility Staffing's
 * per-cell {@code requireRoomFree}/{@code requireCapacityFit} checks don't give the admin until
 * they're already mid-assignment. Cohort strength is admin-selected (picked explicitly via
 * {@code cohortId}) rather than auto-resolved from a CourseOffering, because CourseOffering has
 * no reliable FK to Cohort before that term's offerings/registrations even exist yet.
 */
@Service
@Transactional(readOnly = true)
public class TimetableCapacityPlanningService {

    private static final int WORKING_DAYS_PER_WEEK = 6;

    private final CohortRepository cohortRepository;
    private final CohortSectionRepository cohortSectionRepository;
    private final TermInstanceRepository termInstanceRepository;
    private final StudentTermEnrollmentRepository studentTermEnrollmentRepository;
    private final ClassroomRepository classroomRepository;
    private final LabRepository labRepository;
    private final ClinicalVenueRepository clinicalVenueRepository;
    private final PeriodRepository periodRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final BlockedPeriodRepository blockedPeriodRepository;

    public TimetableCapacityPlanningService(CohortRepository cohortRepository,
                                             CohortSectionRepository cohortSectionRepository,
                                             TermInstanceRepository termInstanceRepository,
                                             StudentTermEnrollmentRepository studentTermEnrollmentRepository,
                                             ClassroomRepository classroomRepository,
                                             LabRepository labRepository,
                                             ClinicalVenueRepository clinicalVenueRepository,
                                             PeriodRepository periodRepository,
                                             ClassScheduleRepository classScheduleRepository,
                                             CalendarEventRepository calendarEventRepository,
                                             CourseOfferingRepository courseOfferingRepository,
                                             BlockedPeriodRepository blockedPeriodRepository) {
        this.cohortRepository = cohortRepository;
        this.cohortSectionRepository = cohortSectionRepository;
        this.termInstanceRepository = termInstanceRepository;
        this.studentTermEnrollmentRepository = studentTermEnrollmentRepository;
        this.classroomRepository = classroomRepository;
        this.labRepository = labRepository;
        this.clinicalVenueRepository = clinicalVenueRepository;
        this.periodRepository = periodRepository;
        this.classScheduleRepository = classScheduleRepository;
        this.calendarEventRepository = calendarEventRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.blockedPeriodRepository = blockedPeriodRepository;
    }

    public CapacityPlanResponse getPlan(Long termInstanceId, Long cohortId, PlanningBasis planningBasisParam) {
        TermInstance term = termInstanceRepository.findById(termInstanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + termInstanceId));
        Cohort cohort = cohortRepository.findByIdWithCourse(cohortId)
            .orElseThrow(() -> new ResourceNotFoundException("Cohort not found with id: " + cohortId));

        long enrolledStrength = studentTermEnrollmentRepository.countByTermInstanceIdAndCohortIdAndStatus(
            termInstanceId, cohortId, EnrollmentStatus.ENROLLED);
        Integer sanctionedStrength = cohort.getSanctionedIntake();

        PlanningBasis planningBasis = planningBasisParam != null ? planningBasisParam : PlanningBasis.ENROLLED;
        // Read-only view: fall back to enrolled headcount if SANCTIONED was requested but this
        // cohort has no seat data configured, rather than failing the whole plan just to show it
        // — the commit path enforces this strictly instead (CohortRoomAllocationService).
        long strength = (planningBasis == PlanningBasis.SANCTIONED && sanctionedStrength != null)
            ? sanctionedStrength : enrolledStrength;

        // A cohort's enrolled students all share one semester in a given term (whole-cohort
        // promotion) -- used to scope the "Create Suggested Batches" subject picker down to this
        // cohort's actual offerings instead of every offering in the shared TermInstance (which
        // packs every concurrent year's subjects together).
        Integer semesterNumber = studentTermEnrollmentRepository
            .findFirstByTermInstanceIdAndCohortIdAndStatus(termInstanceId, cohortId, EnrollmentStatus.ENROLLED)
            .map(StudentTermEnrollment::getSemesterNumber)
            .orElse(null);

        List<Classroom> activeClassrooms = classroomRepository.findByIsActiveTrueOrderByNameAsc();
        List<Lab> activeLabs = labRepository.findAll().stream()
            .filter(l -> l.getStatus() == LabStatus.ACTIVE || l.getStatus() == LabStatus.AVAILABLE)
            .toList();
        List<ClinicalVenue> activeClinicalVenues = clinicalVenueRepository.findByIsActiveTrueOrderByNameAsc();

        List<VenueOptionResponse> fittingClassrooms = activeClassrooms.stream()
            .filter(c -> c.getCapacity() != null && c.getCapacity() >= strength)
            .map(c -> new VenueOptionResponse(c.getId(), c.getName(), c.getCapacity()))
            .toList();
        boolean theoryFits = !fittingClassrooms.isEmpty();
        String theoryShortfall = theoryFits ? null
            : "No single classroom seats " + strength + " students — split this cohort's Theory into sections below.";

        // Every classroom with a genuine active claim this term (from some other cohort -- this
        // plan's own cohort, if it already has a committed allocation, is surfaced via
        // currentAllocation() on the frontend instead of ever reaching this draft-building path).
        // Drives both the sectioning candidate pool below and the Venue Utilization card's
        // "Committed — <cohort>" tag, so a room's unavailability is self-explanatory instead of
        // silently vanishing from the picker with no on-screen reason.
        List<com.cms.model.CohortSection> activeSectionsThisTerm = cohortSectionRepository.findByTermInstanceIdAndIsActiveTrue(termInstanceId);
        Map<Long, String> claimedByCohortLabel = activeSectionsThisTerm.stream()
            .collect(Collectors.toMap(s -> s.getClassroom().getId(), s -> s.getCohortRoomAllocation().getCohort().getDisplayName(),
                (a, b) -> a));
        Set<Long> claimedClassroomIds = claimedByCohortLabel.keySet();

        // Candidate pool for Theory sectioning: every active classroom not already claimed by
        // another cohort's active section this term, sorted biggest-first so the frontend's
        // greedy auto-split fills the fewest possible sections by default.
        List<VenueOptionResponse> classroomsForSectioning = activeClassrooms.stream()
            .filter(c -> !claimedClassroomIds.contains(c.getId()))
            .sorted((a, b) -> Integer.compare(
                b.getCapacity() != null ? b.getCapacity() : 0, a.getCapacity() != null ? a.getCapacity() : 0))
            .map(c -> new VenueOptionResponse(c.getId(), c.getName(), c.getCapacity()))
            .toList();

        List<VenueOptionResponse> fittingLabs = activeLabs.stream()
            .map(l -> new VenueOptionResponse(l.getId(), l.getName(), l.getCapacity()))
            .toList();
        List<VenueOptionResponse> fittingClinicalVenues = activeClinicalVenues.stream()
            .map(v -> new VenueOptionResponse(v.getId(), v.getName(), v.getCapacity()))
            .toList();

        List<Period> activePeriods = periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc();
        double periodDurationMinutes = com.cms.service.CurriculumHoursCalculator.averageDurationMinutes(
            activePeriods.stream().map(Period::getDurationMinutes).toList());

        Set<LocalDate> nonTeachingDates = nonTeachingDates(term);
        int workingDaysInTerm = countWorkingDays(term, nonTeachingDates);
        double totalWorkingPeriodHours = workingDaysInTerm * activePeriods.size() * periodDurationMinutes / 60.0;
        double blockedHours = blockedHoursInTerm(term, nonTeachingDates);
        int curriculumHoursRequired = semesterNumber == null ? 0
            : curriculumHoursRequired(termInstanceId, semesterNumber);
        double bufferHours = totalWorkingPeriodHours - blockedHours - curriculumHoursRequired;

        int totalSlots = WORKING_DAYS_PER_WEEK * activePeriods.size();
        List<ClassSchedule> termSchedule = List.copyOf(
            java.util.stream.Stream.concat(
                classScheduleRepository.findByTermInstanceIdAndStatus(termInstanceId, ClassScheduleStatus.PUBLISHED).stream(),
                classScheduleRepository.findByTermInstanceIdAndStatus(termInstanceId, ClassScheduleStatus.DRAFT).stream()
            ).toList());

        return new CapacityPlanResponse(
            cohortId,
            cohort.getDisplayName(),
            termInstanceId,
            term.getAcademicYear().getName() + " " + term.getTermType(),
            semesterNumber,
            strength,
            enrolledStrength,
            sanctionedStrength,
            workingDaysInTerm,
            totalWorkingPeriodHours,
            blockedHours,
            curriculumHoursRequired,
            bufferHours,
            theoryFits,
            theoryShortfall,
            fittingClassrooms,
            classroomsForSectioning,
            fittingLabs,
            fittingClinicalVenues,
            classroomUtilization(activeClassrooms, termSchedule, totalSlots, claimedByCohortLabel),
            utilization(activeLabs, Lab::getId, Lab::getName, Lab::getCapacity,
                termSchedule, ClassSessionType.LAB, cs -> cs.getLab() != null ? cs.getLab().getId() : null, totalSlots, Map.of()),
            utilization(activeClinicalVenues, ClinicalVenue::getId, ClinicalVenue::getName, ClinicalVenue::getCapacity,
                termSchedule, ClassSessionType.CLINICAL, cs -> cs.getClinicalVenue() != null ? cs.getClinicalVenue().getId() : null, totalSlots,
                Map.of())
        );
    }

    /** Every date in this term covered by a HOLIDAY or EXAM {@link CalendarEvent} -- shared by
     *  {@link #countWorkingDays} and {@link #blockedHoursInTerm} so both agree on which days are
     *  already fully excluded (avoids double-subtracting a blocked period on a day that's already
     *  a holiday). */
    private Set<LocalDate> nonTeachingDates(TermInstance term) {
        List<CalendarEvent> nonTeachingEvents = calendarEventRepository.findNonTeachingDaysOverlapping(
            term.getAcademicYear().getId(), term.getStartDate(), term.getEndDate());

        Set<LocalDate> dates = new HashSet<>();
        for (CalendarEvent event : nonTeachingEvents) {
            LocalDate d = event.getStartDate().isBefore(term.getStartDate()) ? term.getStartDate() : event.getStartDate();
            LocalDate end = event.getEndDate().isAfter(term.getEndDate()) ? term.getEndDate() : event.getEndDate();
            while (!d.isAfter(end)) {
                dates.add(d);
                d = d.plusDays(1);
            }
        }
        return dates;
    }

    /** Actual working days across this term's real date range -- Sundays and any HOLIDAY/EXAM day
     *  don't count. Distinct from the fixed weekly {@code WORKING_DAYS_PER_WEEK} constant used by
     *  venue utilization below (a per-week denominator); this is a term-total count used for the
     *  buffer-hours calculation. */
    private int countWorkingDays(TermInstance term, Set<LocalDate> nonTeachingDates) {
        int workingDays = 0;
        for (LocalDate d = term.getStartDate(); !d.isAfter(term.getEndDate()); d = d.plusDays(1)) {
            if (d.getDayOfWeek() != DayOfWeek.SUNDAY && !nonTeachingDates.contains(d)) {
                workingDays++;
            }
        }
        return workingDays;
    }

    /** Hours lost to ONE_OFF and RECURRING {@link BlockedPeriod} rows within this term -- both
     *  types count here (unlike Skeleton Builder placement, which only hard-blocks RECURRING).
     *  Skips days already excluded as a full holiday/exam day to avoid double-subtracting. */
    private double blockedHoursInTerm(TermInstance term, Set<LocalDate> nonTeachingDates) {
        List<BlockedPeriod> applicable = blockedPeriodRepository.findApplicableInRange(term.getStartDate(), term.getEndDate());
        if (applicable.isEmpty()) {
            return 0.0;
        }
        double totalMinutes = 0;
        for (LocalDate d = term.getStartDate(); !d.isAfter(term.getEndDate()); d = d.plusDays(1)) {
            if (d.getDayOfWeek() == DayOfWeek.SUNDAY || nonTeachingDates.contains(d)) {
                continue;
            }
            com.cms.model.enums.DayOfWeek appDayOfWeek = com.cms.model.enums.DayOfWeek.valueOf(d.getDayOfWeek().name());
            for (BlockedPeriod block : applicable) {
                boolean matches = block.getBlockType() == BlockType.ONE_OFF
                    ? d.equals(block.getSpecificDate())
                    : appDayOfWeek == block.getDayOfWeek()
                        && !d.isBefore(block.getRangeStartDate()) && !d.isAfter(block.getRangeEndDate());
                if (matches) {
                    totalMinutes += block.getPeriod().getDurationMinutes();
                }
            }
        }
        return totalMinutes / 60.0;
    }

    /** Sum of theory+lab+clinical curriculum hours for every non-elective offering at this
     *  cohort's semester in this term -- the "demand" side of the buffer calculation. Electives
     *  are excluded since a student takes only one per elective group, not every one on offer, so
     *  summing them would overstate required hours. */
    private int curriculumHoursRequired(Long termInstanceId, Integer semesterNumber) {
        return courseOfferingRepository.findByTermInstanceIdAndSemesterNumber(termInstanceId, semesterNumber).stream()
            .map(CourseOffering::getCurriculumSemesterCourse)
            .filter(csc -> csc != null && !Boolean.TRUE.equals(csc.getIsElective()))
            .mapToInt(csc -> nullToZero(csc.getTheoryHours()) + nullToZero(csc.getLabHours()) + nullToZero(csc.getClinicalHours()))
            .sum();
    }

    private static int nullToZero(Integer value) {
        return value != null ? value : 0;
    }

    /** Classroom utilization is deliberately NOT raw cross-cohort {@link ClassSchedule} occupancy
     *  the way Lab/Clinical utilization is -- a Theory classroom is exclusively one cohort's home
     *  room per term (enforced by {@code ux_cohort_section_classroom_per_term}), so once that
     *  claim is reverted the room is genuinely free again even though its old DRAFT sessions
     *  usually linger in Skeleton Builder (Theory {@code ClassSchedule} rows aren't cascaded from
     *  {@code CohortRoomAllocation}/{@code CohortSection} the way Lab/Clinical batches are, so
     *  reverting never cleans them up). Showing that leftover schedule debris as "occupied" next
     *  to a room the admin is actively free to claim right now is actively misleading -- so a
     *  classroom with no active claim always reports 0%, full stop, regardless of what stale rows
     *  sit underneath it. Only a classroom someone currently, actively claims shows a real number. */
    private List<VenueUtilizationResponse> classroomUtilization(List<Classroom> classrooms, List<ClassSchedule> termSchedule,
                                                                  int totalSlots, Map<Long, String> claimedByCohortLabel) {
        Map<Long, Long> occupiedByClassroomId = termSchedule.stream()
            .filter(cs -> cs.getSessionType() == ClassSessionType.THEORY && cs.getClassroom() != null)
            .collect(Collectors.groupingBy(cs -> cs.getClassroom().getId(), Collectors.counting()));

        return classrooms.stream()
            .map(c -> {
                boolean claimed = claimedByCohortLabel.containsKey(c.getId());
                long occupied = claimed ? occupiedByClassroomId.getOrDefault(c.getId(), 0L) : 0L;
                double percent = !claimed || totalSlots == 0 ? 0.0 : (occupied * 100.0) / totalSlots;
                return new VenueUtilizationResponse(c.getId(), c.getName(), c.getCapacity(), occupied, totalSlots, percent,
                    claimedByCohortLabel.get(c.getId()));
            })
            .toList();
    }

    /** Occupied-vs-total-slot utilization for Lab/Clinical venues, computed from the term's
     *  already globally-scoped schedule (a TermInstance is shared institution-wide across every
     *  cohort/program, so this is genuinely cross-cohort occupancy) -- unlike classrooms, a
     *  Lab/Clinical venue has no per-term exclusivity lock, so multiple cohorts legitimately share
     *  one venue across different day/period slots and the raw count is the correct signal. */
    private <T> List<VenueUtilizationResponse> utilization(List<T> venues, Function<T, Long> idFn, Function<T, String> nameFn,
                                                             Function<T, Integer> capacityFn, List<ClassSchedule> termSchedule,
                                                             ClassSessionType type, Function<ClassSchedule, Long> venueIdFn,
                                                             int totalSlots, Map<Long, String> claimedByCohortLabel) {
        Map<Long, Long> occupiedByVenueId = termSchedule.stream()
            .filter(cs -> cs.getSessionType() == type && venueIdFn.apply(cs) != null)
            .collect(Collectors.groupingBy(venueIdFn, Collectors.counting()));

        return venues.stream()
            .map(v -> {
                Long id = idFn.apply(v);
                long occupied = occupiedByVenueId.getOrDefault(id, 0L);
                double percent = totalSlots == 0 ? 0.0 : (occupied * 100.0) / totalSlots;
                return new VenueUtilizationResponse(id, nameFn.apply(v), capacityFn.apply(v), occupied, totalSlots, percent,
                    claimedByCohortLabel.get(id));
            })
            .toList();
    }
}
