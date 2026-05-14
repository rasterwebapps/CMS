package com.cms.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cms.dto.ProfileIdentity;
import com.cms.dto.SelfUpdateRequest;
import com.cms.service.ProfileService;

@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileIdentity> getMyProfile() {
        return ResponseEntity.ok(profileService.resolveCurrentUser());
    }

    @GetMapping("/me/photo")
    public ResponseEntity<byte[]> getMyPhoto() {
        return profileService.getPhoto();
    }

    @PostMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadMyPhoto(@RequestPart("file") MultipartFile file) {
        profileService.uploadPhoto(file);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/me/photo")
    public ResponseEntity<Void> deleteMyPhoto() {
        profileService.deletePhoto();
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/self-info")
    public ResponseEntity<Void> updateSelfInfo(@RequestBody SelfUpdateRequest request) {
        profileService.updateSelfInfo(request);
        return ResponseEntity.ok().build();
    }
}
