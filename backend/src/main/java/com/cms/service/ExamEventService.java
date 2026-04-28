package com.cms.service;

import java.util.List;

import com.cms.dto.ExamEventDto;
import com.cms.dto.ExamEventRequest;

public interface ExamEventService {

    ExamEventDto create(ExamEventRequest request);

    ExamEventDto getById(Long id);

    List<ExamEventDto> getByExamSession(Long examSessionId);

    List<ExamEventDto> getByTermInstance(Long termInstanceId);

    ExamEventDto update(Long id, ExamEventRequest request);

    void delete(Long id);
}
