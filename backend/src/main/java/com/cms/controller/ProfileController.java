package com.cms.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cms.dto.ProfileIdentity;
import com.cms.dto.SelfUpdateRequest;
import com.cms.model.enums.DocumentType;
import com.cms.service.ProfileDocumentService;
import com.cms.service.ProfileService;

@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final ProfileDocumentService profileDocumentService;

    public ProfileController(ProfileService profileService,
                             ProfileDocumentService profileDocumentService) {
        this.profileService         = profileService;
        this.profileDocumentService = profileDocumentService;
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

    @GetMapping("/me/cover")
    public ResponseEntity<byte[]> getMyCoverPhoto() {
        return profileService.getCoverPhoto();
    }

    @PostMapping(value = "/me/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadMyCoverPhoto(@RequestPart("file") MultipartFile file) {
        profileService.uploadCoverPhoto(file);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/me/cover")
    public ResponseEntity<Void> deleteMyCoverPhoto() {
        profileService.deleteCoverPhoto();
        return ResponseEntity.noContent().build();
    }

    // ── Self-service documents (BR-30) ────────────────────────────────────────
    // Faculty / students can upload, replace, and delete their own documents.
    // Mutation is blocked by the service once a document reaches VERIFIED status.

    @PostMapping(value = "/me/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> uploadMyDocument(
            @RequestParam("documentType") DocumentType documentType,
            @RequestPart("file") MultipartFile file) {
        Object result = profileDocumentService.uploadMyDocument(documentType, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @DeleteMapping("/me/documents/{id}")
    public ResponseEntity<Void> deleteMyDocument(@PathVariable Long id) {
        profileDocumentService.deleteMyDocument(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/documents/{id}/download")
    public ResponseEntity<Resource> downloadMyDocument(@PathVariable Long id) {
        return profileDocumentService.downloadMyDocument(id);
    }
}
