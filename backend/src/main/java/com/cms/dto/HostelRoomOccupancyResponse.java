package com.cms.dto;

import java.util.List;

/** Per-room occupancy snapshot backing the Room Allocation dashboard's occupancy map. */
public record HostelRoomOccupancyResponse(
    Long hostelRoomId,
    Long roomId,
    String roomNumber,
    Long zoneId,
    String zoneName,
    Long roomTypeId,
    String roomTypeName,
    Integer sharingCapacity,
    Integer occupiedCount,
    List<Occupant> occupants
) {
    public record Occupant(
        Long allocationId,
        Long studentId,
        String studentName,
        java.time.LocalDate startDate
    ) {}
}
