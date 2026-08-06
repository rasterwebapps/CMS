package com.cms.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.dto.ClinicalVenueRequest;
import com.cms.dto.ClinicalVenueResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.ClinicalVenue;
import com.cms.model.Room;
import com.cms.model.enums.RoomPurposeCategoryCode;
import com.cms.repository.ClinicalVenueRepository;
import com.cms.repository.RoomRepository;

@Service
@Transactional(readOnly = true)
public class ClinicalVenueService {

    private final ClinicalVenueRepository clinicalVenueRepository;
    private final RoomRepository roomRepository;

    public ClinicalVenueService(ClinicalVenueRepository clinicalVenueRepository, RoomRepository roomRepository) {
        this.clinicalVenueRepository = clinicalVenueRepository;
        this.roomRepository = roomRepository;
    }

    /** Optional, unlike Classroom/Lab -- an off-campus hospital posting has no Room to link (the
     *  college doesn't model an external hospital's building layout) and stays fully described by
     *  hospitalName/department as before. Only an on-campus clinical/skills space sets roomId, and
     *  mirrors the same Academic-category gate as ClassroomService.resolveRoom when it does. */
    private Room resolveRoom(Long roomId) {
        if (roomId == null) return null;
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));
        if (room.getPurposeCategory() == null || room.getPurposeCategory().getCode() != RoomPurposeCategoryCode.ACADEMIC) {
            throw new IllegalArgumentException(
                "Room must be classified under the Academic purpose category before it can be linked to a clinical venue");
        }
        return room;
    }

    /** Once a physical Room is linked, its capacity is the ground truth every downstream capacity
     *  check (Cohort Room Allocation, Staffing) actually trusts — see
     *  ClassroomService.resolveCapacity. Only used when unlinked (including every off-campus
     *  venue, which never has a room to derive from). */
    private static Integer resolveCapacity(Integer requestedCapacity, Room room) {
        return room != null ? room.getCapacity() : requestedCapacity;
    }

    @Transactional
    public ClinicalVenueResponse create(ClinicalVenueRequest request) {
        String name = requireTrimmed(request.name(), "Clinical venue name is required");

        if (clinicalVenueRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException(
                "A clinical venue with the name '" + name + "' already exists");
        }

        Room room = resolveRoom(request.roomId());
        ClinicalVenue venue = new ClinicalVenue(name, trim(request.hospitalName()), trim(request.department()),
            resolveCapacity(request.capacity(), room));
        if (request.isActive() != null) {
            venue.setIsActive(request.isActive());
        }
        venue.setRoom(room);
        return toResponse(clinicalVenueRepository.save(venue));
    }

    public List<ClinicalVenueResponse> findAll() {
        return clinicalVenueRepository.findAllByOrderByNameAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    public List<ClinicalVenueResponse> findActive() {
        return clinicalVenueRepository.findByIsActiveTrueOrderByNameAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    public Page<ClinicalVenueResponse> findPage(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return clinicalVenueRepository.findAll(pageable).map(this::toResponse);
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        Specification<ClinicalVenue> spec = (root, query, cb) ->
            cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("hospitalName")), pattern),
                cb.like(cb.lower(root.get("department")), pattern)
            );
        return clinicalVenueRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public ClinicalVenueResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public ClinicalVenueResponse update(Long id, ClinicalVenueRequest request) {
        ClinicalVenue venue = findOrThrow(id);
        String name = requireTrimmed(request.name(), "Clinical venue name is required");

        if (clinicalVenueRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException(
                "A clinical venue with the name '" + name + "' already exists");
        }

        Room room = resolveRoom(request.roomId());
        venue.setName(name);
        venue.setHospitalName(trim(request.hospitalName()));
        venue.setDepartment(trim(request.department()));
        venue.setCapacity(resolveCapacity(request.capacity(), room));
        if (request.isActive() != null) {
            venue.setIsActive(request.isActive());
        }
        venue.setRoom(room);
        return toResponse(clinicalVenueRepository.save(venue));
    }

    @Transactional
    public void delete(Long id) {
        if (!clinicalVenueRepository.existsById(id)) {
            throw new ResourceNotFoundException("Clinical venue not found with id: " + id);
        }
        clinicalVenueRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        ClinicalVenue venue = findOrThrow(id);
        venue.setIsActive(Boolean.TRUE.equals(request.isActive()));
        ClinicalVenue saved = clinicalVenueRepository.save(venue);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    public boolean nameExists(String name, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        if (excludeId != null) return clinicalVenueRepository.existsByNameIgnoreCaseAndIdNot(trimmed, excludeId);
        return clinicalVenueRepository.existsByNameIgnoreCase(trimmed);
    }

    private ClinicalVenue findOrThrow(Long id) {
        return clinicalVenueRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Clinical venue not found with id: " + id));
    }

    private ClinicalVenueResponse toResponse(ClinicalVenue v) {
        Room room = v.getRoom();
        String roomLabel = room != null ? room.getZone().getName() + " · " + room.getRoomNumber() : null;
        return new ClinicalVenueResponse(v.getId(), v.getName(), v.getHospitalName(), v.getDepartment(),
            v.getCapacity(), v.getIsActive(), v.getCreatedAt(), v.getUpdatedAt(),
            room != null ? room.getId() : null, roomLabel);
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String requireTrimmed(String s, String message) {
        String t = trim(s);
        if (t == null) throw new IllegalArgumentException(message);
        return t;
    }
}
