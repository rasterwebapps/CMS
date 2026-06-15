package com.cms.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.Student;
import com.cms.model.enums.AdmissionCategory;
import com.cms.model.enums.Gender;
import com.cms.model.enums.StudentStatus;

public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {

    @Query("SELECT s FROM Student s " +
           "LEFT JOIN FETCH s.program " +
           "LEFT JOIN FETCH s.course " +
           "LEFT JOIN FETCH s.speciality " +
           "LEFT JOIN FETCH s.cohort c " +
           "LEFT JOIN FETCH c.admissionAcademicYear " +
           "LEFT JOIN FETCH s.address " +
           "WHERE s.id IN :ids")
    List<Student> findByIdInWithRelations(@Param("ids") Collection<Long> ids);

    Optional<Student> findByRollNumber(String rollNumber);

    Optional<Student> findByAdmissionNumber(String admissionNumber);

    Optional<Student> findByEmail(String email);

    boolean existsByRollNumber(String rollNumber);

    boolean existsByAdmissionNumber(String admissionNumber);

    boolean existsByUniversityRegistrationNumber(String universityRegistrationNumber);

    boolean existsByUmisNumber(String umisNumber);

    boolean existsByEmail(String email);

    List<Student> findByProgramId(Long programId);

    List<Student> findByStatus(StudentStatus status);

    List<Student> findByProgramIdAndSemester(Long programId, Integer semester);

    List<Student> findByLabBatch(String labBatch);

    List<Student> findByRollNumberIsNull();

    List<Student> findByCourseIdAndRollNumberIsNull(Long courseId);

    List<Student> findByProgramIdAndRollNumberIsNull(Long programId);

    List<Student> findByRollNumberContainingIgnoreCase(String rollNumber);

    List<Student> findByAdmissionNumberContainingIgnoreCase(String admissionNumber);

    List<Student> findByCohortIdAndStatus(Long cohortId, StudentStatus status);

    List<Student> findByCohortId(Long cohortId);

    boolean existsByCohortId(Long cohortId);

    List<Student> findByCohortAdmissionAcademicYearId(Long academicYearId);

    List<Student> findByProgramIdAndStatus(Long programId, StudentStatus status);

    long countByGender(Gender gender);

    long countByAdmissionCategory(AdmissionCategory admissionCategory);
    long countByCohortIdAndAdmissionCategory(Long cohortId, AdmissionCategory admissionCategory);
    long countByCohortAdmissionAcademicYearIdAndAdmissionCategory(Long academicYearId, AdmissionCategory admissionCategory);
    long countByCohortAdmissionAcademicYearId(Long academicYearId);
}
