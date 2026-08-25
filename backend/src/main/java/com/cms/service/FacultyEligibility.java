package com.cms.service;

import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Faculty;
import com.cms.model.Subject;
import com.cms.repository.FacultyRepository;

/**
 * Shared eligibility gate for assigning a faculty member to teach a subject -- used by {@link
 * CourseOfferingServiceImpl} (primary/secondary faculty), {@link CourseOfferingSectionFacultyService}
 * (per-section Theory faculty), and {@link ClassScheduleService} (manual session staffing), so every
 * faculty-assignment point in the app enforces the identical rule. Skipped when unassigning, when
 * the subject has no speciality set, and grandfathered when the requested faculty already holds
 * this exact slot. A faculty passes if EITHER their own department (Speciality) matches the
 * subject's, OR they've been explicitly added to the subject's admin-curated {@code eligibleFaculty}
 * list (Subject form) -- the list only ever widens who qualifies, never narrows it, so a subject
 * with no explicit list behaves exactly as before this escape hatch existed.
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
        require(subject, faculty, null);
    }

    /** Same rule, for callers that already hold the resolved {@link Faculty} (and its previous
     *  value) rather than raw ids -- e.g. {@link ClassScheduleService}/{@link
     *  TimetableStaffingService}'s staffing pass. {@code previousFaculty} of {@code null} means
     *  "always check" (no grandfathering), matching the id-based overload's own semantics when it
     *  has no previous value to compare against. */
    static void require(Subject subject, Faculty faculty, Faculty previousFaculty) {
        if (faculty == null || (previousFaculty != null && previousFaculty.getId().equals(faculty.getId()))) {
            return;
        }
        if (subject.getSpeciality() == null) {
            return;
        }
        boolean specialityMatch = subject.getSpeciality().getId().equals(faculty.getSpeciality().getId());
        boolean explicitlyEligible = subject.getEligibleFaculty().stream()
            .anyMatch(f -> f.getId().equals(faculty.getId()));
        if (!specialityMatch && !explicitlyEligible) {
            throw new IllegalArgumentException("Faculty '" + faculty.getFullName() + "' belongs to the "
                + faculty.getSpeciality().getName() + " department and is not eligible to teach '"
                + subject.getName() + "' (" + subject.getSpeciality().getName()
                + ") -- add them to the subject's Eligible Faculty list to allow this assignment.");
        }
    }
}
