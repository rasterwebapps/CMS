package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.config.PermSecurityBean;
import com.cms.dto.NotificationResponse;
import com.cms.model.AppUser;
import com.cms.model.Faculty;
import com.cms.model.Notification;
import com.cms.model.UserNotificationPreference;
import com.cms.repository.AppUserRepository;
import com.cms.repository.NotificationDismissalRepository;
import com.cms.repository.NotificationRepository;
import com.cms.repository.UserNotificationPreferenceRepository;
import com.cms.util.CurrentUserResolver;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationDismissalRepository dismissalRepository;
    @Mock
    private UserNotificationPreferenceRepository preferenceRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private CurrentUserResolver currentUserResolver;
    @Mock
    private PermSecurityBean permSecurityBean;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepository, dismissalRepository, preferenceRepository,
            appUserRepository, currentUserResolver, permSecurityBean);
        when(currentUserResolver.resolve()).thenReturn("devadmin");
    }

    private Notification notification(Long id, String categoryKey) {
        Notification n = new Notification(categoryKey, "title", "message", "/link", "TERM_INSTANCE", 1L);
        n.setId(id);
        return n;
    }

    private Notification recipientNotification(Long id, Long recipientFacultyId) {
        Notification n = new Notification("holidayDisruption", "title", "message", "/link",
            "BLOCKED_PERIOD", 1L, recipientFacultyId);
        n.setId(id);
        return n;
    }

    @Test
    void feed_excludesAcademicTermAlerts_whenUserLacksPermission() {
        when(notificationRepository.findByResolvedAtIsNullOrderByCreatedAtDesc())
            .thenReturn(List.of(notification(1L, "academicTermAlerts")));
        when(permSecurityBean.has("ACADEMIC_YEAR_MANAGE")).thenReturn(false);

        List<NotificationResponse> feed = service.getFeed();

        assertThat(feed).isEmpty();
    }

    @Test
    void feed_includesAcademicTermAlerts_whenUserHasPermission() {
        when(notificationRepository.findByResolvedAtIsNullOrderByCreatedAtDesc())
            .thenReturn(List.of(notification(1L, "academicTermAlerts")));
        when(permSecurityBean.has("ACADEMIC_YEAR_MANAGE")).thenReturn(true);
        when(preferenceRepository.findByUserIdAndCategoryKey("devadmin", "academicTermAlerts"))
            .thenReturn(Optional.empty());
        when(dismissalRepository.existsByNotificationIdAndUserId(any(), any())).thenReturn(false);

        List<NotificationResponse> feed = service.getFeed();

        assertThat(feed).hasSize(1);
        assertThat(feed.get(0).categoryKey()).isEqualTo("academicTermAlerts");
    }

    @Test
    void feed_excludesCategory_whenUserDisabledPreference() {
        when(notificationRepository.findByResolvedAtIsNullOrderByCreatedAtDesc())
            .thenReturn(List.of(notification(1L, "systemAnnouncements")));
        when(preferenceRepository.findByUserIdAndCategoryKey("devadmin", "systemAnnouncements"))
            .thenReturn(Optional.of(new UserNotificationPreference("devadmin", "systemAnnouncements", false, "IN_APP")));

        List<NotificationResponse> feed = service.getFeed();

        assertThat(feed).isEmpty();
    }

    @Test
    void feed_excludesAlreadyDismissedNotifications() {
        Notification n = notification(1L, "systemAnnouncements");
        when(notificationRepository.findByResolvedAtIsNullOrderByCreatedAtDesc()).thenReturn(List.of(n));
        when(preferenceRepository.findByUserIdAndCategoryKey("devadmin", "systemAnnouncements"))
            .thenReturn(Optional.empty());
        when(dismissalRepository.existsByNotificationIdAndUserId(n.getId(), "devadmin")).thenReturn(true);

        List<NotificationResponse> feed = service.getFeed();

        assertThat(feed).isEmpty();
    }

    @Test
    void feed_includesRecipientTargetedNotification_forItsOwnRecipient() {
        Notification n = recipientNotification(1L, 42L);
        Faculty faculty = new Faculty();
        faculty.setId(42L);
        AppUser me = new AppUser("devadmin", "me@example.com", "Me", null, true, "system");
        me.setLinkedFaculty(faculty);

        when(notificationRepository.findByResolvedAtIsNullOrderByCreatedAtDesc()).thenReturn(List.of(n));
        when(appUserRepository.findByKeycloakUsername("devadmin")).thenReturn(Optional.of(me));
        when(preferenceRepository.findByUserIdAndCategoryKey("devadmin", "holidayDisruption"))
            .thenReturn(Optional.empty());
        when(dismissalRepository.existsByNotificationIdAndUserId(any(), any())).thenReturn(false);

        List<NotificationResponse> feed = service.getFeed();

        assertThat(feed).hasSize(1);
    }

    @Test
    void feed_excludesRecipientTargetedNotification_forADifferentRecipient() {
        Notification n = recipientNotification(1L, 42L);
        Faculty faculty = new Faculty();
        faculty.setId(99L);
        AppUser me = new AppUser("devadmin", "me@example.com", "Me", null, true, "system");
        me.setLinkedFaculty(faculty);

        when(notificationRepository.findByResolvedAtIsNullOrderByCreatedAtDesc()).thenReturn(List.of(n));
        when(appUserRepository.findByKeycloakUsername("devadmin")).thenReturn(Optional.of(me));

        List<NotificationResponse> feed = service.getFeed();

        assertThat(feed).isEmpty();
    }

    @Test
    void dismiss_doesNothing_whenAlreadyDismissed() {
        Notification n = notification(1L, "systemAnnouncements");
        when(notificationRepository.findById(n.getId())).thenReturn(Optional.of(n));
        when(dismissalRepository.existsByNotificationIdAndUserId(n.getId(), "devadmin")).thenReturn(true);

        service.dismiss(n.getId());

        verify(dismissalRepository, never()).save(any());
    }
}
