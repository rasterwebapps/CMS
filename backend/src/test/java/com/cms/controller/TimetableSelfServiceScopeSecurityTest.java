package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.MyTimetableResponse;
import com.cms.dto.ProfileIdentity;
import com.cms.service.ClassScheduleService;
import com.cms.service.PersonalTimetableService;
import com.cms.service.ProfileService;
import com.cms.service.ResourceGridService;
import com.cms.service.TimetableGenerationService;
import com.cms.service.TimetableOccurrenceService;
import com.cms.service.TimetableSwapService;
import com.cms.service.UserPermissionService;

/**
 * Full-context proof (real {@code SecurityConfig}, real {@code @PreAuthorize} enforcement via
 * {@link com.cms.config.PermSecurityBean}) that {@code MY_TIMETABLE_VIEW} (V515) can only ever
 * unlock {@code GET /timetables/occurrences?scope=personal} and {@code GET /timetables/me} --
 * never {@code scope=browse}, the whole-term listing. A {@code @WebMvcTest} slice with
 * {@code addFilters=false} (as {@link TimetableControllerTest} uses) never actually exercises
 * {@code @PreAuthorize}, so that alone could not catch a regression here; this mirrors
 * {@code ModuleGatingIntegrationTest}'s pattern of a real {@code @SpringBootTest} with only
 * {@link UserPermissionService} and the controller's own service dependencies mocked.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TimetableSelfServiceScopeSecurityTest.TestConfig.class)
class TimetableSelfServiceScopeSecurityTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserPermissionService userPermissionService;

    @MockBean
    private PersonalTimetableService personalTimetableService;

    @MockBean
    private TimetableOccurrenceService timetableOccurrenceService;

    @MockBean
    private ProfileService profileService;

    @MockBean
    private ClassScheduleService classScheduleService;

    @MockBean
    private ResourceGridService resourceGridService;

    @MockBean
    private TimetableGenerationService timetableGenerationService;

    @MockBean
    private TimetableSwapService timetableSwapService;

    private static final ProfileIdentity STUDENT_45 =
        new ProfileIdentity("STUDENT", 45L, null, null, "Test Student", "x@x.com", null, null, null);

    @Test
    void myTimetableViewOnlyCanReadPersonalScope() throws Exception {
        when(userPermissionService.getPermissions("teststudent45")).thenReturn(Set.of("MY_TIMETABLE_VIEW"));
        when(profileService.resolveCurrentUser()).thenReturn(STUDENT_45);
        when(timetableOccurrenceService.findOccurrences(any(), eq(1L), any(), any(), eq("personal")))
            .thenReturn(List.of());

        mockMvc.perform(get("/timetables/occurrences")
                .param("termInstanceId", "1")
                .param("from", "2026-10-01")
                .param("to", "2026-10-07")
                .param("scope", "personal")
                .with(jwt().jwt(j -> j.claim("preferred_username", "teststudent45"))))
            .andExpect(status().isOk());
    }

    @Test
    void myTimetableViewOnlyCannotReadDefaultBrowseScope() throws Exception {
        when(userPermissionService.getPermissions("teststudent45")).thenReturn(Set.of("MY_TIMETABLE_VIEW"));

        mockMvc.perform(get("/timetables/occurrences")
                .param("termInstanceId", "1")
                .param("from", "2026-10-01")
                .param("to", "2026-10-07")
                // no scope param -> defaults to "browse", the whole-term listing
                .with(jwt().jwt(j -> j.claim("preferred_username", "teststudent45"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void myTimetableViewOnlyCannotReadExplicitBrowseScope() throws Exception {
        when(userPermissionService.getPermissions("teststudent45")).thenReturn(Set.of("MY_TIMETABLE_VIEW"));

        mockMvc.perform(get("/timetables/occurrences")
                .param("termInstanceId", "1")
                .param("from", "2026-10-01")
                .param("to", "2026-10-07")
                .param("scope", "browse")
                .with(jwt().jwt(j -> j.claim("preferred_username", "teststudent45"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void myTimetableViewOnlyCanReadMe() throws Exception {
        when(userPermissionService.getPermissions("teststudent45")).thenReturn(Set.of("MY_TIMETABLE_VIEW"));
        when(profileService.resolveCurrentUser()).thenReturn(STUDENT_45);
        when(personalTimetableService.findMyTimetable(eq(STUDENT_45), anyLong(), any()))
            .thenReturn(new MyTimetableResponse(List.of(), List.of()));

        mockMvc.perform(get("/timetables/me")
                .param("termInstanceId", "1")
                .with(jwt().jwt(j -> j.claim("preferred_username", "teststudent45"))))
            .andExpect(status().isOk());
    }

    @Test
    void timetableViewStillReadsBrowseScopeUnaffected() throws Exception {
        // Regression check: existing faculty/admin callers with only the broader TIMETABLE_VIEW
        // must be completely unaffected by MY_TIMETABLE_VIEW's introduction.
        when(userPermissionService.getPermissions("facultyuser")).thenReturn(Set.of("TIMETABLE_VIEW"));
        when(timetableOccurrenceService.findOccurrences(any(), eq(1L), any(), any(), eq("browse")))
            .thenReturn(List.of());

        mockMvc.perform(get("/timetables/occurrences")
                .param("termInstanceId", "1")
                .param("from", "2026-10-01")
                .param("to", "2026-10-07")
                .with(jwt().jwt(j -> j.claim("preferred_username", "facultyuser"))))
            .andExpect(status().isOk());
    }

    @Test
    void noPermissionAtAllIsForbiddenEverywhere() throws Exception {
        when(userPermissionService.getPermissions("nobody")).thenReturn(Set.of());

        mockMvc.perform(get("/timetables/occurrences")
                .param("termInstanceId", "1")
                .param("from", "2026-10-01")
                .param("to", "2026-10-07")
                .param("scope", "personal")
                .with(jwt().jwt(j -> j.claim("preferred_username", "nobody"))))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/timetables/me")
                .param("termInstanceId", "1")
                .with(jwt().jwt(j -> j.claim("preferred_username", "nobody"))))
            .andExpect(status().isForbidden());
    }
}
