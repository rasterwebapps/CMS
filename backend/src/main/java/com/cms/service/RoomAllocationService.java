package com.cms.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.HostelRoomOccupancyResponse;
import com.cms.dto.RoomAllocationRequest;
import com.cms.dto.RoomAllocationResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.HostelRoom;
import com.cms.model.RoomAllocation;
import com.cms.model.Student;
import com.cms.model.enums.RoomAllocationStatus;
import com.cms.model.enums.StudentType;
import com.cms.repository.HostelRoomRepository;
import com.cms.repository.RoomAllocationRepository;
import com.cms.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class RoomAllocationService {

    private final RoomAllocationRepository roomAllocationRepository;
    private final StudentRepository studentRepository;
    private final HostelRoomRepository hostelRoomRepository;

    public RoomAllocationService(RoomAllocationRepository roomAllocationRepository,
                                  StudentRepository studentRepository,
                                  HostelRoomRepository hostelRoomRepository) {
        this.roomAllocationRepository = roomAllocationRepository;
        this.studentRepository = studentRepository;
        this.hostelRoomRepository = hostelRoomRepository;
    }

    @Transactional
    public RoomAllocationResponse create(RoomAllocationRequest request) {
        Student student = studentRepository.findById(request.studentId())
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + request.studentId()));

        if (student.getStudentType() != StudentType.HOSTELER) {
            throw new IllegalStateException(
                "Only students converted to Hosteler can be allocated a room — convert this student's type first");
        }
        if (roomAllocationRepository.existsByStudentIdAndStatus(student.getId(), RoomAllocationStatus.ACTIVE)) {
            throw new IllegalStateException("This student already has an active room allocation");
        }

        HostelRoom hostelRoom = hostelRoomRepository.findById(request.hostelRoomId())
            .orElseThrow(() -> new ResourceNotFoundException("Hostel room not found with id: " + request.hostelRoomId()));

        long occupied = roomAllocationRepository.countByHostelRoomIdAndStatus(hostelRoom.getId(), RoomAllocationStatus.ACTIVE);
        if (occupied >= hostelRoom.getRoomType().getSharingCapacity()) {
            throw new IllegalStateException(
                "Room " + hostelRoom.getRoom().getRoomNumber() + " is at full capacity ("
                    + hostelRoom.getRoomType().getSharingCapacity() + ")");
        }

        RoomAllocation allocation = new RoomAllocation();
        allocation.setStudent(student);
        allocation.setHostelRoom(hostelRoom);
        allocation.setStartDate(request.startDate());
        allocation.setEndDate(request.endDate());
        allocation.setRemarks(trim(request.remarks()));
        if (request.status() != null) {
            allocation.setStatus(request.status());
        }
        return toResponse(roomAllocationRepository.save(allocation));
    }

    public RoomAllocationResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    public List<RoomAllocationResponse> findByStudentId(Long studentId) {
        return roomAllocationRepository.findByStudentIdOrderByStartDateDesc(studentId).stream()
            .map(this::toResponse)
            .toList();
    }

    public Page<RoomAllocationResponse> findPage(String search, RoomAllocationStatus status, Pageable pageable) {
        Specification<RoomAllocation> spec = Specification.where(null);
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("student").get("firstName")), pattern),
                cb.like(cb.lower(root.get("student").get("lastName")), pattern),
                cb.like(cb.lower(root.get("hostelRoom").get("room").get("roomNumber")), pattern)
            ));
        }
        if (status != null) {
            Specification<RoomAllocation> statusSpec = (root, query, cb) -> cb.equal(root.get("status"), status);
            spec = spec.and(statusSpec);
        }
        return roomAllocationRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional
    public RoomAllocationResponse updateStatus(Long id, RoomAllocationStatus status) {
        RoomAllocation allocation = findOrThrow(id);
        allocation.setStatus(status);
        return toResponse(roomAllocationRepository.save(allocation));
    }

    @Transactional
    public void delete(Long id) {
        if (!roomAllocationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Room allocation not found with id: " + id);
        }
        roomAllocationRepository.deleteById(id);
    }

    public List<HostelRoomOccupancyResponse> findOccupancy() {
        return hostelRoomRepository.findAll().stream()
            .filter(hr -> Boolean.TRUE.equals(hr.getIsActive()))
            .map(hr -> {
                List<RoomAllocation> active = roomAllocationRepository.findByHostelRoomIdAndStatus(hr.getId(), RoomAllocationStatus.ACTIVE);
                List<HostelRoomOccupancyResponse.Occupant> occupants = active.stream()
                    .map(a -> new HostelRoomOccupancyResponse.Occupant(
                        a.getId(),
                        a.getStudent().getId(),
                        (a.getStudent().getFirstName() + " " + a.getStudent().getLastName()).trim(),
                        a.getStartDate()))
                    .toList();
                return new HostelRoomOccupancyResponse(
                    hr.getId(),
                    hr.getRoom().getId(),
                    hr.getRoom().getRoomNumber(),
                    hr.getRoom().getZone().getId(),
                    hr.getRoom().getZone().getName(),
                    hr.getRoomType().getId(),
                    hr.getRoomType().getName(),
                    hr.getRoomType().getSharingCapacity(),
                    occupants.size(),
                    occupants
                );
            })
            .toList();
    }

    private RoomAllocation findOrThrow(Long id) {
        return roomAllocationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Room allocation not found with id: " + id));
    }

    private RoomAllocationResponse toResponse(RoomAllocation a) {
        HostelRoom hostelRoom = a.getHostelRoom();
        Student student = a.getStudent();
        return new RoomAllocationResponse(
            a.getId(),
            student.getId(),
            (student.getFirstName() + " " + student.getLastName()).trim(),
            hostelRoom.getId(),
            hostelRoom.getRoom().getId(),
            hostelRoom.getRoom().getRoomNumber(),
            hostelRoom.getRoom().getZone().getId(),
            hostelRoom.getRoom().getZone().getName(),
            hostelRoom.getRoomType().getId(),
            hostelRoom.getRoomType().getName(),
            a.getStartDate(),
            a.getEndDate(),
            a.getStatus(),
            a.getRemarks(),
            a.getCreatedAt(),
            a.getUpdatedAt()
        );
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
