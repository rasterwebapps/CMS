package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.MaintenanceRequestDto;
import com.cms.dto.MaintenanceRequestResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Equipment;
import com.cms.model.Faculty;
import com.cms.model.MaintenanceRequest;
import com.cms.model.enums.EquipmentStatus;
import com.cms.model.enums.MaintenanceStatus;
import com.cms.repository.EquipmentRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.MaintenanceRequestRepository;

@Service
@Transactional(readOnly = true)
public class MaintenanceRequestService {

    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final EquipmentRepository equipmentRepository;
    private final FacultyRepository facultyRepository;

    public MaintenanceRequestService(MaintenanceRequestRepository maintenanceRequestRepository,
                                      EquipmentRepository equipmentRepository,
                                      FacultyRepository facultyRepository) {
        this.maintenanceRequestRepository = maintenanceRequestRepository;
        this.equipmentRepository = equipmentRepository;
        this.facultyRepository = facultyRepository;
    }

    @Transactional
    public MaintenanceRequestResponse create(MaintenanceRequestDto request) {
        Equipment equipment = equipmentRepository.findById(request.equipmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with id: " + request.equipmentId()));

        MaintenanceRequest maintenanceRequest = new MaintenanceRequest(
            equipment, request.title(), request.maintenanceType(),
            request.priority(), request.status(), request.requestDate()
        );
        maintenanceRequest.setDescription(request.description());
        maintenanceRequest.setScheduledDate(request.scheduledDate());
        maintenanceRequest.setCompletionDate(request.completionDate());
        maintenanceRequest.setEstimatedCost(request.estimatedCost());
        maintenanceRequest.setActualCost(request.actualCost());
        maintenanceRequest.setResolutionNotes(request.resolutionNotes());

        if (request.requestedById() != null) {
            Faculty requestedBy = facultyRepository.findById(request.requestedById())
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + request.requestedById()));
            maintenanceRequest.setRequestedBy(requestedBy);
        }

        if (request.assignedToId() != null) {
            Faculty assignedTo = facultyRepository.findById(request.assignedToId())
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + request.assignedToId()));
            maintenanceRequest.setAssignedTo(assignedTo);
        }

        // Update equipment status if maintenance is initiated
        if (request.status() == MaintenanceStatus.IN_PROGRESS) {
            equipment.setStatus(EquipmentStatus.UNDER_MAINTENANCE);
            equipmentRepository.save(equipment);
        }

        MaintenanceRequest saved = maintenanceRequestRepository.save(maintenanceRequest);
        return toResponse(saved);
    }

    public List<MaintenanceRequestResponse> findAll() {
        return maintenanceRequestRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public MaintenanceRequestResponse findById(Long id) {
        MaintenanceRequest request = maintenanceRequestRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Maintenance request not found with id: " + id));
        return toResponse(request);
    }

    public List<MaintenanceRequestResponse> findByEquipmentId(Long equipmentId) {
        if (!equipmentRepository.existsById(equipmentId)) {
            throw new ResourceNotFoundException("Equipment not found with id: " + equipmentId);
        }
        return maintenanceRequestRepository.findByEquipmentId(equipmentId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<MaintenanceRequestResponse> findByStatus(MaintenanceStatus status) {
        return maintenanceRequestRepository.findByStatus(status).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<MaintenanceRequestResponse> findPendingRequests() {
        return maintenanceRequestRepository.findPendingRequests().stream()
            .map(this::toResponse)
            .toList();
    }

    public List<MaintenanceRequestResponse> findByAssignedToId(Long assignedToId) {
        if (!facultyRepository.existsById(assignedToId)) {
            throw new ResourceNotFoundException("Faculty not found with id: " + assignedToId);
        }
        return maintenanceRequestRepository.findByAssignedToId(assignedToId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public MaintenanceRequestResponse update(Long id, MaintenanceRequestDto request) {
        MaintenanceRequest maintenanceRequest = maintenanceRequestRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Maintenance request not found with id: " + id));

        Equipment equipment = equipmentRepository.findById(request.equipmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with id: " + request.equipmentId()));

        maintenanceRequest.setEquipment(equipment);
        maintenanceRequest.setTitle(request.title());
        maintenanceRequest.setDescription(request.description());
        maintenanceRequest.setMaintenanceType(request.maintenanceType());
        maintenanceRequest.setPriority(request.priority());
        maintenanceRequest.setStatus(request.status());
        maintenanceRequest.setRequestDate(request.requestDate());
        maintenanceRequest.setScheduledDate(request.scheduledDate());
        maintenanceRequest.setCompletionDate(request.completionDate());
        maintenanceRequest.setEstimatedCost(request.estimatedCost());
        maintenanceRequest.setActualCost(request.actualCost());
        maintenanceRequest.setResolutionNotes(request.resolutionNotes());

        if (request.requestedById() != null) {
            Faculty requestedBy = facultyRepository.findById(request.requestedById())
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + request.requestedById()));
            maintenanceRequest.setRequestedBy(requestedBy);
        }

        if (request.assignedToId() != null) {
            Faculty assignedTo = facultyRepository.findById(request.assignedToId())
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + request.assignedToId()));
            maintenanceRequest.setAssignedTo(assignedTo);
        }

        // Update equipment status based on maintenance status
        if (request.status() == MaintenanceStatus.COMPLETED) {
            equipment.setStatus(EquipmentStatus.AVAILABLE);
            equipmentRepository.save(equipment);
        } else if (request.status() == MaintenanceStatus.IN_PROGRESS) {
            equipment.setStatus(EquipmentStatus.UNDER_MAINTENANCE);
            equipmentRepository.save(equipment);
        }

        MaintenanceRequest updated = maintenanceRequestRepository.save(maintenanceRequest);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!maintenanceRequestRepository.existsById(id)) {
            throw new ResourceNotFoundException("Maintenance request not found with id: " + id);
        }
        maintenanceRequestRepository.deleteById(id);
    }

    private MaintenanceRequestResponse toResponse(MaintenanceRequest mr) {
        return new MaintenanceRequestResponse(
            mr.getId(),
            mr.getEquipment().getId(),
            mr.getEquipment().getName(),
            mr.getEquipment().getAssetCode(),
            mr.getEquipment().getLab().getId(),
            mr.getEquipment().getLab().getName(),
            mr.getTitle(),
            mr.getDescription(),
            mr.getMaintenanceType(),
            mr.getPriority(),
            mr.getStatus(),
            mr.getRequestedBy() != null ? mr.getRequestedBy().getId() : null,
            mr.getRequestedBy() != null ?
                mr.getRequestedBy().getFirstName() + " " + mr.getRequestedBy().getLastName() : null,
            mr.getRequestDate(),
            mr.getScheduledDate(),
            mr.getCompletionDate(),
            mr.getAssignedTo() != null ? mr.getAssignedTo().getId() : null,
            mr.getAssignedTo() != null ?
                mr.getAssignedTo().getFirstName() + " " + mr.getAssignedTo().getLastName() : null,
            mr.getEstimatedCost(),
            mr.getActualCost(),
            mr.getResolutionNotes(),
            mr.getCreatedAt(),
            mr.getUpdatedAt()
        );
    }
}
