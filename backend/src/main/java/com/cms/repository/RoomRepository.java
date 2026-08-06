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
     *  zone-scoped finders above which only support Campus Setup's own drill-down UI.
     *
     *  <p>Excludes any Room already linked to another active venue of the SAME type as
     *  {@code venueType} only — e.g. creating a Classroom excludes rooms already taken by another
     *  active Classroom, but a Room already used by a Lab is still offered (a Classroom and a Lab
     *  are allowed to legitimately share one physical space, e.g. used at different times of day;
     *  {@link com.cms.service.TimetableStaffingService} is what actually blocks a real overlapping
     *  double-booking between them, not this picker). Pass {@code venueType} null to skip exclusion
     *  entirely (e.g. a caller that isn't a venue-linking picker at all). Lab has no isActive
     *  boolean like the other two — status &lt;&gt; INACTIVE is its closest equivalent, so
     *  ACTIVE/AVAILABLE/UNDER_MAINTENANCE all still hold the room, only an explicitly INACTIVE lab
     *  releases it. {@code keepRoomId} is the one exception: the venue being edited must still see
     *  its own already-linked Room, not lose it from its own picker just because it's "taken" by
     *  itself. Pass null when creating new. */
    @Query("SELECT r FROM Room r WHERE r.purposeCategory.id = :purposeCategoryId " +
           "AND (:subTypeId IS NULL OR r.subType.id = :subTypeId) " +
           "AND (:minCapacity IS NULL OR r.capacity >= :minCapacity) " +
           "AND r.isActive = true " +
           "AND ( (:keepRoomId IS NOT NULL AND r.id = :keepRoomId) OR (" +
           "  (:venueType IS NULL OR :venueType <> 'CLASSROOM' OR NOT EXISTS (SELECT 1 FROM Classroom c WHERE c.room = r AND c.isActive = true)) " +
           "  AND (:venueType IS NULL OR :venueType <> 'LAB' OR NOT EXISTS (SELECT 1 FROM Lab l WHERE l.room = r AND l.status <> com.cms.model.enums.LabStatus.INACTIVE)) " +
           "  AND (:venueType IS NULL OR :venueType <> 'CLINICAL' OR NOT EXISTS (SELECT 1 FROM ClinicalVenue cv WHERE cv.room = r AND cv.isActive = true))" +
           ") ) " +
           "ORDER BY r.capacity ASC")
    List<Room> findByPurpose(@Param("purposeCategoryId") Long purposeCategoryId,
                              @Param("subTypeId") Long subTypeId,
                              @Param("minCapacity") Integer minCapacity,
                              @Param("keepRoomId") Long keepRoomId,
                              @Param("venueType") String venueType);
}
