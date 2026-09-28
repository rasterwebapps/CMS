package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.StudentBoardingStatusSwitch;

public interface StudentBoardingStatusSwitchRepository extends JpaRepository<StudentBoardingStatusSwitch, Long> {

    List<StudentBoardingStatusSwitch> findByStudentIdOrderBySwitchedAtDesc(Long studentId);
}
