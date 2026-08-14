-- Core Physical Infrastructure & Spatial Visualization Engine — schema-only foundation (Phase 1).
-- Deliberately domain-agnostic: floor_plans/virtual_locations reference the existing
-- Organization/Branch/Block/Floor/Zone/Room hierarchy only via a generic (entity_type, entity_id)
-- pair, never a direct foreign key, so this module stays extractable as a standalone
-- multi-industry "plug and play" component rather than being welded to the college hierarchy.
-- No CAD/DXF parsing yet — floor plans are pre-rendered SVG/image assets uploaded by an admin
-- and stored in MinIO (storage_key), matching the existing FacultyDocument pattern (V93).
CREATE TABLE floor_plans (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    original_file_name VARCHAR(255),
    original_content_type VARCHAR(100),
    unit_system VARCHAR(20) NOT NULL DEFAULT 'METERS',
    origin_anchor VARCHAR(20) NOT NULL DEFAULT 'TOP_LEFT',
    origin_x DOUBLE PRECISION NOT NULL DEFAULT 0,
    origin_y DOUBLE PRECISION NOT NULL DEFAULT 0,
    viewbox_width DOUBLE PRECISION,
    viewbox_height DOUBLE PRECISION,
    scale_factor DOUBLE PRECISION,
    calibration_point1_x DOUBLE PRECISION,
    calibration_point1_y DOUBLE PRECISION,
    calibration_point2_x DOUBLE PRECISION,
    calibration_point2_y DOUBLE PRECISION,
    calibration_physical_length DOUBLE PRECISION,
    is_calibrated BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_floor_plans_entity ON floor_plans (entity_type, entity_id);

CREATE TABLE virtual_locations (
    id BIGSERIAL PRIMARY KEY,
    floor_plan_id BIGINT NOT NULL REFERENCES floor_plans (id),
    entity_type VARCHAR(50),
    entity_id BIGINT,
    name VARCHAR(255) NOT NULL,
    location_type VARCHAR(50) NOT NULL,
    module_tag VARCHAR(100),
    shape_type VARCHAR(20) NOT NULL,
    geometry_json TEXT NOT NULL,
    capacity INTEGER,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    description VARCHAR(1000),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_virtual_locations_floor_plan ON virtual_locations (floor_plan_id);
CREATE INDEX idx_virtual_locations_entity ON virtual_locations (entity_type, entity_id);
