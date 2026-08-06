package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.CapacityPlanResponse;
import com.cms.dto.PortionShortfallResponse;
import com.cms.model.CourseOffering;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.Subject;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.StudentTermEnrollmentRepository;

@ExtendWith(MockitoExtension.class)
class PortionShortfallServiceTest {

    @Mock private PortionBlueprintService portionBlueprintService;
    @Mock private TimetableCapacityPlanningService capacityPlanningService;
    @Mock private CourseOfferingRepository courseOfferingRepository;
    @Mock private StudentTermEnrollmentRepository studentTermEnrollmentRepository;

    private PortionShortfallService service;

    @BeforeEach
    void setUp() {
        service = new PortionShortfallService(portionBlueprintService, capacityPlanningService,
            courseOfferingRepository, studentTermEnrollmentRepository);
    }

    private CourseOffering offering(long id, String name, String code, boolean elective) {
        CurriculumSemesterCourse csc = new CurriculumSemesterCourse();
        csc.setIsElective(elective);
        Subject subject = new Subject();
        subject.setName(name);
        subject.setCode(code);
        CourseOffering offering = new CourseOffering();
        offering.setId(id);
        offering.setCurriculumSemesterCourse(csc);
        offering.setSubject(subject);
        return offering;
    }

    private CapacityPlanResponse planWithBuffer(double bufferHours) {
        return new CapacityPlanResponse(1L, "Cohort", 10L, "Term", 3, 60, 60, null, 100, 500.0, 20.0, 300,
            bufferHours, true, null, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    @Test
    void shouldFlagAtRiskWhenTotalShortfallExceedsBuffer() {
        StudentTermEnrollment enrollment = new StudentTermEnrollment();
        enrollment.setSemesterNumber(3);
        when(studentTermEnrollmentRepository.findFirstByTermInstanceIdAndCohortIdAndStatus(10L, 1L, EnrollmentStatus.ENROLLED))
            .thenReturn(Optional.of(enrollment));
        when(capacityPlanningService.getPlan(10L, 1L, null)).thenReturn(planWithBuffer(5.0));

        CourseOffering anatomy = offering(300L, "Anatomy", "ANAT101", false);
        CourseOffering elective = offering(301L, "Elective Subject", "ELEC1", true);
        when(courseOfferingRepository.findByTermInstanceIdAndSemesterNumber(10L, 3))
            .thenReturn(List.of(anatomy, elective));

        when(portionBlueprintService.remainingShortfallHours(300L)).thenReturn(8.0);
        // Elective is skipped entirely -- remainingShortfallHours must never even be called for it.

        PortionShortfallResponse response = service.checkShortfall(10L, 1L);

        assertThat(response.bufferHours()).isEqualTo(5.0);
        assertThat(response.totalShortfallHours()).isEqualTo(8.0);
        assertThat(response.atRisk()).isTrue();
        assertThat(response.subjects()).hasSize(1);
        assertThat(response.subjects().get(0).subjectCode()).isEqualTo("ANAT101");
        org.mockito.Mockito.verify(portionBlueprintService, org.mockito.Mockito.never()).remainingShortfallHours(301L);
    }

    @Test
    void shouldNotBeAtRiskWhenShortfallFitsWithinBuffer() {
        StudentTermEnrollment enrollment = new StudentTermEnrollment();
        enrollment.setSemesterNumber(2);
        when(studentTermEnrollmentRepository.findFirstByTermInstanceIdAndCohortIdAndStatus(10L, 1L, EnrollmentStatus.ENROLLED))
            .thenReturn(Optional.of(enrollment));
        when(capacityPlanningService.getPlan(10L, 1L, null)).thenReturn(planWithBuffer(10.0));

        CourseOffering physio = offering(302L, "Physiology", "PHYS101", false);
        when(courseOfferingRepository.findByTermInstanceIdAndSemesterNumber(10L, 2)).thenReturn(List.of(physio));
        when(portionBlueprintService.remainingShortfallHours(302L)).thenReturn(3.0);

        PortionShortfallResponse response = service.checkShortfall(10L, 1L);

        assertThat(response.atRisk()).isFalse();
        assertThat(response.totalShortfallHours()).isEqualTo(3.0);
    }

    @Test
    void shouldReturnNoSubjectsWhenCohortHasNoEnrollment() {
        when(studentTermEnrollmentRepository.findFirstByTermInstanceIdAndCohortIdAndStatus(10L, 1L, EnrollmentStatus.ENROLLED))
            .thenReturn(Optional.empty());
        when(capacityPlanningService.getPlan(10L, 1L, null)).thenReturn(planWithBuffer(10.0));

        PortionShortfallResponse response = service.checkShortfall(10L, 1L);

        assertThat(response.subjects()).isEmpty();
        assertThat(response.totalShortfallHours()).isEqualTo(0.0);
        assertThat(response.atRisk()).isFalse();
    }
}
