package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.model.ExamEvent;

@Repository
public interface ExamEventRepository extends JpaRepository<ExamEvent, Long> {

    List<ExamEvent> findByExamSession_Id(Long examSessionId);

    List<ExamEvent> findByExamSession_TermInstance_Id(Long termInstanceId);

    Optional<ExamEvent> findByExamSession_IdAndCourseOffering_Id(Long examSessionId, Long courseOfferingId);
}
