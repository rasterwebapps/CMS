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
 * Thin, scoped editor for the four global {@code timetable.faculty_*_sessions} System
 * Configuration rows (seeded blank by V556/V557) -- replaces hunting for them among every other
 * config in the generic Settings list. Delegates all actual reads/writes to {@link
 * SystemConfigurationService}; this is not a new table, just a friendlier front door onto the same
 * rows {@link TimetableStaffingService#resolveCapSessions} already reads for the three maximum
 * tiers.
 *
 * <p>Sessions (a raw Period-row count), not hours -- 2026-09-24 design decision: a Theory row is 1
 * period, a Clinical block can be 4, so there's no honest hours-equivalent for an institution-wide
 * cap that hasn't picked a concrete session shape yet. Real worked hours (for INC/university
 * reporting) are computed elsewhere, on demand, from each actually-scheduled session's own real
 * Period duration -- never stored here. {@code minWeeklySessions} is the new floor counterpart to
 * the pre-existing three maximums -- advisory only (surfaced as a real-time UI guideline), never a
 * hard block the way the maximums are: refusing to place a session because it would leave someone
 * still under a floor makes no sense the way refusing to place one that would push someone over a
 * ceiling does.
 */
@Service
public class FacultyWorkloadRulesService {

    private static final String DAILY_KEY = "timetable.faculty_max_daily_sessions";
    private static final String WEEKLY_KEY = "timetable.faculty_max_weekly_sessions";
    private static final String CONTINUOUS_KEY = "timetable.faculty_max_continuous_sessions";
    private static final String MIN_WEEKLY_KEY = "timetable.faculty_min_weekly_sessions";
    private static final String CATEGORY = "TIMETABLE";

    private final SystemConfigurationService systemConfigurationService;

    public FacultyWorkloadRulesService(SystemConfigurationService systemConfigurationService) {
        this.systemConfigurationService = systemConfigurationService;
    }

    public FacultyWorkloadRulesResponse get() {
        return new FacultyWorkloadRulesResponse(
            parse(systemConfigurationService.findByKey(DAILY_KEY)),
            parse(systemConfigurationService.findByKey(WEEKLY_KEY)),
            parse(systemConfigurationService.findByKey(CONTINUOUS_KEY)),
            parse(systemConfigurationService.findByKey(MIN_WEEKLY_KEY)));
    }

    @Transactional
    public FacultyWorkloadRulesResponse update(FacultyWorkloadRulesRequest request) {
        upsert(DAILY_KEY, request.maxDailySessions(),
            "Maximum sessions a faculty member can be staffed for in a single day. Blank or 0 = no cap.");
        upsert(WEEKLY_KEY, request.maxWeeklySessions(),
            "Maximum sessions a faculty member can be staffed for across the whole term timetable in one week. Blank or 0 = no cap.");
        upsert(CONTINUOUS_KEY, request.maxContinuousSessions(),
            "Maximum unbroken back-to-back sessions a faculty member can be staffed for in a single day. Blank or 0 = no cap.");
        upsert(MIN_WEEKLY_KEY, request.minWeeklySessions(),
            "Advisory floor: a faculty member below this many sessions/week shows as under-loaded in Assign Faculty and the "
                + "Faculty Workload report. Never blocks staffing. Blank or 0 = no floor.");
        return get();
    }

    private void upsert(String key, Integer value, String description) {
        systemConfigurationService.upsert(new SystemConfigurationRequest(
            key, value != null ? value.toString() : "", description, ConfigDataType.INTEGER, CATEGORY, true));
    }

    /** Mirrors {@code TimetableStaffingService.resolveCapSessions}'s own parsing: blank,
     *  unparseable, or <= 0 all mean "not configured" (null here), never an error. */
    private static Integer parse(Optional<SystemConfigurationResponse> config) {
        return config.map(SystemConfigurationResponse::configValue)
            .filter(value -> value != null && !value.isBlank())
            .flatMap(value -> {
                try {
                    int parsed = Integer.parseInt(value.trim());
                    return parsed > 0 ? Optional.of(parsed) : Optional.<Integer>empty();
                } catch (NumberFormatException e) {
                    return Optional.<Integer>empty();
                }
            })
            .orElse(null);
    }
}
