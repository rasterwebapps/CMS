package com.cms.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.PortionShortfallResponse;
import com.cms.dto.SubjectShortfallDto;
import com.cms.model.CourseOffering;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.StudentTermEnrollmentRepository;

/**
 * "Portions cannot be completed" alert: sums each non-elective subject's own remaining shortfall
 * (hours still needed for not-yet-complete units, minus hours actually still available in the
 * current timetable through term end -- see {@link PortionBlueprintService#remainingShortfallHours})
 * across a cohort-semester, and compares the total against that cohort-semester's existing
 * Capacity Planner buffer ({@link TimetableCapacityPlanningService}). Deliberately reuses
 * bufferHours rather than recomputing it -- see the project plan's confirmed answer that buffer is
 * exactly "total working hours minus curriculum hours required", already computed there.
 */
@Service
@Transactional(readOnly = true)
public class PortionShortfallService {

    private final PortionBlueprintService portionBlueprintService;
    private final TimetableCapacityPlanningService capacityPlanningService;
    private final CourseOfferingRepository courseOfferingRepository;
    private final StudentTermEnrollmentRepository studentTermEnrollmentRepository;

    public PortionShortfallService(PortionBlueprintService portionBlueprintService,
                                    TimetableCapacityPlanningService capacityPlanningService,
                                    CourseOfferingRepository courseOfferingRepository,
                                    StudentTermEnrollmentRepository studentTermEnrollmentRepository) {
        this.portionBlueprintService = portionBlueprintService;
        this.capacityPlanningService = capacityPlanningService;
        this.courseOfferingRepository = courseOfferingRepository;
        this.studentTermEnrollmentRepository = studentTermEnrollmentRepository;
    }

    public PortionShortfallResponse checkShortfall(Long termInstanceId, Long cohortId) {
        double bufferHours = capacityPlanningService.getPlan(termInstanceId, cohortId, null).bufferHours();

        Integer semesterNumber = studentTermEnrollmentRepository
            .findFirstByTermInstanceIdAndCohortIdAndStatus(termInstanceId, cohortId, EnrollmentStatus.ENROLLED)
            .map(StudentTermEnrollment::getSemesterNumber)
            .orElse(null);
        List<CourseOffering> offerings = semesterNumber == null ? List.of()
            : courseOfferingRepository.findByTermInstanceIdAndSemesterNumber(termInstanceId, semesterNumber);

        List<SubjectShortfallDto> subjects = new ArrayList<>();
        double total = 0;
        for (CourseOffering offering : offerings) {
            CurriculumSemesterCourse csc = offering.getCurriculumSemesterCourse();
            if (csc == null || Boolean.TRUE.equals(csc.getIsElective())) {
                continue; // matches TimetableCapacityPlanningService.curriculumHoursRequired's own exclusion
            }
            double shortfall = portionBlueprintService.remainingShortfallHours(offering.getId());
            if (shortfall > 0) {
                subjects.add(new SubjectShortfallDto(offering.getId(), offering.getSubject().getName(),
                    offering.getSubject().getCode(), shortfall));
            }
            total += shortfall;
        }

        return new PortionShortfallResponse(termInstanceId, cohortId, bufferHours, total, total > bufferHours, subjects);
    }
}
