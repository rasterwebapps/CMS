package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.cms.model.Lab;

public interface LabRepository extends JpaRepository<Lab, Long>, JpaSpecificationExecutor<Lab> {

    List<Lab> findBySpecialityId(Long specialityId);

    boolean existsByNameAndSpecialityId(String name, Long specialityId);

    boolean existsByNameIgnoreCaseAndSpecialityId(String name, Long specialityId);

    boolean existsByNameIgnoreCaseAndSpecialityIdAndIdNot(String name, Long specialityId, Long id);
}
