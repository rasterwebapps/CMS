package com.cms.spatial.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cms.spatial.model.enums.ShapeType;
import com.cms.spatial.model.enums.VirtualLocationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A logical location overlaid on a calibrated FloorPlan (a bed, workstation, geofence, etc.),
 * consumed by other modules (asset tracking, access control, clinical ward management) via its
 * id. entityType/entityId are an optional generic anchor back to whichever physical node this
 * location logically belongs to (e.g. a specific Room) — kept generic for the same
 * extractability reason as FloorPlan's own entityType/entityId.
 * geometryJson holds the physical-space (meters/feet) coordinates for the shape: a single
 * {x,y} for POINT, {x,y,width,height} for RECTANGLE, or {points:[{x,y},...]} for POLYGON.
 */
@Entity
@Table(name = "virtual_locations")
@EntityListeners(AuditingEntityListener.class)
public class VirtualLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "floor_plan_id", nullable = false)
    private FloorPlan floorPlan;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(nullable = false)
    private String name;

    @Column(name = "location_type", nullable = false, length = 50)
    private String locationType;

    @Column(name = "module_tag", length = 100)
    private String moduleTag;

    @Enumerated(EnumType.STRING)
    @Column(name = "shape_type", nullable = false, length = 20)
    private ShapeType shapeType;

    @Column(name = "geometry_json", nullable = false)
    private String geometryJson;

    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VirtualLocationStatus status = VirtualLocationStatus.ACTIVE;

    @Column(length = 1000)
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public VirtualLocation() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public FloorPlan getFloorPlan() { return floorPlan; }
    public void setFloorPlan(FloorPlan floorPlan) { this.floorPlan = floorPlan; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocationType() { return locationType; }
    public void setLocationType(String locationType) { this.locationType = locationType; }

    public String getModuleTag() { return moduleTag; }
    public void setModuleTag(String moduleTag) { this.moduleTag = moduleTag; }

    public ShapeType getShapeType() { return shapeType; }
    public void setShapeType(ShapeType shapeType) { this.shapeType = shapeType; }

    public String getGeometryJson() { return geometryJson; }
    public void setGeometryJson(String geometryJson) { this.geometryJson = geometryJson; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public VirtualLocationStatus getStatus() { return status; }
    public void setStatus(VirtualLocationStatus status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
