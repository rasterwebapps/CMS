package com.cms.spatial.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.cms.exception.ResourceNotFoundException;
import com.cms.service.StorageService;
import com.cms.spatial.dto.FloorPlanCalibrationRequest;
import com.cms.spatial.dto.FloorPlanCreateRequest;
import com.cms.spatial.dto.FloorPlanResponse;
import com.cms.spatial.dto.FloorPlanUpdateRequest;
import com.cms.spatial.model.FloorPlan;
import com.cms.spatial.model.VirtualLocation;
import com.cms.spatial.model.enums.OriginAnchor;
import com.cms.spatial.model.enums.UnitSystem;
import com.cms.spatial.repository.FloorPlanRepository;
import com.cms.spatial.repository.VirtualLocationRepository;

@ExtendWith(MockitoExtension.class)
class FloorPlanServiceTest {

    @Mock
    private FloorPlanRepository floorPlanRepository;

    @Mock
    private VirtualLocationRepository virtualLocationRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private FloorPlanService service;

    private FloorPlan plan(Long id) {
        FloorPlan plan = new FloorPlan();
        plan.setId(id);
        plan.setEntityType("ROOM");
        plan.setEntityId(7L);
        plan.setName("Ground Floor");
        plan.setStorageKey("floor_plans/existing.svg");
        plan.setUnitSystem(UnitSystem.METERS);
        plan.setOriginAnchor(OriginAnchor.TOP_LEFT);
        plan.setOriginX(0.0);
        plan.setOriginY(0.0);
        return plan;
    }

    @Test
    void findByIdShouldThrowWhenMissing() {
        when(floorPlanRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Floor plan not found with id: 99");
    }

    @Test
    void createShouldUploadFileAndPersist() {
        when(floorPlanRepository.save(any(FloorPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        FloorPlanCreateRequest request = new FloorPlanCreateRequest(
            "ROOM", 7L, "Ground Floor", UnitSystem.METERS, OriginAnchor.TOP_LEFT, 0.0, 0.0, 1000.0, 800.0);
        MultipartFile file = new MockMultipartFile("file", "plan.svg", "image/svg+xml", "<svg/>".getBytes());

        FloorPlanResponse out = service.create(request, file);

        assertThat(out.name()).isEqualTo("Ground Floor");
        assertThat(out.entityType()).isEqualTo("ROOM");
        assertThat(out.originalFileName()).isEqualTo("plan.svg");
        verify(storageService).upload(any(), any(), eq((long) "<svg/>".getBytes().length), eq("image/svg+xml"));
    }

    @Test
    void createShouldRejectNonImageFile() {
        FloorPlanCreateRequest request = new FloorPlanCreateRequest(
            "ROOM", 7L, "Ground Floor", UnitSystem.METERS, OriginAnchor.TOP_LEFT, 0.0, 0.0, null, null);
        MultipartFile file = new MockMultipartFile("file", "plan.txt", "text/plain", "not an image".getBytes());

        assertThatThrownBy(() -> service.create(request, file))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be an image or SVG");
    }

    @Test
    void createShouldRejectEmptyFile() {
        FloorPlanCreateRequest request = new FloorPlanCreateRequest(
            "ROOM", 7L, "Ground Floor", UnitSystem.METERS, OriginAnchor.TOP_LEFT, 0.0, 0.0, null, null);
        MultipartFile file = new MockMultipartFile("file", "plan.svg", "image/svg+xml", new byte[0]);

        assertThatThrownBy(() -> service.create(request, file))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("File is required");
    }

    @Test
    void updateShouldNotTouchStorageKey() {
        FloorPlan existing = plan(1L);
        when(floorPlanRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(floorPlanRepository.save(any(FloorPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        FloorPlanUpdateRequest request = new FloorPlanUpdateRequest(
            "ROOM", 7L, "Renamed", UnitSystem.FEET, OriginAnchor.CENTER, 5.0, 5.0, 1000.0, 800.0);
        FloorPlanResponse out = service.update(1L, request);

        assertThat(out.name()).isEqualTo("Renamed");
        assertThat(out.unitSystem()).isEqualTo(UnitSystem.FEET);
        assertThat(existing.getStorageKey()).isEqualTo("floor_plans/existing.svg");
        verify(storageService, never()).upload(any(), any(), anyLong(), any());
    }

    @Test
    void calibrateShouldDeriveScaleFactorAndMarkCalibrated() {
        FloorPlan existing = plan(1L);
        when(floorPlanRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(floorPlanRepository.save(any(FloorPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        FloorPlanCalibrationRequest request = new FloorPlanCalibrationRequest(0.0, 0.0, 100.0, 0.0, 5.0);
        FloorPlanResponse out = service.calibrate(1L, request);

        assertThat(out.scaleFactor()).isEqualTo(0.05);
        assertThat(out.isCalibrated()).isTrue();
    }

    @Test
    void calibrateShouldRejectCoincidentPoints() {
        FloorPlan existing = plan(1L);
        when(floorPlanRepository.findById(1L)).thenReturn(Optional.of(existing));

        FloorPlanCalibrationRequest request = new FloorPlanCalibrationRequest(10.0, 10.0, 10.0, 10.0, 5.0);

        assertThatThrownBy(() -> service.calibrate(1L, request))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteShouldSoftDeletePlanAndCascadeToVirtualLocations() {
        FloorPlan existing = plan(1L);
        VirtualLocation loc1 = new VirtualLocation();
        loc1.setId(10L);
        VirtualLocation loc2 = new VirtualLocation();
        loc2.setId(11L);

        when(floorPlanRepository.findById(1L)).thenReturn(Optional.of(existing));
        lenient().when(floorPlanRepository.save(any(FloorPlan.class))).thenAnswer(inv -> inv.getArgument(0));
        when(virtualLocationRepository.findByFloorPlanIdAndIsActiveTrue(1L)).thenReturn(List.of(loc1, loc2));

        service.delete(1L);

        assertThat(existing.getIsActive()).isFalse();
        assertThat(loc1.getIsActive()).isFalse();
        assertThat(loc2.getIsActive()).isFalse();
    }

    @Test
    void deleteShouldThrowWhenMissing() {
        when(floorPlanRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(5L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
