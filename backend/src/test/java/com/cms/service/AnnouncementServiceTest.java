package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.AnnouncementAudienceRequest;
import com.cms.dto.AnnouncementRequest;
import com.cms.dto.AnnouncementResponse;
import com.cms.dto.WardSummaryResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AppRole;
import com.cms.model.AppUser;
import com.cms.model.Announcement;
import com.cms.model.AnnouncementAudience;
import com.cms.model.AnnouncementRead;
import com.cms.model.Cohort;
import com.cms.model.Guardian;
import com.cms.model.Student;
import com.cms.model.enums.AnnouncementAudienceType;
import com.cms.repository.AnnouncementAudienceRepository;
import com.cms.repository.AnnouncementReadRepository;
import com.cms.repository.AnnouncementRepository;
import com.cms.repository.AppRoleRepository;
import com.cms.repository.AppUserRepository;
import com.cms.repository.BatchRepository;
import com.cms.repository.CohortRepository;
import com.cms.repository.CohortSectionRepository;
import com.cms.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock private AnnouncementRepository announcementRepository;
    @Mock private AnnouncementAudienceRepository audienceRepository;
    @Mock private AnnouncementReadRepository readRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private AppRoleRepository appRoleRepository;
    @Mock private CohortRepository cohortRepository;
    @Mock private CohortSectionRepository cohortSectionRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private GuardianService guardianService;

    private AnnouncementService service;

    @BeforeEach
    void setUp() {
        service = new AnnouncementService(announcementRepository, audienceRepository, readRepository,
            appUserRepository, appRoleRepository, cohortRepository, cohortSectionRepository,
            batchRepository, studentRepository, guardianService);
    }

    private AppRole role(Long id, String name) {
        AppRole r = new AppRole(name, name, 5, false, "");
        r.setId(id);
        return r;
    }

    private Student student(Long id, Cohort cohort) {
        Student s = new Student();
        s.setId(id);
        s.setCohort(cohort);
        return s;
    }

    private Cohort cohort(Long id) {
        Cohort c = new Cohort();
        c.setId(id);
        return c;
    }

    // ---- create() audience validation ----

    @Test
    void shouldCreateAnnouncementWithAllAudience() {
        AnnouncementRequest request = new AnnouncementRequest("Title", "Body",
            List.of(new AnnouncementAudienceRequest(AnnouncementAudienceType.ALL, null)));
        when(announcementRepository.save(any(Announcement.class))).thenAnswer(inv -> inv.getArgument(0));

        AnnouncementResponse response = service.create(request, "admin1");

        assertThat(response.title()).isEqualTo("Title");
        assertThat(response.audiences()).hasSize(1);
        assertThat(response.audiences().get(0).audienceLabel()).isEqualTo("Everyone");
    }

    @Test
    void shouldRejectAllAudienceCarryingRefId() {
        AnnouncementRequest request = new AnnouncementRequest("Title", "Body",
            List.of(new AnnouncementAudienceRequest(AnnouncementAudienceType.ALL, 5L)));

        assertThatThrownBy(() -> service.create(request, "admin1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not carry a ref id");

        verify(announcementRepository, never()).save(any());
    }

    @Test
    void shouldRejectRoleAudienceWithUnknownRoleId() {
        AnnouncementRequest request = new AnnouncementRequest("Title", "Body",
            List.of(new AnnouncementAudienceRequest(AnnouncementAudienceType.ROLE, 999L)));
        when(appRoleRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.create(request, "admin1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown role id");
    }

    @Test
    void shouldRejectCohortAudienceWithUnknownCohortId() {
        AnnouncementRequest request = new AnnouncementRequest("Title", "Body",
            List.of(new AnnouncementAudienceRequest(AnnouncementAudienceType.COHORT, 999L)));
        when(cohortRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.create(request, "admin1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown cohort id");
    }

    @Test
    void shouldRejectSectionAudienceWithUnknownSectionId() {
        AnnouncementRequest request = new AnnouncementRequest("Title", "Body",
            List.of(new AnnouncementAudienceRequest(AnnouncementAudienceType.SECTION, 999L)));
        when(cohortSectionRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.create(request, "admin1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown section id");
    }

    @Test
    void shouldAcceptValidRoleAudience() {
        AnnouncementRequest request = new AnnouncementRequest("Title", "Body",
            List.of(new AnnouncementAudienceRequest(AnnouncementAudienceType.ROLE, 6L)));
        when(appRoleRepository.existsById(6L)).thenReturn(true);
        when(appRoleRepository.findById(6L)).thenReturn(Optional.of(role(6L, "PARENT")));
        when(announcementRepository.save(any(Announcement.class))).thenAnswer(inv -> inv.getArgument(0));

        AnnouncementResponse response = service.create(request, "admin1");

        assertThat(response.audiences().get(0).audienceLabel()).isEqualTo("PARENT");
    }

    // ---- findMyFeed() ----

    @Test
    void shouldReturnEmptyFeedForUnknownCaller() {
        when(appUserRepository.findByKeycloakUsername("ghost")).thenReturn(Optional.empty());

        assertThat(service.findMyFeed("ghost")).isEmpty();
    }

    @Test
    void shouldMatchFeedByStudentsOwnCohortAndSection() {
        AppUser user = new AppUser();
        user.setAppRole(role(6L, "STUDENT"));
        user.setLinkedStudent(student(10L, cohort(3L)));
        when(appUserRepository.findByKeycloakUsername("stud1")).thenReturn(Optional.of(user));
        when(batchRepository.findActiveSectionIdsByStudentId(10L)).thenReturn(List.of(7L));

        Announcement a = new Announcement("Title", "Body", "admin1");
        a.addAudience(new AnnouncementAudience(AnnouncementAudienceType.COHORT, 3L));
        when(audienceRepository.findMatchingAnnouncementIds(eq(List.of(6L)), eq(List.of(3L)), eq(List.of(7L))))
            .thenReturn(List.of(100L));
        when(readRepository.findByUserIdAndAnnouncementIdIn(eq("stud1"), anyList())).thenReturn(List.of());
        when(announcementRepository.findAllById(List.of(100L))).thenReturn(List.of(a));

        List<AnnouncementResponse> feed = service.findMyFeed("stud1");

        assertThat(feed).hasSize(1);
        assertThat(feed.get(0).read()).isFalse();
    }

    @Test
    void shouldMatchFeedThroughGuardiansLinkedWard() {
        AppUser user = new AppUser();
        user.setAppRole(role(20L, "PARENT"));
        Guardian g = new Guardian();
        g.setId(1L);
        user.setLinkedGuardian(g);
        user.setKeycloakUsername("parent1");
        when(appUserRepository.findByKeycloakUsername("parent1")).thenReturn(Optional.of(user));
        when(guardianService.findMyWards("parent1")).thenReturn(
            List.of(new WardSummaryResponse(45L, "Ward One", "R1", true)));
        when(studentRepository.findById(45L)).thenReturn(Optional.of(student(45L, cohort(9L))));
        when(batchRepository.findActiveSectionIdsByStudentId(45L)).thenReturn(List.of());

        when(audienceRepository.findMatchingAnnouncementIds(eq(List.of(20L)), eq(List.of(9L)), eq(List.of(-1L))))
            .thenReturn(List.of());

        List<AnnouncementResponse> feed = service.findMyFeed("parent1");

        assertThat(feed).isEmpty();
    }

    // ---- markRead() ----

    @Test
    void shouldRecordReadOnFirstMark() {
        when(announcementRepository.existsById(5L)).thenReturn(true);
        when(announcementRepository.getReferenceById(5L)).thenReturn(new Announcement("T", "B", "admin1"));
        when(readRepository.existsByAnnouncementIdAndUserId(5L, "stud1")).thenReturn(false);

        service.markRead(5L, "stud1");

        verify(readRepository).save(any(AnnouncementRead.class));
    }

    @Test
    void shouldNotDuplicateReadRecordOnRepeatMark() {
        when(announcementRepository.existsById(5L)).thenReturn(true);
        when(readRepository.existsByAnnouncementIdAndUserId(5L, "stud1")).thenReturn(true);

        service.markRead(5L, "stud1");

        verify(readRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenMarkingUnknownAnnouncementRead() {
        when(announcementRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.markRead(99L, "stud1"))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
