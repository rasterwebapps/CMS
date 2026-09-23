package com.cms.inventory.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.cms.inventory.catalog.model.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {

    boolean existsByNameIgnoreCaseAndParentCategoryId(String name, Long parentCategoryId);
    boolean existsByNameIgnoreCaseAndParentCategoryIdAndIdNot(String name, Long parentCategoryId, Long id);

    boolean existsByNameIgnoreCaseAndParentCategoryIdIsNull(String name);
    boolean existsByNameIgnoreCaseAndParentCategoryIdIsNullAndIdNot(String name, Long id);

    boolean existsByShortCodeIgnoreCase(String shortCode);
    boolean existsByShortCodeIgnoreCaseAndIdNot(String shortCode, Long id);

    boolean existsByParentCategoryId(Long parentCategoryId);

    List<Category> findByIsActiveTrueOrderByNameAsc();

    List<Category> findAllByOrderByNameAsc();

    /** Categories that already have at least one product but no short code set — the hard-stop
     *  precondition for ProductCodeGeneratorService.regenerateAllCodes. */
    @Query("SELECT DISTINCT c FROM Category c WHERE c.shortCode IS NULL AND EXISTS "
         + "(SELECT 1 FROM Product p WHERE p.category = c)")
    List<Category> findCategoriesWithProductsMissingShortCode();
}
