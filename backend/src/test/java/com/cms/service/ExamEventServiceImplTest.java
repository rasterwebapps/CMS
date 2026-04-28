package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.ExamEventRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.CourseOffering;
import com.cms.model.ExamEvent;
import com.cms.model.ExamSession;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.ExamSessionStatus;
import com.cms.model.enums.ExamSessionType;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.ExamEventRepository;
import com.cms.repository.ExamSessionRepository;

@ExtendWith(MockitoExtension.class)
class ExamEventServiceImplTest {

    @Mock
    private ExamEventRepository examEventRepository;
    @Mock
    private ExamSessionRepository examSessionRepository;
    @Mock
    private CourseOfferingRepository courseOfferingRepository;

    private ExamEventServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ExamEventServiceImpl(examEventRepository, examSessionRepository, courseOfferingRepository);
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

    private ExamSession createSession(Long id, TermInstance ti, ExamSessionStatus status) {
        ExamSession s = new ExamSession(ti, ExamSessionType.INTERNAL1, status,
            LocalDate.of(2024, 10, 1), LocalDate.of(2024, 10, 30));
        s.setId(id);
        return s;
    }

    private Subject createSubject(Long id) {
        Subject s = new Subject();
        s.setId(id);
        s.setName("Mathematics");
        s.setCode("MATH101");
        return s;
    }

    private CourseOffering createOffering(Long id, TermInstance ti, Subject subject) {
        CourseOffering o = new CourseOffering();
        o.setId(id);
        o.setTermInstance(ti);
        o.setSubject(subject);
        o.setSemesterNumber(1);
        o.setIsActive(true);
        o.setCreatedAt(Instant.now());
        o.setUpdatedAt(Instant.now());
        return o;
    }

    private ExamEvent createEvent(Long id, ExamSession session, CourseOffering offering) {
        ExamEvent e = new ExamEvent(session, offering, LocalDate.of(2024, 10, 10),
            new BigDecimal("100"), new BigDecimal("40"));
        e.setId(id);
        return e;
    }

    @Test
    void create_success() {
        AcademicYear ay = createAY(1L, "2024-2025");
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti, ExamSessionStatus.PUBLISHED);
        Subject subject = createSubject(1L);
        CourseOffering offering = createOffering(1L, ti, subject);

        ExamEventRequest request = new ExamEventRequest(1L, 1L, LocalDate.of(2024, 10, 10),
            new BigDecimal("100"), new BigDecimal("40"));

        when(examSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(courseOfferingRepository.findById(1L)).thenReturn(Optional.of(offering));
        when(examEventRepository.findByExamSession_IdAndCourseOffering_Id(1L, 1L))
            .thenReturn(Optional.empty());
        when(examEventRepository.save(any(ExamEvent.class))).thenAnswer(inv -> {
            ExamEvent ev = inv.getArgument(0);
            ev.setId(1L);
            return ev;
        });

        var result = service.create(request);

        assertThat(result.maxMarks()).isEqualByComparingTo(new BigDecimal("100"));
        verify(examEventRepository).save(any(ExamEvent.class));
    }

    @Test
    void create_lockedSession_throws() {
        AcademicYear ay = createAY(1L, "2024-2025");
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti, ExamSessionStatus.LOCKED);

        ExamEventRequest request = new ExamEventRequest(1L, 1L, null,
            new BigDecimal("100"), new BigDecimal("40"));
        when(examSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("LOCKED");
    }

    @Test
    void create_sessionNotFound_throws() {
        ExamEventRequest request = new ExamEventRequest(99L, 1L, null,
            new BigDecimal("50"), new BigDecimal("20"));
        when(examSessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_offeringNotFound_throws() {
        AcademicYear ay = createAY(1L, "2024-2025");
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti, ExamSessionStatus.DRAFT);

        ExamEventRequest request = new ExamEventRequest(1L, 99L, null,
            new BigDecimal("50"), new BigDecimal("20"));
        when(examSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(courseOfferingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_duplicateEvent_throws() {
        AcademicYear ay = createAY(1L, "2024-2025");
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti, ExamSessionStatus.DRAFT);
        Subject subject = createSubject(1L);
        CourseOffering offering = createOffering(1L, ti, subject);
        ExamEvent existing = createEvent(1L, session, offering);

        ExamEventRequest request = new ExamEventRequest(1L, 1L, null,
            new BigDecimal("100"), new BigDecimal("40"));
        when(examSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(courseOfferingRepository.findById(1L)).thenReturn(Optional.of(offering));
        when(examEventRepository.findByExamSession_IdAndCourseOffering_Id(1L, 1L))
            .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void delete_unlockedSession_success() {
        AcademicYear ay = createAY(1L, "2024-2025");
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti, ExamSessionStatus.PUBLISHED);
        Subject subject = createSubject(1L);
        CourseOffering offering = createOffering(1L, ti, subject);
        ExamEvent event = createEvent(1L, session, offering);

        when(examEventRepository.findById(1L)).thenReturn(Optional.of(event));

        service.delete(1L);

        verify(examEventRepository).delete(event);
    }

    @Test
    void delete_lockedSession_throws() {
        AcademicYear ay = createAY(1L, "2024-2025");
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti, ExamSessionStatus.LOCKED);
        Subject subject = createSubject(1L);
        CourseOffering offering = createOffering(1L, ti, subject);
        ExamEvent event = createEvent(1L, session, offering);

        when(examEventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.delete(1L))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getByExamSession_returnsList() {
        AcademicYear ay = createAY(1L, "2024-2025");
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti, ExamSessionStatus.PUBLISHED);
        Subject subject = createSubject(1L);
        CourseOffering offering = createOffering(1L, ti, subject);
        ExamEvent event = createEvent(1L, session, offering);

        when(examEventRepository.findByExamSession_Id(1L)).thenReturn(List.of(event));

        var results = service.getByExamSession(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).subjectName()).isEqualTo("Mathematics");
    }

    @Test
    void getById_success() {
        AcademicYear ay = createAY(1L, "2024-2025");
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti, ExamSessionStatus.PUBLISHED);
        Subject subject = createSubject(1L);
        CourseOffering offering = createOffering(1L, ti, subject);
        ExamEvent event = createEvent(1L, session, offering);

        when(examEventRepository.findById(1L)).thenReturn(Optional.of(event));

        var result = service.getById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.maxMarks()).isEqualByComparingTo(new BigDecimal("100"));
    }

    @Test
    void getById_notFound_throws() {
        when(examEventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByTermInstance_returnsList() {
        AcademicYear ay = createAY(1L, "2024-2025");
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti, ExamSessionStatus.PUBLISHED);
        Subject subject = createSubject(1L);
        CourseOffering offering = createOffering(1L, ti, subject);
        ExamEvent event = createEvent(1L, session, offering);

        when(examEventRepository.findByExamSession_TermInstance_Id(1L)).thenReturn(List.of(event));

        var results = service.getByTermInstance(1L);

        assertThat(results).hasSize(1);
    }

    @Test
    void update_success() {
        AcademicYear ay = createAY(1L, "2024-2025");
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti, ExamSessionStatus.DRAFT);
        Subject subject = createSubject(1L);
        CourseOffering offering = createOffering(1L, ti, subject);
        ExamEvent event = createEvent(1L, session, offering);

        ExamEventRequest request = new ExamEventRequest(1L, 1L, LocalDate.of(2024, 10, 20),
            new BigDecimal("80"), new BigDecimal("32"));

        when(examEventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(examEventRepository.save(any(ExamEvent.class))).thenReturn(event);

        var result = service.update(1L, request);

        assertThat(result).isNotNull();
        verify(examEventRepository).save(event);
    }

    @Test
    void update_lockedSession_throws() {
        AcademicYear ay = createAY(1L, "2024-2025");
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti, ExamSessionStatus.LOCKED);
        Subject subject = createSubject(1L);
        CourseOffering offering = createOffering(1L, ti, subject);
        ExamEvent event = createEvent(1L, session, offering);

        ExamEventRequest request = new ExamEventRequest(1L, 1L, null,
            new BigDecimal("80"), new BigDecimal("32"));
        when(examEventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.update(1L, request))
            .isInstanceOf(IllegalStateException.class);
    }
}
