package com.cms.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AttendanceThresholdDto;
import com.cms.dto.AttendanceThresholdRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AttendanceThreshold;
import com.cms.model.CourseOffering;
import com.cms.model.CourseRegistration;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.enums.AttendanceType;
import com.cms.repository.AttendanceThresholdRepository;
import com.cms.repository.CourseRegistrationRepository;
import com.cms.repository.CurriculumSemesterCourseRepository;

@Service
@Transactional(readOnly = true)
public class AttendanceThresholdService {

    /** Applied when no override exists at any step of the resolution chain. */
    public static final BigDecimal DEFAULT_MIN_PERCENTAGE = new BigDecimal("75.00");

    private final AttendanceThresholdRepository thresholdRepository;
    private final CurriculumSemesterCourseRepository curriculumSemesterCourseRepository;
    private final CourseRegistrationRepository courseRegistrationRepository;

    public AttendanceThresholdService(AttendanceThresholdRepository thresholdRepository,
                                       CurriculumSemesterCourseRepository curriculumSemesterCourseRepository,
                                       CourseRegistrationRepository courseRegistrationRepository) {
        this.thresholdRepository = thresholdRepository;
        this.curriculumSemesterCourseRepository = curriculumSemesterCourseRepository;
        this.courseRegistrationRepository = courseRegistrationRepository;
    }

    /**
     * Resolves the minimum attendance percentage that applies to a student's attendance in a
     * given subject and component type. Walks: the student's most recent course registration for
     * this subject -> the course offering's curriculum mapping -> an explicit threshold override
     * for that mapping + type. Falls back to {@link #DEFAULT_MIN_PERCENTAGE} at any missing step
     * (no registration found, offering not linked to a curriculum mapping, or no override set).
     */
    public BigDecimal resolveThreshold(Long studentId, Long subjectId, AttendanceType type) {
        List<CourseRegistration> registrations =
            courseRegistrationRepository.findByStudentIdAndSubjectId(studentId, subjectId);
        if (registrations.isEmpty()) {
            return DEFAULT_MIN_PERCENTAGE;
        }

        CourseOffering offering = registrations.get(0).getCourseOffering();
        CurriculumSemesterCourse mapping = offering.getCurriculumSemesterCourse();
        if (mapping == null) {
            return DEFAULT_MIN_PERCENTAGE;
        }

        return thresholdRepository.findByCurriculumSemesterCourseIdAndAttendanceType(mapping.getId(), type)
            .map(AttendanceThreshold::getMinPercentage)
            .orElse(DEFAULT_MIN_PERCENTAGE);
    }

    public List<AttendanceThresholdDto> getThresholdsForMapping(Long curriculumTermCourseId) {
        return thresholdRepository.findByCurriculumSemesterCourseId(curriculumTermCourseId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional
    public AttendanceThresholdDto upsertThreshold(AttendanceThresholdRequest request) {
        CurriculumSemesterCourse mapping = curriculumSemesterCourseRepository.findById(request.curriculumTermCourseId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Curriculum semester course not found with id: " + request.curriculumTermCourseId()));

        AttendanceThreshold threshold = thresholdRepository
            .findByCurriculumSemesterCourseIdAndAttendanceType(mapping.getId(), request.attendanceType())
            .orElseGet(() -> new AttendanceThreshold(mapping, request.attendanceType(), request.minPercentage()));
        threshold.setMinPercentage(request.minPercentage());

        return toDto(thresholdRepository.save(threshold));
    }

    @Transactional
    public void deleteThreshold(Long id) {
        if (!thresholdRepository.existsById(id)) {
            throw new ResourceNotFoundException("Attendance threshold not found with id: " + id);
        }
        thresholdRepository.deleteById(id);
    }

    private AttendanceThresholdDto toDto(AttendanceThreshold t) {
        return new AttendanceThresholdDto(
            t.getId(),
            t.getCurriculumSemesterCourse().getId(),
            t.getAttendanceType(),
            t.getMinPercentage(),
            t.getCreatedAt(),
            t.getUpdatedAt()
        );
    }
}
