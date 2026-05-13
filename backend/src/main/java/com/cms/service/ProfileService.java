package com.cms.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ProfileIdentity;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.AdmissionRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class ProfileService {

    private final FacultyRepository facultyRepository;
    private final StudentRepository studentRepository;
    private final AdmissionRepository admissionRepository;

    public ProfileService(FacultyRepository facultyRepository,
                          StudentRepository studentRepository,
                          AdmissionRepository admissionRepository) {
        this.facultyRepository = facultyRepository;
        this.studentRepository = studentRepository;
        this.admissionRepository = admissionRepository;
    }

    public ProfileIdentity resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new ResourceNotFoundException("No authenticated user found");
        }

        String email    = jwt.getClaimAsString("email");
        String username = jwt.getClaimAsString("preferred_username");
        String fullName = jwt.getClaimAsString("name");
        String display  = fullName != null && !fullName.isBlank() ? fullName : username;

        if (email != null && !email.isBlank()) {
            // Try faculty first
            var faculty = facultyRepository.findByEmail(email);
            if (faculty.isPresent()) {
                return new ProfileIdentity(
                    "FACULTY",
                    faculty.get().getId(),
                    null, null,
                    faculty.get().getFullName(),
                    email
                );
            }

            // Try student
            var student = studentRepository.findByEmail(email);
            if (student.isPresent()) {
                Long admissionId = admissionRepository
                    .findByStudentId(student.get().getId())
                    .map(a -> a.getId())
                    .orElse(null);
                Long programId = student.get().getProgram() != null
                    ? student.get().getProgram().getId() : null;
                return new ProfileIdentity(
                    "STUDENT",
                    student.get().getId(),
                    admissionId,
                    programId,
                    student.get().getFullName(),
                    email
                );
            }
        }

        // Authenticated but not a faculty or student (admin, support, etc.)
        return new ProfileIdentity("ADMIN", null, null, null, display, email);
    }
}
