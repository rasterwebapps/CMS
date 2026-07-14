package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.CourseRegistration;
import com.cms.model.Student;
import com.cms.model.enums.RegistrationStatus;

public interface CourseRegistrationRepository extends JpaRepository<CourseRegistration, Long> {

    List<CourseRegistration> findByStudentTermEnrollmentId(Long enrollmentId);

    List<CourseRegistration> findByCourseOfferingId(Long offeringId);

    Optional<CourseRegistration> findByStudentTermEnrollmentIdAndCourseOfferingId(
        Long enrollmentId, Long offeringId);

    List<CourseRegistration> findByCourseOfferingIdAndStatus(Long offeringId, RegistrationStatus status);

    @Query("SELECT cr FROM CourseRegistration cr " +
           "WHERE cr.studentTermEnrollment.student.id = :studentId " +
           "AND cr.courseOffering.subject.id = :subjectId " +
           "ORDER BY cr.studentTermEnrollment.termInstance.id DESC")
    List<CourseRegistration> findByStudentIdAndSubjectId(@Param("studentId") Long studentId,
                                                          @Param("subjectId") Long subjectId);

    @Query("SELECT DISTINCT cr.studentTermEnrollment.student FROM CourseRegistration cr " +
           "WHERE cr.courseOffering.subject.id = :subjectId AND cr.status = com.cms.model.enums.RegistrationStatus.REGISTERED")
    List<Student> findRegisteredStudentsBySubjectId(@Param("subjectId") Long subjectId);
}
