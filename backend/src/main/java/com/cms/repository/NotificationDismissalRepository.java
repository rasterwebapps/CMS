package com.cms.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.NotificationDismissal;

public interface NotificationDismissalRepository extends JpaRepository<NotificationDismissal, Long> {

    boolean existsByNotificationIdAndUserId(Long notificationId, String userId);
}
