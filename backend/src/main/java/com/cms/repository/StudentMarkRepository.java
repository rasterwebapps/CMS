package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.model.StudentMark;

@Repository
public interface StudentMarkRepository extends JpaRepository<StudentMark, Long> {

    List<StudentMark> findByExamEvent_Id(Long examEventId);

    List<StudentMark> findByCourseRegistration_StudentTermEnrollment_Id(Long enrollmentId);

    Optional<StudentMark> findByExamEvent_IdAndCourseRegistration_Id(Long examEventId, Long courseRegistrationId);
}
