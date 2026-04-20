package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.Student;
import com.cms.model.enums.StudentStatus;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByRollNumber(String rollNumber);

    Optional<Student> findByEmail(String email);

    boolean existsByRollNumber(String rollNumber);

    boolean existsByEmail(String email);

    List<Student> findByProgramId(Long programId);

    List<Student> findByStatus(StudentStatus status);

    List<Student> findByProgramIdAndSemester(Long programId, Integer semester);

    List<Student> findByLabBatch(String labBatch);

    List<Student> findByRollNumberIsNull();

    List<Student> findByCourseIdAndRollNumberIsNull(Long courseId);

    List<Student> findByProgramIdAndRollNumberIsNull(Long programId);

    List<Student> findByRollNumberContainingIgnoreCase(String rollNumber);
}
