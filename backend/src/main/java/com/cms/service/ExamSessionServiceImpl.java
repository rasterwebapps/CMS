package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ExamSessionDto;
import com.cms.dto.ExamSessionRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.ExamSession;
import com.cms.model.TermInstance;
import com.cms.model.enums.ExamSessionStatus;
import com.cms.repository.ExamSessionRepository;
import com.cms.repository.TermInstanceRepository;

@Service
@Transactional(readOnly = true)
public class ExamSessionServiceImpl implements ExamSessionService {

    private final ExamSessionRepository examSessionRepository;
    private final TermInstanceRepository termInstanceRepository;

    public ExamSessionServiceImpl(ExamSessionRepository examSessionRepository,
                                   TermInstanceRepository termInstanceRepository) {
        this.examSessionRepository = examSessionRepository;
        this.termInstanceRepository = termInstanceRepository;
    }

    @Override
    @Transactional
    public ExamSessionDto create(ExamSessionRequest request) {
        TermInstance termInstance = termInstanceRepository.findById(request.termInstanceId())
            .orElseThrow(() -> new ResourceNotFoundException("TermInstance not found: " + request.termInstanceId()));

        examSessionRepository.findByTermInstance_IdAndSessionType(request.termInstanceId(), request.sessionType())
            .ifPresent(s -> { throw new IllegalStateException(
                "ExamSession already exists for this term and session type"); });

        ExamSession session = new ExamSession(termInstance, request.sessionType(),
            ExamSessionStatus.DRAFT, request.startDate(), request.endDate());
        return toDto(examSessionRepository.save(session));
    }

    @Override
    public ExamSessionDto getById(Long id) {
        return toDto(findById(id));
    }

    @Override
    public List<ExamSessionDto> getByTermInstance(Long termInstanceId) {
        return examSessionRepository.findByTermInstance_Id(termInstanceId).stream()
            .map(this::toDto).toList();
    }

    @Override
    @Transactional
    public ExamSessionDto publish(Long id) {
        ExamSession session = findById(id);
        if (session.getStatus() != ExamSessionStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT sessions can be published");
        }
        session.setStatus(ExamSessionStatus.PUBLISHED);
        return toDto(examSessionRepository.save(session));
    }

    @Override
    @Transactional
    public ExamSessionDto lock(Long id) {
        ExamSession session = findById(id);
        if (session.getStatus() != ExamSessionStatus.PUBLISHED) {
            throw new IllegalStateException("Only PUBLISHED sessions can be locked");
        }
        session.setStatus(ExamSessionStatus.LOCKED);
        return toDto(examSessionRepository.save(session));
    }

    private ExamSession findById(Long id) {
        return examSessionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ExamSession not found: " + id));
    }

    private ExamSessionDto toDto(ExamSession s) {
        String label = s.getTermInstance().getAcademicYear().getName()
            + " " + s.getTermInstance().getTermType();
        return new ExamSessionDto(s.getId(), s.getTermInstance().getId(), label,
            s.getSessionType(), s.getStatus(), s.getStartDate(), s.getEndDate());
    }
}
