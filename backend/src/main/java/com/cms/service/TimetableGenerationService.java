package com.cms.service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import com.cms.repository.FacultyRepository;
import com.cms.repository.LabRepository;
import com.cms.repository.LabSlotRepository;
import com.cms.repository.PeriodRepository;
import com.cms.repository.TermInstanceRepository;

/**
 * "Suggest-and-approve" draft generation, not fully automatic: Generate produces DRAFT rows an
 * admin reviews (and can hand-fix via the extended /lab-schedules form) before Approve flips
 * them to PUBLISHED. v1 assumes full faculty/room availability — placement only avoids
 * double-booking against rows placed earlier in the *same run* (never against pre-existing
 * PUBLISHED rows for the term, since {@link #generate} refuses to run at all when any row
 * already exists for that term — see the regeneration guard, and the plan's note that checking
 * PUBLISHED rows would be dead code here). 1 Period/LabSlot = 1 curriculum hour for v1; lab and
 * clinical hours are summed into one LAB placement count (the schema discriminator is only
 * THEORY/LAB). Elective offerings are skipped, matching the existing bulk course-registration
 * generation precedent (CourseRegistrationServiceImpl) of leaving electives for manual/individual
 * assignment.
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

    public TimetableGenerationService(ClassScheduleRepository classScheduleRepository,
                                       CourseOfferingRepository courseOfferingRepository,
                                       TermInstanceRepository termInstanceRepository,
                                       BatchRepository batchRepository,
                                       FacultyRepository facultyRepository,
                                       ClassroomRepository classroomRepository,
                                       PeriodRepository periodRepository,
                                       LabRepository labRepository,
                                       LabSlotRepository labSlotRepository) {
        this.classScheduleRepository = classScheduleRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.termInstanceRepository = termInstanceRepository;
        this.batchRepository = batchRepository;
        this.facultyRepository = facultyRepository;
        this.classroomRepository = classroomRepository;
        this.periodRepository = periodRepository;
        this.labRepository = labRepository;
        this.labSlotRepository = labSlotRepository;
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
        if (classScheduleRepository.existsByTermInstanceId(termInstanceId)) {
            throw new LifecycleConflictException(
                "A timetable already exists for this term. Clear it before regenerating.",
                "TIMETABLE_ALREADY_EXISTS", "TermInstance", termInstanceId, null);
        }
        TermInstance termInstance = termInstanceRepository.findById(termInstanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + termInstanceId));

        List<CourseOffering> offerings = courseOfferingRepository.findByTermInstanceIdAndIsActiveTrue(termInstanceId);
        List<Period> periods = periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc();
        List<LabSlot> labSlots = labSlotRepository.findByIsActiveTrueOrderBySlotOrderAsc();
        List<Classroom> classrooms = classroomRepository.findByIsActiveTrueOrderByNameAsc();
        List<Lab> labs = labRepository.findAll();

        List<Occupied> occupied = new ArrayList<>();
        List<String> unplaceable = new ArrayList<>();
        int generatedCount = 0;

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

            int theoryHours = csc.getTheoryHours() != null ? csc.getTheoryHours() : 0;
            if (theoryHours > 0) {
                int placed = placeTheory(offering, faculty, theoryHours, periods, classrooms, occupied, termInstance);
                if (placed < theoryHours) {
                    unplaceable.add(subjectName + ": placed " + placed + "/" + theoryHours
                        + " theory periods (no free day/period/classroom combination left)");
                }
                generatedCount += placed;
            }

            int labClinicalHours = (csc.getLabHours() != null ? csc.getLabHours() : 0)
                + (csc.getClinicalHours() != null ? csc.getClinicalHours() : 0);
            if (labClinicalHours > 0) {
                List<Batch> batches = batchRepository.findByCourseOfferingId(offering.getId());
                if (batches.isEmpty()) {
                    unplaceable.add(subjectName + ": " + labClinicalHours
                        + " lab/clinical hours needed but no batches are defined for this offering");
                } else {
                    for (Batch batch : batches) {
                        int placed = placeLab(offering, faculty, batch, labClinicalHours, labSlots, labs, occupied, termInstance);
                        if (placed < labClinicalHours) {
                            unplaceable.add(subjectName + " (" + batch.getName() + "): placed " + placed + "/" + labClinicalHours
                                + " lab/clinical periods (no free day/slot/lab combination left)");
                        }
                        generatedCount += placed;
                    }
                }
            }
        }

        return new TimetableGenerationResponse(generatedCount, unplaceable);
    }

    private int placeTheory(CourseOffering offering, Faculty faculty, int hoursNeeded,
                             List<Period> periods, List<Classroom> classrooms,
                             List<Occupied> occupied, TermInstance termInstance) {
        int placed = 0;
        for (int i = 0; i < hoursNeeded; i++) {
            boolean found = false;
            for (Period period : periods) {
                if (found) break;
                for (DayOfWeek day : DayOfWeek.values()) {
                    if (found) break;
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

    private int placeLab(CourseOffering offering, Faculty faculty, Batch batch, int hoursNeeded,
                          List<LabSlot> labSlots, List<Lab> labs,
                          List<Occupied> occupied, TermInstance termInstance) {
        int placed = 0;
        for (int i = 0; i < hoursNeeded; i++) {
            boolean found = false;
            for (LabSlot slot : labSlots) {
                if (found) break;
                for (DayOfWeek day : DayOfWeek.values()) {
                    if (found) break;
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
}
