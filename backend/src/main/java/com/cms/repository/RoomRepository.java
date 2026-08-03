package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByZoneIdOrderByOrderIndexAsc(Long zoneId);
    List<Room> findByZoneIdAndIsActiveTrueOrderByOrderIndexAsc(Long zoneId);

    boolean existsByZoneIdAndRoomNumberIgnoreCase(Long zoneId, String roomNumber);
    boolean existsByZoneIdAndRoomNumberIgnoreCaseAndIdNot(Long zoneId, String roomNumber, Long id);

    /** Flat, campus-wide (cross-zone/branch) lookup — used to pick a venue by purpose across the
     *  whole org (e.g. a clinical venue that lives under a hospital Branch), unlike the
     *  zone-scoped finders above which only support Campus Setup's own drill-down UI. */
    @Query("SELECT r FROM Room r WHERE r.purposeCategory.id = :purposeCategoryId " +
           "AND (:subTypeId IS NULL OR r.subType.id = :subTypeId) " +
           "AND (:minCapacity IS NULL OR r.capacity >= :minCapacity) " +
           "AND r.isActive = true " +
           "ORDER BY r.capacity ASC")
    List<Room> findByPurpose(@Param("purposeCategoryId") Long purposeCategoryId,
                              @Param("subTypeId") Long subTypeId,
                              @Param("minCapacity") Integer minCapacity);
}
