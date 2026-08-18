package com.cms.spatial.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cms.dto.DocumentFileDownload;
import com.cms.exception.ResourceNotFoundException;
import com.cms.service.StorageService;
import com.cms.spatial.dto.FloorPlanCalibrationRequest;
import com.cms.spatial.dto.FloorPlanCreateRequest;
import com.cms.spatial.dto.FloorPlanResponse;
import com.cms.spatial.dto.FloorPlanUpdateRequest;
import com.cms.spatial.model.FloorPlan;
import com.cms.spatial.model.enums.OriginAnchor;
import com.cms.spatial.model.enums.UnitSystem;
import com.cms.spatial.repository.FloorPlanRepository;
import com.cms.spatial.repository.VirtualLocationRepository;
import com.cms.spatial.util.SpatialTransform;

@Service
@Transactional(readOnly = true)
public class FloorPlanService {

    public static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L;
    private static final String STORAGE_FOLDER = "floor_plans";

    private final FloorPlanRepository floorPlanRepository;
    private final VirtualLocationRepository virtualLocationRepository;
    private final StorageService storageService;

    public FloorPlanService(FloorPlanRepository floorPlanRepository,
                             VirtualLocationRepository virtualLocationRepository,
                             StorageService storageService) {
        this.floorPlanRepository = floorPlanRepository;
        this.virtualLocationRepository = virtualLocationRepository;
        this.storageService = storageService;
    }

    public List<FloorPlanResponse> findByEntity(String entityType, Long entityId) {
        return floorPlanRepository.findByEntityTypeAndEntityIdAndIsActiveTrue(entityType, entityId).stream()
            .map(this::toResponse)
            .toList();
    }

    public FloorPlanResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public FloorPlanResponse create(FloorPlanCreateRequest request, MultipartFile file) {
        if (request.entityType() == null || request.entityType().isBlank()) {
            throw new IllegalArgumentException("entityType is required");
        }
        if (request.entityId() == null) {
            throw new IllegalArgumentException("entityId is required");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        validateFile(file);

        FloorPlan plan = new FloorPlan();
        plan.setEntityType(request.entityType());
        plan.setEntityId(request.entityId());
        plan.setName(request.name());
        plan.setUnitSystem(request.unitSystem() != null ? request.unitSystem() : UnitSystem.METERS);
        plan.setOriginAnchor(request.originAnchor() != null ? request.originAnchor() : OriginAnchor.TOP_LEFT);
        plan.setOriginX(request.originX() != null ? request.originX() : 0.0);
        plan.setOriginY(request.originY() != null ? request.originY() : 0.0);
        plan.setViewboxWidth(request.viewboxWidth());
        plan.setViewboxHeight(request.viewboxHeight());

        storeFile(plan, file);

        return toResponse(floorPlanRepository.save(plan));
    }

    @Transactional
    public FloorPlanResponse update(Long id, FloorPlanUpdateRequest request) {
        FloorPlan plan = getOrThrow(id);
        plan.setEntityType(request.entityType());
        plan.setEntityId(request.entityId());
        plan.setName(request.name());
        plan.setUnitSystem(request.unitSystem());
        plan.setOriginAnchor(request.originAnchor());
        plan.setOriginX(request.originX());
        plan.setOriginY(request.originY());
        plan.setViewboxWidth(request.viewboxWidth());
        plan.setViewboxHeight(request.viewboxHeight());
        return toResponse(floorPlanRepository.save(plan));
    }

    @Transactional
    public FloorPlanResponse replaceFile(Long id, MultipartFile file) {
        FloorPlan plan = getOrThrow(id);
        validateFile(file);
        storeFile(plan, file);
        return toResponse(floorPlanRepository.save(plan));
    }

    @Transactional
    public FloorPlanResponse calibrate(Long id, FloorPlanCalibrationRequest request) {
        FloorPlan plan = getOrThrow(id);
        double scaleFactor = SpatialTransform.computeScaleFactor(
            request.point1X(), request.point1Y(), request.point2X(), request.point2Y(), request.physicalLength());

        plan.setCalibrationPoint1X(request.point1X());
        plan.setCalibrationPoint1Y(request.point1Y());
        plan.setCalibrationPoint2X(request.point2X());
        plan.setCalibrationPoint2Y(request.point2Y());
        plan.setCalibrationPhysicalLength(request.physicalLength());
        plan.setScaleFactor(scaleFactor);
        plan.setIsCalibrated(true);

        return toResponse(floorPlanRepository.save(plan));
    }

    /** Soft-deletes the floor plan and cascades to its virtual locations — an inactive floor
     *  plan with active locations still pointing at it is an inconsistent state. */
    @Transactional
    public void delete(Long id) {
        FloorPlan plan = getOrThrow(id);
        plan.setIsActive(false);
        floorPlanRepository.save(plan);

        virtualLocationRepository.findByFloorPlanIdAndIsActiveTrue(id).forEach(loc -> loc.setIsActive(false));
    }

    public DocumentFileDownload getFileForDownload(Long id) {
        FloorPlan plan = getOrThrow(id);
        String fileName = plan.getOriginalFileName() != null ? plan.getOriginalFileName() : plan.getName();
        String contentType = plan.getOriginalContentType() != null
            ? plan.getOriginalContentType() : "application/octet-stream";
        return new DocumentFileDownload(fileName, contentType,
            storageService.downloadBytes(plan.getStorageKey()));
    }

    private void storeFile(FloorPlan plan, MultipartFile file) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read uploaded file", ex);
        }

        String sanitizedName = sanitizeFileName(file.getOriginalFilename());
        String contentType = file.getContentType();
        // A floor plan's storage_key is NOT NULL from the very first insert (no metadata-only
        // create), so unlike MinioStorageService.buildKey this can't prefix the row's own id —
        // it doesn't exist yet on first save. UUID-only key is still globally unique.
        String objectKey = STORAGE_FOLDER + "/" + java.util.UUID.randomUUID() + "-" + sanitizedName;

        storageService.upload(objectKey, new ByteArrayInputStream(bytes), bytes.length, contentType);

        plan.setStorageKey(objectKey);
        plan.setOriginalFileName(sanitizedName);
        plan.setOriginalContentType(contentType);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                "File exceeds maximum allowed size of " + MAX_FILE_SIZE_BYTES + " bytes");
        }
        String contentType = file.getContentType();
        boolean isImage = contentType != null
            && (contentType.startsWith("image/") || contentType.equals("application/pdf"));
        if (!isImage) {
            throw new IllegalArgumentException(
                "Floor plan file must be an image or SVG (got " + contentType + ")");
        }
    }

    private String sanitizeFileName(String original) {
        if (original == null) return null;
        String name = original.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) name = name.substring(slash + 1);
        return name.isBlank() ? null : name;
    }

    private FloorPlan getOrThrow(Long id) {
        return floorPlanRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Floor plan not found with id: " + id));
    }

    private FloorPlanResponse toResponse(FloorPlan plan) {
        return new FloorPlanResponse(
            plan.getId(),
            plan.getEntityType(),
            plan.getEntityId(),
            plan.getName(),
            plan.getOriginalFileName(),
            plan.getOriginalContentType(),
            plan.getUnitSystem(),
            plan.getOriginAnchor(),
            plan.getOriginX(),
            plan.getOriginY(),
            plan.getViewboxWidth(),
            plan.getViewboxHeight(),
            plan.getScaleFactor(),
            plan.getCalibrationPoint1X(),
            plan.getCalibrationPoint1Y(),
            plan.getCalibrationPoint2X(),
            plan.getCalibrationPoint2Y(),
            plan.getCalibrationPhysicalLength(),
            plan.getIsCalibrated(),
            plan.getIsActive(),
            plan.getCreatedAt(),
            plan.getUpdatedAt()
        );
    }
}
