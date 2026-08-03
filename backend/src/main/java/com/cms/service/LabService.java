package com.cms.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.SpecialityResponse;
import com.cms.dto.LabInChargeAssignmentRequest;
import com.cms.dto.LabInChargeAssignmentResponse;
import com.cms.dto.LabRequest;
import com.cms.dto.LabResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Speciality;
import com.cms.model.Lab;
import com.cms.model.LabInChargeAssignment;
import com.cms.model.Room;
import com.cms.model.enums.LabStatus;
import com.cms.model.enums.LabType;
import com.cms.repository.SpecialityRepository;
import com.cms.repository.LabInChargeAssignmentRepository;
import com.cms.repository.LabRepository;
import com.cms.repository.RoomRepository;

@Service
@Transactional(readOnly = true)
public class LabService {

    private final LabRepository labRepository;
    private final SpecialityRepository specialityRepository;
    private final LabInChargeAssignmentRepository assignmentRepository;
    private final RoomRepository roomRepository;

    public LabService(LabRepository labRepository, SpecialityRepository specialityRepository,
                      LabInChargeAssignmentRepository assignmentRepository, RoomRepository roomRepository) {
        this.labRepository = labRepository;
        this.specialityRepository = specialityRepository;
        this.assignmentRepository = assignmentRepository;
        this.roomRepository = roomRepository;
    }

    /** Mirrors the isResidential gate on HostelRoom assignment — see ClassroomService.resolveRoom. */
    private Room resolveRoom(Long roomId) {
        if (roomId == null) return null;
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));
        if (room.getPurposeCategory() == null || !"ACADEMIC".equals(room.getPurposeCategory().getCode())) {
            throw new IllegalArgumentException(
                "Room must be classified under the Academic purpose category before it can be linked to a lab");
        }
        return room;
    }

    @Transactional
    public LabResponse create(LabRequest request) {
        Speciality speciality = specialityRepository.findById(request.specialityId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Speciality not found with id: " + request.specialityId()));
        String name = requireTrimmed(request.name(), "Lab name is required");

        if (labRepository.existsByNameIgnoreCaseAndSpecialityId(name, request.specialityId())) {
            throw new IllegalArgumentException(
                "A lab with the name '" + name + "' already exists in this speciality");
        }

        Lab lab = new Lab(
            name,
            request.labType(),
            speciality,
            trim(request.building()),
            trim(request.roomNumber()),
            request.capacity(),
            request.status()
        );
        lab.setRoom(resolveRoom(request.roomId()));
        Lab saved = labRepository.save(lab);
        return toResponse(saved);
    }

    public List<LabResponse> findAll() {
        return labRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public Page<LabResponse> findPage(String search, Long specialityId, String labType, String status, Pageable pageable) {
        Specification<Lab> spec = Specification.where(null);
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("building")), pattern),
                cb.like(cb.lower(root.get("roomNumber")), pattern),
                cb.like(cb.lower(root.join("speciality").get("name")), pattern)
            ));
        }
        if (specialityId != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("speciality").get("id"), specialityId));
        }
        if (labType != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("labType"), LabType.valueOf(labType)));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("status"), LabStatus.valueOf(status)));
        }
        return labRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public LabResponse findById(Long id) {
        Lab lab = labRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lab not found with id: " + id));
        return toResponse(lab);
    }

    public List<LabResponse> findBySpecialityId(Long specialityId) {
        if (!specialityRepository.existsById(specialityId)) {
            throw new ResourceNotFoundException("Speciality not found with id: " + specialityId);
        }
        return labRepository.findBySpecialityId(specialityId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public LabResponse update(Long id, LabRequest request) {
        Lab lab = labRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lab not found with id: " + id));

        Speciality speciality = specialityRepository.findById(request.specialityId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Speciality not found with id: " + request.specialityId()));
        String name = requireTrimmed(request.name(), "Lab name is required");

        if (labRepository.existsByNameIgnoreCaseAndSpecialityIdAndIdNot(
                name, request.specialityId(), id)) {
            throw new IllegalArgumentException(
                "A lab with the name '" + name
                + "' already exists in this speciality");
        }

        lab.setName(name);
        lab.setLabType(request.labType());
        lab.setSpeciality(speciality);
        lab.setBuilding(trim(request.building()));
        lab.setRoomNumber(trim(request.roomNumber()));
        lab.setCapacity(request.capacity());
        lab.setStatus(request.status());
        lab.setRoom(resolveRoom(request.roomId()));

        Lab updated = labRepository.save(lab);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!labRepository.existsById(id)) {
            throw new ResourceNotFoundException("Lab not found with id: " + id);
        }
        assignmentRepository.deleteByLabId(id);
        labRepository.deleteById(id);
    }

    @Transactional
    public LabInChargeAssignmentResponse assignInCharge(Long labId, LabInChargeAssignmentRequest request) {
        Lab lab = labRepository.findById(labId)
            .orElseThrow(() -> new ResourceNotFoundException("Lab not found with id: " + labId));

        LabInChargeAssignment assignment = new LabInChargeAssignment(
            lab,
            request.assigneeId(),
            request.assigneeName(),
            request.role(),
            request.assignedDate()
        );
        LabInChargeAssignment saved = assignmentRepository.save(assignment);
        return toAssignmentResponse(saved);
    }

    @Transactional
    public void removeAssignment(Long labId, Long assignmentId) {
        if (!labRepository.existsById(labId)) {
            throw new ResourceNotFoundException("Lab not found with id: " + labId);
        }
        LabInChargeAssignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Assignment not found with id: " + assignmentId));

        if (!assignment.getLab().getId().equals(labId)) {
            throw new ResourceNotFoundException(
                "Assignment " + assignmentId + " does not belong to lab " + labId);
        }
        assignmentRepository.deleteById(assignmentId);
    }

    public List<LabInChargeAssignmentResponse> findAssignmentsByLabId(Long labId) {
        if (!labRepository.existsById(labId)) {
            throw new ResourceNotFoundException("Lab not found with id: " + labId);
        }
        return assignmentRepository.findByLabId(labId).stream()
            .map(this::toAssignmentResponse)
            .toList();
    }

    private LabResponse toResponse(Lab lab) {
        Speciality speciality = lab.getSpeciality();
        SpecialityResponse specialityResponse = new SpecialityResponse(
            speciality.getId(),
            speciality.getName(),
            speciality.getCode(),
            speciality.getDescription(),
            speciality.getHodFacultyId(),
            speciality.getHodName(),
            speciality.getCreatedAt(),
            speciality.getUpdatedAt()
        );

        Room room = lab.getRoom();
        String roomLabel = room != null ? room.getZone().getName() + " · " + room.getRoomNumber() : null;
        return new LabResponse(
            lab.getId(),
            lab.getName(),
            lab.getLabType(),
            specialityResponse,
            lab.getBuilding(),
            lab.getRoomNumber(),
            lab.getCapacity(),
            lab.getStatus(),
            lab.getCreatedAt(),
            lab.getUpdatedAt(),
            room != null ? room.getId() : null,
            roomLabel
        );
    }

    private LabInChargeAssignmentResponse toAssignmentResponse(LabInChargeAssignment assignment) {
        return new LabInChargeAssignmentResponse(
            assignment.getId(),
            assignment.getLab().getId(),
            assignment.getLab().getName(),
            assignment.getAssigneeId(),
            assignment.getAssigneeName(),
            assignment.getRole(),
            assignment.getAssignedDate(),
            assignment.getCreatedAt(),
            assignment.getUpdatedAt()
        );
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String requireTrimmed(String s, String message) {
        String t = trim(s);
        if (t == null) {
            throw new IllegalArgumentException(message);
        }
        return t;
    }
}
