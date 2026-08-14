package com.cms.spatial.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.spatial.model.enums.OriginAnchor;
import com.cms.spatial.model.enums.UnitSystem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A calibrated floor-plan asset attached to a physical node via a generic
 * (entityType, entityId) pair rather than a foreign key — entityType/entityId
 * deliberately do not reference com.cms.model.Floor/Block/etc. directly so this
 * module stays extractable as a standalone spatial component. The rendered SVG
 * (and, if uploaded, the original file) live in MinIO under storageKey; this row
 * only holds the reference plus calibration metadata.
 */
@Entity
@Table(name = "floor_plans")
@EntityListeners(AuditingEntityListener.class)
public class FloorPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(nullable = false)
    private String name;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(name = "original_content_type")
    private String originalContentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_system", nullable = false, length = 20)
    private UnitSystem unitSystem = UnitSystem.METERS;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin_anchor", nullable = false, length = 20)
    private OriginAnchor originAnchor = OriginAnchor.TOP_LEFT;

    @Column(name = "origin_x", nullable = false)
    private Double originX = 0.0;

    @Column(name = "origin_y", nullable = false)
    private Double originY = 0.0;

    @Column(name = "viewbox_width")
    private Double viewboxWidth;

    @Column(name = "viewbox_height")
    private Double viewboxHeight;

    @Column(name = "scale_factor")
    private Double scaleFactor;

    @Column(name = "calibration_point1_x")
    private Double calibrationPoint1X;

    @Column(name = "calibration_point1_y")
    private Double calibrationPoint1Y;

    @Column(name = "calibration_point2_x")
    private Double calibrationPoint2X;

    @Column(name = "calibration_point2_y")
    private Double calibrationPoint2Y;

    @Column(name = "calibration_physical_length")
    private Double calibrationPhysicalLength;

    @Column(name = "is_calibrated", nullable = false)
    private Boolean isCalibrated = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public FloorPlan() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getOriginalContentType() { return originalContentType; }
    public void setOriginalContentType(String originalContentType) { this.originalContentType = originalContentType; }

    public UnitSystem getUnitSystem() { return unitSystem; }
    public void setUnitSystem(UnitSystem unitSystem) { this.unitSystem = unitSystem; }

    public OriginAnchor getOriginAnchor() { return originAnchor; }
    public void setOriginAnchor(OriginAnchor originAnchor) { this.originAnchor = originAnchor; }

    public Double getOriginX() { return originX; }
    public void setOriginX(Double originX) { this.originX = originX; }

    public Double getOriginY() { return originY; }
    public void setOriginY(Double originY) { this.originY = originY; }

    public Double getViewboxWidth() { return viewboxWidth; }
    public void setViewboxWidth(Double viewboxWidth) { this.viewboxWidth = viewboxWidth; }

    public Double getViewboxHeight() { return viewboxHeight; }
    public void setViewboxHeight(Double viewboxHeight) { this.viewboxHeight = viewboxHeight; }

    public Double getScaleFactor() { return scaleFactor; }
    public void setScaleFactor(Double scaleFactor) { this.scaleFactor = scaleFactor; }

    public Double getCalibrationPoint1X() { return calibrationPoint1X; }
    public void setCalibrationPoint1X(Double calibrationPoint1X) { this.calibrationPoint1X = calibrationPoint1X; }

    public Double getCalibrationPoint1Y() { return calibrationPoint1Y; }
    public void setCalibrationPoint1Y(Double calibrationPoint1Y) { this.calibrationPoint1Y = calibrationPoint1Y; }

    public Double getCalibrationPoint2X() { return calibrationPoint2X; }
    public void setCalibrationPoint2X(Double calibrationPoint2X) { this.calibrationPoint2X = calibrationPoint2X; }

    public Double getCalibrationPoint2Y() { return calibrationPoint2Y; }
    public void setCalibrationPoint2Y(Double calibrationPoint2Y) { this.calibrationPoint2Y = calibrationPoint2Y; }

    public Double getCalibrationPhysicalLength() { return calibrationPhysicalLength; }
    public void setCalibrationPhysicalLength(Double calibrationPhysicalLength) { this.calibrationPhysicalLength = calibrationPhysicalLength; }

    public Boolean getIsCalibrated() { return isCalibrated; }
    public void setIsCalibrated(Boolean isCalibrated) { this.isCalibrated = isCalibrated; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
