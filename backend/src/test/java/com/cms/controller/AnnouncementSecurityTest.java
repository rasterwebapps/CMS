package com.cms.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.AnnouncementAudienceRequest;
import com.cms.dto.AnnouncementRequest;
import com.cms.model.enums.AnnouncementAudienceType;
import com.cms.service.AnnouncementService;
import com.cms.service.UserPermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Full-context proof (real {@code SecurityConfig}, real {@code @PreAuthorize} enforcement) that
 *  {@code ANNOUNCEMENT_VIEW} and {@code ANNOUNCEMENT_MANAGE} stay properly separated -- a
 *  view-only caller (STUDENT/FACULTY/PARENT) must never be able to author an announcement, per
 *  this project's operation-wise permission mapping hard gate. Same pattern as
 *  {@code GuardianFeeSecurityTest}: a {@code @WebMvcTest(addFilters=false)} slice never actually
 *  exercises {@code @PreAuthorize}, so only a real {@code @SpringBootTest} can catch a
 *  regression here. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AnnouncementSecurityTest.TestConfig.class)
class AnnouncementSecurityTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserPermissionService userPermissionService;

    @MockBean
    private AnnouncementService announcementService;

    @Test
    void viewOnlyCanReadFeedButNotAuthorAnnouncements() throws Exception {
        when(userPermissionService.getPermissions("parent1")).thenReturn(Set.of("ANNOUNCEMENT_VIEW"));
        when(announcementService.findMyFeed("parent1")).thenReturn(List.of());

        mockMvc.perform(get("/announcements/my")
                .with(jwt().jwt(j -> j.claim("preferred_username", "parent1"))))
            .andExpect(status().isOk());

        AnnouncementRequest request = new AnnouncementRequest("Title", "Body",
            List.of(new AnnouncementAudienceRequest(AnnouncementAudienceType.ALL, null)));
        mockMvc.perform(post("/announcements")
                .with(jwt().jwt(j -> j.claim("preferred_username", "parent1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    void manageOnlyCanAuthorButFeedStillRequiresView() throws Exception {
        when(userPermissionService.getPermissions("admin1")).thenReturn(Set.of("ANNOUNCEMENT_MANAGE"));

        mockMvc.perform(get("/announcements/my")
                .with(jwt().jwt(j -> j.claim("preferred_username", "admin1"))))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/announcements")
                .with(jwt().jwt(j -> j.claim("preferred_username", "admin1"))))
            .andExpect(status().isOk());
    }

    @Test
    void noPermissionAtAllIsForbiddenEverywhere() throws Exception {
        when(userPermissionService.getPermissions("nobody")).thenReturn(Set.of());

        mockMvc.perform(get("/announcements/my")
                .with(jwt().jwt(j -> j.claim("preferred_username", "nobody"))))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/announcements/1/read")
                .with(jwt().jwt(j -> j.claim("preferred_username", "nobody"))))
            .andExpect(status().isForbidden());
    }
}
