package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.cms.dto.GuardianRequest;
import com.cms.dto.GuardianResponse;
import com.cms.dto.WardSummaryResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AppUser;
import com.cms.model.Guardian;
import com.cms.model.Student;
import com.cms.model.StudentGuardian;
import com.cms.repository.AppUserRepository;
import com.cms.repository.GuardianRepository;
import com.cms.repository.StudentGuardianRepository;
import com.cms.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class GuardianServiceTest {

    @Mock
    private GuardianRepository guardianRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private StudentGuardianRepository studentGuardianRepository;
    @Mock
    private AppUserRepository appUserRepository;

    private GuardianService guardianService;

    @BeforeEach
    void setUp() {
        guardianService = new GuardianService(guardianRepository, studentRepository,
            studentGuardianRepository, appUserRepository);
    }

    private Guardian guardian(Long id, String email) {
        Guardian g = new Guardian();
        g.setId(id);
        g.setFirstName("Test");
        g.setLastName("Guardian");
        g.setEmail(email);
        g.setCreatedAt(Instant.now());
        return g;
    }

    private Student student(Long id, String first, String last, String roll) {
        Student s = new Student();
        s.setId(id);
        s.setFirstName(first);
        s.setLastName(last);
        s.setRollNumber(roll);
        return s;
    }

    @Test
    void shouldCreateGuardianWhenEmailNotAlreadyRegistered() {
        GuardianRequest request = new GuardianRequest("Test", "Guardian", "new@test.com", "999", "Mother");
        when(guardianRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(guardianRepository.save(any(Guardian.class))).thenAnswer(inv -> {
            Guardian g = inv.getArgument(0);
            g.setId(1L);
            g.setCreatedAt(Instant.now());
            return g;
        });

        GuardianResponse response = guardianService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("new@test.com");
    }

    @Test
    void shouldRejectDuplicateGuardianEmail() {
        GuardianRequest request = new GuardianRequest("Test", "Guardian", "dup@test.com", null, null);
        when(guardianRepository.existsByEmail("dup@test.com")).thenReturn(true);

        assertThatThrownBy(() -> guardianService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already registered");

        verify(guardianRepository, never()).save(any());
    }

    @Test
    void shouldLinkGuardianToStudent() {
        Guardian g = guardian(1L, "g@test.com");
        Student s = student(10L, "Ward", "One", "R1");
        when(guardianRepository.findById(1L)).thenReturn(Optional.of(g));
        when(studentRepository.findById(10L)).thenReturn(Optional.of(s));
        when(studentGuardianRepository.existsByStudentIdAndGuardianId(10L, 1L)).thenReturn(false);

        guardianService.linkToStudent(1L, 10L, true);

        verify(studentGuardianRepository).save(any(StudentGuardian.class));
    }

    @Test
    void shouldRejectDuplicateLink() {
        Guardian g = guardian(1L, "g@test.com");
        Student s = student(10L, "Ward", "One", "R1");
        when(guardianRepository.findById(1L)).thenReturn(Optional.of(g));
        when(studentRepository.findById(10L)).thenReturn(Optional.of(s));
        when(studentGuardianRepository.existsByStudentIdAndGuardianId(10L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> guardianService.linkToStudent(1L, 10L, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already linked");

        verify(studentGuardianRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenLinkingNonExistentGuardian() {
        when(guardianRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guardianService.linkToStudent(99L, 10L, false))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldReturnMultipleWardsForLinkedGuardian() {
        Guardian g = guardian(1L, "g@test.com");
        AppUser appUser = new AppUser();
        appUser.setLinkedGuardian(g);

        StudentGuardian linkA = new StudentGuardian();
        linkA.setStudent(student(45L, "Oviya", "Thangam", "GNM4-015"));
        linkA.setGuardian(g);
        linkA.setPrimary(true);

        StudentGuardian linkB = new StudentGuardian();
        linkB.setStudent(student(46L, "Pavithra", "Umapathy", "GNM4-016"));
        linkB.setGuardian(g);
        linkB.setPrimary(false);

        when(appUserRepository.findByKeycloakUsername("parent1")).thenReturn(Optional.of(appUser));
        when(studentGuardianRepository.findByGuardianId(1L)).thenReturn(List.of(linkA, linkB));

        List<WardSummaryResponse> wards = guardianService.findMyWards("parent1");

        assertThat(wards).hasSize(2);
        assertThat(wards).extracting(WardSummaryResponse::studentId).containsExactly(45L, 46L);
        assertThat(wards.get(0).isPrimary()).isTrue();
        assertThat(wards.get(1).isPrimary()).isFalse();
    }

    @Test
    void shouldReturnEmptyWardsWhenCallerHasNoLinkedGuardian() {
        AppUser appUser = new AppUser();
        when(appUserRepository.findByKeycloakUsername("admin1")).thenReturn(Optional.of(appUser));

        List<WardSummaryResponse> wards = guardianService.findMyWards("admin1");

        assertThat(wards).isEmpty();
    }

    @Test
    void shouldAllowAccessToActualWard() {
        Guardian g = guardian(1L, "g@test.com");
        AppUser appUser = new AppUser();
        appUser.setLinkedGuardian(g);
        when(appUserRepository.findByKeycloakUsername("parent1")).thenReturn(Optional.of(appUser));
        when(studentGuardianRepository.existsByGuardianIdAndStudentId(1L, 45L)).thenReturn(true);

        guardianService.assertIsMyWard("parent1", 45L);
        // No exception thrown == pass.
    }

    @Test
    void shouldRejectAccessToStudentThatIsNotAWard() {
        Guardian g = guardian(1L, "g@test.com");
        AppUser appUser = new AppUser();
        appUser.setLinkedGuardian(g);
        when(appUserRepository.findByKeycloakUsername("parent1")).thenReturn(Optional.of(appUser));
        when(studentGuardianRepository.existsByGuardianIdAndStudentId(1L, 999L)).thenReturn(false);

        assertThatThrownBy(() -> guardianService.assertIsMyWard("parent1", 999L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("not one of your wards");
    }

    @Test
    void shouldRejectWardCheckWhenCallerHasNoLinkedGuardian() {
        AppUser appUser = new AppUser();
        when(appUserRepository.findByKeycloakUsername("nobody")).thenReturn(Optional.of(appUser));

        assertThatThrownBy(() -> guardianService.assertIsMyWard("nobody", 45L))
            .isInstanceOf(ResponseStatusException.class);
    }
}
