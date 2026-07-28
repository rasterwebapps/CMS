package com.cms.service;

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.TimetableActionResponse;
import com.cms.dto.TimetableGenerationResponse;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Batch;
import com.cms.model.ClassSchedule;
import com.cms.model.Classroom;
import com.cms.model.CourseOffering;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.Faculty;
import com.cms.model.FacultyAvailability;
import com.cms.model.Lab;
import com.cms.model.LabSlot;
import com.cms.model.Period;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.repository.BatchRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.FacultyAvailabilityRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.LabAttendanceRepository;
import com.cms.repository.LabRepository;
import com.cms.repository.LabSlotRepository;
import com.cms.repository.PeriodRepository;
import com.cms.repository.TermInstanceRepository;

/**
 * "Suggest-and-approve" draft generation, not fully automatic: Generate produces DRAFT rows an
 * admin reviews (and can hand-fix via the extended /lab-schedules form, or the in-grid Swap
 * feature) before Approve flips them to PUBLISHED. Conflict-checking is only against rows placed
 * earlier in the *same run* (never against pre-existing PUBLISHED rows for the term) — this is
 * safe because {@link #generate} refuses to run at all once the term has been published (see the
 * regeneration guard below); a DRAFT-only term has no PUBLISHED rows to conflict with in the first
 * place. {@link CurriculumSemesterCourse} hours are 60-minute CLOCK hours (INC/UGC convention —
 * a syllabus stating "160 hours" means 160 real hours of instruction, regardless of how long a
 * campus's periods are), so 1 Period/LabSlot is <b>not</b> 1 curriculum hour whenever a period runs
 * shorter or longer than 60 minutes — see {@link #sessionsPerWeek}. Lab and clinical hours are
 * summed into one LAB placement count (the schema discriminator is only THEORY/LAB) and placed via
 * LabSlot, matching how the schema already lumps them — a genuinely distinct shift-based clinical
 * model is a separate, larger change, not done here. Elective offerings are skipped, matching the
 * existing bulk course-registration generation precedent (CourseRegistrationServiceImpl) of leaving
 * electives for manual/individual assignment.
 *
 * <p>{@link CurriculumSemesterCourse} hours are total-term hours, not a literal count of slots to
 * place — {@link ClassSchedule} has no date, only a {@code dayOfWeek} + {@code termInstance} (it's
 * already a weekly-recurring template, see {@link PersonalTimetableService}), so one placed row
 * delivers {@code weeksInTerm} occurrences over the term. Placement therefore targets
 * sessions-per-week = ceil((totalHours × 60 / slotDurationMinutes) / weeksInTerm), rounding up so
 * the term always meets or slightly exceeds the required clock-hours rather than falling short. A
 * same-subject/same-day cap (one theory session per subject per day, one lab session per
 * subject+batch per day) keeps the now much smaller placement count from clustering multiple
 * sessions of the same subject on one day just because slots happened to be free.
 *
 * <p><b>Regenerate is the same call as Generate.</b> {@link #generate} deletes any existing DRAFT
 * rows for the term up front and re-places from scratch — so it doubles as an in-place "try again"
 * action that discards whatever manual Swap tweaks were made to the current draft. The
 * period/classroom/lab/day iteration order is shuffled per call (see {@link #shuffledCopy}) so
 * consecutive calls on the same input tend to produce a different arrangement rather than the
 * same deterministic layout every time.
 */
@Service
@Transactional(readOnly = true)
public class TimetableGenerationService {

    private final ClassScheduleRepository classScheduleRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final TermInstanceRepository termInstanceRepository;
    private final BatchRepository batchRepository;
    private final FacultyRepository facultyRepository;
    private final ClassroomRepository classroomRepository;
    private final PeriodRepository periodRepository;
    private final LabRepository labRepository;
    private final LabSlotRepository labSlotRepository;
    private final FacultyAvailabilityRepository facultyAvailabilityRepository;
    private final LabAttendanceRepository labAttendanceRepository;

    public TimetableGenerationService(ClassScheduleRepository classScheduleRepository,
                                       CourseOfferingRepository courseOfferingRepository,
                                       TermInstanceRepository termInstanceRepository,
                                       BatchRepository batchRepository,
                                       FacultyRepository facultyRepository,
                                       ClassroomRepository classroomRepository,
                                       PeriodRepository periodRepository,
                                       LabRepository labRepository,
                                       LabSlotRepository labSlotRepository,
                                       FacultyAvailabilityRepository facultyAvailabilityRepository,
                                       LabAttendanceRepository labAttendanceRepository) {
        this.classScheduleRepository = classScheduleRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.termInstanceRepository = termInstanceRepository;
        this.batchRepository = batchRepository;
        this.facultyRepository = facultyRepository;
        this.classroomRepository = classroomRepository;
        this.periodRepository = periodRepository;
        this.labRepository = labRepository;
        this.labSlotRepository = labSlotRepository;
        this.facultyAvailabilityRepository = facultyAvailabilityRepository;
        this.labAttendanceRepository = labAttendanceRepository;
    }

    /** One placement already committed this run, tracked purely in-memory for conflict-checking
     *  subsequent candidates — see the class-level note on why pre-existing PUBLISHED rows are
     *  never a factor here. */
    private record Occupied(DayOfWeek day, LocalTime start, LocalTime end,
                             String roomKey, Long facultyId, String audienceKey) {
        boolean overlaps(DayOfWeek day, LocalTime start, LocalTime end) {
            return this.day == day && this.start.isBefore(end) && this.end.isAfter(start);
        }
    }

    @Transactional
    public TimetableGenerationResponse generate(Long termInstanceId) {
        if (classScheduleRepository.existsByTermInstanceIdAndStatus(termInstanceId, ClassScheduleStatus.PUBLISHED)) {
            throw new LifecycleConflictException(
                "This term's timetable has already been approved and published. Discard it before regenerating.",
                "TIMETABLE_ALREADY_PUBLISHED", "TermInstance", termInstanceId, null);
        }
        TermInstance termInstance = termInstanceRepository.findById(termInstanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + termInstanceId));

        // In-place regenerate: wipe whatever DRAFT rows (and any manual Swap tweaks) already exist
        // for this term and re-place from scratch — see class javadoc.
        classScheduleRepository.deleteByTermInstanceIdAndStatus(termInstanceId, ClassScheduleStatus.DRAFT);

        List<CourseOffering> offerings = courseOfferingRepository.findByTermInstanceIdAndIsActiveTrue(termInstanceId);
        List<Period> periods = shuffledCopy(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc());
        List<LabSlot> labSlots = shuffledCopy(labSlotRepository.findByIsActiveTrueOrderBySlotOrderAsc());
        List<Classroom> classrooms = shuffledCopy(classroomRepository.findByIsActiveTrueOrderByNameAsc());
        List<Lab> labs = shuffledCopy(labRepository.findAll());
        List<DayOfWeek> days = shuffledCopy(List.of(DayOfWeek.values()));

        List<Occupied> occupied = new ArrayList<>();
        List<String> unplaceable = new ArrayList<>();
        Map<Long, List<FacultyAvailability>> availabilityByFaculty = new HashMap<>();
        int generatedCount = 0;
        int weeksInTerm = weeksInTerm(termInstance);
        double periodDurationMinutes = averageDurationMinutes(
            periods.stream().map(Period::getDurationMinutes).toList());
        double labSlotDurationMinutes = averageDurationMinutes(
            labSlots.stream().map(s -> (int) Duration.between(s.getStartTime(), s.getEndTime()).toMinutes()).toList());

        for (CourseOffering offering : offerings) {
            CurriculumSemesterCourse csc = offering.getCurriculumSemesterCourse();
            String subjectName = offering.getSubject().getName();

            if (csc == null) {
                unplaceable.add(subjectName + ": no resolved curriculum mapping, skipped");
                continue;
            }
            if (Boolean.TRUE.equals(csc.getIsElective())) {
                continue; // left for manual Elective Assignment, matching existing bulk-generate precedent
            }
            if (offering.getFacultyId() == null) {
                unplaceable.add(subjectName + ": no faculty assigned to this offering yet");
                continue;
            }
            Optional<Faculty> facultyOpt = facultyRepository.findById(offering.getFacultyId());
            if (facultyOpt.isEmpty()) {
                unplaceable.add(subjectName + ": assigned faculty not found");
                continue;
            }
            Faculty faculty = facultyOpt.get();
            List<FacultyAvailability> availabilityBlocks = availabilityByFaculty.computeIfAbsent(
                faculty.getId(), facultyAvailabilityRepository::findByFacultyIdOrderByDayOfWeekAscStartTimeAsc);

            int theoryHours = csc.getTheoryHours() != null ? csc.getTheoryHours() : 0;
            if (theoryHours > 0) {
                int sessionsPerWeek = sessionsPerWeek(theoryHours, weeksInTerm, periodDurationMinutes);
                int placed = placeTheory(offering, faculty, sessionsPerWeek, periods, classrooms, days,
                    availabilityBlocks, occupied, termInstance);
                if (placed < sessionsPerWeek) {
                    unplaceable.add(subjectName + ": placed " + placed + "/" + sessionsPerWeek
                        + " weekly theory session(s) (" + theoryHours + " hrs/term over " + weeksInTerm
                        + " weeks; no free day/period/classroom combination left)");
                }
                generatedCount += placed;
            }

            int labClinicalHours = (csc.getLabHours() != null ? csc.getLabHours() : 0)
                + (csc.getClinicalHours() != null ? csc.getClinicalHours() : 0);
            if (labClinicalHours > 0) {
                int sessionsPerWeek = sessionsPerWeek(labClinicalHours, weeksInTerm, labSlotDurationMinutes);
                List<Batch> batches = batchRepository.findByCourseOfferingId(offering.getId());
                if (batches.isEmpty()) {
                    unplaceable.add(subjectName + ": " + labClinicalHours
                        + " lab/clinical hours needed but no batches are defined for this offering");
                } else {
                    for (Batch batch : batches) {
                        int placed = placeLab(offering, faculty, batch, sessionsPerWeek, labSlots, labs, days,
                            availabilityBlocks, occupied, termInstance);
                        if (placed < sessionsPerWeek) {
                            unplaceable.add(subjectName + " (" + batch.getName() + "): placed " + placed + "/" + sessionsPerWeek
                                + " weekly lab/clinical session(s) (" + labClinicalHours + " hrs/term over " + weeksInTerm
                                + " weeks; no free day/slot/lab combination left)");
                        }
                        generatedCount += placed;
                    }
                }
            }
        }

        return new TimetableGenerationResponse(generatedCount, unplaceable);
    }

    private int placeTheory(CourseOffering offering, Faculty faculty, int sessionsPerWeek,
                             List<Period> periods, List<Classroom> classrooms, List<DayOfWeek> days,
                             List<FacultyAvailability> availabilityBlocks,
                             List<Occupied> occupied, TermInstance termInstance) {
        int placed = 0;
        Set<DayOfWeek> daysUsed = EnumSet.noneOf(DayOfWeek.class);
        for (int i = 0; i < sessionsPerWeek; i++) {
            boolean found = false;
            for (Period period : periods) {
                if (found) break;
                for (DayOfWeek day : days) {
                    if (found) break;
                    if (daysUsed.contains(day)) continue;
                    if (!isFacultyAvailable(availabilityBlocks, day, period.getStartTime(), period.getEndTime())) continue;
                    for (Classroom classroom : classrooms) {
                        String roomKey = "THEORY:" + classroom.getId();
                        String audienceKey = "THEORY:" + offering.getId();
                        if (!isFree(occupied, day, period.getStartTime(), period.getEndTime(), roomKey, faculty.getId(), audienceKey)) {
                            continue;
                        }
                        ClassSchedule cs = new ClassSchedule();
                        cs.setSessionType(ClassSessionType.THEORY);
                        cs.setStatus(ClassScheduleStatus.DRAFT);
                        cs.setSubject(offering.getSubject());
                        cs.setFaculty(faculty);
                        cs.setDayOfWeek(day);
                        cs.setTermInstance(termInstance);
                        cs.setClassroom(classroom);
                        cs.setPeriod(period);
                        cs.setCourseOffering(offering);
                        cs.setIsActive(true);
                        classScheduleRepository.save(cs);

                        occupied.add(new Occupied(day, period.getStartTime(), period.getEndTime(), roomKey, faculty.getId(), audienceKey));
                        daysUsed.add(day);
                        placed++;
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                break;
            }
        }
        return placed;
    }

    private int placeLab(CourseOffering offering, Faculty faculty, Batch batch, int sessionsPerWeek,
                          List<LabSlot> labSlots, List<Lab> labs, List<DayOfWeek> days,
                          List<FacultyAvailability> availabilityBlocks,
                          List<Occupied> occupied, TermInstance termInstance) {
        int placed = 0;
        Set<DayOfWeek> daysUsed = EnumSet.noneOf(DayOfWeek.class);
        for (int i = 0; i < sessionsPerWeek; i++) {
            boolean found = false;
            for (LabSlot slot : labSlots) {
                if (found) break;
                for (DayOfWeek day : days) {
                    if (found) break;
                    if (daysUsed.contains(day)) continue;
                    if (!isFacultyAvailable(availabilityBlocks, day, slot.getStartTime(), slot.getEndTime())) continue;
                    for (Lab lab : labs) {
                        String roomKey = "LAB:" + lab.getId();
                        String audienceKey = "LAB:" + batch.getId();
                        if (!isFree(occupied, day, slot.getStartTime(), slot.getEndTime(), roomKey, faculty.getId(), audienceKey)) {
                            continue;
                        }
                        ClassSchedule cs = new ClassSchedule();
                        cs.setSessionType(ClassSessionType.LAB);
                        cs.setStatus(ClassScheduleStatus.DRAFT);
                        cs.setSubject(offering.getSubject());
                        cs.setFaculty(faculty);
                        cs.setDayOfWeek(day);
                        cs.setTermInstance(termInstance);
                        cs.setLab(lab);
                        cs.setLabSlot(slot);
                        cs.setBatch(batch);
                        cs.setBatchName(batch.getName());
                        cs.setCourseOffering(offering);
                        cs.setIsActive(true);
                        classScheduleRepository.save(cs);

                        occupied.add(new Occupied(day, slot.getStartTime(), slot.getEndTime(), roomKey, faculty.getId(), audienceKey));
                        daysUsed.add(day);
                        placed++;
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                break;
            }
        }
        return placed;
    }

    /** Whole weeks spanned by the term, rounded up so a partial trailing week still counts as a
     *  full placement opportunity; at least 1 to avoid a divide-by-zero for a same-day term. */
    private static int weeksInTerm(TermInstance termInstance) {
        long days = ChronoUnit.DAYS.between(termInstance.getStartDate(), termInstance.getEndDate()) + 1;
        return (int) Math.max(1, Math.ceil(days / 7.0));
    }

    /** Total term hours are 60-minute CLOCK hours delivered by a recurring weekly slot (see class
     *  javadoc) whose own duration may not be 60 minutes — a 50-minute period needs more weekly
     *  occurrences than a 60-minute one to deliver the same clock-hours. Converts totalHours to
     *  minutes, divides by the slot's actual duration to get total slots needed over the term,
     *  then spreads that across the term's weeks — rounded up at each step so the term never falls
     *  short of the required clock-hours. */
    private static int sessionsPerWeek(int totalHours, int weeksInTerm, double slotDurationMinutes) {
        if (totalHours <= 0) {
            return 0;
        }
        double slotsNeededOverTerm = (totalHours * 60.0) / slotDurationMinutes;
        return (int) Math.ceil(slotsNeededOverTerm / weeksInTerm);
    }

    /** One representative duration for a pool of periods/lab-slots (per the agreed v1 scope: a
     *  single duration per placement type, not exact per-slot minute accumulation — correct today
     *  since every period/lab-slot pool in this system is configured uniformly, and a reasonable
     *  approximation if that ever changes). Falls back to 60 minutes for an empty pool, where the
     *  value is moot anyway since nothing will be placed. */
    private static double averageDurationMinutes(List<Integer> durationsMinutes) {
        return durationsMinutes.stream()
            .mapToInt(Integer::intValue)
            .average()
            .orElse(60.0);
    }

    private boolean isFree(List<Occupied> occupied, DayOfWeek day, LocalTime start, LocalTime end,
                            String roomKey, Long facultyId, String audienceKey) {
        for (Occupied o : occupied) {
            if (!o.overlaps(day, start, end)) continue;
            if (o.roomKey().equals(roomKey)) return false;
            if (o.facultyId().equals(facultyId)) return false;
            if (o.audienceKey().equals(audienceKey)) return false;
        }
        return true;
    }

    private boolean isFacultyAvailable(List<FacultyAvailability> availabilityBlocks, DayOfWeek day,
                                        LocalTime start, LocalTime end) {
        for (FacultyAvailability block : availabilityBlocks) {
            if (block.getDayOfWeek() == day && block.getStartTime().isBefore(end) && block.getEndTime().isAfter(start)) {
                return false;
            }
        }
        return true;
    }

    /** Fresh shuffled copy so consecutive {@link #generate} calls vary the placement order —
     *  never mutates the caller's/repository's list. */
    private static <T> List<T> shuffledCopy(List<T> source) {
        List<T> copy = new ArrayList<>(source);
        Collections.shuffle(copy);
        return copy;
    }

    @Transactional
    public TimetableActionResponse clear(Long termInstanceId) {
        if (!termInstanceRepository.existsById(termInstanceId)) {
            throw new ResourceNotFoundException("Term instance not found with id: " + termInstanceId);
        }
        List<ClassSchedule> existing = classScheduleRepository.findByTermInstanceId(termInstanceId);
        classScheduleRepository.deleteByTermInstanceId(termInstanceId);
        return new TimetableActionResponse(existing.size());
    }

    @Transactional
    public TimetableActionResponse approve(Long termInstanceId) {
        List<ClassSchedule> drafts = classScheduleRepository.findByTermInstanceIdAndStatus(termInstanceId, ClassScheduleStatus.DRAFT);
        if (drafts.isEmpty()) {
            throw new ResourceNotFoundException("No draft timetable found for term instance id: " + termInstanceId);
        }
        for (ClassSchedule cs : drafts) {
            cs.setStatus(ClassScheduleStatus.PUBLISHED);
            classScheduleRepository.save(cs);
        }
        return new TimetableActionResponse(drafts.size());
    }

    /**
     * Un-publishes a live timetable back to DRAFT so it can be edited/swapped and re-approved,
     * without losing the placed sessions (unlike {@link #clear}, which deletes them outright).
     * Blocked once any {@code LabAttendance} has been recorded against the term's sessions —
     * {@code lab_attendances.lab_schedule_id} has no {@code ON DELETE}/status-transition handling,
     * so silently reverting attendance-backed sessions back to DRAFT would let a subsequent
     * regenerate/clear wipe out attendance history's session linkage.
     */
    @Transactional
    public TimetableActionResponse revertToDraft(Long termInstanceId) {
        List<ClassSchedule> published = classScheduleRepository.findByTermInstanceIdAndStatus(
            termInstanceId, ClassScheduleStatus.PUBLISHED);
        if (published.isEmpty()) {
            throw new ResourceNotFoundException("No published timetable found for term instance id: " + termInstanceId);
        }
        if (labAttendanceRepository.existsByLabScheduleTermInstanceId(termInstanceId)) {
            throw new LifecycleConflictException(
                "Attendance has already been recorded against this term's timetable. It can no longer be reverted to draft.",
                "TIMETABLE_ATTENDANCE_RECORDED", "TermInstance", termInstanceId, null);
        }
        for (ClassSchedule cs : published) {
            cs.setStatus(ClassScheduleStatus.DRAFT);
            classScheduleRepository.save(cs);
        }
        return new TimetableActionResponse(published.size());
    }
}
