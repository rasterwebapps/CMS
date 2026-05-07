package com.cms.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.SemesterResultDto;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.SemesterResult;
import com.cms.model.StudentMark;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.TermInstance;
import com.cms.model.enums.AssessmentPattern;
import com.cms.model.enums.ExamSessionStatus;
import com.cms.model.enums.ResultStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.ExamSessionRepository;
import com.cms.repository.SemesterResultRepository;
import com.cms.repository.StudentMarkRepository;
import com.cms.repository.StudentTermEnrollmentRepository;
import com.cms.repository.TermInstanceRepository;

@Service
@Transactional(readOnly = true)
public class SemesterResultServiceImpl implements SemesterResultService {

    private static final BigDecimal PASS_THRESHOLD = new BigDecimal("40");

    private final SemesterResultRepository semesterResultRepository;
    private final StudentTermEnrollmentRepository studentTermEnrollmentRepository;
    private final StudentMarkRepository studentMarkRepository;
    private final ExamSessionRepository examSessionRepository;
    private final TermInstanceRepository termInstanceRepository;

    public SemesterResultServiceImpl(SemesterResultRepository semesterResultRepository,
                                      StudentTermEnrollmentRepository studentTermEnrollmentRepository,
                                      StudentMarkRepository studentMarkRepository,
                                      ExamSessionRepository examSessionRepository,
                                      TermInstanceRepository termInstanceRepository) {
        this.semesterResultRepository = semesterResultRepository;
        this.studentTermEnrollmentRepository = studentTermEnrollmentRepository;
        this.studentMarkRepository = studentMarkRepository;
        this.examSessionRepository = examSessionRepository;
        this.termInstanceRepository = termInstanceRepository;
    }

    @Override
    @Transactional
    public SemesterResultDto computeForEnrollment(Long enrollmentId) {
        StudentTermEnrollment enrollment = studentTermEnrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new ResourceNotFoundException("StudentTermEnrollment not found: " + enrollmentId));

        AssessmentPattern pattern = enrollment.getCohort().getProgram().getAssessmentPattern();
        TermType termType = enrollment.getTermInstance().getTermType();

        if (pattern == AssessmentPattern.YEARLY && termType == TermType.ODD) {
            throw new IllegalStateException(
                "Annual results for yearly programs are computed at the end of the EVEN term, not ODD");
        }

        List<StudentMark> marks = (pattern == AssessmentPattern.YEARLY)
            ? collectYearlyMarks(enrollment)
            : studentMarkRepository.findByCourseRegistration_StudentTermEnrollment_Id(enrollmentId);

        BigDecimal totalMax = marks.stream()
            .map(m -> m.getExamEvent().getMaxMarks())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalObtained = marks.stream()
            .map(m -> m.getMarksObtained() != null ? m.getMarksObtained() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal percentage = totalMax.compareTo(BigDecimal.ZERO) == 0
            ? BigDecimal.ZERO
            : totalObtained.multiply(new BigDecimal("100"))
                .divide(totalMax, 2, RoundingMode.HALF_UP);

        ResultStatus status = percentage.compareTo(PASS_THRESHOLD) >= 0
            ? ResultStatus.PASS : ResultStatus.FAIL;

        SemesterResult result = semesterResultRepository
            .findByStudentTermEnrollment_Id(enrollmentId)
            .orElse(new SemesterResult());

        if (Boolean.TRUE.equals(result.getIsLocked())) {
            throw new IllegalStateException("Result is already locked and cannot be recomputed");
        }

        result.setStudentTermEnrollment(enrollment);
        result.setTotalMaxMarks(totalMax);
        result.setTotalMarksObtained(totalObtained);
        result.setPercentage(percentage);
        result.setResultStatus(status);

        return toDto(semesterResultRepository.save(result));
    }

    @Override
    @Transactional
    public void computeResultsForTermInstance(Long termInstanceId) {
        TermInstance termInstance = termInstanceRepository.findById(termInstanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found: " + termInstanceId));

        boolean allLocked = examSessionRepository.findByTermInstance_Id(termInstanceId).stream()
            .allMatch(s -> s.getStatus() == ExamSessionStatus.LOCKED);
        if (!allLocked) {
            throw new IllegalStateException("All exam sessions must be LOCKED before computing results");
        }

        List<StudentTermEnrollment> enrollments =
            studentTermEnrollmentRepository.findByTermInstanceId(termInstanceId);

        for (StudentTermEnrollment enrollment : enrollments) {
            AssessmentPattern pattern = enrollment.getCohort().getProgram().getAssessmentPattern();

            // Yearly programs: annual result is stored on the EVEN term enrollment only
            if (pattern == AssessmentPattern.YEARLY && termInstance.getTermType() == TermType.ODD) {
                continue;
            }

            semesterResultRepository.findByStudentTermEnrollment_Id(enrollment.getId())
                .filter(r -> Boolean.TRUE.equals(r.getIsLocked()))
                .ifPresentOrElse(
                    r -> { /* skip already-locked results */ },
                    () -> computeForEnrollment(enrollment.getId())
                );
        }
    }

    @Override
    public SemesterResultDto getByEnrollment(Long enrollmentId) {
        return toDto(semesterResultRepository.findByStudentTermEnrollment_Id(enrollmentId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "SemesterResult not found for enrollment: " + enrollmentId)));
    }

    @Override
    public List<SemesterResultDto> getByTermInstance(Long termInstanceId) {
        return semesterResultRepository.findByStudentTermEnrollment_TermInstance_Id(termInstanceId)
            .stream().map(this::toDto).toList();
    }

    @Override
    public List<SemesterResultDto> getByStudent(Long studentId) {
        return semesterResultRepository.findByStudentTermEnrollment_Student_Id(studentId)
            .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public SemesterResultDto lockResult(Long id) {
        SemesterResult result = semesterResultRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("SemesterResult not found: " + id));
        result.setIsLocked(true);
        result.setResultStatus(ResultStatus.PASS.equals(result.getResultStatus())
            ? ResultStatus.PASS : ResultStatus.FAIL);
        return toDto(semesterResultRepository.save(result));
    }

    /**
     * For yearly programs the annual result aggregates marks from both ODD (teaching half) and
     * EVEN (assessment half) enrollments of the same academic year and year-of-study position.
     * The result is stored on the EVEN enrollment. If the ODD enrollment or its marks are absent
     * (e.g., no assessments were held mid-year), only EVEN marks are used.
     */
    private List<StudentMark> collectYearlyMarks(StudentTermEnrollment evenEnrollment) {
        Long academicYearId = evenEnrollment.getTermInstance().getAcademicYear().getId();
        Long studentId = evenEnrollment.getStudent().getId();

        List<StudentMark> evenMarks = studentMarkRepository
            .findByCourseRegistration_StudentTermEnrollment_Id(evenEnrollment.getId());

        Optional<TermInstance> oddTermOpt = termInstanceRepository
            .findByAcademicYearIdAndTermType(academicYearId, TermType.ODD);

        if (oddTermOpt.isEmpty()) {
            return evenMarks;
        }

        Optional<StudentTermEnrollment> oddEnrollmentOpt = studentTermEnrollmentRepository
            .findByStudentIdAndTermInstanceId(studentId, oddTermOpt.get().getId());

        if (oddEnrollmentOpt.isEmpty()) {
            return evenMarks;
        }

        List<StudentMark> oddMarks = studentMarkRepository
            .findByCourseRegistration_StudentTermEnrollment_Id(oddEnrollmentOpt.get().getId());

        return Stream.concat(oddMarks.stream(), evenMarks.stream()).toList();
    }

    private SemesterResultDto toDto(SemesterResult r) {
        var enrollment = r.getStudentTermEnrollment();
        var student = enrollment.getStudent();
        var termInstance = enrollment.getTermInstance();
        String label = termInstance.getAcademicYear().getName() + " " + termInstance.getTermType();
        return new SemesterResultDto(r.getId(), enrollment.getId(), student.getId(),
            student.getFullName(), termInstance.getId(), label, r.getTotalMaxMarks(),
            r.getTotalMarksObtained(), r.getPercentage(), r.getResultStatus(), r.getIsLocked());
    }
}
