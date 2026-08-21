package com.cms.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.CourseRegistrationDto;
import com.cms.dto.ElectiveBulkAssignmentResponse;
import com.cms.dto.ElectiveGroupSummaryResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.CourseOffering;
import com.cms.model.CourseRegistration;
import com.cms.model.CurriculumElectiveGroup;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.RegistrationStatus;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.repository.AttendanceRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.CourseRegistrationRepository;
import com.cms.repository.StudentTermEnrollmentRepository;
import com.cms.repository.TermInstanceRepository;

@Service
@Transactional(readOnly = true)
public class CourseRegistrationServiceImpl implements CourseRegistrationService {

    private final CourseRegistrationRepository courseRegistrationRepository;
    private final StudentTermEnrollmentRepository enrollmentRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final TermInstanceRepository termInstanceRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final AttendanceRepository attendanceRepository;

    public CourseRegistrationServiceImpl(CourseRegistrationRepository courseRegistrationRepository,
                                          StudentTermEnrollmentRepository enrollmentRepository,
                                          CourseOfferingRepository courseOfferingRepository,
                                          TermInstanceRepository termInstanceRepository,
                                          ClassScheduleRepository classScheduleRepository,
                                          AttendanceRepository attendanceRepository) {
        this.courseRegistrationRepository = courseRegistrationRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.termInstanceRepository = termInstanceRepository;
        this.classScheduleRepository = classScheduleRepository;
        this.attendanceRepository = attendanceRepository;
    }

    @Override
    @Transactional
    public int generateRegistrationsForTermInstance(Long termInstanceId) {
        if (!termInstanceRepository.existsById(termInstanceId)) {
            throw new ResourceNotFoundException("Term instance not found with id: " + termInstanceId);
        }

        // All enrolled StudentTermEnrollments for this term
        List<StudentTermEnrollment> enrollments = enrollmentRepository.findByTermInstanceId(termInstanceId)
            .stream()
            .filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED)
            .toList();

        int count = 0;
        for (StudentTermEnrollment enrollment : enrollments) {
            // Find active course offerings for this term, matching the enrollment's semester number
            // and belonging to the cohort's own program AND course. The program check alone isn't
            // enough — e.g. MSc Nursing (Adult) and (Child) share one Program, so without the course
            // check students would get cross-registered into the other specialty's subjects. Every
            // CurriculumVersion is now itself mandatorily scoped to one course, so its course is
            // always the authoritative match — no more per-row course-restriction fallback needed.
            List<CourseOffering> offerings = courseOfferingRepository
                .findByTermInstanceIdAndSemesterNumber(termInstanceId, enrollment.getSemesterNumber())
                .stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsActive()))
                .filter(o -> o.getCurriculumVersion().getProgram().getId()
                    .equals(enrollment.getCohort().getProgram().getId()))
                .filter(o -> o.getCurriculumVersion().getCourse().getId()
                    .equals(enrollment.getCohort().getCourse().getId()))
                // Choice-based electives are not bulk-registered — a student is enrolled into
                // exactly one option via assignElectiveChoice(), picked by an admin. An offering
                // with no resolved curriculum mapping (legacy/unresolved) is treated as non-elective,
                // matching pre-existing behaviour.
                .filter(o -> {
                    CurriculumSemesterCourse csc = o.getCurriculumSemesterCourse();
                    return csc == null || !Boolean.TRUE.equals(csc.getIsElective());
                })
                .toList();

            for (CourseOffering offering : offerings) {
                Optional<CourseRegistration> existing =
                    courseRegistrationRepository.findByStudentTermEnrollmentIdAndCourseOfferingId(
                        enrollment.getId(), offering.getId());
                if (existing.isEmpty()) {
                    CourseRegistration registration = new CourseRegistration();
                    registration.setStudentTermEnrollment(enrollment);
                    registration.setCourseOffering(offering);
                    registration.setStatus(RegistrationStatus.REGISTERED);
                    courseRegistrationRepository.save(registration);
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    @Transactional
    public CourseRegistrationDto assignElectiveChoice(Long enrollmentId, Long courseOfferingId) {
        StudentTermEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Student term enrollment not found with id: " + enrollmentId));
        CourseOffering offering = courseOfferingRepository.findById(courseOfferingId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Course offering not found with id: " + courseOfferingId));

        CurriculumSemesterCourse csc = offering.getCurriculumSemesterCourse();
        if (csc == null || !Boolean.TRUE.equals(csc.getIsElective()) || csc.getElectiveGroup() == null) {
            throw new IllegalArgumentException(
                "Course offering " + courseOfferingId + " is not a choice-based elective");
        }
        Long groupId = csc.getElectiveGroup().getId();
        TermInstance term = enrollment.getTermInstance();
        requireTermNotLocked(term);

        List<CourseRegistration> existingForEnrollment =
            courseRegistrationRepository.findByStudentTermEnrollmentId(enrollmentId);

        CourseRegistration current = null;
        for (CourseRegistration existing : existingForEnrollment) {
            if (existing.getStatus() == RegistrationStatus.DROPPED) {
                continue;
            }
            CurriculumSemesterCourse otherCsc = existing.getCourseOffering().getCurriculumSemesterCourse();
            if (otherCsc != null && otherCsc.getElectiveGroup() != null
                    && otherCsc.getElectiveGroup().getId().equals(groupId)) {
                current = existing;
                break;
            }
        }

        if (current != null) {
            if (current.getCourseOffering().getId().equals(courseOfferingId)) {
                return toDto(current);
            }
            // Changing an existing choice -- not a fresh pick -- so it's subject to the same
            // integrity guard as bulkAssignElectiveChoice: switching a student off an elective
            // that's already on the timetable or already has attendance recorded against it would
            // silently strand those sessions/records.
            requireSafeToChangeElectiveChoice(term.getId(), groupId, enrollment.getStudent().getId(),
                current.getCourseOffering().getSubject());
            current.setStatus(RegistrationStatus.DROPPED);
            // saveAndFlush for the same reason bulkAssignElectiveChoice does: the new REGISTERED
            // insert below must not race the drop past V385's one-active-registration-per-group
            // unique index.
            courseRegistrationRepository.saveAndFlush(current);
        }

        return toDto(createOrReactivateRegistration(enrollment, offering));
    }

    @Override
    @Transactional
    public ElectiveBulkAssignmentResponse bulkAssignElectiveChoice(
            Long termInstanceId, Long electiveGroupId, Long courseOfferingId) {
        TermInstance term = termInstanceRepository.findById(termInstanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found with id: " + termInstanceId));
        requireTermNotLocked(term);

        CourseOffering offering = courseOfferingRepository.findById(courseOfferingId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Course offering not found with id: " + courseOfferingId));

        CurriculumSemesterCourse csc = offering.getCurriculumSemesterCourse();
        if (csc == null || !Boolean.TRUE.equals(csc.getIsElective()) || csc.getElectiveGroup() == null
                || !csc.getElectiveGroup().getId().equals(electiveGroupId)) {
            throw new IllegalArgumentException(
                "Course offering " + courseOfferingId + " does not belong to elective group " + electiveGroupId);
        }
        CurriculumElectiveGroup group = csc.getElectiveGroup();
        Long courseId = group.getCurriculumVersion().getCourse().getId();

        List<StudentTermEnrollment> eligibleEnrollments = enrollmentRepository
            .findByTermInstanceIdAndSemesterNumberAndCohortCourseIdAndStatus(
                termInstanceId, group.getTermNumber(), courseId, EnrollmentStatus.ENROLLED);

        // Computed once, not per student: whether the group is scheduled is uniform across every
        // student in it, so this would otherwise be the same repeated query N times over.
        boolean groupScheduled = isElectiveGroupScheduled(termInstanceId, electiveGroupId);

        int assigned = 0;
        int blocked = 0;
        for (StudentTermEnrollment enrollment : eligibleEnrollments) {
            boolean alreadyOnThisOffering = false;
            CourseRegistration toChange = null;
            for (CourseRegistration existing : courseRegistrationRepository.findByStudentTermEnrollmentId(enrollment.getId())) {
                if (existing.getStatus() == RegistrationStatus.DROPPED) {
                    continue;
                }
                CurriculumSemesterCourse otherCsc = existing.getCourseOffering().getCurriculumSemesterCourse();
                if (otherCsc == null || otherCsc.getElectiveGroup() == null
                        || !otherCsc.getElectiveGroup().getId().equals(electiveGroupId)) {
                    continue;
                }
                if (existing.getCourseOffering().getId().equals(courseOfferingId)) {
                    alreadyOnThisOffering = true;
                } else {
                    toChange = existing;
                }
            }
            if (alreadyOnThisOffering) {
                continue;
            }
            if (toChange != null) {
                // Reassigning this student off their current elective -- same integrity guard as
                // a single-student change: skip (don't fail the whole bulk run) if it's unsafe.
                boolean unsafe = groupScheduled
                    || attendanceRepository.existsByStudentIdAndSubjectId(
                        enrollment.getStudent().getId(), toChange.getCourseOffering().getSubject().getId());
                if (unsafe) {
                    blocked++;
                    continue;
                }
                toChange.setStatus(RegistrationStatus.DROPPED);
                // saveAndFlush, not save: Hibernate's flush-action ordering always runs queued
                // inserts before queued updates regardless of code order, so a deferred save()
                // here would let this student's new REGISTERED insert (below) hit the DB before
                // this drop does -- transiently violating V385's one-active-registration-per-
                // elective-group unique index even though this method never intends both rows
                // to be active at once.
                courseRegistrationRepository.saveAndFlush(toChange);
            }
            createOrReactivateRegistration(enrollment, offering);
            assigned++;
        }
        return new ElectiveBulkAssignmentResponse(eligibleEnrollments.size(), assigned, blocked);
    }

    @Override
    public List<CourseRegistrationDto> getRegistrationsByEnrollment(Long enrollmentId) {
        return courseRegistrationRepository.findByStudentTermEnrollmentId(enrollmentId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Override
    public List<CourseRegistrationDto> getRegistrationsByCourseOffering(Long courseOfferingId) {
        return courseRegistrationRepository.findByCourseOfferingId(courseOfferingId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Override
    public CourseRegistrationDto getById(Long id) {
        CourseRegistration registration = courseRegistrationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Course registration not found with id: " + id));
        return toDto(registration);
    }

    @Override
    @Transactional
    public CourseRegistrationDto dropRegistration(Long id) {
        CourseRegistration registration = courseRegistrationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Course registration not found with id: " + id));
        registration.setStatus(RegistrationStatus.DROPPED);
        return toDto(courseRegistrationRepository.save(registration));
    }

    @Override
    public List<ElectiveGroupSummaryResponse> getElectiveGroupSummaries(Long termInstanceId) {
        if (!termInstanceRepository.existsById(termInstanceId)) {
            throw new ResourceNotFoundException("Term instance not found with id: " + termInstanceId);
        }
        List<CourseOffering> electiveOfferings = courseOfferingRepository.findByTermInstanceId(termInstanceId)
            .stream()
            .filter(o -> {
                CurriculumSemesterCourse csc = o.getCurriculumSemesterCourse();
                return csc != null && Boolean.TRUE.equals(csc.getIsElective()) && csc.getElectiveGroup() != null;
            })
            .toList();

        Map<Long, List<CourseOffering>> offeringsByGroup = electiveOfferings.stream()
            .collect(Collectors.groupingBy(o -> o.getCurriculumSemesterCourse().getElectiveGroup().getId()));

        List<ElectiveGroupSummaryResponse> summaries = new ArrayList<>();
        for (List<CourseOffering> offerings : offeringsByGroup.values()) {
            CurriculumElectiveGroup group = offerings.get(0).getCurriculumSemesterCourse().getElectiveGroup();
            List<Long> offeringIds = offerings.stream().map(CourseOffering::getId).toList();
            Long courseId = group.getCurriculumVersion().getCourse().getId();

            int eligibleCount = enrollmentRepository.findByTermInstanceIdAndSemesterNumberAndCohortCourseIdAndStatus(
                termInstanceId, group.getTermNumber(), courseId, EnrollmentStatus.ENROLLED).size();
            int assignedCount = (int) courseRegistrationRepository
                .countByCourseOfferingIdInAndStatus(offeringIds, RegistrationStatus.REGISTERED);
            boolean scheduled = !classScheduleRepository
                .findByTermInstanceIdAndCourseOfferingIdIn(termInstanceId, offeringIds).isEmpty();

            summaries.add(new ElectiveGroupSummaryResponse(
                group.getId(), group.getGroupName(), group.getSelectionMode(), group.getTermNumber(),
                eligibleCount, assignedCount, scheduled));
        }
        return summaries;
    }

    /** V71's original UNIQUE(student_term_enrollment_id, course_offering_id) constraint is not
     *  scoped to active rows -- it still blocks a fresh INSERT for a pair that already has a
     *  DROPPED row (e.g. a student moved X -> Y -> back to X). Reactivating the existing row
     *  instead of always inserting a new one is what makes "change back to a prior choice" work
     *  at all, for both the single-student and bulk paths. */
    private CourseRegistration createOrReactivateRegistration(StudentTermEnrollment enrollment, CourseOffering offering) {
        CourseRegistration registration = courseRegistrationRepository
            .findByStudentTermEnrollmentIdAndCourseOfferingId(enrollment.getId(), offering.getId())
            .orElseGet(CourseRegistration::new);
        registration.setStudentTermEnrollment(enrollment);
        registration.setCourseOffering(offering);
        registration.setStatus(RegistrationStatus.REGISTERED);
        return courseRegistrationRepository.save(registration);
    }

    private boolean isElectiveGroupScheduled(Long termInstanceId, Long electiveGroupId) {
        List<Long> siblingIds = courseOfferingRepository
            .findByTermInstanceIdAndCurriculumSemesterCourse_ElectiveGroupId(termInstanceId, electiveGroupId)
            .stream().map(CourseOffering::getId).toList();
        return !siblingIds.isEmpty()
            && !classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(termInstanceId, siblingIds).isEmpty();
    }

    /** Blanket rule shared with every other lifecycle guard in the app: a LOCKED term is frozen,
     *  full stop -- applies even to a student's first-ever elective pick, not just changes. */
    private void requireTermNotLocked(TermInstance term) {
        if (term.getStatus() == TermInstanceStatus.LOCKED) {
            throw new IllegalArgumentException(
                "Cannot modify elective choices -- this term is locked.");
        }
    }

    /** Guards CHANGING an already-assigned elective choice specifically -- a fresh (first-time)
     *  pick is never subject to this, only swapping a student off a choice they already have,
     *  since only a swap risks stranding something that already happened. */
    private void requireSafeToChangeElectiveChoice(Long termInstanceId, Long electiveGroupId,
                                                     Long studentId, Subject oldSubject) {
        if (isElectiveGroupScheduled(termInstanceId, electiveGroupId)) {
            throw new IllegalArgumentException(
                "Cannot change elective choice -- this elective group already has sessions placed in Skeleton Builder.");
        }
        if (attendanceRepository.existsByStudentIdAndSubjectId(studentId, oldSubject.getId())) {
            throw new IllegalArgumentException(
                "Cannot change elective choice -- attendance has already been recorded for \""
                    + oldSubject.getName() + "\".");
        }
    }

    private CourseRegistrationDto toDto(CourseRegistration r) {
        StudentTermEnrollment enrollment = r.getStudentTermEnrollment();
        CourseOffering offering = r.getCourseOffering();
        return new CourseRegistrationDto(
            r.getId(),
            enrollment.getId(),
            enrollment.getStudent().getId(),
            enrollment.getStudent().getFullName(),
            enrollment.getCohort().getCohortCode(),
            offering.getId(),
            offering.getSubject().getName(),
            offering.getSubject().getCode(),
            offering.getSemesterNumber(),
            r.getStatus(),
            r.getCreatedAt(),
            r.getUpdatedAt()
        );
    }
}
