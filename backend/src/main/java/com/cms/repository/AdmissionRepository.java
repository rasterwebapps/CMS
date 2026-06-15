package com.cms.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.Admission;

public interface AdmissionRepository extends JpaRepository<Admission, Long>, JpaSpecificationExecutor<Admission> {

    Optional<Admission> findByStudentId(Long studentId);

    Optional<Admission> findByEnquiryId(Long enquiryId);

    List<Admission> findByJoiningAcademicYearId(Long joiningAcademicYearId);

    List<Admission> findByStudentIdIn(Collection<Long> studentIds);

    boolean existsByStudentId(Long studentId);

    /** Single query replacing findAll() in AdmissionService — eliminates 4N+1 lazy loads. */
    @Query("SELECT DISTINCT a FROM Admission a " +
           "LEFT JOIN FETCH a.student s " +
           "LEFT JOIN FETCH s.program " +
           "LEFT JOIN FETCH s.course " +
           "LEFT JOIN FETCH a.joiningAcademicYear")
    List<Admission> findAllWithRelations();

    /** Used by StudentService and FeeExplorerService — fetches student and joiningAcademicYear eagerly. */
    @Query("SELECT a FROM Admission a LEFT JOIN FETCH a.student LEFT JOIN FETCH a.joiningAcademicYear WHERE a.student.id IN :studentIds")
    List<Admission> findByStudentIdInFetchJoiningYear(@Param("studentIds") Collection<Long> studentIds);

    /** Fetch full admission graph for a known set of IDs — used by explorer pagination (step 2 after spec query). */
    @Query("SELECT DISTINCT a FROM Admission a " +
           "LEFT JOIN FETCH a.student s " +
           "LEFT JOIN FETCH s.program " +
           "LEFT JOIN FETCH s.course " +
           "LEFT JOIN FETCH a.joiningAcademicYear " +
           "WHERE a.id IN :ids")
    List<Admission> findByIdInWithRelations(@Param("ids") Collection<Long> ids);
}
