package com.cms.service;

import java.util.List;
import java.util.Comparator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ClassInchargeAssignment;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.CohortSection;
import com.cms.model.Faculty;
import com.cms.repository.CohortSectionRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.TermInstanceRepository;

/**
 * Manages Class Teacher / Class Incharge assignment for a term's committed {@link CohortSection}s
 * (see {@code class_incharge_faculty_id} on that entity). Structurally, sections are created in
 * Capacity Planner; who is incharge of one is a staffing decision, assigned here from Assign
 * Faculty -- same split as {@link CourseOfferingSectionFacultyService} for Theory faculty and
 * {@code Batch#getCoordinatorFaculty()} for batches. No department-eligibility gate: unlike
 * subject-teaching faculty, a class incharge isn't tied to any one subject's Speciality.
 */
@Service
public class ClassInchargeService {

    private final CohortSectionRepository cohortSectionRepository;
    private final TermInstanceRepository termInstanceRepository;
    private final FacultyRepository facultyRepository;

    public ClassInchargeService(CohortSectionRepository cohortSectionRepository,
                                 TermInstanceRepository termInstanceRepository,
                                 FacultyRepository facultyRepository) {
        this.cohortSectionRepository = cohortSectionRepository;
        this.termInstanceRepository = termInstanceRepository;
        this.facultyRepository = facultyRepository;
    }

    @Transactional(readOnly = true)
    public List<ClassInchargeAssignment> getForTermInstance(Long termInstanceId) {
        if (!termInstanceRepository.existsById(termInstanceId)) {
            throw new ResourceNotFoundException("Term instance not found with id: " + termInstanceId);
        }
        return cohortSectionRepository.findByTermInstanceIdAndIsActiveTrue(termInstanceId).stream()
            .map(ClassInchargeService::toAssignment)
            .sorted(Comparator.comparing(ClassInchargeAssignment::cohortName)
                .thenComparing(ClassInchargeAssignment::sectionLabel))
            .toList();
    }

    @Transactional
    public ClassInchargeAssignment upsert(Long cohortSectionId, Long facultyId) {
        CohortSection section = cohortSectionRepository.findById(cohortSectionId)
            .orElseThrow(() -> new ResourceNotFoundException("Cohort section not found with id: " + cohortSectionId));

        if (facultyId == null) {
            section.setClassInchargeFaculty(null);
        } else {
            Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + facultyId));
            section.setClassInchargeFaculty(faculty);
        }
        cohortSectionRepository.save(section);
        return toAssignment(section);
    }

    private static ClassInchargeAssignment toAssignment(CohortSection section) {
        Faculty incharge = section.getClassInchargeFaculty();
        return new ClassInchargeAssignment(
            section.getId(),
            section.getCohortRoomAllocation().getCohort().getDisplayName(),
            section.getSectionLabel(),
            section.getClassroom().getName(),
            incharge != null ? incharge.getId() : null,
            incharge != null ? incharge.getFullName() : null);
    }
}
