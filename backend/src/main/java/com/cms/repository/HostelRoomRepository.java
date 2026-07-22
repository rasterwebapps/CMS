package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.HostelRoom;

public interface HostelRoomRepository extends JpaRepository<HostelRoom, Long> {
    Optional<HostelRoom> findByRoomId(Long roomId);
    boolean existsByRoomId(Long roomId);

    List<HostelRoom> findByRoomTypeIdOrderById(Long roomTypeId);
}
