package com.cms.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AttendanceAnalyticsReportResponse;
import com.cms.dto.ExamResultResponse;
import com.cms.dto.LabContinuousEvaluationResponse;
import com.cms.dto.LabUtilizationReportResponse;
import com.cms.dto.StudentPerformanceReportResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Student;
import com.cms.repository.AttendanceRepository;
import com.cms.repository.ExamResultRepository;
import com.cms.repository.LabContinuousEvaluationRepository;
import com.cms.repository.LabRepository;
import com.cms.repository.LabScheduleRepository;
import com.cms.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final LabRepository labRepository;
    private final LabScheduleRepository labScheduleRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final ExamResultRepository examResultRepository;
    private final LabContinuousEvaluationRepository labContinuousEvaluationRepository;

    public ReportService(LabRepository labRepository,
                          LabScheduleRepository labScheduleRepository,
                          StudentRepository studentRepository,
                          AttendanceRepository attendanceRepository,
                          ExamResultRepository examResultRepository,
                          LabContinuousEvaluationRepository labContinuousEvaluationRepository) {
        this.labRepository = labRepository;
        this.labScheduleRepository = labScheduleRepository;
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
        this.examResultRepository = examResultRepository;
        this.labContinuousEvaluationRepository = labContinuousEvaluationRepository;
    }

    public LabUtilizationReportResponse getLabUtilizationReport() {
        long totalLabs = labRepository.count();
        long totalSchedules = labScheduleRepository.count();
        double avg = totalLabs > 0 ? (double) totalSchedules / totalLabs : 0.0;
        return new LabUtilizationReportResponse(totalLabs, totalSchedules, avg);
    }

    public StudentPerformanceReportResponse getStudentPerformanceReport(Long studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
        var examResults = examResultRepository.findByStudentId(studentId).stream()
            .map(er -> new ExamResultResponse(
                er.getId(),
                er.getExamination().getId(),
                er.getExamination().getName(),
                er.getStudent().getId(),
                er.getStudent().getFullName(),
                er.getStudent().getRollNumber(),
                er.getMarksObtained(),
                er.getGrade(),
                er.getStatus(),
                er.getCreatedAt(),
                er.getUpdatedAt()
            )).toList();
        var labEvaluations = labContinuousEvaluationRepository.findByStudentId(studentId).stream()
            .map(le -> new LabContinuousEvaluationResponse(
                le.getId(),
                le.getExperiment().getId(),
                le.getExperiment().getName(),
                le.getStudent().getId(),
                le.getStudent().getFullName(),
                le.getStudent().getRollNumber(),
                le.getRecordMarks(),
                le.getVivaMarks(),
                le.getPerformanceMarks(),
                le.getTotalMarks(),
                le.getEvaluationDate(),
                le.getEvaluatedBy(),
                le.getCreatedAt(),
                le.getUpdatedAt()
            )).toList();
        return new StudentPerformanceReportResponse(
            student.getId(), student.getFullName(), examResults, labEvaluations
        );
    }

    public AttendanceAnalyticsReportResponse getAttendanceAnalyticsReport() {
        long totalStudents = studentRepository.count();
        long totalAttendanceRecords = attendanceRepository.count();
        return new AttendanceAnalyticsReportResponse(totalStudents, totalAttendanceRecords);
    }
}
