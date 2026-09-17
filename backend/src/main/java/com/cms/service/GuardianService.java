package com.cms.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.cms.dto.GuardianRequest;
import com.cms.dto.GuardianResponse;
import com.cms.dto.WardSummaryResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Guardian;
import com.cms.model.Student;
import com.cms.model.StudentGuardian;
import com.cms.repository.AppUserRepository;
import com.cms.repository.GuardianRepository;
import com.cms.repository.StudentGuardianRepository;
import com.cms.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class GuardianService {

    private final GuardianRepository guardianRepository;
    private final StudentRepository studentRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final AppUserRepository appUserRepository;

    public GuardianService(GuardianRepository guardianRepository,
                            StudentRepository studentRepository,
                            StudentGuardianRepository studentGuardianRepository,
                            AppUserRepository appUserRepository) {
        this.guardianRepository = guardianRepository;
        this.studentRepository = studentRepository;
        this.studentGuardianRepository = studentGuardianRepository;
        this.appUserRepository = appUserRepository;
    }

    public List<GuardianResponse> findAll() {
        return guardianRepository.findAll().stream().map(this::toResponse).toList();
    }

    public boolean emailExists(String email, Long excludeId) {
        return excludeId != null
            ? guardianRepository.existsByEmailAndIdNot(email, excludeId)
            : guardianRepository.existsByEmail(email);
    }

    @Transactional
    public GuardianResponse create(GuardianRequest request) {
        if (guardianRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email '" + request.email() + "' is already registered to a guardian");
        }
        Guardian guardian = new Guardian();
        guardian.setFirstName(request.firstName());
        guardian.setLastName(request.lastName());
        guardian.setEmail(request.email());
        guardian.setPhone(request.phone());
        guardian.setRelationshipHint(request.relationshipHint());
        return toResponse(guardianRepository.save(guardian));
    }

    /** Links an existing guardian to an existing student (ward). Both must already exist --
     *  this never creates either side, matching the "explicit provisioning" model the whole
     *  Student Portal precedent already uses (never inferring identity/links implicitly). */
    @Transactional
    public void linkToStudent(Long guardianId, Long studentId, boolean isPrimary) {
        Guardian guardian = guardianRepository.findById(guardianId)
            .orElseThrow(() -> new ResourceNotFoundException("Guardian not found with id: " + guardianId));
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
        if (studentGuardianRepository.existsByStudentIdAndGuardianId(studentId, guardianId)) {
            throw new IllegalArgumentException("This guardian is already linked to this student");
        }
        StudentGuardian link = new StudentGuardian();
        link.setStudent(student);
        link.setGuardian(guardian);
        link.setPrimary(isPrimary);
        studentGuardianRepository.save(link);
    }

    /** Current authenticated guardian's own wards (parent self-service portal). Resolves the
     *  caller's linked {@code Guardian} via the {@code app_users} FK, the same shape
     *  {@code AttendanceService#findMyAttendance} and {@code LibraryIssueService#findMyIssues}
     *  already use -- never a client-supplied guardianId. */
    public List<WardSummaryResponse> findMyWards(String keycloakUsername) {
        return appUserRepository.findByKeycloakUsername(keycloakUsername)
            .map(user -> user.getLinkedGuardian() != null
                ? studentGuardianRepository.findByGuardianId(user.getLinkedGuardian().getId()).stream()
                    .map(link -> new WardSummaryResponse(
                        link.getStudent().getId(),
                        link.getStudent().getFirstName() + " " + link.getStudent().getLastName(),
                        link.getStudent().getRollNumber(),
                        link.isPrimary()))
                    .toList()
                : List.<WardSummaryResponse>of())
            .orElse(List.of());
    }

    /** Validates that {@code studentId} is actually one of the caller's own wards before any
     *  ward-scoped self-service data is returned -- never trusts the client-supplied studentId
     *  on its own, the same rule OC-236 enforced for direct student self-service. Throws 403,
     *  not a silent empty result, so a guardian probing another family's student id gets a
     *  clear denial rather than data that merely looks accidentally empty. */
    public void assertIsMyWard(String keycloakUsername, Long studentId) {
        Long guardianId = currentGuardianId(keycloakUsername);
        if (guardianId == null || !studentGuardianRepository.existsByGuardianIdAndStudentId(guardianId, studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student " + studentId + " is not one of your wards");
        }
    }

    /** The caller's own guardianId (via the {@code app_users} FK), or {@code null} if the caller
     *  has no linked guardian account. */
    public Long currentGuardianId(String keycloakUsername) {
        return appUserRepository.findByKeycloakUsername(keycloakUsername)
            .map(user -> user.getLinkedGuardian() != null ? user.getLinkedGuardian().getId() : null)
            .orElse(null);
    }

    private GuardianResponse toResponse(Guardian g) {
        return new GuardianResponse(g.getId(), g.getFirstName(), g.getLastName(), g.getEmail(),
            g.getPhone(), g.getRelationshipHint(), g.getCreatedAt());
    }
}
