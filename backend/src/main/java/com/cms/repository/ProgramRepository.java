package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.Program;
import com.cms.model.enums.ProgramStatus;

public interface ProgramRepository extends JpaRepository<Program, Long> {

    Optional<Program> findByCode(String code);

    List<Program> findByStatus(ProgramStatus status);

    boolean existsByCode(String code);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
