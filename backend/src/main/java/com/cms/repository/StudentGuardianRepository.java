package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.StudentGuardian;

public interface StudentGuardianRepository extends JpaRepository<StudentGuardian, Long> {

    List<StudentGuardian> findByGuardianId(Long guardianId);

    List<StudentGuardian> findByStudentId(Long studentId);

    boolean existsByStudentIdAndGuardianId(Long studentId, Long guardianId);

    boolean existsByGuardianIdAndStudentId(Long guardianId, Long studentId);

    void deleteByGuardianIdAndStudentId(Long guardianId, Long studentId);
}
