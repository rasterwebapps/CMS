package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.model.HolidayTemplate;

@Repository
public interface HolidayTemplateRepository extends JpaRepository<HolidayTemplate, Long>,
        JpaSpecificationExecutor<HolidayTemplate> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<HolidayTemplate> findByIsActiveTrue();
}
