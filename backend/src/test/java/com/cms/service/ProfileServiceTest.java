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

import com.cms.dto.SelfUpdateRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AppUser;
import com.cms.model.Faculty;
import com.cms.model.Student;
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
            "9876543210", "O+", null, "Main Road", "Erode", "Erode", "Tamil Nadu", "638001");

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
            "9123456780", "AB+", null, null, "Salem", null, "Tamil Nadu", null);

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
            new SelfUpdateRequest("1", "B+", null, null, null, null, null, null)))
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
            new UsernamePasswordAuthenticationToken(jwt, null));
    }
}

