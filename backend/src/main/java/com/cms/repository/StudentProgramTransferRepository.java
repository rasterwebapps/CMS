package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.model.StudentProgramTransfer;

@Repository
public interface StudentProgramTransferRepository extends JpaRepository<StudentProgramTransfer, Long> {

    List<StudentProgramTransfer> findByStudentIdOrderByTransferredAtDesc(Long studentId);
}
