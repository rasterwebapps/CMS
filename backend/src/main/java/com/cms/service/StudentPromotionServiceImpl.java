package com.cms.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AttendanceReportResponse;
import com.cms.dto.CohortTermOption;
import com.cms.dto.PromotionArrearSubject;
import com.cms.dto.PromotionDecisionInput;
import com.cms.dto.PromotionExecuteRequest;
import com.cms.dto.PromotionExecuteResponse;
import com.cms.dto.PromotionPreviewRequest;
import com.cms.dto.PromotionPreviewResponse;
import com.cms.dto.PromotionRejectedDecision;
import com.cms.dto.StudentPromotionDecisionDto;
import com.cms.dto.StudentPromotionPreviewRow;
import com.cms.dto.SubjectExamOutcome;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Cohort;
import com.cms.model.ExamResult;
import com.cms.model.Program;
import com.cms.model.Student;
import com.cms.model.StudentPromotionDecision;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.AssessmentPattern;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.ExamOutcome;
import com.cms.model.enums.ExamResultStatus;
import com.cms.model.enums.PromotionOutcome;
import com.cms.model.enums.RegistrationStatus;
import com.cms.model.enums.StudentStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.CohortRepository;
import com.cms.repository.CourseRegistrationRepository;
import com.cms.repository.ExamResultRepository;
import com.cms.repository.StudentPromotionDecisionRepository;
import com.cms.repository.StudentRepository;
import com.cms.repository.StudentTermEnrollmentRepository;
import com.cms.repository.SubjectRepository;
import com.cms.repository.TermInstanceRepository;

@Service
@Transactional(readOnly = true)
public class StudentPromotionServiceImpl implements StudentPromotionService {

    private static final String BLOCK_ARREARS_AT_FINAL_YEAR_GATE = "ARREARS_AT_FINAL_YEAR_GATE";
    private static final String BLOCK_MAX_DURATION_EXCEEDED = "MAX_DURATION_EXCEEDED";

    private final CohortRepository cohortRepository;
    private final TermInstanceRepository termInstanceRepository;
    private final AcademicYearRepository academicYearRepository;
    private final StudentTermEnrollmentRepository enrollmentRepository;
    private final CourseRegistrationRepository courseRegistrationRepository;
    private final ExamResultRepository examResultRepository;
    private final StudentPromotionDecisionRepository decisionRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final AttendanceService attendanceService;
    private final CourseRegistrationService courseRegistrationService;
    private final FeeDemandService feeDemandService;

    public StudentPromotionServiceImpl(CohortRepository cohortRepository,
                                        TermInstanceRepository termInstanceRepository,
                                        AcademicYearRepository academicYearRepository,
                                        StudentTermEnrollmentRepository enrollmentRepository,
                                        CourseRegistrationRepository courseRegistrationRepository,
                                        ExamResultRepository examResultRepository,
                                        StudentPromotionDecisionRepository decisionRepository,
                                        StudentRepository studentRepository,
                                        SubjectRepository subjectRepository,
                                        AttendanceService attendanceService,
                                        CourseRegistrationService courseRegistrationService,
                                        FeeDemandService feeDemandService) {
        this.cohortRepository = cohortRepository;
        this.termInstanceRepository = termInstanceRepository;
        this.academicYearRepository = academicYearRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseRegistrationRepository = courseRegistrationRepository;
        this.examResultRepository = examResultRepository;
        this.decisionRepository = decisionRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.attendanceService = attendanceService;
        this.courseRegistrationService = courseRegistrationService;
        this.feeDemandService = feeDemandService;
    }

    @Override
    public List<CohortTermOption> getActiveTermsForCohort(Long cohortId) {
        if (!cohortRepository.existsById(cohortId)) {
            throw new ResourceNotFoundException("Cohort not found with id: " + cohortId);
        }
        List<StudentTermEnrollment> enrollments =
            enrollmentRepository.findByCohortIdAndStatus(cohortId, EnrollmentStatus.ENROLLED);

        Map<Long, List<StudentTermEnrollment>> byTermId = enrollments.stream()
            .collect(Collectors.groupingBy(e -> e.getTermInstance().getId()));

        return byTermId.values().stream()
            .sorted(Comparator.<List<StudentTermEnrollment>>comparingLong(
                    group -> group.get(0).getTermInstance().getAcademicYear().getStartDate().toEpochDay())
                .thenComparing(group -> group.get(0).getTermInstance().getTermType())
                .reversed())
            .map(group -> {
                TermInstance ti = group.get(0).getTermInstance();
                return new CohortTermOption(ti.getId(), termLabel(ti), group.size());
            })
            .toList();
    }

    @Override
    public CohortTermOption suggestNextTerm(Long fromTermInstanceId) {
        TermInstance fromTerm = termInstanceRepository.findById(fromTermInstanceId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Term instance not found with id: " + fromTermInstanceId));

        TermInstance suggested;
        if (fromTerm.getTermType() == TermType.ODD) {
            suggested = termInstanceRepository
                .findByAcademicYearIdAndTermType(fromTerm.getAcademicYear().getId(), TermType.EVEN)
                .orElse(null);
        } else {
            List<AcademicYear> nextYears = academicYearRepository
                .findByStartDateGreaterThanOrderByStartDateAsc(fromTerm.getAcademicYear().getStartDate());
            suggested = nextYears.isEmpty() ? null : termInstanceRepository
                .findByAcademicYearIdAndTermType(nextYears.get(0).getId(), TermType.ODD)
                .orElse(null);
        }
        return suggested != null ? new CohortTermOption(suggested.getId(), termLabel(suggested), 0) : null;
    }

    @Override
    public PromotionPreviewResponse previewPromotion(PromotionPreviewRequest request) {
        Cohort cohort = cohortRepository.findById(request.cohortId())
            .orElseThrow(() -> new ResourceNotFoundException("Cohort not found with id: " + request.cohortId()));
        TermInstance fromTerm = termInstanceRepository.findById(request.fromTermInstanceId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Term instance not found with id: " + request.fromTermInstanceId()));
        TermInstance toTerm = termInstanceRepository.findById(request.toTermInstanceId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Term instance not found with id: " + request.toTermInstanceId()));
        Program program = cohort.getProgram();

        List<StudentTermEnrollment> enrollments = enrollmentRepository
            .findByTermInstanceIdAndCohortId(fromTerm.getId(), cohort.getId())
            .stream()
            .filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED)
            .toList();

        List<StudentPromotionPreviewRow> rows = enrollments.stream()
            .map(enrollment -> buildPreviewRow(enrollment, cohort, program, fromTerm, toTerm))
            .toList();

        return new PromotionPreviewResponse(
            cohort.getId(),
            cohort.getCohortCode(),
            fromTerm.getId(),
            termLabel(fromTerm),
            toTerm.getId(),
            termLabel(toTerm),
            program.getTotalTerms(),
            program.getDurationYears() * 2,
            rows
        );
    }

    private StudentPromotionPreviewRow buildPreviewRow(StudentTermEnrollment enrollment, Cohort cohort,
                                                         Program program, TermInstance fromTerm, TermInstance toTerm) {
        Student student = enrollment.getStudent();
        Integer totalTerms = program.getTotalTerms();
        int toSemesterNumber = enrollment.getSemesterNumber() + 1;
        boolean isEnteringFinalOrBeyond = totalTerms != null && toSemesterNumber >= totalTerms;

        // Subjects the student is actually registered for in the FROM term.
        List<Subject> subjectsThisTerm = courseRegistrationRepository
            .findByStudentTermEnrollmentId(enrollment.getId())
            .stream()
            .filter(r -> r.getStatus() != RegistrationStatus.DROPPED)
            .map(r -> r.getCourseOffering().getSubject())
            .distinct()
            .toList();
        Set<Long> subjectIdsThisTerm = subjectsThisTerm.stream().map(Subject::getId).collect(Collectors.toSet());

        List<AttendanceReportResponse> attendance = new ArrayList<>();
        for (Subject subject : subjectsThisTerm) {
            attendance.addAll(attendanceService.getAttendanceReport(student.getId(), subject.getId()));
        }
        Set<Long> lowAttendanceSubjectIds = attendance.stream()
            .filter(AttendanceReportResponse::lowAttendance)
            .map(AttendanceReportResponse::subjectId)
            .collect(Collectors.toSet());

        // Latest PUBLISHED exam result per subject across the student's whole history.
        Map<Long, ExamResult> latestPublishedBySubject =
            latestPublishedPerSubject(examResultRepository.findByStudentId(student.getId()));

        List<SubjectExamOutcome> subjectExamOutcomes = subjectsThisTerm.stream()
            .map(subject -> {
                ExamResult latest = latestPublishedBySubject.get(subject.getId());
                ExamOutcome outcome = latest != null ? latest.getOutcome() : null;
                return new SubjectExamOutcome(subject.getId(), subject.getName(), subject.getCode(), outcome);
            })
            .toList();

        Set<Long> newArrearIds = subjectsThisTerm.stream()
            .map(Subject::getId)
            .filter(id -> lowAttendanceSubjectIds.contains(id)
                || latestPublishedBySubject.containsKey(id)
                    && latestPublishedBySubject.get(id).getOutcome() == ExamOutcome.FAIL)
            .collect(Collectors.toSet());

        Map<Long, Subject> subjectById = new HashMap<>();
        subjectsThisTerm.forEach(s -> subjectById.put(s.getId(), s));
        latestPublishedBySubject.forEach((id, result) -> subjectById.putIfAbsent(id, result.getExamination().getSubject()));

        List<PromotionArrearSubject> carriedArrears = latestPublishedBySubject.entrySet().stream()
            .filter(e -> !subjectIdsThisTerm.contains(e.getKey()))
            .filter(e -> e.getValue().getOutcome() == ExamOutcome.FAIL)
            .map(e -> toArrearSubject(subjectById.get(e.getKey())))
            .toList();

        List<PromotionArrearSubject> newArrears = newArrearIds.stream()
            .map(id -> toArrearSubject(subjectById.get(id)))
            .toList();

        Set<Long> totalArrearIds = new HashSet<>(newArrearIds);
        carriedArrears.forEach(a -> totalArrearIds.add(a.subjectId()));
        List<PromotionArrearSubject> totalArrears = totalArrearIds.stream()
            .map(id -> toArrearSubject(subjectById.get(id)))
            .toList();

        boolean hasArrears = !totalArrears.isEmpty();

        TermInstance referenceTerm = toSemesterNumber > (totalTerms != null ? totalTerms : Integer.MAX_VALUE)
            ? fromTerm : toTerm;
        int admissionStartYear = cohort.getAdmissionAcademicYear().getStartDate().getYear();
        int referenceStartYear = referenceTerm.getAcademicYear().getStartDate().getYear();
        int maxDurationYears = program.getDurationYears() * 2;
        boolean maxDurationExceeded = (referenceStartYear - admissionStartYear) >= maxDurationYears;

        List<String> blockReasons = new ArrayList<>();
        if (maxDurationExceeded) {
            blockReasons.add(BLOCK_MAX_DURATION_EXCEEDED);
        }
        if (isEnteringFinalOrBeyond && hasArrears) {
            blockReasons.add(BLOCK_ARREARS_AT_FINAL_YEAR_GATE);
        }

        PromotionOutcome recommendedOutcome;
        if (!blockReasons.isEmpty()) {
            recommendedOutcome = null;
        } else if (totalTerms != null && toSemesterNumber > totalTerms) {
            recommendedOutcome = PromotionOutcome.GRADUATED;
        } else if (hasArrears) {
            recommendedOutcome = PromotionOutcome.PROMOTED_WITH_ARREARS;
        } else {
            recommendedOutcome = PromotionOutcome.PROMOTED;
        }

        return new StudentPromotionPreviewRow(
            student.getId(),
            student.getFullName(),
            student.getRollNumber(),
            enrollment.getId(),
            attendance,
            subjectExamOutcomes,
            carriedArrears,
            newArrears,
            totalArrears,
            recommendedOutcome,
            blockReasons
        );
    }

    @Override
    @Transactional
    public PromotionExecuteResponse executePromotion(PromotionExecuteRequest request, String decidedBy) {
        Cohort cohort = cohortRepository.findById(request.cohortId())
            .orElseThrow(() -> new ResourceNotFoundException("Cohort not found with id: " + request.cohortId()));
        TermInstance fromTerm = termInstanceRepository.findById(request.fromTermInstanceId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Term instance not found with id: " + request.fromTermInstanceId()));
        TermInstance toTerm = termInstanceRepository.findById(request.toTermInstanceId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Term instance not found with id: " + request.toTermInstanceId()));
        Program program = cohort.getProgram();

        // Re-validate every decision against a freshly recomputed preview — never trust
        // client-supplied eligibility.
        PromotionPreviewResponse preview = previewPromotion(
            new PromotionPreviewRequest(cohort.getId(), fromTerm.getId(), toTerm.getId()));
        Map<Long, StudentPromotionPreviewRow> previewByStudent = preview.students().stream()
            .collect(Collectors.toMap(StudentPromotionPreviewRow::studentId, r -> r));

        int promoted = 0;
        int promotedWithArrears = 0;
        int detained = 0;
        int graduated = 0;
        int excluded = 0;
        List<PromotionRejectedDecision> rejected = new ArrayList<>();
        Instant now = Instant.now();

        for (PromotionDecisionInput decision : request.decisions()) {
            StudentPromotionPreviewRow row = previewByStudent.get(decision.studentId());
            if (row == null) {
                rejected.add(new PromotionRejectedDecision(decision.studentId(),
                    "Student not enrolled in this cohort/term"));
                continue;
            }

            PromotionOutcome outcome = decision.outcome();
            boolean isBlocked = !row.blockReasons().isEmpty();
            boolean isAdvancingOutcome = outcome == PromotionOutcome.PROMOTED
                || outcome == PromotionOutcome.PROMOTED_WITH_ARREARS
                || outcome == PromotionOutcome.GRADUATED;
            if (isBlocked && isAdvancingOutcome) {
                rejected.add(new PromotionRejectedDecision(decision.studentId(),
                    "Blocked: " + String.join(", ", row.blockReasons())));
                continue;
            }

            StudentTermEnrollment enrollment = enrollmentRepository.findById(row.enrollmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Enrollment not found with id: " + row.enrollmentId()));
            Student student = enrollment.getStudent();

            if (outcome == PromotionOutcome.EXCLUDED) {
                excluded++;
                continue;
            }

            TermInstance decisionToTerm = null;
            if (outcome == PromotionOutcome.DETAINED_REPEAT) {
                // Enrollment stays ENROLLED at the current term — the student reappears in the
                // next promotion cycle's preview unchanged.
                detained++;
            } else if (outcome == PromotionOutcome.GRADUATED) {
                enrollment.setStatus(EnrollmentStatus.COMPLETED);
                enrollmentRepository.save(enrollment);
                student.setStatus(StudentStatus.GRADUATED);
                studentRepository.save(student);
                graduated++;
            } else {
                enrollment.setStatus(EnrollmentStatus.COMPLETED);
                enrollmentRepository.save(enrollment);

                int nextSemesterNumber = enrollment.getSemesterNumber() + 1;
                StudentTermEnrollment nextEnrollment = enrollmentRepository
                    .findByStudentIdAndTermInstanceId(student.getId(), toTerm.getId())
                    .orElseGet(() -> {
                        StudentTermEnrollment created = new StudentTermEnrollment();
                        created.setStudent(student);
                        created.setTermInstance(toTerm);
                        created.setCohort(cohort);
                        created.setSemesterNumber(nextSemesterNumber);
                        created.setYearOfStudy(computeYearOfStudy(nextSemesterNumber, program));
                        created.setStatus(EnrollmentStatus.ENROLLED);
                        return enrollmentRepository.save(created);
                    });

                student.setSemester(nextEnrollment.getYearOfStudy());
                studentRepository.save(student);
                decisionToTerm = toTerm;

                if (outcome == PromotionOutcome.PROMOTED) {
                    promoted++;
                } else {
                    promotedWithArrears++;
                }
            }

            StudentPromotionDecision decisionRecord = new StudentPromotionDecision();
            decisionRecord.setStudent(student);
            decisionRecord.setCohort(cohort);
            decisionRecord.setFromTermInstance(fromTerm);
            decisionRecord.setToTermInstance(decisionToTerm);
            decisionRecord.setOutcome(outcome);
            decisionRecord.setArrearSubjectIds(row.totalArrearSubjects().stream()
                .map(PromotionArrearSubject::subjectId)
                .collect(Collectors.toSet()));
            decisionRecord.setDecidedBy(decidedBy);
            decisionRecord.setDecidedAt(now);
            decisionRecord.setRemarks(decision.remarks());
            decisionRepository.save(decisionRecord);
        }

        Integer registrationsGenerated = null;
        if (request.generateCourseRegistrations() && (promoted + promotedWithArrears) > 0) {
            registrationsGenerated = courseRegistrationService.generateRegistrationsForTermInstance(toTerm.getId());
        }

        Integer feeDemandsGenerated = null;
        if (request.generateFeeDemands() && (promoted + promotedWithArrears) > 0) {
            feeDemandsGenerated = feeDemandService.generateDemandsForTermInstance(toTerm.getId()).demandsCreated();
        }

        return new PromotionExecuteResponse(
            promoted, promotedWithArrears, detained, graduated, excluded, rejected,
            registrationsGenerated, feeDemandsGenerated);
    }

    @Override
    public List<StudentPromotionDecisionDto> getHistoryByCohort(Long cohortId) {
        if (!cohortRepository.existsById(cohortId)) {
            throw new ResourceNotFoundException("Cohort not found with id: " + cohortId);
        }
        return decisionRepository.findByCohortId(cohortId).stream().map(this::toDto).toList();
    }

    @Override
    public List<StudentPromotionDecisionDto> getHistoryByStudent(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found with id: " + studentId);
        }
        return decisionRepository.findByStudentId(studentId).stream().map(this::toDto).toList();
    }

    private int computeYearOfStudy(int semesterNumber, Program program) {
        AssessmentPattern pattern = program.getAssessmentPattern();
        return pattern == AssessmentPattern.YEARLY ? semesterNumber : (int) Math.ceil(semesterNumber / 2.0);
    }

    private Map<Long, ExamResult> latestPublishedPerSubject(List<ExamResult> results) {
        Map<Long, ExamResult> latest = new HashMap<>();
        for (ExamResult result : results) {
            if (result.getStatus() != ExamResultStatus.PUBLISHED || result.getOutcome() == null) {
                continue;
            }
            Long subjectId = result.getExamination().getSubject().getId();
            ExamResult current = latest.get(subjectId);
            if (current == null || isLater(result, current)) {
                latest.put(subjectId, result);
            }
        }
        return latest;
    }

    private boolean isLater(ExamResult candidate, ExamResult current) {
        LocalDate candidateDate = candidate.getExamination().getDate();
        LocalDate currentDate = current.getExamination().getDate();
        if (candidateDate == null || currentDate == null) {
            return candidate.getId() > current.getId();
        }
        return candidateDate.isAfter(currentDate);
    }

    private PromotionArrearSubject toArrearSubject(Subject subject) {
        return new PromotionArrearSubject(subject.getId(), subject.getName(), subject.getCode());
    }

    private String termLabel(TermInstance term) {
        return term.getAcademicYear().getName() + " " + term.getTermType();
    }

    private StudentPromotionDecisionDto toDto(StudentPromotionDecision d) {
        List<PromotionArrearSubject> arrearSubjects = subjectRepository.findAllById(d.getArrearSubjectIds())
            .stream()
            .map(this::toArrearSubject)
            .toList();
        return new StudentPromotionDecisionDto(
            d.getId(),
            d.getStudent().getId(),
            d.getStudent().getFullName(),
            d.getStudent().getRollNumber(),
            d.getCohort().getId(),
            d.getCohort().getCohortCode(),
            d.getFromTermInstance().getId(),
            termLabel(d.getFromTermInstance()),
            d.getToTermInstance() != null ? d.getToTermInstance().getId() : null,
            d.getToTermInstance() != null ? termLabel(d.getToTermInstance()) : null,
            d.getOutcome(),
            arrearSubjects,
            d.getDecidedBy(),
            d.getDecidedAt(),
            d.getRemarks()
        );
    }
}
