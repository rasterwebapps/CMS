package com.cms.spatial.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.exception.ResourceNotFoundException;
import com.cms.spatial.dto.VirtualLocationRequest;
import com.cms.spatial.dto.VirtualLocationResponse;
import com.cms.spatial.model.FloorPlan;
import com.cms.spatial.model.VirtualLocation;
import com.cms.spatial.model.enums.VirtualLocationStatus;
import com.cms.spatial.repository.FloorPlanRepository;
import com.cms.spatial.repository.VirtualLocationRepository;

@Service
@Transactional(readOnly = true)
public class VirtualLocationService {

    private final VirtualLocationRepository virtualLocationRepository;
    private final FloorPlanRepository floorPlanRepository;

    public VirtualLocationService(VirtualLocationRepository virtualLocationRepository,
                                   FloorPlanRepository floorPlanRepository) {
        this.virtualLocationRepository = virtualLocationRepository;
        this.floorPlanRepository = floorPlanRepository;
    }

    public List<VirtualLocationResponse> findByFloorPlan(Long floorPlanId) {
        return virtualLocationRepository.findByFloorPlanIdAndIsActiveTrue(floorPlanId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<VirtualLocationResponse> findByEntity(String entityType, Long entityId) {
        return virtualLocationRepository.findByEntityTypeAndEntityIdAndIsActiveTrue(entityType, entityId).stream()
            .map(this::toResponse)
            .toList();
    }

    public VirtualLocationResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public VirtualLocationResponse create(VirtualLocationRequest request) {
        FloorPlan floorPlan = floorPlanRepository.findById(request.floorPlanId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Floor plan not found with id: " + request.floorPlanId()));
        if (!Boolean.TRUE.equals(floorPlan.getIsActive())) {
            throw new IllegalStateException("Floor plan " + floorPlan.getId() + " is not active");
        }

        VirtualLocation location = new VirtualLocation();
        location.setFloorPlan(floorPlan);
        applyRequest(location, request);

        return toResponse(virtualLocationRepository.save(location));
    }

    @Transactional
    public VirtualLocationResponse update(Long id, VirtualLocationRequest request) {
        VirtualLocation location = getOrThrow(id);
        if (!request.floorPlanId().equals(location.getFloorPlan().getId())) {
            FloorPlan floorPlan = floorPlanRepository.findById(request.floorPlanId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Floor plan not found with id: " + request.floorPlanId()));
            location.setFloorPlan(floorPlan);
        }
        applyRequest(location, request);
        return toResponse(virtualLocationRepository.save(location));
    }

    @Transactional
    public void delete(Long id) {
        VirtualLocation location = getOrThrow(id);
        location.setIsActive(false);
        virtualLocationRepository.save(location);
    }

    private void applyRequest(VirtualLocation location, VirtualLocationRequest request) {
        location.setEntityType(request.entityType());
        location.setEntityId(request.entityId());
        location.setName(request.name());
        location.setLocationType(request.locationType());
        location.setModuleTag(request.moduleTag());
        location.setShapeType(request.shapeType());
        location.setGeometryJson(request.geometryJson());
        location.setCapacity(request.capacity());
        location.setStatus(request.status() != null ? request.status() : VirtualLocationStatus.ACTIVE);
        location.setDescription(request.description());
    }

    private VirtualLocation getOrThrow(Long id) {
        return virtualLocationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Virtual location not found with id: " + id));
    }

    private VirtualLocationResponse toResponse(VirtualLocation location) {
        return new VirtualLocationResponse(
            location.getId(),
            location.getFloorPlan().getId(),
            location.getEntityType(),
            location.getEntityId(),
            location.getName(),
            location.getLocationType(),
            location.getModuleTag(),
            location.getShapeType(),
            location.getGeometryJson(),
            location.getCapacity(),
            location.getStatus(),
            location.getDescription(),
            location.getIsActive(),
            location.getCreatedAt(),
            location.getUpdatedAt()
        );
    }
}
