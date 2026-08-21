package com.cms.service;

import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Faculty;
import com.cms.model.Subject;
import com.cms.repository.FacultyRepository;

/**
 * Shared department-level (Speciality) eligibility gate for assigning a faculty member to teach a
 * subject -- used by {@link CourseOfferingServiceImpl} (primary/secondary faculty) and {@link
 * CourseOfferingSectionFacultyService} (per-section Theory faculty), so every faculty-assignment
 * point in the app enforces the identical rule. Skipped when unassigning, when the subject has no
 * speciality set, and grandfathered when the requested faculty already holds this exact slot.
 */
final class FacultyEligibility {

    private FacultyEligibility() {
    }

    static void require(Subject subject, Long facultyId, Long previousFacultyId, FacultyRepository facultyRepository) {
        if (facultyId == null || facultyId.equals(previousFacultyId)) {
            return;
        }
        if (subject.getSpeciality() == null) {
            return;
        }
        Faculty faculty = facultyRepository.findById(facultyId)
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + facultyId));
        if (!subject.getSpeciality().getId().equals(faculty.getSpeciality().getId())) {
            throw new IllegalArgumentException("Faculty '" + faculty.getFullName() + "' belongs to the "
                + faculty.getSpeciality().getName() + " department and is not eligible to teach '"
                + subject.getName() + "' (" + subject.getSpeciality().getName() + ")");
        }
    }
}
