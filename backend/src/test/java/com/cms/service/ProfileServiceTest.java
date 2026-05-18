package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import com.cms.dto.ProfileIdentity;
import com.cms.dto.SelfUpdateRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AppUser;
import com.cms.model.Faculty;
import com.cms.model.Student;
import com.cms.model.Program;
import com.cms.model.enums.StudentStatus;
import com.cms.repository.AdmissionRepository;
import com.cms.repository.AppUserRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private FacultyRepository facultyRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private AdmissionRepository admissionRepository;

    @Mock
    private AppUserRepository appUserRepository;

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(
            facultyRepository, studentRepository, admissionRepository, appUserRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadPhotoStoresImageOnCurrentAppUser() {
        setJwt("faculty.user", "faculty@college.edu");
        AppUser user = new AppUser();
        when(appUserRepository.findByKeycloakUsername("faculty.user")).thenReturn(Optional.of(user));

        MockMultipartFile file = new MockMultipartFile(
            "file", "avatar.jpg", "image/jpeg", new byte[] {1, 2, 3});

        profileService.uploadPhoto(file);

        assertThat(user.getProfilePhoto()).containsExactly(1, 2, 3);
        assertThat(user.getProfilePhotoType()).isEqualTo("image/jpeg");
        verify(appUserRepository).save(user);
    }

    @Test
    void uploadPhotoRejectsNonImageMimeType() {
        setJwt("faculty.user", "faculty@college.edu");
        MockMultipartFile file = new MockMultipartFile(
            "file", "avatar.pdf", "application/pdf", new byte[] {1});

        assertThatThrownBy(() -> profileService.uploadPhoto(file))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("JPEG or PNG");
    }

    @Test
    void uploadPhotoRejectsOversizedPhoto() {
        setJwt("faculty.user", "faculty@college.edu");
        byte[] oversized = new byte[(2 * 1024 * 1024) + 1];
        MockMultipartFile file = new MockMultipartFile(
            "file", "avatar.jpg", "image/jpeg", oversized);

        assertThatThrownBy(() -> profileService.uploadPhoto(file))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("2 MB");
    }

    @Test
    void getPhotoReturnsNoContentWhenMissing() {
        setJwt("student.user", "student@college.edu");
        AppUser user = new AppUser();
        when(appUserRepository.findByKeycloakUsername("student.user")).thenReturn(Optional.of(user));

        var response = profileService.getPhoto();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void getPhotoReturnsImageWhenPresent() {
        setJwt("student.user", "student@college.edu");
        AppUser user = new AppUser();
        user.setProfilePhoto(new byte[] {9, 8, 7});
        user.setProfilePhotoType("image/png");
        when(appUserRepository.findByKeycloakUsername("student.user")).thenReturn(Optional.of(user));

        var response = profileService.getPhoto();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(9, 8, 7);
    }

    @Test
    void deletePhotoClearsStoredImage() {
        setJwt("admin", "admin@cms.local");
        AppUser user = new AppUser();
        user.setProfilePhoto(new byte[] {1});
        user.setProfilePhotoType("image/png");
        when(appUserRepository.findByKeycloakUsername("admin")).thenReturn(Optional.of(user));

        profileService.deletePhoto();

        assertThat(user.getProfilePhoto()).isNull();
        assertThat(user.getProfilePhotoType()).isNull();
        verify(appUserRepository).save(user);
    }

    @Test
    void updateSelfInfoUpdatesOnlyFacultyWhitelistedFields() {
        setJwt("faculty.user", "faculty@college.edu");
        Faculty faculty = new Faculty();
        when(facultyRepository.findByEmail("faculty@college.edu")).thenReturn(Optional.of(faculty));

        SelfUpdateRequest request = new SelfUpdateRequest(
            "9876543210", "O+", null, null, "Main Road", "Erode", "Erode", "Tamil Nadu", "638001",
            null, null, null);

        profileService.updateSelfInfo(request);

        assertThat(faculty.getPhone()).isEqualTo("9876543210");
        assertThat(faculty.getBloodGroup()).isEqualTo("O+");
        assertThat(faculty.getAddress().getCity()).isEqualTo("Erode");
        verify(facultyRepository).save(faculty);
        verify(studentRepository, never()).save(any());
    }

    @Test
    void updateSelfInfoUpdatesStudentWhenEmailMatchesStudent() {
        setJwt("student.user", "student@college.edu");
        Student student = new Student();
        when(facultyRepository.findByEmail("student@college.edu")).thenReturn(Optional.empty());
        when(studentRepository.findByEmail("student@college.edu")).thenReturn(Optional.of(student));

        SelfUpdateRequest request = new SelfUpdateRequest(
            "9123456780", "AB+", null, null, null, "Salem", null, "Tamil Nadu", null,
            null, null, null);

        profileService.updateSelfInfo(request);

        assertThat(student.getPhone()).isEqualTo("9123456780");
        assertThat(student.getBloodGroup()).isEqualTo("AB+");
        assertThat(student.getAddress().getCity()).isEqualTo("Salem");
        verify(studentRepository).save(student);
    }

    @Test
    void updateSelfInfoDoesNothingForAdminWithoutEmail() {
        setJwt("devadmin", null);

        assertThatCode(() -> profileService.updateSelfInfo(
            new SelfUpdateRequest("1", "B+", null, null, null, null, null, null, null, null, null, null)))
            .doesNotThrowAnyException();

        verify(facultyRepository, never()).save(any());
        verify(studentRepository, never()).save(any());
    }

    @Test
    void getPhotoFailsWhenAppUserCannotBeResolved() {
        setJwt("missing", "missing@college.edu");
        when(appUserRepository.findByKeycloakUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getPhoto())
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User not found");
    }

    // ─── Cover photo tests ───────────────────────────────────────────────────

    @Test
    void getCoverPhotoReturnsNoContentWhenMissing() {
        setJwt("user1", "user1@college.edu");
        AppUser user = new AppUser();
        user.setCoverPhoto(null);
        when(appUserRepository.findByKeycloakUsername("user1")).thenReturn(Optional.of(user));

        var response = profileService.getCoverPhoto();

        assertThat(response.getStatusCode().value()).isEqualTo(204);
    }

    @Test
    void getCoverPhotoReturnsImageWhenPresent() {
        setJwt("user1", "user1@college.edu");
        AppUser user = new AppUser();
        user.setCoverPhoto(new byte[]{10, 20, 30});
        user.setCoverPhotoType("image/jpeg");
        when(appUserRepository.findByKeycloakUsername("user1")).thenReturn(Optional.of(user));

        var response = profileService.getCoverPhoto();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsExactly(10, 20, 30);
    }

    @Test
    void getCoverPhotoUsesDefaultContentTypeWhenNull() {
        setJwt("user1", "user1@college.edu");
        AppUser user = new AppUser();
        user.setCoverPhoto(new byte[]{1});
        user.setCoverPhotoType(null);
        when(appUserRepository.findByKeycloakUsername("user1")).thenReturn(Optional.of(user));

        var response = profileService.getCoverPhoto();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void uploadCoverPhotoStoresImageOnUser() {
        setJwt("user1", "user1@college.edu");
        AppUser user = new AppUser();
        when(appUserRepository.findByKeycloakUsername("user1")).thenReturn(Optional.of(user));

        MockMultipartFile file = new MockMultipartFile(
            "cover", "cover.jpg", "image/jpeg", new byte[]{5, 6, 7});

        profileService.uploadCoverPhoto(file);

        assertThat(user.getCoverPhoto()).containsExactly(5, 6, 7);
        verify(appUserRepository).save(user);
    }

    @Test
    void uploadCoverPhotoRejectsNonImageFile() {
        setJwt("user1", "user1@college.edu");

        MockMultipartFile file = new MockMultipartFile(
            "cover", "doc.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> profileService.uploadCoverPhoto(file))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("JPEG or PNG");
    }

    @Test
    void uploadCoverPhotoRejectsNullFile() {
        setJwt("user1", "user1@college.edu");

        assertThatThrownBy(() -> profileService.uploadCoverPhoto(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("required");
    }

    @Test
    void deleteCoverPhotoClearsStoredImage() {
        setJwt("user1", "user1@college.edu");
        AppUser user = new AppUser();
        user.setCoverPhoto(new byte[]{1, 2, 3});
        user.setCoverPhotoType("image/jpeg");
        when(appUserRepository.findByKeycloakUsername("user1")).thenReturn(Optional.of(user));

        profileService.deleteCoverPhoto();

        assertThat(user.getCoverPhoto()).isNull();
        assertThat(user.getCoverPhotoType()).isNull();
        verify(appUserRepository).save(user);
    }

    @Test
    void resolveCurrentUserReturnsFacultyIdentityWhenEmailMatchesFaculty() {
        setJwt("faculty.user", "faculty@college.edu");
        Faculty faculty = new Faculty();
        faculty.setId(5L);
        faculty.setFirstName("Jane");
        faculty.setLastName("Smith");
        faculty.setBio("Faculty bio");
        faculty.setPhone("9876543210");
        when(facultyRepository.findByEmail("faculty@college.edu")).thenReturn(Optional.of(faculty));

        ProfileIdentity identity = profileService.resolveCurrentUser();

        assertThat(identity.entityType()).isEqualTo("FACULTY");
        assertThat(identity.entityId()).isEqualTo(5L);
        assertThat(identity.email()).isEqualTo("faculty@college.edu");
    }

    @Test
    void resolveCurrentUserReturnsStudentIdentityWhenEmailMatchesStudent() {
        setJwt("student.user", "student@college.edu");
        when(facultyRepository.findByEmail("student@college.edu")).thenReturn(Optional.empty());

        Student student = new Student();
        student.setId(10L);
        student.setFirstName("Bob");
        student.setLastName("Jones");
        when(studentRepository.findByEmail("student@college.edu")).thenReturn(Optional.of(student));
        when(admissionRepository.findByStudentId(10L)).thenReturn(Optional.empty());

        ProfileIdentity identity = profileService.resolveCurrentUser();

        assertThat(identity.entityType()).isEqualTo("STUDENT");
        assertThat(identity.entityId()).isEqualTo(10L);
        assertThat(identity.admissionId()).isNull();
    }

    @Test
    void resolveCurrentUserReturnsAdminIdentityWhenNeitherFacultyNorStudent() {
        setJwt("admin", "admin@cms.edu");
        when(facultyRepository.findByEmail("admin@cms.edu")).thenReturn(Optional.empty());
        when(studentRepository.findByEmail("admin@cms.edu")).thenReturn(Optional.empty());
        AppUser adminUser = new AppUser();
        adminUser.setBio("Admin bio");
        when(appUserRepository.findByKeycloakUsername("admin")).thenReturn(Optional.of(adminUser));

        ProfileIdentity identity = profileService.resolveCurrentUser();

        assertThat(identity.entityType()).isEqualTo("ADMIN");
        assertThat(identity.entityId()).isNull();
        assertThat(identity.email()).isEqualTo("admin@cms.edu");
    }

    @Test
    void resolveCurrentUserReturnsAdminWhenEmailIsNull() {
        setJwt("noemail", null);
        AppUser adminUser = new AppUser();
        when(appUserRepository.findByKeycloakUsername("noemail")).thenReturn(Optional.of(adminUser));

        ProfileIdentity identity = profileService.resolveCurrentUser();

        assertThat(identity.entityType()).isEqualTo("ADMIN");
    }

    @Test
    void updateSelfInfoUpdatesAdminFields() {
        setJwt("devadmin", "admin@college.edu");
        when(facultyRepository.findByEmail("admin@college.edu")).thenReturn(Optional.empty());
        when(studentRepository.findByEmail("admin@college.edu")).thenReturn(Optional.empty());
        AppUser user = new AppUser();
        when(appUserRepository.findByKeycloakUsername("devadmin")).thenReturn(Optional.of(user));

        profileService.updateSelfInfo(
            new SelfUpdateRequest("9876543210", "B+", null, "My bio", null, null, null, null, null, null, null, null));

        assertThat(user.getPhone()).isEqualTo("9876543210");
        assertThat(user.getBloodGroup()).isEqualTo("B+");
        verify(appUserRepository).save(user);
    }

    private void setJwt(String username, String email) {
        var builder = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("preferred_username", username)
            .claim("name", "Test User");
        if (email != null) {
            builder.claim("email", email);
        }
        Jwt jwt = builder.build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(jwt, null, java.util.List.of()));
    }
}

