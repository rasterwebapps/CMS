package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.AnnouncementAudienceRequest;
import com.cms.dto.AnnouncementAudienceResponse;
import com.cms.dto.AnnouncementRequest;
import com.cms.dto.AnnouncementResponse;
import com.cms.model.enums.AnnouncementAudienceType;
import com.cms.service.AnnouncementService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = AnnouncementController.class)
@AutoConfigureMockMvc(addFilters = false)
class AnnouncementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AnnouncementService announcementService;

    private AnnouncementResponse response() {
        return new AnnouncementResponse(1L, "Title", "Body", "admin1", Instant.now(),
            List.of(new AnnouncementAudienceResponse(AnnouncementAudienceType.ALL, null, "Everyone")), false);
    }

    @Test
    void shouldCreateAnnouncement() throws Exception {
        AnnouncementRequest request = new AnnouncementRequest("Title", "Body",
            List.of(new AnnouncementAudienceRequest(AnnouncementAudienceType.ALL, null)));
        when(announcementService.create(any(AnnouncementRequest.class), eq(""))).thenReturn(response());

        mockMvc.perform(post("/announcements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Title"));
    }

    @Test
    void shouldRejectCreateWithNoAudience() throws Exception {
        AnnouncementRequest invalid = new AnnouncementRequest("Title", "Body", List.of());

        mockMvc.perform(post("/announcements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldListAllAnnouncements() throws Exception {
        when(announcementService.findAll()).thenReturn(List.of(response()));

        mockMvc.perform(get("/announcements"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void shouldReturnMyFeed() throws Exception {
        when(announcementService.findMyFeed("")).thenReturn(List.of(response()));

        mockMvc.perform(get("/announcements/my"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Title"));
    }

    @Test
    void shouldMarkAnnouncementRead() throws Exception {
        mockMvc.perform(post("/announcements/1/read"))
            .andExpect(status().isNoContent());

        verify(announcementService).markRead(1L, "");
    }

    @Test
    void shouldReturnUnreadCount() throws Exception {
        when(announcementService.unreadCount("")).thenReturn(3L);

        mockMvc.perform(get("/announcements/my/unread-count"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unreadCount").value(3));
    }
}
