package com.cms.service;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cms.dto.ProfileIdentity;
import com.cms.dto.SelfUpdateRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Address;
import com.cms.model.AppUser;
import com.cms.model.Faculty;
import com.cms.model.Student;
import com.cms.repository.AdmissionRepository;
import com.cms.repository.AppUserRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class ProfileService {

    private static final long MAX_PHOTO_BYTES = 2L * 1024 * 1024;

    private final FacultyRepository facultyRepository;
    private final StudentRepository studentRepository;
    private final AdmissionRepository admissionRepository;
    private final AppUserRepository appUserRepository;

    public ProfileService(FacultyRepository facultyRepository,
                          StudentRepository studentRepository,
                          AdmissionRepository admissionRepository,
                          AppUserRepository appUserRepository) {
        this.facultyRepository = facultyRepository;
        this.studentRepository = studentRepository;
        this.admissionRepository = admissionRepository;
        this.appUserRepository = appUserRepository;
    }

    public ProfileIdentity resolveCurrentUser() {
        Jwt jwt = resolveJwt();
        String email    = jwt.getClaimAsString("email");
        String username = jwt.getClaimAsString("preferred_username");
        String fullName = jwt.getClaimAsString("name");
        String display  = fullName != null && !fullName.isBlank() ? fullName : username;

        if (email != null && !email.isBlank()) {
            var faculty = facultyRepository.findByEmail(email);
            if (faculty.isPresent()) {
                return new ProfileIdentity(
                    "FACULTY",
                    faculty.get().getId(),
                    null, null,
                    faculty.get().getFullName(),
                    email
                );
            }

            var student = studentRepository.findByEmail(email);
            if (student.isPresent()) {
                Long admissionId = admissionRepository
                    .findByStudentId(student.get().getId())
                    .map(a -> a.getId())
                    .orElse(null);
                Long programId = student.get().getProgram() != null
                    ? student.get().getProgram().getId() : null;
                return new ProfileIdentity(
                    "STUDENT",
                    student.get().getId(),
                    admissionId,
                    programId,
                    student.get().getFullName(),
                    email
                );
            }
        }

        return new ProfileIdentity("ADMIN", null, null, null, display, email);
    }

    public ResponseEntity<byte[]> getPhoto() {
        AppUser user = resolveAppUser();
        if (user.getProfilePhoto() == null || user.getProfilePhoto().length == 0) {
            return ResponseEntity.noContent().build();
        }
        String contentType = user.getProfilePhotoType() == null || user.getProfilePhotoType().isBlank()
            ? MediaType.IMAGE_JPEG_VALUE
            : user.getProfilePhotoType();
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .body(user.getProfilePhoto());
    }

    @Transactional
    public void uploadPhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        String contentType = file.getContentType();
        if (!MediaType.IMAGE_JPEG_VALUE.equals(contentType) && !MediaType.IMAGE_PNG_VALUE.equals(contentType)) {
            throw new IllegalArgumentException("Only JPEG or PNG images are allowed");
        }
        if (file.getSize() > MAX_PHOTO_BYTES) {
            throw new IllegalArgumentException("Photo exceeds the 2 MB limit");
        }

        AppUser user = resolveAppUser();
        try {
            user.setProfilePhoto(file.getBytes());
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to read uploaded photo", ex);
        }
        user.setProfilePhotoType(contentType);
        appUserRepository.save(user);
    }

    @Transactional
    public void deletePhoto() {
        AppUser user = resolveAppUser();
        user.setProfilePhoto(null);
        user.setProfilePhotoType(null);
        appUserRepository.save(user);
    }

    @Transactional
    public void updateSelfInfo(SelfUpdateRequest request) {
        Jwt jwt = resolveJwt();
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            return;
        }

        var faculty = facultyRepository.findByEmail(email);
        if (faculty.isPresent()) {
            Faculty current = faculty.get();
            applySelfFields(current, request);
            facultyRepository.save(current);
            return;
        }

        var student = studentRepository.findByEmail(email);
        if (student.isPresent()) {
            Student current = student.get();
            applySelfFields(current, request);
            studentRepository.save(current);
        }
    }

    private void applySelfFields(Faculty faculty, SelfUpdateRequest request) {
        if (request.phone() != null) {
            faculty.setPhone(trimToNull(request.phone()));
        }
        if (request.bloodGroup() != null) {
            faculty.setBloodGroup(trimToNull(request.bloodGroup()));
        }
        faculty.setAddress(mergeAddress(faculty.getAddress(), request));
    }

    private void applySelfFields(Student student, SelfUpdateRequest request) {
        if (request.phone() != null) {
            student.setPhone(trimToNull(request.phone()));
        }
        if (request.bloodGroup() != null) {
            student.setBloodGroup(trimToNull(request.bloodGroup()));
        }
        student.setAddress(mergeAddress(student.getAddress(), request));
    }

    private Address mergeAddress(Address existing, SelfUpdateRequest request) {
        Address address = existing == null ? new Address() : existing;
        if (request.postalAddress() != null) address.setPostalAddress(trimToNull(request.postalAddress()));
        if (request.street() != null) address.setStreet(trimToNull(request.street()));
        if (request.city() != null) address.setCity(trimToNull(request.city()));
        if (request.district() != null) address.setDistrict(trimToNull(request.district()));
        if (request.state() != null) address.setState(trimToNull(request.state()));
        if (request.pincode() != null) address.setPincode(trimToNull(request.pincode()));
        return address;
    }

    private AppUser resolveAppUser() {
        Jwt jwt = resolveJwt();
        String username = jwt.getClaimAsString("preferred_username");
        if (username == null || username.isBlank()) {
            throw new ResourceNotFoundException("No authenticated user found");
        }
        return appUserRepository.findByKeycloakUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private Jwt resolveJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new ResourceNotFoundException("No authenticated user found");
        }
        return jwt;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
