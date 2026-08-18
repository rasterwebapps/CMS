package com.cms.spatial.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.exception.ResourceNotFoundException;
import com.cms.spatial.dto.VirtualLocationRequest;
import com.cms.spatial.dto.VirtualLocationResponse;
import com.cms.spatial.model.FloorPlan;
import com.cms.spatial.model.VirtualLocation;
import com.cms.spatial.model.enums.ShapeType;
import com.cms.spatial.model.enums.VirtualLocationStatus;
import com.cms.spatial.repository.FloorPlanRepository;
import com.cms.spatial.repository.VirtualLocationRepository;

@ExtendWith(MockitoExtension.class)
class VirtualLocationServiceTest {

    @Mock
    private VirtualLocationRepository virtualLocationRepository;

    @Mock
    private FloorPlanRepository floorPlanRepository;

    @InjectMocks
    private VirtualLocationService service;

    private FloorPlan activeFloorPlan(Long id) {
        FloorPlan plan = new FloorPlan();
        plan.setId(id);
        plan.setIsActive(true);
        return plan;
    }

    @Test
    void createShouldThrowWhenFloorPlanMissing() {
        when(floorPlanRepository.findById(1L)).thenReturn(Optional.empty());

        VirtualLocationRequest req = new VirtualLocationRequest(
            1L, "ROOM", 7L, "Bed 3", "BED", null, ShapeType.POINT, "{\"x\":1,\"y\":2}", 1, null, null);

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Floor plan not found with id: 1");
    }

    @Test
    void createShouldRejectInactiveFloorPlan() {
        FloorPlan plan = activeFloorPlan(1L);
        plan.setIsActive(false);
        when(floorPlanRepository.findById(1L)).thenReturn(Optional.of(plan));

        VirtualLocationRequest req = new VirtualLocationRequest(
            1L, "ROOM", 7L, "Bed 3", "BED", null, ShapeType.POINT, "{\"x\":1,\"y\":2}", 1, null, null);

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not active");
    }

    @Test
    void createShouldDefaultStatusToActive() {
        FloorPlan plan = activeFloorPlan(1L);
        when(floorPlanRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(virtualLocationRepository.save(any(VirtualLocation.class))).thenAnswer(inv -> inv.getArgument(0));

        VirtualLocationRequest req = new VirtualLocationRequest(
            1L, "ROOM", 7L, "Bed 3", "BED", "hostel", ShapeType.POINT, "{\"x\":1,\"y\":2}", 1, null, "near window");

        VirtualLocationResponse out = service.create(req);

        assertThat(out.status()).isEqualTo(VirtualLocationStatus.ACTIVE);
        assertThat(out.floorPlanId()).isEqualTo(1L);
        assertThat(out.name()).isEqualTo("Bed 3");
    }

    @Test
    void updateShouldReassignFloorPlanWhenChanged() {
        FloorPlan originalPlan = activeFloorPlan(1L);
        FloorPlan newPlan = activeFloorPlan(2L);

        VirtualLocation existing = new VirtualLocation();
        existing.setId(50L);
        existing.setFloorPlan(originalPlan);

        when(virtualLocationRepository.findById(50L)).thenReturn(Optional.of(existing));
        when(floorPlanRepository.findById(2L)).thenReturn(Optional.of(newPlan));
        when(virtualLocationRepository.save(any(VirtualLocation.class))).thenAnswer(inv -> inv.getArgument(0));

        VirtualLocationRequest req = new VirtualLocationRequest(
            2L, null, null, "Bed 3", "BED", null, ShapeType.POINT, "{\"x\":1,\"y\":2}", null, null, null);

        VirtualLocationResponse out = service.update(50L, req);

        assertThat(out.floorPlanId()).isEqualTo(2L);
    }

    @Test
    void deleteShouldSoftDelete() {
        VirtualLocation existing = new VirtualLocation();
        existing.setId(50L);
        when(virtualLocationRepository.findById(50L)).thenReturn(Optional.of(existing));
        when(virtualLocationRepository.save(any(VirtualLocation.class))).thenAnswer(inv -> inv.getArgument(0));

        service.delete(50L);

        assertThat(existing.getIsActive()).isFalse();
    }

    @Test
    void findByFloorPlanShouldMapResponses() {
        FloorPlan plan = activeFloorPlan(1L);
        VirtualLocation loc = new VirtualLocation();
        loc.setId(50L);
        loc.setFloorPlan(plan);
        loc.setName("Bed 3");
        loc.setShapeType(ShapeType.POINT);
        loc.setGeometryJson("{}");
        loc.setStatus(VirtualLocationStatus.ACTIVE);

        when(virtualLocationRepository.findByFloorPlanIdAndIsActiveTrue(1L)).thenReturn(List.of(loc));

        List<VirtualLocationResponse> out = service.findByFloorPlan(1L);

        assertThat(out).hasSize(1);
        assertThat(out.getFirst().name()).isEqualTo("Bed 3");
    }
}
