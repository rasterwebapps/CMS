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
                Faculty f = faculty.get();
                return new ProfileIdentity(
                    "FACULTY", f.getId(), null, null,
                    f.getFullName(), email,
                    f.getBio(), f.getPhone(), f.getBloodGroup()
                );
            }

            var student = studentRepository.findByEmail(email);
            if (student.isPresent()) {
                Student st = student.get();
                Long admissionId = admissionRepository
                    .findByStudentId(st.getId()).map(a -> a.getId()).orElse(null);
                Long programId = st.getProgram() != null ? st.getProgram().getId() : null;
                return new ProfileIdentity(
                    "STUDENT", st.getId(), admissionId, programId,
                    st.getFullName(), email,
                    st.getBio(), st.getPhone(), st.getBloodGroup()
                );
            }
        }

        // Admin — personal info stored on app_users
        String adminBio = null; String adminPhone = null; String adminBlood = null;
        try {
            AppUser adminUser = resolveAppUser();
            adminBio   = adminUser.getBio();
            adminPhone = adminUser.getPhone();
            adminBlood = adminUser.getBloodGroup();
        } catch (Exception ignored) {}
        return new ProfileIdentity("ADMIN", null, null, null, display, email,
                                  adminBio, adminPhone, adminBlood);
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

    // ── Cover photo ────────────────────────────────────────────────────────────

    public ResponseEntity<byte[]> getCoverPhoto() {
        AppUser user = resolveAppUser();
        if (user.getCoverPhoto() == null || user.getCoverPhoto().length == 0) {
            return ResponseEntity.noContent().build();
        }
        String contentType = user.getCoverPhotoType() == null || user.getCoverPhotoType().isBlank()
            ? MediaType.IMAGE_JPEG_VALUE : user.getCoverPhotoType();
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .body(user.getCoverPhoto());
    }

    @Transactional
    public void uploadCoverPhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File is required");
        String contentType = file.getContentType();
        if (!MediaType.IMAGE_JPEG_VALUE.equals(contentType) && !MediaType.IMAGE_PNG_VALUE.equals(contentType)) {
            throw new IllegalArgumentException("Only JPEG or PNG images are allowed");
        }
        if (file.getSize() > MAX_PHOTO_BYTES) throw new IllegalArgumentException("Cover photo exceeds the 2 MB limit");
        AppUser user = resolveAppUser();
        try { user.setCoverPhoto(file.getBytes()); } catch (IOException ex) { throw new IllegalArgumentException("Unable to read uploaded photo", ex); }
        user.setCoverPhotoType(contentType);
        appUserRepository.save(user);
    }

    @Transactional
    public void deleteCoverPhoto() {
        AppUser user = resolveAppUser();
        user.setCoverPhoto(null);
        user.setCoverPhotoType(null);
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
            return;
        }

        // Admin user — bio, phone, bloodGroup are self-editable
        AppUser user = resolveAppUser();
        boolean changed = false;
        if (request.bio() != null)        { user.setBio(trimToNull(request.bio()));               changed = true; }
        if (request.phone() != null)      { user.setPhone(trimToNull(request.phone()));            changed = true; }
        if (request.bloodGroup() != null) { user.setBloodGroup(trimToNull(request.bloodGroup())); changed = true; }
        if (request.emergencyContactName() != null)         { user.setEmergencyContactName(trimToNull(request.emergencyContactName()));               changed = true; }
        if (request.emergencyContactRelationship() != null) { user.setEmergencyContactRelationship(trimToNull(request.emergencyContactRelationship())); changed = true; }
        if (request.emergencyContactPhone() != null)        { user.setEmergencyContactPhone(trimToNull(request.emergencyContactPhone()));             changed = true; }
        if (changed) appUserRepository.save(user);
    }

    private void applySelfFields(Faculty faculty, SelfUpdateRequest request) {
        if (request.phone() != null)      faculty.setPhone(trimToNull(request.phone()));
        if (request.bloodGroup() != null) faculty.setBloodGroup(trimToNull(request.bloodGroup()));
        if (request.bio() != null)        faculty.setBio(trimToNull(request.bio()));
        if (request.emergencyContactName() != null)         faculty.setEmergencyContactName(trimToNull(request.emergencyContactName()));
        if (request.emergencyContactRelationship() != null) faculty.setEmergencyContactRelationship(trimToNull(request.emergencyContactRelationship()));
        if (request.emergencyContactPhone() != null)        faculty.setEmergencyContactPhone(trimToNull(request.emergencyContactPhone()));
        faculty.setAddress(mergeAddress(faculty.getAddress(), request));
    }

    private void applySelfFields(Student student, SelfUpdateRequest request) {
        if (request.phone() != null)      student.setPhone(trimToNull(request.phone()));
        if (request.bloodGroup() != null) student.setBloodGroup(trimToNull(request.bloodGroup()));
        if (request.bio() != null)        student.setBio(trimToNull(request.bio()));
        if (request.emergencyContactName() != null)         student.setEmergencyContactName(trimToNull(request.emergencyContactName()));
        if (request.emergencyContactRelationship() != null) student.setEmergencyContactRelationship(trimToNull(request.emergencyContactRelationship()));
        if (request.emergencyContactPhone() != null)        student.setEmergencyContactPhone(trimToNull(request.emergencyContactPhone()));
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
