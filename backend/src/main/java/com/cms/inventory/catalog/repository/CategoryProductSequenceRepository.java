package com.cms.inventory.catalog.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cms.inventory.catalog.model.CategoryProductSequence;

import jakarta.persistence.LockModeType;

@Repository
public interface CategoryProductSequenceRepository extends JpaRepository<CategoryProductSequence, Long> {

    Optional<CategoryProductSequence> findByCategoryId(Long categoryId);

    /** Pessimistic write lock to serialize concurrent Product-code generation for the same category. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM CategoryProductSequence s WHERE s.categoryId = :categoryId")
    Optional<CategoryProductSequence> findByCategoryIdForUpdate(@Param("categoryId") Long categoryId);
}
