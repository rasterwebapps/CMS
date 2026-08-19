package com.cms.spatial.service;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.config.PermSecurityBean;
import com.cms.exception.ResourceNotFoundException;
import com.cms.spatial.dto.VirtualLocationRequest;
import com.cms.spatial.dto.VirtualLocationResponse;
import com.cms.spatial.model.FloorPlan;
import com.cms.spatial.model.VirtualLocation;
import com.cms.spatial.model.enums.ShapeType;
import com.cms.spatial.model.enums.VirtualLocationStatus;
import com.cms.spatial.repository.FloorPlanRepository;
import com.cms.spatial.repository.VirtualLocationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class VirtualLocationService {

    private final VirtualLocationRepository virtualLocationRepository;
    private final FloorPlanRepository floorPlanRepository;
    private final ObjectMapper objectMapper;
    private final PermSecurityBean permSecurityBean;

    public VirtualLocationService(VirtualLocationRepository virtualLocationRepository,
                                   FloorPlanRepository floorPlanRepository,
                                   ObjectMapper objectMapper,
                                   PermSecurityBean permSecurityBean) {
        this.virtualLocationRepository = virtualLocationRepository;
        this.floorPlanRepository = floorPlanRepository;
        this.objectMapper = objectMapper;
        this.permSecurityBean = permSecurityBean;
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
        requireLinkPermission(request.entityType());

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
        requireLinkPermission(request.entityType());
        applyRequest(location, request);
        return toResponse(virtualLocationRepository.save(location));
    }

    /**
     * A marker's own {@code SPATIAL_VIRTUAL_LOCATION_MANAGE} permission only covers placing/moving
     * the marker itself — it says nothing about whether the caller may link to the specific
     * Equipment/InventoryItem catalog row being pointed at. Block/Zone/Room links don't need an
     * extra check here: those go through {@code CampusInfrastructureController}'s own
     * {@code CAMPUS_INFRASTRUCTURE_MANAGE} gate first when created via the normal flows.
     */
    private void requireLinkPermission(String entityType) {
        String requiredPermission = switch (entityType) {
            case null -> null;
            case "EQUIPMENT" -> "EQUIPMENT_MANAGE";
            case "INVENTORY_ITEM" -> "INVENTORY_MANAGE";
            default -> null;
        };
        if (requiredPermission != null && !permSecurityBean.has(requiredPermission)) {
            throw new AccessDeniedException("Linking a marker to " + entityType + " requires the " + requiredPermission + " permission");
        }
    }

    @Transactional
    public void delete(Long id) {
        VirtualLocation location = getOrThrow(id);
        location.setIsActive(false);
        virtualLocationRepository.save(location);
    }

    private void applyRequest(VirtualLocation location, VirtualLocationRequest request) {
        validateGeometry(request.shapeType(), request.geometryJson());

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

    /**
     * geometryJson is flat SVG-style coordinates, not GeoJSON:
     * POINT {x,y} · RECTANGLE {x,y,width,height} (x,y = top-left) · POLYGON {points:[{x,y},...]} (min 3).
     */
    private void validateGeometry(ShapeType shapeType, String geometryJson) {
        JsonNode node;
        try {
            node = objectMapper.readTree(geometryJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("geometryJson is not valid JSON");
        }
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException("geometryJson must be a JSON object");
        }

        switch (shapeType) {
            case POINT -> requireNumericFields(node, "x", "y");
            case RECTANGLE -> requireNumericFields(node, "x", "y", "width", "height");
            case POLYGON -> {
                JsonNode points = node.get("points");
                if (points == null || !points.isArray() || points.size() < 3) {
                    throw new IllegalArgumentException(
                        "POLYGON geometryJson must have a 'points' array with at least 3 entries");
                }
                for (JsonNode point : points) {
                    if (point == null || !point.isObject()) {
                        throw new IllegalArgumentException("Each POLYGON point must be a JSON object with x, y");
                    }
                    requireNumericFields(point, "x", "y");
                }
            }
        }
    }

    private void requireNumericFields(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value == null || !value.isNumber()) {
                throw new IllegalArgumentException("geometryJson field '" + field + "' must be a number");
            }
        }
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
