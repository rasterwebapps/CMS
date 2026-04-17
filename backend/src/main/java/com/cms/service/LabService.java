package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.DepartmentResponse;
import com.cms.dto.LabInChargeAssignmentRequest;
import com.cms.dto.LabInChargeAssignmentResponse;
import com.cms.dto.LabRequest;
import com.cms.dto.LabResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Department;
import com.cms.model.Lab;
import com.cms.model.LabInChargeAssignment;
import com.cms.repository.DepartmentRepository;
import com.cms.repository.LabInChargeAssignmentRepository;
import com.cms.repository.LabRepository;

@Service
@Transactional(readOnly = true)
public class LabService {

    private final LabRepository labRepository;
    private final DepartmentRepository departmentRepository;
    private final LabInChargeAssignmentRepository assignmentRepository;

    public LabService(LabRepository labRepository, DepartmentRepository departmentRepository,
                      LabInChargeAssignmentRepository assignmentRepository) {
        this.labRepository = labRepository;
        this.departmentRepository = departmentRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional
    public LabResponse create(LabRequest request) {
        Department department = departmentRepository.findById(request.departmentId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Department not found with id: " + request.departmentId()));

        Lab lab = new Lab(
            request.name(),
            request.labType(),
            department,
            request.building(),
            request.roomNumber(),
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

    public List<LabResponse> findByDepartmentId(Long departmentId) {
        if (!departmentRepository.existsById(departmentId)) {
            throw new ResourceNotFoundException("Department not found with id: " + departmentId);
        }
        return labRepository.findByDepartmentId(departmentId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public LabResponse update(Long id, LabRequest request) {
        Lab lab = labRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lab not found with id: " + id));

        Department department = departmentRepository.findById(request.departmentId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Department not found with id: " + request.departmentId()));

        if (labRepository.existsByNameAndDepartmentIdAndIdNot(
                request.name(), request.departmentId(), id)) {
            throw new IllegalArgumentException(
                "A lab with the name '" + request.name()
                + "' already exists in this department");
        }

        lab.setName(request.name());
        lab.setLabType(request.labType());
        lab.setDepartment(department);
        lab.setBuilding(request.building());
        lab.setRoomNumber(request.roomNumber());
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
        Department department = lab.getDepartment();
        DepartmentResponse departmentResponse = new DepartmentResponse(
            department.getId(),
            department.getName(),
            department.getCode(),
            department.getDescription(),
            department.getHodName(),
            department.getCreatedAt(),
            department.getUpdatedAt()
        );

        return new LabResponse(
            lab.getId(),
            lab.getName(),
            lab.getLabType(),
            departmentResponse,
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
}
