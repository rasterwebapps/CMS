package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cms.model.FacultyDocumentTypeRequirement;

@Repository
public interface FacultyDocumentTypeRequirementRepository
        extends JpaRepository<FacultyDocumentTypeRequirement, Long> {

    /**
     * Returns distinct document_type names where ANY criterion matches the faculty's attributes.
     * Null faculty attributes never match — null parameters simply skip that clause.
     */
    @Query(value = """
            SELECT DISTINCT document_type
            FROM faculty_document_type_requirements
            WHERE (designation   IS NOT NULL AND designation   = :designation)
               OR (speciality_id IS NOT NULL AND speciality_id = :specialityId)
               OR (qualification IS NOT NULL AND qualification = :qualification)
            """, nativeQuery = true)
    List<String> findMatchingDocumentTypeNames(
            @Param("designation") String designation,
            @Param("specialityId") Long specialityId,
            @Param("qualification") String qualification);
}
