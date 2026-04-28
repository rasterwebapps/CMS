package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.ExamSessionRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.ExamSession;
import com.cms.model.TermInstance;
import com.cms.model.enums.ExamSessionStatus;
import com.cms.model.enums.ExamSessionType;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.ExamSessionRepository;
import com.cms.repository.TermInstanceRepository;

@ExtendWith(MockitoExtension.class)
class ExamSessionServiceImplTest {

    @Mock
    private ExamSessionRepository examSessionRepository;
    @Mock
    private TermInstanceRepository termInstanceRepository;

    private ExamSessionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ExamSessionServiceImpl(examSessionRepository, termInstanceRepository);
    }

    private AcademicYear createAY(Long id, String name) {
        AcademicYear ay = new AcademicYear(name, LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31), false);
        ay.setId(id);
        return ay;
    }

    private TermInstance createTermInstance(Long id, AcademicYear ay) {
        TermInstance ti = new TermInstance(ay, TermType.ODD, LocalDate.of(2024, 6, 1),
            LocalDate.of(2024, 11, 30), TermInstanceStatus.OPEN);
        ti.setId(id);
        return ti;
    }

    private ExamSession createSession(Long id, TermInstance ti, ExamSessionType type, ExamSessionStatus status) {
        ExamSession s = new ExamSession(ti, type, status, LocalDate.of(2024, 10, 1), LocalDate.of(2024, 10, 30));
        s.setId(id);
        return s;
    }

    @Test
    void create_success() {
        AcademicYear ay = createAY(1L, "2024-2025");
        TermInstance ti = createTermInstance(1L, ay);
        ExamSessionRequest request = new ExamSessionRequest(1L, ExamSessionType.INTERNAL1,
            LocalDate.of(2024, 10, 1), LocalDate.of(2024, 10, 15));

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(ti));
        when(examSessionRepository.findByTermInstance_IdAndSessionType(1L, ExamSessionType.INTERNAL1))
            .thenReturn(Optional.empty());
        when(examSessionRepository.save(any(ExamSession.class))).thenAnswer(inv -> {
            ExamSession s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        var result = service.create(request);

        assertThat(result.sessionType()).isEqualTo(ExamSessionType.INTERNAL1);
        assertThat(result.status()).isEqualTo(ExamSessionStatus.DRAFT);
        verify(examSessionRepository).save(any(ExamSession.class));
    }

    @Test
    void create_termInstanceNotFound_throws() {
        ExamSessionRequest request = new ExamSessionRequest(99L, ExamSessionType.FINAL, null, null);
        when(termInstanceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_duplicate_throws() {
        AcademicYear ay = createAY(1L, "2024-2025");
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession existing = createSession(1L, ti, ExamSessionType.INTERNAL1, ExamSessionStatus.DRAFT);
        ExamSessionRequest request = new ExamSessionRequest(1L, ExamSessionType.INTERNAL1, null, null);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(ti));
        when(examSessionRepository.findByTermInstance_IdAndSessionType(1L, ExamSessionType.INTERNAL1))
            .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void publish_draftSession_success() {
        AcademicYear ay = createAY(1L, "2024-2025");
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti, ExamSessionType.INTERNAL1, ExamSessionStatus.DRAFT);

        when(examSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(examSessionRepository.save(any(ExamSession.class))).thenReturn(session);

        var result = service.publish(1L);

        assertThat(result.status()).isEqualTo(ExamSessionStatus.PUBLISHED);
    }

    @Test
    void publish_nonDraftSession_throws() {
        AcademicYear ay = createAY(1L, "2024-2025");
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti, ExamSessionType.INTERNAL1, ExamSessionStatus.PUBLISHED);

        when(examSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.publish(1L))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void lock_publishedSession_success() {
        AcademicYear ay = createAY(1L, "2024-2025");
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti, ExamSessionType.FINAL, ExamSessionStatus.PUBLISHED);

        when(examSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(examSessionRepository.save(any(ExamSession.class))).thenReturn(session);

        var result = service.lock(1L);

        assertThat(result.status()).isEqualTo(ExamSessionStatus.LOCKED);
    }

    @Test
    void lock_nonPublishedSession_throws() {
        AcademicYear ay = createAY(1L, "2024-2025");
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti, ExamSessionType.FINAL, ExamSessionStatus.DRAFT);

        when(examSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.lock(1L))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getById_notFound_throws() {
        when(examSessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByTermInstance_returnsList() {
        AcademicYear ay = createAY(1L, "2024-2025");
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession s1 = createSession(1L, ti, ExamSessionType.INTERNAL1, ExamSessionStatus.DRAFT);
        ExamSession s2 = createSession(2L, ti, ExamSessionType.FINAL, ExamSessionStatus.PUBLISHED);

        when(examSessionRepository.findByTermInstance_Id(1L)).thenReturn(List.of(s1, s2));

        var results = service.getByTermInstance(1L);

        assertThat(results).hasSize(2);
    }
}
