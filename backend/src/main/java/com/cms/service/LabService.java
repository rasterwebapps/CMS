package com.cms.service;

import java.util.List;

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
import com.cms.repository.SpecialityRepository;
import com.cms.repository.LabInChargeAssignmentRepository;
import com.cms.repository.LabRepository;

@Service
@Transactional(readOnly = true)
public class LabService {

    private final LabRepository labRepository;
    private final SpecialityRepository specialityRepository;
    private final LabInChargeAssignmentRepository assignmentRepository;

    public LabService(LabRepository labRepository, SpecialityRepository specialityRepository,
                      LabInChargeAssignmentRepository assignmentRepository) {
        this.labRepository = labRepository;
        this.specialityRepository = specialityRepository;
        this.assignmentRepository = assignmentRepository;
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
        Lab saved = labRepository.save(lab);
        return toResponse(saved);
    }

    public List<LabResponse> findAll() {
        return labRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
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
            lab.getUpdatedAt()
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
