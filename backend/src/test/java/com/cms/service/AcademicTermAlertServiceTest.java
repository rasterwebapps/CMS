package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.model.AcademicYear;
import com.cms.model.Notification;
import com.cms.model.TermInstance;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.NotificationRepository;
import com.cms.repository.TermInstanceRepository;

@ExtendWith(MockitoExtension.class)
class AcademicTermAlertServiceTest {

    @Mock
    private TermInstanceRepository termInstanceRepository;
    @Mock
    private NotificationRepository notificationRepository;

    private AcademicTermAlertService service;

    @BeforeEach
    void setUp() {
        service = new AcademicTermAlertService(termInstanceRepository, notificationRepository);
    }

    private TermInstance plannedTerm(Long id, LocalDate startDate) {
        AcademicYear ay = new AcademicYear("2025-2026", startDate.minusMonths(1), startDate.plusMonths(11), false);
        ay.setId(1L);
        TermInstance term = new TermInstance(ay, TermType.ODD, startDate, startDate.plusMonths(5),
            TermInstanceStatus.PLANNED);
        term.setId(id);
        return term;
    }

    @Test
    void raisesAlert_forPlannedTermStartingWithinLeadWindow() {
        TermInstance term = plannedTerm(3L, LocalDate.now().plusDays(10));
        when(termInstanceRepository.findByStatus(TermInstanceStatus.PLANNED)).thenReturn(List.of(term));
        when(notificationRepository.findBySourceTypeAndSourceIdAndCategoryKeyAndResolvedAtIsNull(
            "TERM_INSTANCE", 3L, "academicTermAlerts")).thenReturn(Optional.empty());
        when(notificationRepository.findByCategoryKeyAndResolvedAtIsNull("academicTermAlerts"))
            .thenReturn(List.of());

        service.checkOverdueTerms();

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getCategoryKey()).isEqualTo("academicTermAlerts");
        assertThat(captor.getValue().getSourceId()).isEqualTo(3L);
    }

    @Test
    void skipsPlannedTerm_startingBeyondLeadWindow() {
        TermInstance term = plannedTerm(4L, LocalDate.now().plusDays(30));
        when(termInstanceRepository.findByStatus(TermInstanceStatus.PLANNED)).thenReturn(List.of(term));
        when(notificationRepository.findByCategoryKeyAndResolvedAtIsNull("academicTermAlerts"))
            .thenReturn(List.of());

        service.checkOverdueTerms();

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void doesNotDuplicate_whenAnUnresolvedAlertAlreadyExists() {
        TermInstance term = plannedTerm(5L, LocalDate.now().plusDays(2));
        when(termInstanceRepository.findByStatus(TermInstanceStatus.PLANNED)).thenReturn(List.of(term));
        when(notificationRepository.findBySourceTypeAndSourceIdAndCategoryKeyAndResolvedAtIsNull(
            "TERM_INSTANCE", 5L, "academicTermAlerts"))
            .thenReturn(Optional.of(new Notification("academicTermAlerts", "t", "m", "l", "TERM_INSTANCE", 5L)));
        when(notificationRepository.findByCategoryKeyAndResolvedAtIsNull("academicTermAlerts"))
            .thenReturn(List.of());

        service.checkOverdueTerms();

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void resolvesAlert_onceTermIsNoLongerPlanned() {
        Notification alert = new Notification("academicTermAlerts", "t", "m", "l", "TERM_INSTANCE", 6L);
        when(termInstanceRepository.findByStatus(TermInstanceStatus.PLANNED)).thenReturn(List.of());
        when(notificationRepository.findByCategoryKeyAndResolvedAtIsNull("academicTermAlerts"))
            .thenReturn(List.of(alert));

        AcademicYear ay = new AcademicYear("2025-2026", LocalDate.now(), LocalDate.now().plusYears(1), false);
        ay.setId(1L);
        TermInstance openedTerm = new TermInstance(ay, TermType.ODD, LocalDate.now(), LocalDate.now().plusMonths(5),
            TermInstanceStatus.OPEN);
        openedTerm.setId(6L);
        when(termInstanceRepository.findById(6L)).thenReturn(Optional.of(openedTerm));

        service.checkOverdueTerms();

        assertThat(alert.getResolvedAt()).isNotNull();
    }

    @Test
    void leavesAlertUnresolved_ifTermStillPlanned() {
        Notification alert = new Notification("academicTermAlerts", "t", "m", "l", "TERM_INSTANCE", 7L);
        when(termInstanceRepository.findByStatus(TermInstanceStatus.PLANNED)).thenReturn(List.of());
        when(notificationRepository.findByCategoryKeyAndResolvedAtIsNull("academicTermAlerts"))
            .thenReturn(List.of(alert));

        TermInstance stillPlanned = plannedTerm(7L, LocalDate.now().plusDays(5));
        when(termInstanceRepository.findById(7L)).thenReturn(Optional.of(stillPlanned));

        service.checkOverdueTerms();

        assertThat(alert.getResolvedAt()).isNull();
    }
}
