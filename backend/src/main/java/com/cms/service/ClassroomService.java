package com.cms.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.dto.ClassroomRequest;
import com.cms.dto.ClassroomResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Classroom;
import com.cms.model.Room;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.RoomRepository;

@Service
@Transactional(readOnly = true)
public class ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final RoomRepository roomRepository;

    public ClassroomService(ClassroomRepository classroomRepository, RoomRepository roomRepository) {
        this.classroomRepository = classroomRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional
    public ClassroomResponse create(ClassroomRequest request) {
        String name = requireTrimmed(request.name(), "Classroom name is required");

        if (classroomRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException(
                "A classroom with the name '" + name + "' already exists");
        }

        Classroom classroom = new Classroom(name, trim(request.building()), trim(request.roomNumber()), request.capacity());
        if (request.isActive() != null) {
            classroom.setIsActive(request.isActive());
        }
        classroom.setRoom(resolveRoom(request.roomId()));
        return toResponse(classroomRepository.save(classroom));
    }

    /** Mirrors the isResidential gate on HostelRoom assignment: a Room must be classified under
     *  the Academic purpose category before it can become a Classroom's physical location, so an
     *  admin can't accidentally (or deliberately) mislabel a dorm room as a teaching space. */
    private Room resolveRoom(Long roomId) {
        if (roomId == null) return null;
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));
        if (room.getPurposeCategory() == null || !"ACADEMIC".equals(room.getPurposeCategory().getCode())) {
            throw new IllegalArgumentException(
                "Room must be classified under the Academic purpose category before it can be linked to a classroom");
        }
        return room;
    }

    public List<ClassroomResponse> findAll() {
        return classroomRepository.findAllByOrderByNameAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    public List<ClassroomResponse> findActive() {
        return classroomRepository.findByIsActiveTrueOrderByNameAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    public Page<ClassroomResponse> findPage(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return classroomRepository.findAll(pageable).map(this::toResponse);
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        Specification<Classroom> spec = (root, query, cb) ->
            cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("building")), pattern),
                cb.like(cb.lower(root.get("roomNumber")), pattern)
            );
        return classroomRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public ClassroomResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public ClassroomResponse update(Long id, ClassroomRequest request) {
        Classroom classroom = findOrThrow(id);
        String name = requireTrimmed(request.name(), "Classroom name is required");

        if (classroomRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException(
                "A classroom with the name '" + name + "' already exists");
        }

        classroom.setName(name);
        classroom.setBuilding(trim(request.building()));
        classroom.setRoomNumber(trim(request.roomNumber()));
        classroom.setCapacity(request.capacity());
        if (request.isActive() != null) {
            classroom.setIsActive(request.isActive());
        }
        classroom.setRoom(resolveRoom(request.roomId()));
        return toResponse(classroomRepository.save(classroom));
    }

    @Transactional
    public void delete(Long id) {
        if (!classroomRepository.existsById(id)) {
            throw new ResourceNotFoundException("Classroom not found with id: " + id);
        }
        classroomRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        Classroom classroom = findOrThrow(id);
        classroom.setIsActive(Boolean.TRUE.equals(request.isActive()));
        Classroom saved = classroomRepository.save(classroom);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    public boolean nameExists(String name, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        if (excludeId != null) return classroomRepository.existsByNameIgnoreCaseAndIdNot(trimmed, excludeId);
        return classroomRepository.existsByNameIgnoreCase(trimmed);
    }

    private Classroom findOrThrow(Long id) {
        return classroomRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Classroom not found with id: " + id));
    }

    private ClassroomResponse toResponse(Classroom c) {
        Room room = c.getRoom();
        String roomLabel = room != null ? room.getZone().getName() + " · " + room.getRoomNumber() : null;
        return new ClassroomResponse(c.getId(), c.getName(), c.getBuilding(), c.getRoomNumber(),
            c.getCapacity(), c.getIsActive(), c.getCreatedAt(), c.getUpdatedAt(),
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
