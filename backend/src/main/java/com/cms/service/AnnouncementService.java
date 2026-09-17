package com.cms.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AnnouncementAudienceRequest;
import com.cms.dto.AnnouncementAudienceResponse;
import com.cms.dto.AnnouncementRequest;
import com.cms.dto.AnnouncementResponse;
import com.cms.dto.WardSummaryResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AppUser;
import com.cms.model.Announcement;
import com.cms.model.AnnouncementAudience;
import com.cms.model.AnnouncementRead;
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

/**
 * Institution-wide Announcements (OC-255). An announcement is created once with one or more
 * audience targets (ROLE / COHORT / SECTION / ALL); a viewer's feed is computed at read time by
 * matching their own resolved identity (role id, and — for a student or a guardian's linked
 * ward(s) — cohort id and active section id(s)) against every announcement's audience rows,
 * mirroring how {@link com.cms.model.Notification} visibility is computed at read time rather
 * than fanned out to per-user rows at creation.
 */
@Service
@Transactional(readOnly = true)
public class AnnouncementService {

    private static final List<Long> NO_MATCH = List.of(-1L);

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementAudienceRepository audienceRepository;
    private final AnnouncementReadRepository readRepository;
    private final AppUserRepository appUserRepository;
    private final AppRoleRepository appRoleRepository;
    private final CohortRepository cohortRepository;
    private final CohortSectionRepository cohortSectionRepository;
    private final BatchRepository batchRepository;
    private final StudentRepository studentRepository;
    private final GuardianService guardianService;

    public AnnouncementService(AnnouncementRepository announcementRepository,
                                AnnouncementAudienceRepository audienceRepository,
                                AnnouncementReadRepository readRepository,
                                AppUserRepository appUserRepository,
                                AppRoleRepository appRoleRepository,
                                CohortRepository cohortRepository,
                                CohortSectionRepository cohortSectionRepository,
                                BatchRepository batchRepository,
                                StudentRepository studentRepository,
                                GuardianService guardianService) {
        this.announcementRepository = announcementRepository;
        this.audienceRepository = audienceRepository;
        this.readRepository = readRepository;
        this.appUserRepository = appUserRepository;
        this.appRoleRepository = appRoleRepository;
        this.cohortRepository = cohortRepository;
        this.cohortSectionRepository = cohortSectionRepository;
        this.batchRepository = batchRepository;
        this.studentRepository = studentRepository;
        this.guardianService = guardianService;
    }

    @Transactional
    public AnnouncementResponse create(AnnouncementRequest request, String authorUsername) {
        Announcement announcement = new Announcement(request.title(), request.body(), authorUsername);
        for (AnnouncementAudienceRequest ar : request.audiences()) {
            validateAudience(ar);
            announcement.addAudience(new AnnouncementAudience(ar.audienceType(), ar.audienceRefId()));
        }
        return toResponse(announcementRepository.save(announcement), false);
    }

    /** Admin-facing full list, newest first. */
    public List<AnnouncementResponse> findAll() {
        return announcementRepository.findAll(Sort.by(Sort.Direction.DESC, "publishedAt")).stream()
            .map(a -> toResponse(a, false))
            .toList();
    }

    /** Current authenticated user's own feed -- every announcement whose audience matches their
     *  role, or (for a student, or a guardian's linked ward(s)) their cohort/active section --
     *  never a client-supplied identity. Empty (not an error) for an unlinked/unknown caller. */
    public List<AnnouncementResponse> findMyFeed(String keycloakUsername) {
        AppUser user = appUserRepository.findByKeycloakUsername(keycloakUsername).orElse(null);
        if (user == null) {
            return List.of();
        }

        List<Long> roleIds = user.getAppRole() != null ? List.of(user.getAppRole().getId()) : NO_MATCH;
        List<Long> cohortIds = resolveCohortIds(user);
        List<Long> sectionIds = resolveSectionIds(user);

        List<Long> matchingIds = audienceRepository.findMatchingAnnouncementIds(
            roleIds, orNoMatch(cohortIds), orNoMatch(sectionIds));
        if (matchingIds.isEmpty()) {
            return List.of();
        }

        Set<Long> readIds = readRepository.findByUserIdAndAnnouncementIdIn(keycloakUsername, matchingIds).stream()
            .map(r -> r.getAnnouncement().getId())
            .collect(Collectors.toSet());

        return announcementRepository.findAllById(matchingIds).stream()
            .sorted(Comparator.comparing(Announcement::getPublishedAt).reversed())
            .map(a -> toResponse(a, readIds.contains(a.getId())))
            .toList();
    }

    @Transactional
    public void markRead(Long announcementId, String keycloakUsername) {
        if (!announcementRepository.existsById(announcementId)) {
            throw new ResourceNotFoundException("Announcement not found with id: " + announcementId);
        }
        if (!readRepository.existsByAnnouncementIdAndUserId(announcementId, keycloakUsername)) {
            readRepository.save(new AnnouncementRead(
                announcementRepository.getReferenceById(announcementId), keycloakUsername));
        }
    }

    public long unreadCount(String keycloakUsername) {
        return findMyFeed(keycloakUsername).stream().filter(a -> !a.read()).count();
    }

    private void validateAudience(AnnouncementAudienceRequest ar) {
        switch (ar.audienceType()) {
            case ALL -> {
                if (ar.audienceRefId() != null) {
                    throw new IllegalArgumentException("ALL audience must not carry a ref id");
                }
            }
            case ROLE -> {
                if (ar.audienceRefId() == null || !appRoleRepository.existsById(ar.audienceRefId())) {
                    throw new IllegalArgumentException("Unknown role id: " + ar.audienceRefId());
                }
            }
            case COHORT -> {
                if (ar.audienceRefId() == null || !cohortRepository.existsById(ar.audienceRefId())) {
                    throw new IllegalArgumentException("Unknown cohort id: " + ar.audienceRefId());
                }
            }
            case SECTION -> {
                if (ar.audienceRefId() == null || !cohortSectionRepository.existsById(ar.audienceRefId())) {
                    throw new IllegalArgumentException("Unknown section id: " + ar.audienceRefId());
                }
            }
        }
    }

    private List<Long> resolveCohortIds(AppUser user) {
        List<Long> ids = new ArrayList<>();
        if (user.getLinkedStudent() != null && user.getLinkedStudent().getCohort() != null) {
            ids.add(user.getLinkedStudent().getCohort().getId());
        }
        if (user.getLinkedGuardian() != null) {
            for (WardSummaryResponse ward : guardianService.findMyWards(user.getKeycloakUsername())) {
                studentRepository.findById(ward.studentId())
                    .map(s -> s.getCohort())
                    .ifPresent(c -> ids.add(c.getId()));
            }
        }
        return ids;
    }

    private List<Long> resolveSectionIds(AppUser user) {
        List<Long> ids = new ArrayList<>();
        if (user.getLinkedStudent() != null) {
            ids.addAll(batchRepository.findActiveSectionIdsByStudentId(user.getLinkedStudent().getId()));
        }
        if (user.getLinkedGuardian() != null) {
            for (WardSummaryResponse ward : guardianService.findMyWards(user.getKeycloakUsername())) {
                ids.addAll(batchRepository.findActiveSectionIdsByStudentId(ward.studentId()));
            }
        }
        return ids;
    }

    private List<Long> orNoMatch(List<Long> ids) {
        return ids.isEmpty() ? NO_MATCH : ids;
    }

    private AnnouncementResponse toResponse(Announcement a, boolean read) {
        List<AnnouncementAudienceResponse> audiences = a.getAudiences().stream()
            .map(this::toAudienceResponse)
            .toList();
        return new AnnouncementResponse(a.getId(), a.getTitle(), a.getBody(), a.getCreatedBy(),
            a.getPublishedAt(), audiences, read);
    }

    private AnnouncementAudienceResponse toAudienceResponse(AnnouncementAudience aa) {
        String label = switch (aa.getAudienceType()) {
            case ALL -> "Everyone";
            case ROLE -> appRoleRepository.findById(aa.getAudienceRefId())
                .map(r -> r.getDisplayName()).orElse("Unknown role");
            case COHORT -> cohortRepository.findById(aa.getAudienceRefId())
                .map(c -> c.getDisplayName()).orElse("Unknown cohort");
            case SECTION -> cohortSectionRepository.findById(aa.getAudienceRefId())
                .map(s -> s.getSectionLabel()).orElse("Unknown section");
        };
        return new AnnouncementAudienceResponse(aa.getAudienceType(), aa.getAudienceRefId(), label);
    }
}
