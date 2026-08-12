package com.cms.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.FacultyWorkloadRulesRequest;
import com.cms.dto.FacultyWorkloadRulesResponse;
import com.cms.dto.SystemConfigurationRequest;
import com.cms.dto.SystemConfigurationResponse;
import com.cms.model.enums.ConfigDataType;

/**
 * Thin, scoped editor for the three global {@code timetable.faculty_max_*_hours} System
 * Configuration rows (seeded blank by V370) -- replaces hunting for them among every other config
 * in the generic Settings list. Delegates all actual reads/writes to {@link
 * SystemConfigurationService}; this is not a new table, just a friendlier front door onto the
 * same three rows {@link TimetableStaffingService#resolveCapHours} already reads.
 */
@Service
public class FacultyWorkloadRulesService {

    private static final String DAILY_KEY = "timetable.faculty_max_daily_hours";
    private static final String WEEKLY_KEY = "timetable.faculty_max_weekly_hours";
    private static final String CONTINUOUS_KEY = "timetable.faculty_max_continuous_hours";
    private static final String CATEGORY = "TIMETABLE";

    private final SystemConfigurationService systemConfigurationService;

    public FacultyWorkloadRulesService(SystemConfigurationService systemConfigurationService) {
        this.systemConfigurationService = systemConfigurationService;
    }

    public FacultyWorkloadRulesResponse get() {
        return new FacultyWorkloadRulesResponse(
            parse(systemConfigurationService.findByKey(DAILY_KEY)),
            parse(systemConfigurationService.findByKey(WEEKLY_KEY)),
            parse(systemConfigurationService.findByKey(CONTINUOUS_KEY)));
    }

    @Transactional
    public FacultyWorkloadRulesResponse update(FacultyWorkloadRulesRequest request) {
        upsert(DAILY_KEY, request.maxDailyHours(),
            "Maximum teaching hours a faculty member can be staffed for in a single day. Blank or 0 = no cap.");
        upsert(WEEKLY_KEY, request.maxWeeklyHours(),
            "Maximum teaching hours a faculty member can be staffed for across the whole term timetable in one week. Blank or 0 = no cap.");
        upsert(CONTINUOUS_KEY, request.maxContinuousHours(),
            "Maximum unbroken back-to-back teaching hours a faculty member can be staffed for in a single day. Blank or 0 = no cap.");
        return get();
    }

    private void upsert(String key, Double value, String description) {
        systemConfigurationService.upsert(new SystemConfigurationRequest(
            key, value != null ? value.toString() : "", description, ConfigDataType.DECIMAL, CATEGORY, true));
    }

    /** Mirrors {@code TimetableStaffingService.resolveCapHours}'s own parsing: blank, unparseable,
     *  or <= 0 all mean "no cap configured" (null here), never an error. */
    private static Double parse(Optional<SystemConfigurationResponse> config) {
        return config.map(SystemConfigurationResponse::configValue)
            .filter(value -> value != null && !value.isBlank())
            .flatMap(value -> {
                try {
                    double parsed = Double.parseDouble(value.trim());
                    return parsed > 0 ? Optional.of(parsed) : Optional.<Double>empty();
                } catch (NumberFormatException e) {
                    return Optional.<Double>empty();
                }
            })
            .orElse(null);
    }
}
