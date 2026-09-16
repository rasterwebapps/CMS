package com.cms.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.Guardian;

public interface GuardianRepository extends JpaRepository<Guardian, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);
}
