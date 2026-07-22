package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.model.HostelRoomType;

@Repository
public interface HostelRoomTypeRepository extends JpaRepository<HostelRoomType, Long>, JpaSpecificationExecutor<HostelRoomType> {

    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    List<HostelRoomType> findAllByOrderByNameAsc();

    List<HostelRoomType> findByIsActiveTrueOrderByNameAsc();
}
