package com.cms.service;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.model.AuditLog;
import com.cms.repository.AuditLogRepository;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @Test
    void record_savesAuditEntryWithCorrectFields() {
        auditLogService.record("admin", "ROLE_CREATED", "AppRole", "5", "Created role CUSTOM");

        verify(auditLogRepository).save(argThat((AuditLog log) ->
            "admin".equals(log.getActor()) &&
            "ROLE_CREATED".equals(log.getAction()) &&
            "AppRole".equals(log.getEntityType()) &&
            "5".equals(log.getEntityId()) &&
            "Created role CUSTOM".equals(log.getDetail())
        ));
    }

    @Test
    void record_savesAuditEntryForUserMutation() {
        auditLogService.record("college_admin", "USER_DEACTIVATED", "AppUser", "42", "Deactivated user faculty1");

        verify(auditLogRepository).save(argThat((AuditLog log) ->
            "college_admin".equals(log.getActor()) &&
            "USER_DEACTIVATED".equals(log.getAction()) &&
            "AppUser".equals(log.getEntityType()) &&
            "42".equals(log.getEntityId())
        ));
    }

    @Test
    void record_savesAuditEntryForPermissionUpdate() {
        auditLogService.record("admin", "PERMISSIONS_UPDATED", "AppRole", "3",
            "Updated permissions for role FACULTY");

        verify(auditLogRepository).save(argThat((AuditLog log) ->
            "PERMISSIONS_UPDATED".equals(log.getAction()) &&
            "AppRole".equals(log.getEntityType())
        ));
    }
}

