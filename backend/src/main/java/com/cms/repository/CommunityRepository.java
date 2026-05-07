package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.Community;

public interface CommunityRepository extends JpaRepository<Community, Long> {
    List<Community> findByIsActiveTrueOrderByNameAsc();
    List<Community> findAllByOrderByNameAsc();
    boolean existsByCode(String code);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    Optional<Community> findByCode(String code);
}

