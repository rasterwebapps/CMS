package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.BlockRequest;
import com.cms.dto.BlockResponse;
import com.cms.dto.BranchRequest;
import com.cms.dto.BranchResponse;
import com.cms.dto.FloorRequest;
import com.cms.dto.FloorResponse;
import com.cms.dto.HostelRoomRequest;
import com.cms.dto.HostelRoomResponse;
import com.cms.dto.OrganizationRequest;
import com.cms.dto.OrganizationResponse;
import com.cms.dto.RoomRequest;
import com.cms.dto.RoomResponse;
import com.cms.dto.ZoneRequest;
import com.cms.dto.ZoneResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Block;
import com.cms.model.Branch;
import com.cms.model.Floor;
import com.cms.model.HostelRoom;
import com.cms.model.HostelRoomType;
import com.cms.model.Organization;
import com.cms.model.Room;
import com.cms.model.Zone;
import com.cms.model.enums.GenderRestriction;
import com.cms.repository.BlockRepository;
import com.cms.repository.BranchRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.FloorRepository;
import com.cms.repository.HostelRoomRepository;
import com.cms.repository.HostelRoomTypeRepository;
import com.cms.repository.OrganizationRepository;
import com.cms.repository.RoomRepository;
import com.cms.repository.ZoneRepository;

@ExtendWith(MockitoExtension.class)
class CampusInfrastructureServiceTest {

    @Mock private OrganizationRepository organizationRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private BlockRepository blockRepository;
    @Mock private FloorRepository floorRepository;
    @Mock private ZoneRepository zoneRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private HostelRoomRepository hostelRoomRepository;
    @Mock private FacultyRepository facultyRepository;
    @Mock private HostelRoomTypeRepository hostelRoomTypeRepository;

    private CampusInfrastructureService service;

    @BeforeEach
    void setUp() {
        service = new CampusInfrastructureService(organizationRepository, branchRepository, blockRepository,
            floorRepository, zoneRepository, roomRepository, hostelRoomRepository, facultyRepository, hostelRoomTypeRepository);
    }

    // ─── Organizations ───────────────────────────────────────────────────────

    @Test
    void shouldCreateOrganization() {
        OrganizationRequest request = new OrganizationRequest("Raster Images Pvt Ltd", "RASTER", null, true);
        Organization saved = organization(1L, "Raster Images Pvt Ltd", "RASTER");

        when(organizationRepository.existsByNameIgnoreCase("Raster Images Pvt Ltd")).thenReturn(false);
        when(organizationRepository.existsByCodeIgnoreCase("RASTER")).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenReturn(saved);

        OrganizationResponse response = service.createOrganization(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Raster Images Pvt Ltd");
    }

    @Test
    void shouldThrowWhenDuplicateOrganizationName() {
        OrganizationRequest request = new OrganizationRequest("Raster Images Pvt Ltd", "RASTER2", null, null);
        when(organizationRepository.existsByNameIgnoreCase("Raster Images Pvt Ltd")).thenReturn(true);

        assertThatThrownBy(() -> service.createOrganization(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");

        verify(organizationRepository, never()).save(any());
    }

    // ─── Branches ────────────────────────────────────────────────────────────

    @Test
    void shouldCreateBranchUnderOrganization() {
        Organization org = organization(1L, "Raster Images Pvt Ltd", "RASTER");
        BranchRequest request = new BranchRequest("SKSCON Campus", "SKSCON", null, true, 1L);
        Branch saved = branch(1L, org, "SKSCON Campus", "SKSCON");

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(org));
        when(branchRepository.existsByOrganizationIdAndNameIgnoreCase(1L, "SKSCON Campus")).thenReturn(false);
        when(branchRepository.existsByOrganizationIdAndCodeIgnoreCase(1L, "SKSCON")).thenReturn(false);
        when(branchRepository.save(any(Branch.class))).thenReturn(saved);

        BranchResponse response = service.createBranch(request);

        assertThat(response.name()).isEqualTo("SKSCON Campus");
        assertThat(response.organizationId()).isEqualTo(1L);
        assertThat(response.organizationName()).isEqualTo("Raster Images Pvt Ltd");
    }

    @Test
    void shouldThrowWhenCreatingBranchWithoutOrganization() {
        BranchRequest request = new BranchRequest("SKSCON Campus", "SKSCON", null, null, null);

        assertThatThrownBy(() -> service.createBranch(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Organization is required");

        verify(branchRepository, never()).save(any());
    }

    // ─── Blocks ──────────────────────────────────────────────────────────────

    @Test
    void shouldCreateBlockUnderBranch() {
        Branch br = branch(1L, organization(1L, "Org", "ORG"), "SKSCON Campus", "SKSCON");
        BlockRequest request = new BlockRequest("Hostel Block A", "HOSTEL_A", null, true, GenderRestriction.GIRLS, true, 1L);
        Block saved = block(1L, br, "Hostel Block A", "HOSTEL_A");

        when(branchRepository.findById(1L)).thenReturn(Optional.of(br));
        when(blockRepository.existsByBranchIdAndNameIgnoreCase(1L, "Hostel Block A")).thenReturn(false);
        when(blockRepository.existsByBranchIdAndCodeIgnoreCase(1L, "HOSTEL_A")).thenReturn(false);
        when(blockRepository.save(any(Block.class))).thenReturn(saved);

        BlockResponse response = service.createBlock(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Hostel Block A");
        assertThat(response.branchId()).isEqualTo(1L);
        verify(blockRepository).save(any(Block.class));
    }

    @Test
    void shouldCascadeDefaultFloorZoneAndRoomWhenCreatingBlock() {
        Branch br = branch(1L, organization(1L, "Org", "ORG"), "SKSCON Campus", "SKSCON");
        BlockRequest request = new BlockRequest("Hostel Block A", "HOSTEL_A", null, true, GenderRestriction.GIRLS, true, 1L);
        Block savedBlock = block(1L, br, "Hostel Block A", "HOSTEL_A");
        Floor savedFloor = floor(100L, savedBlock, "Ground Floor", 0);
        Zone savedZone = zone(200L, savedFloor, "Main Zone", null, null);

        when(branchRepository.findById(1L)).thenReturn(Optional.of(br));
        when(blockRepository.existsByBranchIdAndNameIgnoreCase(1L, "Hostel Block A")).thenReturn(false);
        when(blockRepository.existsByBranchIdAndCodeIgnoreCase(1L, "HOSTEL_A")).thenReturn(false);
        when(blockRepository.save(any(Block.class))).thenReturn(savedBlock);
        when(floorRepository.save(any(Floor.class))).thenReturn(savedFloor);
        when(zoneRepository.save(any(Zone.class))).thenReturn(savedZone);

        service.createBlock(request);

        ArgumentCaptor<Floor> floorCaptor = ArgumentCaptor.forClass(Floor.class);
        verify(floorRepository).save(floorCaptor.capture());
        assertThat(floorCaptor.getValue().getName()).isEqualTo("Ground Floor");
        assertThat(floorCaptor.getValue().getFloorNumber()).isEqualTo(0);
        assertThat(floorCaptor.getValue().getBlock()).isEqualTo(savedBlock);

        ArgumentCaptor<Zone> zoneCaptor = ArgumentCaptor.forClass(Zone.class);
        verify(zoneRepository).save(zoneCaptor.capture());
        assertThat(zoneCaptor.getValue().getName()).isEqualTo("Main Zone");
        assertThat(zoneCaptor.getValue().getFloor()).isEqualTo(savedFloor);

        ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(roomCaptor.capture());
        assertThat(roomCaptor.getValue().getRoomNumber()).isEqualTo("Main");
        assertThat(roomCaptor.getValue().getZone()).isEqualTo(savedZone);
    }

    @Test
    void shouldThrowWhenDuplicateBlockNameInBranch() {
        Branch br = branch(1L, organization(1L, "Org", "ORG"), "SKSCON Campus", "SKSCON");
        BlockRequest request = new BlockRequest("Hostel Block A", "HOSTEL_A2", null, null, null, null, 1L);

        when(branchRepository.findById(1L)).thenReturn(Optional.of(br));
        when(blockRepository.existsByBranchIdAndNameIgnoreCase(1L, "Hostel Block A")).thenReturn(true);

        assertThatThrownBy(() -> service.createBlock(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");

        verify(blockRepository, never()).save(any());
    }

    @Test
    void shouldCascadeHostelAndGenderFromBlockDownToFloorsAndZones() {
        Branch br = branch(1L, organization(1L, "Org", "ORG"), "SKSCON Campus", "SKSCON");
        Block block = block(1L, br, "Hostel Block A", "HOSTEL_A");
        BlockRequest request = new BlockRequest("Hostel Block A", "HOSTEL_A", null, true, GenderRestriction.BOYS, true, 1L);
        Floor childFloor = floor(10L, block, "Ground Floor", 0);
        Zone childZone = zone(20L, childFloor, "Wing A", null, null);

        when(blockRepository.findById(1L)).thenReturn(Optional.of(block));
        when(branchRepository.findById(1L)).thenReturn(Optional.of(br));
        when(blockRepository.existsByBranchIdAndNameIgnoreCaseAndIdNot(1L, "Hostel Block A", 1L)).thenReturn(false);
        when(blockRepository.existsByBranchIdAndCodeIgnoreCaseAndIdNot(1L, "HOSTEL_A", 1L)).thenReturn(false);
        when(blockRepository.save(any(Block.class))).thenReturn(block);
        when(floorRepository.findByBlockIdOrderByFloorNumberAsc(1L)).thenReturn(List.of(childFloor));
        when(floorRepository.save(any(Floor.class))).thenReturn(childFloor);
        when(zoneRepository.findByFloorIdOrderByOrderIndexAsc(10L)).thenReturn(List.of(childZone));
        when(zoneRepository.save(any(Zone.class))).thenReturn(childZone);

        service.updateBlock(1L, request);

        ArgumentCaptor<Floor> floorCaptor = ArgumentCaptor.forClass(Floor.class);
        verify(floorRepository).save(floorCaptor.capture());
        assertThat(floorCaptor.getValue().getIsHostel()).isTrue();
        assertThat(floorCaptor.getValue().getGenderRestriction()).isEqualTo(GenderRestriction.BOYS);

        ArgumentCaptor<Zone> zoneCaptor = ArgumentCaptor.forClass(Zone.class);
        verify(zoneRepository).save(zoneCaptor.capture());
        assertThat(zoneCaptor.getValue().getIsHostel()).isTrue();
        assertThat(zoneCaptor.getValue().getGenderRestriction()).isEqualTo(GenderRestriction.BOYS);
    }

    @Test
    void shouldCascadeHostelAndGenderFromFloorDownToZonesOnly() {
        Branch br = branch(1L, organization(1L, "Org", "ORG"), "SKSCON Campus", "SKSCON");
        Block block = block(1L, br, "Hostel Block A", "HOSTEL_A");
        Floor floor = floor(10L, block, "Ground Floor", 0);
        FloorRequest request = new FloorRequest("Ground Floor", 0, true, GenderRestriction.GIRLS, null, true, 1L);
        Zone childZone = zone(20L, floor, "Wing A", null, null);

        when(floorRepository.findById(10L)).thenReturn(Optional.of(floor));
        when(blockRepository.findById(1L)).thenReturn(Optional.of(block));
        when(floorRepository.existsByBlockIdAndNameIgnoreCaseAndIdNot(1L, "Ground Floor", 10L)).thenReturn(false);
        when(floorRepository.existsByBlockIdAndFloorNumberAndIdNot(1L, 0, 10L)).thenReturn(false);
        when(floorRepository.save(any(Floor.class))).thenReturn(floor);
        when(zoneRepository.findByFloorIdOrderByOrderIndexAsc(10L)).thenReturn(List.of(childZone));
        when(zoneRepository.save(any(Zone.class))).thenReturn(childZone);

        service.updateFloor(10L, request);

        ArgumentCaptor<Zone> zoneCaptor = ArgumentCaptor.forClass(Zone.class);
        verify(zoneRepository).save(zoneCaptor.capture());
        assertThat(zoneCaptor.getValue().getIsHostel()).isTrue();
        assertThat(zoneCaptor.getValue().getGenderRestriction()).isEqualTo(GenderRestriction.GIRLS);
        verify(blockRepository, never()).save(any());
    }

    // ─── Floors ──────────────────────────────────────────────────────────────

    @Test
    void shouldCreateFloorUnderBlock() {
        Branch br = branch(1L, organization(1L, "Org", "ORG"), "SKSCON Campus", "SKSCON");
        Block block = block(1L, br, "Hostel Block A", "HOSTEL_A");
        FloorRequest request = new FloorRequest("Ground Floor", 0, true, GenderRestriction.GIRLS, null, true, 1L);
        Floor saved = floor(1L, block, "Ground Floor", 0);

        when(blockRepository.findById(1L)).thenReturn(Optional.of(block));
        when(floorRepository.existsByBlockIdAndNameIgnoreCase(1L, "Ground Floor")).thenReturn(false);
        when(floorRepository.existsByBlockIdAndFloorNumber(1L, 0)).thenReturn(false);
        when(floorRepository.save(any(Floor.class))).thenReturn(saved);

        FloorResponse response = service.createFloor(request);

        assertThat(response.name()).isEqualTo("Ground Floor");
        assertThat(response.blockId()).isEqualTo(1L);
        assertThat(response.blockName()).isEqualTo("Hostel Block A");
    }

    @Test
    void shouldThrowWhenFloorNumberDuplicateInBlock() {
        Branch br = branch(1L, organization(1L, "Org", "ORG"), "SKSCON Campus", "SKSCON");
        Block block = block(1L, br, "Hostel Block A", "HOSTEL_A");
        FloorRequest request = new FloorRequest("1st Floor", 0, null, null, null, null, 1L);

        when(blockRepository.findById(1L)).thenReturn(Optional.of(block));
        when(floorRepository.existsByBlockIdAndNameIgnoreCase(1L, "1st Floor")).thenReturn(false);
        when(floorRepository.existsByBlockIdAndFloorNumber(1L, 0)).thenReturn(true);

        assertThatThrownBy(() -> service.createFloor(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");

        verify(floorRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenCreatingFloorWithoutBlock() {
        FloorRequest request = new FloorRequest("Ground Floor", 0, null, null, null, null, null);

        assertThatThrownBy(() -> service.createFloor(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Block is required");

        verify(floorRepository, never()).save(any());
    }

    // ─── Zones ───────────────────────────────────────────────────────────────

    @Test
    void shouldCreateZoneWithGenderRestrictionAndAutoCreateDefaultRoom() {
        Branch br = branch(1L, organization(1L, "Org", "ORG"), "SKSCON Campus", "SKSCON");
        Block block = block(1L, br, "Hostel Block A", "HOSTEL_A");
        Floor floor = floor(1L, block, "Ground Floor", 0);
        ZoneRequest request = new ZoneRequest("Girls Wing", true, GenderRestriction.GIRLS, null, true, 1L);
        Zone saved = zone(1L, floor, "Girls Wing", GenderRestriction.GIRLS, null);

        when(floorRepository.findById(1L)).thenReturn(Optional.of(floor));
        when(zoneRepository.existsByFloorIdAndNameIgnoreCase(1L, "Girls Wing")).thenReturn(false);
        when(zoneRepository.save(any(Zone.class))).thenReturn(saved);

        ZoneResponse response = service.createZone(request);

        assertThat(response.name()).isEqualTo("Girls Wing");
        assertThat(response.genderRestriction()).isEqualTo(GenderRestriction.GIRLS);
        assertThat(response.floorId()).isEqualTo(1L);

        ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository, times(1)).save(roomCaptor.capture());
        assertThat(roomCaptor.getValue().getRoomNumber()).isEqualTo("Main");
        assertThat(roomCaptor.getValue().getZone()).isEqualTo(saved);
    }

    // ─── Rooms ───────────────────────────────────────────────────────────────

    @Test
    void shouldCreateRoomUnderZone() {
        Branch br = branch(1L, organization(1L, "Org", "ORG"), "SKSCON Campus", "SKSCON");
        Block block = block(1L, br, "Hostel Block A", "HOSTEL_A");
        Floor floor = floor(1L, block, "Ground Floor", 0);
        Zone zone = zone(1L, floor, "Girls Wing", GenderRestriction.GIRLS, null);
        RoomRequest request = new RoomRequest("G-101", 2, null, true, 1L);
        Room saved = room(1L, zone, "G-101", 2);

        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(roomRepository.existsByZoneIdAndRoomNumberIgnoreCase(1L, "G-101")).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenReturn(saved);
        when(hostelRoomRepository.findByRoomId(1L)).thenReturn(Optional.empty());

        RoomResponse response = service.createRoom(request);

        assertThat(response.roomNumber()).isEqualTo("G-101");
        assertThat(response.zoneId()).isEqualTo(1L);
        assertThat(response.hostelRoomId()).isNull();
    }

    @Test
    void shouldThrowWhenRoomNumberDuplicateInZone() {
        Branch br = branch(1L, organization(1L, "Org", "ORG"), "SKSCON Campus", "SKSCON");
        Zone zone = zone(1L, floor(1L, block(1L, br, "B", "B"), "F", 0), "Z", null, null);
        RoomRequest request = new RoomRequest("G-101", null, null, null, 1L);

        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(roomRepository.existsByZoneIdAndRoomNumberIgnoreCase(1L, "G-101")).thenReturn(true);

        assertThatThrownBy(() -> service.createRoom(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");

        verify(roomRepository, never()).save(any());
    }

    // ─── Hostel Room attachment ──────────────────────────────────────────────

    @Test
    void shouldAssignHostelRoomToRoom() {
        Branch br = branch(1L, organization(1L, "Org", "ORG"), "SKSCON Campus", "SKSCON");
        Room room = room(1L, zone(1L, floor(1L, block(1L, br, "B", "B"), "F", 0), "Z", null, null), "G-101", 2);
        HostelRoomType roomType = new HostelRoomType("AC Double", "AC_DOUBLE", 2, true, new BigDecimal("45000.00"), null);
        roomType.setId(1L);
        HostelRoomRequest request = new HostelRoomRequest(1L, true);
        HostelRoom saved = new HostelRoom(room, roomType);
        saved.setId(1L);

        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(hostelRoomTypeRepository.findById(1L)).thenReturn(Optional.of(roomType));
        when(hostelRoomRepository.findByRoomId(1L)).thenReturn(Optional.empty());
        when(hostelRoomRepository.save(any(HostelRoom.class))).thenReturn(saved);

        HostelRoomResponse response = service.assignHostelRoom(1L, request);

        assertThat(response.roomId()).isEqualTo(1L);
        assertThat(response.roomTypeName()).isEqualTo("AC Double");
        assertThat(response.sharingCapacity()).isEqualTo(2);
    }

    @Test
    void shouldThrowWhenUnassigningRoomWithNoHostelRoom() {
        when(hostelRoomRepository.findByRoomId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.unassignHostelRoom(1L))
            .isInstanceOf(ResourceNotFoundException.class);

        verify(hostelRoomRepository, never()).delete(any());
    }

    @Test
    void shouldFindFloorsByBlock() {
        Branch br = branch(1L, organization(1L, "Org", "ORG"), "SKSCON Campus", "SKSCON");
        Block block = block(1L, br, "Hostel Block A", "HOSTEL_A");
        Floor floor = floor(1L, block, "Ground Floor", 0);
        when(blockRepository.findById(1L)).thenReturn(Optional.of(block));
        when(floorRepository.findByBlockIdOrderByFloorNumberAsc(1L)).thenReturn(List.of(floor));

        List<FloorResponse> responses = service.findFloorsByBlock(1L);

        assertThat(responses).hasSize(1);
    }

    @Test
    void shouldThrowWhenFindingFloorsForNonExistentBlock() {
        when(blockRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findFloorsByBlock(999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    private Organization organization(Long id, String name, String code) {
        Organization o = new Organization(name, code, null);
        o.setId(id);
        Instant now = Instant.now();
        o.setCreatedAt(now);
        o.setUpdatedAt(now);
        return o;
    }

    private Branch branch(Long id, Organization organization, String name, String code) {
        Branch b = new Branch(organization, name, code, null);
        b.setId(id);
        Instant now = Instant.now();
        b.setCreatedAt(now);
        b.setUpdatedAt(now);
        return b;
    }

    private Block block(Long id, Branch branch, String name, String code) {
        Block b = new Block(branch, name, code, null);
        b.setId(id);
        Instant now = Instant.now();
        b.setCreatedAt(now);
        b.setUpdatedAt(now);
        return b;
    }

    private Floor floor(Long id, Block block, String name, Integer floorNumber) {
        Floor f = new Floor(block, name, floorNumber);
        f.setId(id);
        Instant now = Instant.now();
        f.setCreatedAt(now);
        f.setUpdatedAt(now);
        return f;
    }

    private Zone zone(Long id, Floor floor, String name, GenderRestriction genderRestriction, com.cms.model.Faculty warden) {
        Zone z = new Zone(floor, name, genderRestriction, warden);
        z.setId(id);
        Instant now = Instant.now();
        z.setCreatedAt(now);
        z.setUpdatedAt(now);
        return z;
    }

    private Room room(Long id, Zone zone, String roomNumber, Integer capacity) {
        Room r = new Room(zone, roomNumber, capacity, null);
        r.setId(id);
        Instant now = Instant.now();
        r.setCreatedAt(now);
        r.setUpdatedAt(now);
        return r;
    }
}
