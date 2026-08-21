package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.Batch;

public interface BatchRepository extends JpaRepository<Batch, Long> {

    List<Batch> findByCourseOfferingId(Long courseOfferingId);

    List<Batch> findByCohortRoomAllocationId(Long cohortRoomAllocationId);

    boolean existsByCourseOfferingIdAndName(Long courseOfferingId, String name);

    @Query("SELECT COUNT(s) FROM Batch b JOIN b.students s WHERE b.id = :batchId")
    long countStudents(@Param("batchId") Long batchId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM Batch b JOIN b.students s WHERE b.id = :batchId AND s.id = :studentId")
    boolean existsStudentInBatch(@Param("batchId") Long batchId, @Param("studentId") Long studentId);

    @Query("SELECT b FROM Batch b JOIN b.students s WHERE b.termInstance.id = :termInstanceId AND s.id = :studentId")
    List<Batch> findByTermInstanceIdAndStudentId(@Param("termInstanceId") Long termInstanceId, @Param("studentId") Long studentId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM Batch b JOIN b.students s WHERE b.courseOffering.id = :courseOfferingId")
    boolean existsAnyStudentInBatchesForOffering(@Param("courseOfferingId") Long courseOfferingId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM Batch b JOIN b.students s WHERE b.courseOffering.subject.id = :subjectId")
    boolean existsAnyStudentInBatchesForSubject(@Param("subjectId") Long subjectId);
}
