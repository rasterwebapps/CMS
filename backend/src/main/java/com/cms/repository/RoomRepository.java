package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByZoneIdOrderByOrderIndexAsc(Long zoneId);
    List<Room> findByZoneIdAndIsActiveTrueOrderByOrderIndexAsc(Long zoneId);

    boolean existsByZoneIdAndRoomNumberIgnoreCase(Long zoneId, String roomNumber);
    boolean existsByZoneIdAndRoomNumberIgnoreCaseAndIdNot(Long zoneId, String roomNumber, Long id);
}
