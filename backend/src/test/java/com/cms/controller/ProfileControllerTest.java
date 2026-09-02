package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.ProfileIdentity;
import com.cms.dto.SelfUpdateRequest;
import com.cms.service.ProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = ProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private com.cms.service.ProfileDocumentService profileDocumentService;

    @Test
    void getMyProfileReturnsIdentity() throws Exception {
        when(profileService.resolveCurrentUser()).thenReturn(
            new ProfileIdentity("FACULTY", 1L, null, null, "Dr Test", "test@college.edu", null, null, null));

        mockMvc.perform(get("/profile/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entityType").value("FACULTY"))
            .andExpect(jsonPath("$.entityId").value(1))
            .andExpect(jsonPath("$.displayName").value("Dr Test"));
    }

    @Test
    void getMyPhotoReturnsImageWhenPresent() throws Exception {
        byte[] image = new byte[] {1, 2, 3};
        when(profileService.getPhoto()).thenReturn(
            ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(image));

        mockMvc.perform(get("/profile/me/photo"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.IMAGE_PNG))
            .andExpect(content().bytes(image));
    }

    @Test
    void getMyPhotoReturnsNoContentWhenMissing() throws Exception {
        when(profileService.getPhoto()).thenReturn(ResponseEntity.noContent().build());

        mockMvc.perform(get("/profile/me/photo"))
            .andExpect(status().isNoContent());
    }

    @Test
    void uploadMyPhotoDelegatesToService() throws Exception {
        doNothing().when(profileService).uploadPhoto(any());
        MockMultipartFile file = new MockMultipartFile(
            "file", "avatar.png", "image/png", new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/profile/me/photo").file(file))
            .andExpect(status().isOk());

        verify(profileService).uploadPhoto(any());
    }

    @Test
    void deleteMyPhotoDelegatesToService() throws Exception {
        mockMvc.perform(delete("/profile/me/photo"))
            .andExpect(status().isNoContent());

        verify(profileService).deletePhoto();
    }

    @Test
    void updateSelfInfoDelegatesToService() throws Exception {
        SelfUpdateRequest request = new SelfUpdateRequest(
            "9876543210", "B+", null, null, "Main Road", "Chennai", "Chennai", "Tamil Nadu", "600001",
            null, null, null);

        mockMvc.perform(put("/profile/me/self-info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        verify(profileService).updateSelfInfo(any(SelfUpdateRequest.class));
    }
}

