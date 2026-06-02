package com.cms.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.cms.dto.AppUserResponse;
import com.cms.dto.CreateUserRequest;
import com.cms.dto.UpdateUserRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AppRole;
import com.cms.model.AppUser;
import com.cms.repository.AppRoleRepository;
import com.cms.repository.AppUserRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class AppUserService {

    private static final Logger log = LoggerFactory.getLogger(AppUserService.class);

    private final AppUserRepository appUserRepository;
    private final AppRoleRepository appRoleRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final UserPermissionService userPermissionService;
    private final AuditLogService auditLogService;
    private final KeycloakAdminService keycloakAdminService;

    public AppUserService(AppUserRepository appUserRepository,
                          AppRoleRepository appRoleRepository,
                          StudentRepository studentRepository,
                          FacultyRepository facultyRepository,
                          UserPermissionService userPermissionService,
                          AuditLogService auditLogService,
                          KeycloakAdminService keycloakAdminService) {
        this.appUserRepository     = appUserRepository;
        this.appRoleRepository     = appRoleRepository;
        this.studentRepository     = studentRepository;
        this.facultyRepository     = facultyRepository;
        this.userPermissionService = userPermissionService;
        this.auditLogService       = auditLogService;
        this.keycloakAdminService  = keycloakAdminService;
    }

    public AppUserResponse findByUsername(String username) {
        AppUser user = appUserRepository.findByKeycloakUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return toResponse(user);
    }

    /** Returns users whose role hierarchy_level is strictly greater than the requester's level. */
    public List<AppUserResponse> findManageable(int requesterLevel) {
        return appUserRepository.findByAppRoleHierarchyLevelGreaterThan(requesterLevel).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public AppUserResponse create(CreateUserRequest request, String createdBy, int requesterLevel) {
        AppRole targetRole = resolveRole(request.roleName());
        enforceHierarchy(targetRole.getHierarchyLevel(), requesterLevel,
            "You cannot create a user with a role at or above your own level");

        if (appUserRepository.existsByKeycloakUsername(request.keycloakUsername())) {
            throw new IllegalArgumentException(
                "Username '" + request.keycloakUsername() + "' is already registered");
        }
        if (appUserRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException(
                "Email '" + request.email() + "' is already registered");
        }

        // 1. Create the user in Keycloak (sets a temporary password — user must change on first login).
        //    This runs BEFORE the DB save so that a Keycloak failure aborts the whole operation cleanly.
        String keycloakUserId = keycloakAdminService.createUser(
            request.keycloakUsername(), request.email(), request.fullName(), request.password());

        // 2. Persist the CMS record. If this fails, roll back the Keycloak user so the two stores stay in sync.
        try {
            AppUser user = new AppUser(
                request.keycloakUsername(),
                request.email(),
                request.fullName(),
                targetRole,
                true,
                createdBy
            );
            user.setKeycloakUserId(keycloakUserId);

            // Link to the specific student or faculty record (eliminates email-based identity guessing)
            if (request.studentId() != null) {
                studentRepository.findById(request.studentId()).ifPresent(user::setLinkedStudent);
            }
            if (request.facultyId() != null) {
                facultyRepository.findById(request.facultyId()).ifPresent(user::setLinkedFaculty);
            }

            AppUser saved = appUserRepository.save(user);
            auditLogService.record(createdBy, "USER_CREATED", "AppUser",
                String.valueOf(saved.getId()),
                "Created user '" + saved.getKeycloakUsername() + "' with role '" + targetRole.getName() + "'");
            return toResponse(saved);

        } catch (Exception ex) {
            // Best-effort rollback: delete the Keycloak user so we don't have an orphan account.
            keycloakAdminService.deleteUser(keycloakUserId);
            throw ex;
        }
    }

    @Transactional
    public AppUserResponse update(Long id, UpdateUserRequest request, int requesterLevel, String actor) {
        AppUser user = appUserRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        enforceHierarchy(user.getAppRole().getHierarchyLevel(), requesterLevel,
            "You cannot modify a user with a role at or above your own level");

        StringBuilder changes = new StringBuilder();
        if (request.fullName() != null) {
            changes.append("fullName='").append(request.fullName()).append("' ");
            user.setFullName(request.fullName());
        }
        if (request.email() != null) {
            changes.append("email='").append(request.email()).append("' ");
            user.setEmail(request.email());
            if (user.getKeycloakUserId() != null) {
                keycloakAdminService.updateUserEmail(user.getKeycloakUserId(), request.email());
            } else {
                log.warn("User {} has no keycloakUserId — email change not synced to Keycloak", user.getKeycloakUsername());
            }
        }
        if (request.roleName() != null) {
            AppRole newRole = resolveRole(request.roleName());
            enforceHierarchy(newRole.getHierarchyLevel(), requesterLevel,
                "You cannot assign a role at or above your own level");
            changes.append("role='").append(newRole.getName()).append("' ");
            user.setAppRole(newRole);
            userPermissionService.evict(user.getKeycloakUsername());
        }
        if (request.isActive() != null) {
            changes.append("isActive=").append(request.isActive()).append(' ');
            user.setActive(request.isActive());
        }

        AppUser updated = appUserRepository.save(user);
        auditLogService.record(actor, "USER_UPDATED", "AppUser",
            String.valueOf(updated.getId()), changes.toString().trim());
        return toResponse(updated);
    }

    /**
     * Backwards-compatible overload — actor defaults to "system".
     * @deprecated Prefer {@link #update(Long, UpdateUserRequest, int, String)}.
     */
    @Deprecated
    @Transactional
    public AppUserResponse update(Long id, UpdateUserRequest request, int requesterLevel) {
        return update(id, request, requesterLevel, "system");
    }

    @Transactional
    public void deactivate(Long id, int requesterLevel, String actor) {
        AppUser user = appUserRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        enforceHierarchy(user.getAppRole().getHierarchyLevel(), requesterLevel,
            "You cannot deactivate a user with a role at or above your own level");

        user.setActive(false);
        appUserRepository.save(user);
        userPermissionService.evict(user.getKeycloakUsername());
        auditLogService.record(actor, "USER_DEACTIVATED", "AppUser",
            String.valueOf(id), "Deactivated user '" + user.getKeycloakUsername() + "'");
    }

    /**
     * Backwards-compatible overload — actor defaults to "system".
     * @deprecated Prefer {@link #deactivate(Long, int, String)}.
     */
    @Deprecated
    @Transactional
    public void deactivate(Long id, int requesterLevel) {
        deactivate(id, requesterLevel, "system");
    }

    @Transactional
    public void reactivate(Long id, int requesterLevel, String actor) {
        AppUser user = appUserRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        enforceHierarchy(user.getAppRole().getHierarchyLevel(), requesterLevel,
            "You cannot reactivate a user with a role at or above your own level");

        user.setActive(true);
        appUserRepository.save(user);
        auditLogService.record(actor, "USER_REACTIVATED", "AppUser",
            String.valueOf(id), "Reactivated user '" + user.getKeycloakUsername() + "'");
    }

    /**
     * Backwards-compatible overload — actor defaults to "system".
     * @deprecated Prefer {@link #reactivate(Long, int, String)}.
     */
    @Deprecated
    @Transactional
    public void reactivate(Long id, int requesterLevel) {
        reactivate(id, requesterLevel, "system");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private AppRole resolveRole(String roleName) {
        return appRoleRepository.findByName(roleName)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
    }

    private void enforceHierarchy(int targetLevel, int requesterLevel, String message) {
        if (targetLevel <= requesterLevel) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }

    private AppUserResponse toResponse(AppUser user) {
        AppRole role = user.getAppRole();
        return new AppUserResponse(
            user.getId(),
            user.getKeycloakUsername(),
            user.getEmail(),
            user.getFullName(),
            role != null ? role.getName() : null,
            role != null ? role.getDisplayName() : null,
            role != null ? role.getHierarchyLevel() : null,
            user.isActive(),
            user.getCreatedBy(),
            user.getCreatedAt()
        );
    }
}
