package com.cms.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.FacultyWorkloadReportResponse;
import com.cms.dto.FacultyWorkloadRow;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.ClassSchedule;
import com.cms.model.CourseOffering;
import com.cms.model.DesignationMaster;
import com.cms.model.Faculty;
import com.cms.model.FacultyAvailability;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.FacultyAvailabilityRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.TermInstanceRepository;

/**
 * "Does this term have enough faculty-hours to cover the work" report. Shows two independent
 * demand figures side by side rather than picking one: curriculum-required hours per {@link
 * CourseOffering} (visible before any staffing happens) and hours actually committed via placed
 * {@link ClassSchedule} rows (the real load once Skeleton Builder/Staffing has run), both against
 * a designation-default-with-per-faculty-override capacity, net of {@link FacultyAvailability}
 * blocked (prep/eval) time. Never auto-allocates and never blocks anything itself — purely a
 * dashboard — but {@link #resolveEffectiveCapacity} is also the single source of truth {@link
 * TimetableStaffingService#checkWithinWorkloadCaps}'s weekly hard-cap gate resolves a faculty's
 * capacity from, so this report and that enforcement gate can never disagree on the number.
 */
@Service
@Transactional(readOnly = true)
public class FacultyWorkloadCapacityService {

    private final TermInstanceRepository termInstanceRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final FacultyRepository facultyRepository;
    private final FacultyAvailabilityRepository facultyAvailabilityRepository;
    private final TimetableGlobalAutoScheduleService timetableGlobalAutoScheduleService;

    public FacultyWorkloadCapacityService(TermInstanceRepository termInstanceRepository,
                                           ClassScheduleRepository classScheduleRepository,
                                           FacultyRepository facultyRepository,
                                           FacultyAvailabilityRepository facultyAvailabilityRepository,
                                           TimetableGlobalAutoScheduleService timetableGlobalAutoScheduleService) {
        this.termInstanceRepository = termInstanceRepository;
        this.classScheduleRepository = classScheduleRepository;
        this.facultyRepository = facultyRepository;
        this.facultyAvailabilityRepository = facultyAvailabilityRepository;
        this.timetableGlobalAutoScheduleService = timetableGlobalAutoScheduleService;
    }

    public FacultyWorkloadReportResponse getTermWorkloadReport(Long termInstanceId) {
        TermInstance term = termInstanceRepository.findById(termInstanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + termInstanceId));
        int weeksInTerm = CurriculumHoursCalculator.weeksInTerm(term);

        // Reuses TimetableGlobalAutoScheduleService's per-cohort/per-section/per-batch demand
        // attribution (term-total hours) rather than a second, coarser computation here -- the old
        // version summed each offering's curriculum hours exactly once regardless of how many
        // cohorts shared it, silently under-counting shared offerings.
        Map<Long, Double> demandByFaculty = new HashMap<>();
        timetableGlobalAutoScheduleService.getTermTotalDemandByFaculty(termInstanceId)
            .forEach((facultyId, totalHours) -> demandByFaculty.put(facultyId, totalHours / weeksInTerm));

        // Matches TimetableStaffingService.checkWithinWorkloadCaps's own {PUBLISHED, DRAFT}
        // filter -- previously this summed every status, which could silently disagree with the
        // hard-cap gate's own committed-hours count for the same faculty/term.
        Map<Long, Double> committedByFaculty = new HashMap<>();
        for (ClassSchedule schedule : Stream.of(ClassScheduleStatus.PUBLISHED, ClassScheduleStatus.DRAFT)
                .flatMap(status -> classScheduleRepository.findByTermInstanceIdAndStatus(termInstanceId, status).stream())
                .toList()) {
            Faculty faculty = schedule.getFaculty();
            if (faculty == null || faculty.getId() == null || schedule.getPeriod() == null) {
                continue;
            }
            double hours = schedule.getPeriod().getDurationMinutes() / 60.0;
            committedByFaculty.merge(faculty.getId(), hours, Double::sum);
        }

        Set<Long> facultyIds = new HashSet<>(demandByFaculty.keySet());
        facultyIds.addAll(committedByFaculty.keySet());
        List<Faculty> faculties = facultyRepository.findAllById(facultyIds);

        Map<Long, Double> blockedByFaculty = new HashMap<>();
        for (FacultyAvailability block : facultyAvailabilityRepository
                .findByFacultyIdInOrderByDayOfWeekAscStartTimeAsc(new ArrayList<>(facultyIds))) {
            Long facultyId = block.getFaculty().getId();
            double hours = Duration.between(block.getStartTime(), block.getEndTime()).toMinutes() / 60.0;
            blockedByFaculty.merge(facultyId, hours, Double::sum);
        }

        List<FacultyWorkloadRow> rows = new ArrayList<>();
        double totalDemand = 0;
        double totalCommitted = 0;
        double totalConfiguredCapacity = 0;
        int unconfiguredCount = 0;

        for (Faculty faculty : faculties) {
            double demand = demandByFaculty.getOrDefault(faculty.getId(), 0.0);
            double committed = committedByFaculty.getOrDefault(faculty.getId(), 0.0);
            double blocked = blockedByFaculty.getOrDefault(faculty.getId(), 0.0);

            Integer effective = resolveEffectiveCapacity(faculty);
            boolean configured = effective != null;
            Double netCapacity = configured ? Math.max(0.0, effective - blocked) : null;

            rows.add(new FacultyWorkloadRow(
                faculty.getId(), faculty.getFullName(), designationName(faculty),
                demand, committed, blocked,
                configured, configured ? effective.doubleValue() : null, netCapacity,
                configured && demand > netCapacity, configured && committed > netCapacity));

            totalDemand += demand;
            totalCommitted += committed;
            if (configured) {
                totalConfiguredCapacity += netCapacity;
            } else {
                unconfiguredCount++;
            }
        }
        rows.sort((a, b) -> a.facultyName().compareToIgnoreCase(b.facultyName()));

        return new FacultyWorkloadReportResponse(termInstanceId, rows,
            totalDemand, totalCommitted, totalConfiguredCapacity, unconfiguredCount);
    }

    static Integer resolveEffectiveCapacity(Faculty faculty) {
        return resolveEffective(faculty, Faculty::getPlannedWeeklyHoursOverride, DesignationMaster::getDefaultWeeklyTeachingHours);
    }

    /** Same per-faculty-then-designation precedence as {@link #resolveEffectiveCapacity}, feeding
     *  {@link TimetableStaffingService}'s daily hard-cap gate instead of the weekly report. */
    static Integer resolveEffectiveDailyCapacity(Faculty faculty) {
        return resolveEffective(faculty, Faculty::getPlannedDailyHoursOverride, DesignationMaster::getDefaultDailyTeachingHours);
    }

    /** Same per-faculty-then-designation precedence as {@link #resolveEffectiveCapacity}, feeding
     *  {@link TimetableStaffingService}'s continuous (unbroken run) hard-cap gate. */
    static Integer resolveEffectiveContinuousCapacity(Faculty faculty) {
        return resolveEffective(faculty, Faculty::getPlannedContinuousHoursOverride, DesignationMaster::getDefaultContinuousTeachingHours);
    }

    private static Integer resolveEffective(Faculty faculty,
                                              java.util.function.Function<Faculty, Integer> facultyOverride,
                                              java.util.function.Function<DesignationMaster, Integer> designationDefault) {
        Integer override = facultyOverride.apply(faculty);
        if (override != null) {
            return override;
        }
        DesignationMaster designation = faculty.getDesignation();
        return designation != null ? designationDefault.apply(designation) : null;
    }

    private static String designationName(Faculty faculty) {
        DesignationMaster designation = faculty.getDesignation();
        return designation != null ? designation.getName() : null;
    }
}
