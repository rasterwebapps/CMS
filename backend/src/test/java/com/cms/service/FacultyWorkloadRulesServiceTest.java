package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.FacultyWorkloadRulesRequest;
import com.cms.dto.FacultyWorkloadRulesResponse;
import com.cms.dto.SystemConfigurationRequest;
import com.cms.dto.SystemConfigurationResponse;
import com.cms.model.enums.ConfigDataType;

@ExtendWith(MockitoExtension.class)
class FacultyWorkloadRulesServiceTest {

    @Mock
    private SystemConfigurationService systemConfigurationService;

    private FacultyWorkloadRulesService service;

    private SystemConfigurationResponse configResponse(String key, String value) {
        return new SystemConfigurationResponse(1L, key, value, "desc", ConfigDataType.INTEGER, "TIMETABLE", true, null, null);
    }

    @Test
    void getResolvesAllFourKeysAsSessionCounts() {
        service = new FacultyWorkloadRulesService(systemConfigurationService);
        when(systemConfigurationService.findByKey("timetable.faculty_max_daily_sessions"))
            .thenReturn(Optional.of(configResponse("timetable.faculty_max_daily_sessions", "7")));
        when(systemConfigurationService.findByKey("timetable.faculty_max_weekly_sessions"))
            .thenReturn(Optional.of(configResponse("timetable.faculty_max_weekly_sessions", "35")));
        when(systemConfigurationService.findByKey("timetable.faculty_max_continuous_sessions"))
            .thenReturn(Optional.of(configResponse("timetable.faculty_max_continuous_sessions", "4")));
        when(systemConfigurationService.findByKey("timetable.faculty_min_weekly_sessions"))
            .thenReturn(Optional.of(configResponse("timetable.faculty_min_weekly_sessions", "16")));

        FacultyWorkloadRulesResponse response = service.get();

        assertThat(response.maxDailySessions()).isEqualTo(7);
        assertThat(response.maxWeeklySessions()).isEqualTo(35);
        assertThat(response.maxContinuousSessions()).isEqualTo(4);
        assertThat(response.minWeeklySessions()).isEqualTo(16);
    }

    @Test
    void blankZeroOrUnparseableValuesAllResolveToNotConfigured() {
        service = new FacultyWorkloadRulesService(systemConfigurationService);
        when(systemConfigurationService.findByKey("timetable.faculty_max_daily_sessions"))
            .thenReturn(Optional.of(configResponse("timetable.faculty_max_daily_sessions", "")));
        when(systemConfigurationService.findByKey("timetable.faculty_max_weekly_sessions"))
            .thenReturn(Optional.of(configResponse("timetable.faculty_max_weekly_sessions", "0")));
        when(systemConfigurationService.findByKey("timetable.faculty_max_continuous_sessions"))
            .thenReturn(Optional.of(configResponse("timetable.faculty_max_continuous_sessions", "not-a-number")));
        when(systemConfigurationService.findByKey("timetable.faculty_min_weekly_sessions"))
            .thenReturn(Optional.empty());

        FacultyWorkloadRulesResponse response = service.get();

        assertThat(response.maxDailySessions()).isNull();
        assertThat(response.maxWeeklySessions()).isNull();
        assertThat(response.maxContinuousSessions()).isNull();
        assertThat(response.minWeeklySessions()).isNull();
    }

    @Test
    void updateUpsertsAllFourKeysAsIntegerSessionCounts() {
        service = new FacultyWorkloadRulesService(systemConfigurationService);
        when(systemConfigurationService.upsert(any())).thenReturn(configResponse("k", "v"));
        when(systemConfigurationService.findByKey(any())).thenReturn(Optional.empty());

        service.update(new FacultyWorkloadRulesRequest(7, 35, 4, 16));

        ArgumentCaptor<SystemConfigurationRequest> captor = ArgumentCaptor.forClass(SystemConfigurationRequest.class);
        verify(systemConfigurationService, org.mockito.Mockito.times(4)).upsert(captor.capture());

        assertThat(captor.getAllValues()).extracting(SystemConfigurationRequest::configKey)
            .containsExactlyInAnyOrder(
                "timetable.faculty_max_daily_sessions",
                "timetable.faculty_max_weekly_sessions",
                "timetable.faculty_max_continuous_sessions",
                "timetable.faculty_min_weekly_sessions");
        assertThat(captor.getAllValues()).allSatisfy(req -> {
            assertThat(req.dataType()).isEqualTo(ConfigDataType.INTEGER);
            assertThat(req.configValue()).doesNotContain(".");
        });
    }

    @Test
    void updateWithNullValueStoresBlankNotZero() {
        service = new FacultyWorkloadRulesService(systemConfigurationService);
        when(systemConfigurationService.upsert(any())).thenReturn(configResponse("k", "v"));
        when(systemConfigurationService.findByKey(any())).thenReturn(Optional.empty());

        service.update(new FacultyWorkloadRulesRequest(null, null, null, null));

        ArgumentCaptor<SystemConfigurationRequest> captor = ArgumentCaptor.forClass(SystemConfigurationRequest.class);
        verify(systemConfigurationService, org.mockito.Mockito.times(4)).upsert(captor.capture());
        assertThat(captor.getAllValues()).extracting(SystemConfigurationRequest::configValue).containsOnly("");
    }
}
