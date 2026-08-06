package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.model.RoomPurposeCategory;
import com.cms.model.enums.RoomPurposeCategoryCode;

@Repository
public interface RoomPurposeCategoryRepository extends JpaRepository<RoomPurposeCategory, Long>, JpaSpecificationExecutor<RoomPurposeCategory> {

    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByCode(RoomPurposeCategoryCode code);

    List<RoomPurposeCategory> findAllByOrderByNameAsc();

    List<RoomPurposeCategory> findByIsActiveTrueOrderByNameAsc();
}
