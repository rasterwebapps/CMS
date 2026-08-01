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
import com.cms.repository.BlockedPeriodRepository;
import com.cms.repository.CalendarEventRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.ClinicalVenueRepository;
import com.cms.repository.CohortRepository;
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

    /** Used only when the caller doesn't supply one; overridable per-request, never hardcoded
     *  as policy — this is just a reasonable starting point for the input field. */
    private static final int DEFAULT_TARGET_BATCH_SIZE = 30;
    private static final int WORKING_DAYS_PER_WEEK = 6;

    private final CohortRepository cohortRepository;
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

    public CapacityPlanResponse getPlan(Long termInstanceId, Long cohortId, Integer targetBatchSizeParam) {
        TermInstance term = termInstanceRepository.findById(termInstanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + termInstanceId));
        Cohort cohort = cohortRepository.findByIdWithCourse(cohortId)
            .orElseThrow(() -> new ResourceNotFoundException("Cohort not found with id: " + cohortId));

        int targetBatchSize = (targetBatchSizeParam != null && targetBatchSizeParam > 0)
            ? targetBatchSizeParam : DEFAULT_TARGET_BATCH_SIZE;

        long strength = studentTermEnrollmentRepository.countByTermInstanceIdAndCohortIdAndStatus(
            termInstanceId, cohortId, EnrollmentStatus.ENROLLED);

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
            : "No single classroom seats " + strength + " students — consider splitting this cohort's Theory into more than one section.";

        int labBatchesNeeded = strength == 0 ? 0 : (int) Math.ceil((double) strength / targetBatchSize);
        List<VenueOptionResponse> fittingLabs = activeLabs.stream()
            .filter(l -> l.getCapacity() != null && l.getCapacity() >= targetBatchSize)
            .map(l -> new VenueOptionResponse(l.getId(), l.getName(), l.getCapacity()))
            .toList();

        int clinicalBatchesNeeded = labBatchesNeeded;
        List<VenueOptionResponse> fittingClinicalVenues = activeClinicalVenues.stream()
            .filter(v -> v.getCapacity() != null && v.getCapacity() >= targetBatchSize)
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
            workingDaysInTerm,
            totalWorkingPeriodHours,
            blockedHours,
            curriculumHoursRequired,
            bufferHours,
            targetBatchSize,
            theoryFits,
            theoryShortfall,
            fittingClassrooms,
            labBatchesNeeded,
            fittingLabs,
            clinicalBatchesNeeded,
            fittingClinicalVenues,
            utilization(activeClassrooms, Classroom::getId, Classroom::getName, Classroom::getCapacity,
                termSchedule, ClassSessionType.THEORY, cs -> cs.getClassroom() != null ? cs.getClassroom().getId() : null, totalSlots),
            utilization(activeLabs, Lab::getId, Lab::getName, Lab::getCapacity,
                termSchedule, ClassSessionType.LAB, cs -> cs.getLab() != null ? cs.getLab().getId() : null, totalSlots),
            utilization(activeClinicalVenues, ClinicalVenue::getId, ClinicalVenue::getName, ClinicalVenue::getCapacity,
                termSchedule, ClassSessionType.CLINICAL, cs -> cs.getClinicalVenue() != null ? cs.getClinicalVenue().getId() : null, totalSlots)
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

    /** Occupied-vs-total-slot utilization for one venue type, computed from the term's already
     *  globally-scoped schedule (a TermInstance is shared institution-wide across every
     *  cohort/program, so this is cross-cohort occupancy, not just this planning run's cohort). */
    private <T> List<VenueUtilizationResponse> utilization(List<T> venues, Function<T, Long> idFn, Function<T, String> nameFn,
                                                             Function<T, Integer> capacityFn, List<ClassSchedule> termSchedule,
                                                             ClassSessionType type, Function<ClassSchedule, Long> venueIdFn,
                                                             int totalSlots) {
        Map<Long, Long> occupiedByVenueId = termSchedule.stream()
            .filter(cs -> cs.getSessionType() == type && venueIdFn.apply(cs) != null)
            .collect(Collectors.groupingBy(venueIdFn, Collectors.counting()));

        return venues.stream()
            .map(v -> {
                Long id = idFn.apply(v);
                long occupied = occupiedByVenueId.getOrDefault(id, 0L);
                double percent = totalSlots == 0 ? 0.0 : (occupied * 100.0) / totalSlots;
                return new VenueUtilizationResponse(id, nameFn.apply(v), capacityFn.apply(v), occupied, totalSlots, percent);
            })
            .toList();
    }
}
