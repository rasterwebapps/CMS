package com.cms.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.model.Notification;
import com.cms.model.TermInstance;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.repository.NotificationRepository;
import com.cms.repository.TermInstanceRepository;

/**
 * BR-53: warns admins ahead of time when a term is about to start but is still PLANNED (never
 * advanced to OPEN) — the exact failure mode that let 2025-2026 silently stall behind 2026-2027.
 * Runs daily; alerts are idempotent per term (one active row at a time) and auto-resolve once an
 * admin actually advances the term's status, regardless of whether anyone dismissed the alert.
 */
@Service
public class AcademicTermAlertService {

    private static final String CATEGORY_KEY = "academicTermAlerts";
    private static final String SOURCE_TYPE = "TERM_INSTANCE";
    private static final int LEAD_DAYS = 14;

    private final TermInstanceRepository termInstanceRepository;
    private final NotificationRepository notificationRepository;

    public AcademicTermAlertService(TermInstanceRepository termInstanceRepository,
                                     NotificationRepository notificationRepository) {
        this.termInstanceRepository = termInstanceRepository;
        this.notificationRepository = notificationRepository;
    }

    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void checkOverdueTerms() {
        raiseAlertsForPlannedTermsStartingSoon();
        resolveAlertsForTermsNoLongerPlanned();
    }

    private void raiseAlertsForPlannedTermsStartingSoon() {
        LocalDate cutoff = LocalDate.now().plusDays(LEAD_DAYS);
        List<TermInstance> planned = termInstanceRepository.findByStatus(TermInstanceStatus.PLANNED);
        for (TermInstance term : planned) {
            if (term.getStartDate().isAfter(cutoff)) {
                continue;
            }
            boolean alreadyAlerted = notificationRepository
                .findBySourceTypeAndSourceIdAndCategoryKeyAndResolvedAtIsNull(SOURCE_TYPE, term.getId(), CATEGORY_KEY)
                .isPresent();
            if (alreadyAlerted) {
                continue;
            }
            notificationRepository.save(new Notification(
                CATEGORY_KEY,
                "Term not yet opened: " + termLabel(term),
                termLabel(term) + " starts on " + term.getStartDate()
                    + " but is still PLANNED. Advance it to OPEN so course offerings, enrollment,"
                    + " and fee collection are ready before the term begins.",
                "/academic-years/" + term.getAcademicYear().getId() + "/edit",
                SOURCE_TYPE,
                term.getId()
            ));
        }
    }

    private void resolveAlertsForTermsNoLongerPlanned() {
        List<Notification> active = notificationRepository.findByCategoryKeyAndResolvedAtIsNull(CATEGORY_KEY);
        for (Notification notification : active) {
            termInstanceRepository.findById(notification.getSourceId())
                .filter(term -> term.getStatus() != TermInstanceStatus.PLANNED)
                .ifPresent(term -> notification.setResolvedAt(java.time.Instant.now()));
        }
    }

    private String termLabel(TermInstance term) {
        return term.getAcademicYear().getName() + " " + term.getTermType();
    }
}
