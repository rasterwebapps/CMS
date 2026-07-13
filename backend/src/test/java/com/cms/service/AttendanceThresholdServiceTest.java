package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.model.AttendanceThreshold;
import com.cms.model.CourseOffering;
import com.cms.model.CourseRegistration;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.Student;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.enums.AttendanceType;
import com.cms.repository.AttendanceThresholdRepository;
import com.cms.repository.CourseRegistrationRepository;
import com.cms.repository.CurriculumSemesterCourseRepository;

@ExtendWith(MockitoExtension.class)
class AttendanceThresholdServiceTest {

    @Mock
    private AttendanceThresholdRepository thresholdRepository;
    @Mock
    private CurriculumSemesterCourseRepository curriculumSemesterCourseRepository;
    @Mock
    private CourseRegistrationRepository courseRegistrationRepository;

    private AttendanceThresholdService service;

    @BeforeEach
    void setUp() {
        service = new AttendanceThresholdService(
            thresholdRepository, curriculumSemesterCourseRepository, courseRegistrationRepository);
    }

    @Test
    void shouldResolveExplicitOverrideWhenSet() {
        CurriculumSemesterCourse mapping = new CurriculumSemesterCourse();
        mapping.setId(5L);
        CourseOffering offering = new CourseOffering();
        offering.setCurriculumSemesterCourse(mapping);
        CourseRegistration registration = registrationFor(offering);

        AttendanceThreshold threshold = new AttendanceThreshold(mapping, AttendanceType.CLINICAL, new BigDecimal("100.00"));

        when(courseRegistrationRepository.findByStudentIdAndSubjectId(1L, 1L))
            .thenReturn(List.of(registration));
        when(thresholdRepository.findByCurriculumSemesterCourseIdAndAttendanceType(5L, AttendanceType.CLINICAL))
            .thenReturn(Optional.of(threshold));

        BigDecimal resolved = service.resolveThreshold(1L, 1L, AttendanceType.CLINICAL);

        assertThat(resolved).isEqualTo(new BigDecimal("100.00"));
    }

    @Test
    void shouldFallBackToDefaultWhenNoRegistrationFound() {
        when(courseRegistrationRepository.findByStudentIdAndSubjectId(1L, 1L)).thenReturn(List.of());

        BigDecimal resolved = service.resolveThreshold(1L, 1L, AttendanceType.THEORY);

        assertThat(resolved).isEqualTo(AttendanceThresholdService.DEFAULT_MIN_PERCENTAGE);
    }

    @Test
    void shouldFallBackToDefaultWhenOfferingHasNoCurriculumMapping() {
        CourseOffering offering = new CourseOffering();
        // no curriculumSemesterCourse set — simulates a legacy offering predating the backfill
        CourseRegistration registration = registrationFor(offering);

        when(courseRegistrationRepository.findByStudentIdAndSubjectId(1L, 1L))
            .thenReturn(List.of(registration));

        BigDecimal resolved = service.resolveThreshold(1L, 1L, AttendanceType.THEORY);

        assertThat(resolved).isEqualTo(AttendanceThresholdService.DEFAULT_MIN_PERCENTAGE);
    }

    @Test
    void shouldFallBackToDefaultWhenNoThresholdRowExists() {
        CurriculumSemesterCourse mapping = new CurriculumSemesterCourse();
        mapping.setId(5L);
        CourseOffering offering = new CourseOffering();
        offering.setCurriculumSemesterCourse(mapping);
        CourseRegistration registration = registrationFor(offering);

        when(courseRegistrationRepository.findByStudentIdAndSubjectId(1L, 1L))
            .thenReturn(List.of(registration));
        when(thresholdRepository.findByCurriculumSemesterCourseIdAndAttendanceType(5L, AttendanceType.LAB))
            .thenReturn(Optional.empty());

        BigDecimal resolved = service.resolveThreshold(1L, 1L, AttendanceType.LAB);

        assertThat(resolved).isEqualTo(AttendanceThresholdService.DEFAULT_MIN_PERCENTAGE);
    }

    private CourseRegistration registrationFor(CourseOffering offering) {
        Student student = new Student();
        student.setId(1L);
        StudentTermEnrollment enrollment = new StudentTermEnrollment();
        enrollment.setStudent(student);
        CourseRegistration registration = new CourseRegistration();
        registration.setStudentTermEnrollment(enrollment);
        registration.setCourseOffering(offering);
        return registration;
    }
}
