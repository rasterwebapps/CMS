package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.model.ExamSession;
import com.cms.model.enums.ExamSessionType;

@Repository
public interface ExamSessionRepository extends JpaRepository<ExamSession, Long> {

    List<ExamSession> findByTermInstance_Id(Long termInstanceId);

    Optional<ExamSession> findByTermInstance_IdAndSessionType(Long termInstanceId, ExamSessionType sessionType);
}
