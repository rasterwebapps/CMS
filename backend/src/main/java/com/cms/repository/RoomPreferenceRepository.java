package com.cms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cms.model.RoomPreference;

@Repository
public interface RoomPreferenceRepository extends JpaRepository<RoomPreference, Long>, JpaSpecificationExecutor<RoomPreference> {

    Optional<RoomPreference> findByEnquiryId(Long enquiryId);
    Optional<RoomPreference> findByStudentId(Long studentId);
}
