package com.cms.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.CourseStatsDto;
import com.cms.dto.SemesterSummaryDto;
import com.cms.dto.StudentResultSheetDto;
import com.cms.dto.StudentResultSheetDto.SubjectMarkRow;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.SemesterResult;
import com.cms.model.StudentMark;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.enums.ResultStatus;
import com.cms.repository.ExamEventRepository;
import com.cms.repository.SemesterResultRepository;
import com.cms.repository.StudentMarkRepository;
import com.cms.repository.StudentTermEnrollmentRepository;

@Service
@Transactional(readOnly = true)
public class ResultReportServiceImpl implements ResultReportService {

    private final SemesterResultRepository semesterResultRepository;
    private final StudentMarkRepository studentMarkRepository;
    private final StudentTermEnrollmentRepository studentTermEnrollmentRepository;
    private final ExamEventRepository examEventRepository;

    public ResultReportServiceImpl(SemesterResultRepository semesterResultRepository,
                                    StudentMarkRepository studentMarkRepository,
                                    StudentTermEnrollmentRepository studentTermEnrollmentRepository,
                                    ExamEventRepository examEventRepository) {
        this.semesterResultRepository = semesterResultRepository;
        this.studentMarkRepository = studentMarkRepository;
        this.studentTermEnrollmentRepository = studentTermEnrollmentRepository;
        this.examEventRepository = examEventRepository;
    }

    @Override
    public StudentResultSheetDto getResultSheet(Long enrollmentId) {
        StudentTermEnrollment enrollment = studentTermEnrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new ResourceNotFoundException("StudentTermEnrollment not found: " + enrollmentId));

        SemesterResult result = semesterResultRepository.findByStudentTermEnrollment_Id(enrollmentId)
            .orElseThrow(() -> new ResourceNotFoundException("SemesterResult not found for enrollment: " + enrollmentId));

        List<StudentMark> marks = studentMarkRepository
            .findByCourseRegistration_StudentTermEnrollment_Id(enrollmentId);

        List<SubjectMarkRow> rows = marks.stream().map(m -> new SubjectMarkRow(
            m.getExamEvent().getCourseOffering().getSubject().getName(),
            m.getExamEvent().getCourseOffering().getSubject().getCode(),
            m.getExamEvent().getExamSession().getSessionType().name(),
            m.getExamEvent().getMaxMarks(),
            m.getMarksObtained(),
            m.getMarkStatus().name()
        )).toList();

        var student = enrollment.getStudent();
        var termInstance = enrollment.getTermInstance();
        String label = termInstance.getAcademicYear().getName() + " " + termInstance.getTermType();

        return new StudentResultSheetDto(student.getId(), student.getFullName(),
            student.getRollNumber(), termInstance.getId(), label, rows,
            result.getTotalMaxMarks(), result.getTotalMarksObtained(),
            result.getPercentage(), result.getResultStatus());
    }

    @Override
    public List<SemesterSummaryDto> getSummaryByTermInstance(Long termInstanceId) {
        List<SemesterResult> results = semesterResultRepository
            .findByStudentTermEnrollment_TermInstance_Id(termInstanceId);

        Map<Long, List<SemesterResult>> byCohort = results.stream()
            .collect(Collectors.groupingBy(r -> r.getStudentTermEnrollment().getCohort().getId()));

        List<SemesterSummaryDto> summaries = new ArrayList<>();
        for (var entry : byCohort.entrySet()) {
            Long cohortId = entry.getKey();
            List<SemesterResult> cohortResults = entry.getValue();
            var sampleEnrollment = cohortResults.get(0).getStudentTermEnrollment();
            var termInstance = sampleEnrollment.getTermInstance();
            String label = termInstance.getAcademicYear().getName() + " " + termInstance.getTermType();

            long passCount = cohortResults.stream()
                .filter(r -> r.getResultStatus() == ResultStatus.PASS).count();
            long failCount = cohortResults.stream()
                .filter(r -> r.getResultStatus() == ResultStatus.FAIL).count();

            BigDecimal avgPct = cohortResults.isEmpty() ? BigDecimal.ZERO
                : cohortResults.stream().map(SemesterResult::getPercentage)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(cohortResults.size()), 2, RoundingMode.HALF_UP);

            summaries.add(new SemesterSummaryDto(
                termInstance.getId(), label, cohortId,
                sampleEnrollment.getCohort().getCohortCode(),
                cohortResults.size(), (int) passCount, (int) failCount, avgPct));
        }
        return summaries;
    }

    @Override
    public List<CourseStatsDto> getCourseStatsByTermInstance(Long termInstanceId) {
        var events = examEventRepository.findByExamSession_TermInstance_Id(termInstanceId);
        List<CourseStatsDto> stats = new ArrayList<>();

        for (var event : events) {
            List<StudentMark> marks = studentMarkRepository.findByExamEvent_Id(event.getId());
            var termInstance = event.getExamSession().getTermInstance();
            String label = termInstance.getAcademicYear().getName() + " " + termInstance.getTermType();

            long presentCount = marks.stream()
                .filter(m -> m.getMarkStatus().name().equals("PRESENT")).count();
            long absentCount = marks.stream()
                .filter(m -> m.getMarkStatus().name().equals("ABSENT")).count();
            long malpracticeCount = marks.stream()
                .filter(m -> m.getMarkStatus().name().equals("MALPRACTICE")).count();

            BigDecimal avgMarks = marks.isEmpty() ? BigDecimal.ZERO
                : marks.stream()
                    .map(m -> m.getMarksObtained() != null ? m.getMarksObtained() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(marks.size()), 2, RoundingMode.HALF_UP);

            stats.add(new CourseStatsDto(
                event.getCourseOffering().getId(),
                event.getCourseOffering().getSubject().getName(),
                event.getCourseOffering().getSubject().getCode(),
                termInstance.getId(), label, marks.size(),
                (int) presentCount, (int) absentCount, (int) malpracticeCount,
                avgMarks, event.getMaxMarks()));
        }
        return stats;
    }
}
