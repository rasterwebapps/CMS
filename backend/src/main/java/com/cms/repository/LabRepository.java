package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.Lab;

public interface LabRepository extends JpaRepository<Lab, Long> {

    List<Lab> findBySpecialityId(Long specialityId);

    boolean existsByNameAndSpecialityId(String name, Long specialityId);

    boolean existsByNameIgnoreCaseAndSpecialityId(String name, Long specialityId);

    boolean existsByNameIgnoreCaseAndSpecialityIdAndIdNot(String name, Long specialityId, Long id);
}
