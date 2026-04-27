package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.CourseRegistration;
import com.cms.model.enums.RegistrationStatus;

public interface CourseRegistrationRepository extends JpaRepository<CourseRegistration, Long> {

    List<CourseRegistration> findByStudentTermEnrollmentId(Long enrollmentId);

    List<CourseRegistration> findByCourseOfferingId(Long offeringId);

    Optional<CourseRegistration> findByStudentTermEnrollmentIdAndCourseOfferingId(
        Long enrollmentId, Long offeringId);

    List<CourseRegistration> findByCourseOfferingIdAndStatus(Long offeringId, RegistrationStatus status);
}
