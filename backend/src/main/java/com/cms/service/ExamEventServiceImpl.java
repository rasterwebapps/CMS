package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ExamEventDto;
import com.cms.dto.ExamEventRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.CourseOffering;
import com.cms.model.ExamEvent;
import com.cms.model.ExamSession;
import com.cms.model.enums.ExamSessionStatus;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.ExamEventRepository;
import com.cms.repository.ExamSessionRepository;

@Service
@Transactional(readOnly = true)
public class ExamEventServiceImpl implements ExamEventService {

    private final ExamEventRepository examEventRepository;
    private final ExamSessionRepository examSessionRepository;
    private final CourseOfferingRepository courseOfferingRepository;

    public ExamEventServiceImpl(ExamEventRepository examEventRepository,
                                 ExamSessionRepository examSessionRepository,
                                 CourseOfferingRepository courseOfferingRepository) {
        this.examEventRepository = examEventRepository;
        this.examSessionRepository = examSessionRepository;
        this.courseOfferingRepository = courseOfferingRepository;
    }

    @Override
    @Transactional
    public ExamEventDto create(ExamEventRequest request) {
        ExamSession session = findSession(request.examSessionId());
        checkNotLocked(session);

        CourseOffering offering = courseOfferingRepository.findById(request.courseOfferingId())
            .orElseThrow(() -> new ResourceNotFoundException("CourseOffering not found: " + request.courseOfferingId()));

        examEventRepository.findByExamSession_IdAndCourseOffering_Id(request.examSessionId(), request.courseOfferingId())
            .ifPresent(e -> { throw new IllegalStateException(
                "ExamEvent already exists for this session and course offering"); });

        ExamEvent event = new ExamEvent(session, offering, request.examDate(),
            request.maxMarks(), request.passMarks());
        return toDto(examEventRepository.save(event));
    }

    @Override
    public ExamEventDto getById(Long id) {
        return toDto(findEvent(id));
    }

    @Override
    public List<ExamEventDto> getByExamSession(Long examSessionId) {
        return examEventRepository.findByExamSession_Id(examSessionId).stream()
            .map(this::toDto).toList();
    }

    @Override
    public List<ExamEventDto> getByTermInstance(Long termInstanceId) {
        return examEventRepository.findByExamSession_TermInstance_Id(termInstanceId).stream()
            .map(this::toDto).toList();
    }

    @Override
    @Transactional
    public ExamEventDto update(Long id, ExamEventRequest request) {
        ExamEvent event = findEvent(id);
        checkNotLocked(event.getExamSession());
        event.setExamDate(request.examDate());
        event.setMaxMarks(request.maxMarks());
        event.setPassMarks(request.passMarks());
        return toDto(examEventRepository.save(event));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ExamEvent event = findEvent(id);
        checkNotLocked(event.getExamSession());
        examEventRepository.delete(event);
    }

    private void checkNotLocked(ExamSession session) {
        if (session.getStatus() == ExamSessionStatus.LOCKED) {
            throw new IllegalStateException("Cannot modify events in a LOCKED exam session");
        }
    }

    private ExamSession findSession(Long id) {
        return examSessionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ExamSession not found: " + id));
    }

    private ExamEvent findEvent(Long id) {
        return examEventRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ExamEvent not found: " + id));
    }

    private ExamEventDto toDto(ExamEvent e) {
        return new ExamEventDto(e.getId(), e.getExamSession().getId(),
            e.getExamSession().getSessionType(), e.getExamSession().getStatus(),
            e.getCourseOffering().getId(),
            e.getCourseOffering().getSubject().getName(),
            e.getCourseOffering().getSubject().getCode(),
            e.getExamDate(), e.getMaxMarks(), e.getPassMarks());
    }
}
