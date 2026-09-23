package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.config.PermSecurityBean;
import com.cms.dto.NotificationResponse;
import com.cms.model.AppUser;
import com.cms.model.Faculty;
import com.cms.model.Notification;
import com.cms.model.NotificationDismissal;
import com.cms.repository.AppUserRepository;
import com.cms.repository.NotificationDismissalRepository;
import com.cms.repository.NotificationRepository;
import com.cms.repository.UserNotificationPreferenceRepository;
import com.cms.util.CurrentUserResolver;

/**
 * Read/dismiss side of the in-app notification feed (BR-53). Notifications themselves are
 * broadcast-style rows (see {@link Notification}) — this service computes, per request, which of
 * the currently-active ones the calling user should actually see: their own category preference,
 * a permission gate for admin-only categories, and their own dismissal history.
 */
@Service
@Transactional(readOnly = true)
public class NotificationService {

    /** Categories gated by a permission beyond simple role/preference — checked via {@link PermSecurityBean}. */
    private static final String ACADEMIC_TERM_ALERTS_CATEGORY = "academicTermAlerts";
    private static final String ACADEMIC_TERM_ALERTS_PERMISSION = "ACADEMIC_YEAR_MANAGE";

    private final NotificationRepository notificationRepository;
    private final NotificationDismissalRepository dismissalRepository;
    private final UserNotificationPreferenceRepository preferenceRepository;
    private final AppUserRepository appUserRepository;
    private final CurrentUserResolver currentUserResolver;
    private final PermSecurityBean permSecurityBean;

    public NotificationService(NotificationRepository notificationRepository,
                                NotificationDismissalRepository dismissalRepository,
                                UserNotificationPreferenceRepository preferenceRepository,
                                AppUserRepository appUserRepository,
                                CurrentUserResolver currentUserResolver,
                                PermSecurityBean permSecurityBean) {
        this.notificationRepository = notificationRepository;
        this.dismissalRepository = dismissalRepository;
        this.preferenceRepository = preferenceRepository;
        this.appUserRepository = appUserRepository;
        this.currentUserResolver = currentUserResolver;
        this.permSecurityBean = permSecurityBean;
    }

    public List<NotificationResponse> getFeed() {
        String userId = currentUserResolver.resolve();
        if (userId == null) {
            return List.of();
        }
        boolean canSeeTermAlerts = permSecurityBean.has(ACADEMIC_TERM_ALERTS_PERMISSION);
        Long facultyId = currentFacultyId(userId);

        return notificationRepository.findByResolvedAtIsNullOrderByCreatedAtDesc().stream()
            .filter(n -> !ACADEMIC_TERM_ALERTS_CATEGORY.equals(n.getCategoryKey()) || canSeeTermAlerts)
            .filter(n -> n.getRecipientFacultyId() == null || n.getRecipientFacultyId().equals(facultyId))
            .filter(n -> isCategoryEnabled(userId, n.getCategoryKey()))
            .filter(n -> !dismissalRepository.existsByNotificationIdAndUserId(n.getId(), userId))
            .map(this::toResponse)
            .toList();
    }

    private Long currentFacultyId(String userId) {
        return appUserRepository.findByKeycloakUsername(userId)
            .map(AppUser::getLinkedFaculty)
            .map(Faculty::getId)
            .orElse(null);
    }

    @Transactional
    public void dismiss(Long notificationId) {
        String userId = currentUserResolver.resolve();
        if (userId == null) {
            return;
        }
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null || dismissalRepository.existsByNotificationIdAndUserId(notificationId, userId)) {
            return;
        }
        dismissalRepository.save(new NotificationDismissal(notification, userId));
    }

    private boolean isCategoryEnabled(String userId, String categoryKey) {
        return preferenceRepository.findByUserIdAndCategoryKey(userId, categoryKey)
            .map(p -> Boolean.TRUE.equals(p.getEnabled()))
            .orElse(true);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getCategoryKey(), n.getTitle(), n.getMessage(),
            n.getLink(), n.getCreatedAt());
    }
}
