package com.cms.service;

import java.util.List;

import com.cms.dto.ExamSessionDto;
import com.cms.dto.ExamSessionRequest;

public interface ExamSessionService {

    ExamSessionDto create(ExamSessionRequest request);

    ExamSessionDto getById(Long id);

    List<ExamSessionDto> getByTermInstance(Long termInstanceId);

    ExamSessionDto publish(Long id);

    ExamSessionDto lock(Long id);
}
