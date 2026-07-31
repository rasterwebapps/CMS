package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.model.RoomSubType;

@Repository
public interface RoomSubTypeRepository extends JpaRepository<RoomSubType, Long>, JpaSpecificationExecutor<RoomSubType> {

    boolean existsByNameIgnoreCaseAndPurposeCategoryId(String name, Long purposeCategoryId);
    boolean existsByNameIgnoreCaseAndPurposeCategoryIdAndIdNot(String name, Long purposeCategoryId, Long id);

    boolean existsByCodeIgnoreCaseAndPurposeCategoryId(String code, Long purposeCategoryId);
    boolean existsByCodeIgnoreCaseAndPurposeCategoryIdAndIdNot(String code, Long purposeCategoryId, Long id);

    List<RoomSubType> findByPurposeCategoryIdOrderByNameAsc(Long purposeCategoryId);

    List<RoomSubType> findByPurposeCategoryIdAndIsActiveTrueOrderByNameAsc(Long purposeCategoryId);

    List<RoomSubType> findByIsActiveTrueOrderByNameAsc();
}
