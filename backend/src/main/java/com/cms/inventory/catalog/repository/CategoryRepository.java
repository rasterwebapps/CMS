package com.cms.inventory.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.inventory.catalog.model.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {

    boolean existsByNameIgnoreCaseAndParentCategoryId(String name, Long parentCategoryId);
    boolean existsByNameIgnoreCaseAndParentCategoryIdAndIdNot(String name, Long parentCategoryId, Long id);

    boolean existsByNameIgnoreCaseAndParentCategoryIdIsNull(String name);
    boolean existsByNameIgnoreCaseAndParentCategoryIdIsNullAndIdNot(String name, Long id);

    boolean existsByParentCategoryId(Long parentCategoryId);

    List<Category> findByIsActiveTrueOrderByNameAsc();

    List<Category> findAllByOrderByNameAsc();
}
