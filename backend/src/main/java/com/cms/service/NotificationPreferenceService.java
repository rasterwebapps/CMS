package com.cms.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.NotificationPreferenceRequest;
import com.cms.dto.NotificationPreferenceResponse;
import com.cms.model.UserNotificationPreference;
import com.cms.repository.UserNotificationPreferenceRepository;
import com.cms.util.CurrentUserResolver;

@Service
@Transactional(readOnly = true)
public class NotificationPreferenceService {

    private static final Set<String> VALID_CHANNELS = Set.of("IN_APP", "EMAIL", "BOTH");

    /** Default categories with their default enabled state. */
    private static final Map<String, Boolean> CATEGORY_DEFAULTS = new LinkedHashMap<>();

    static {
        CATEGORY_DEFAULTS.put("systemAnnouncements", true);
        CATEGORY_DEFAULTS.put("documentReminders",   true);
        CATEGORY_DEFAULTS.put("admissionUpdates",    true);
        CATEGORY_DEFAULTS.put("feeAlerts",           true);
        CATEGORY_DEFAULTS.put("examSchedule",        true);
        CATEGORY_DEFAULTS.put("attendanceAlerts",    true);
        CATEGORY_DEFAULTS.put("academicTermAlerts",  true);
    }

    private final UserNotificationPreferenceRepository preferenceRepository;
    private final CurrentUserResolver currentUserResolver;

    public NotificationPreferenceService(UserNotificationPreferenceRepository preferenceRepository,
                                          CurrentUserResolver currentUserResolver) {
        this.preferenceRepository = preferenceRepository;
        this.currentUserResolver  = currentUserResolver;
    }

    /** Return all preferences for the current user, merging stored rows with defaults. */
    public List<NotificationPreferenceResponse> getPreferences() {
        String userId = currentUserResolver.resolve();
        Map<String, UserNotificationPreference> stored = preferenceRepository.findByUserId(userId).stream()
            .collect(Collectors.toMap(UserNotificationPreference::getCategoryKey, p -> p));

        return CATEGORY_DEFAULTS.entrySet().stream().map(entry -> {
            String key = entry.getKey();
            if (stored.containsKey(key)) {
                UserNotificationPreference p = stored.get(key);
                return new NotificationPreferenceResponse(key, p.getEnabled(), p.getChannel());
            }
            return new NotificationPreferenceResponse(key, entry.getValue(), "IN_APP");
        }).toList();
    }

    /** Upsert a batch of preference updates for the current user. */
    @Transactional
    public List<NotificationPreferenceResponse> updatePreferences(NotificationPreferenceRequest request) {
        String userId = currentUserResolver.resolve();
        for (NotificationPreferenceRequest.PreferenceItem item : request.preferences()) {
            if (!CATEGORY_DEFAULTS.containsKey(item.categoryKey())) continue;
            String channel = VALID_CHANNELS.contains(item.channel()) ? item.channel() : "IN_APP";
            UserNotificationPreference pref = preferenceRepository
                .findByUserIdAndCategoryKey(userId, item.categoryKey())
                .orElseGet(() -> new UserNotificationPreference(userId, item.categoryKey(), item.enabled(), channel));
            pref.setEnabled(item.enabled());
            pref.setChannel(channel);
            preferenceRepository.save(pref);
        }
        return getPreferences();
    }
}
