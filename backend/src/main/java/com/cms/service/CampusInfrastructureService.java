package com.cms.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
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
import com.cms.dto.ReorderRequest;
import com.cms.dto.RoomRequest;
import com.cms.dto.RoomResponse;
import com.cms.dto.ZoneRequest;
import com.cms.dto.ZoneResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Block;
import com.cms.model.Branch;
import com.cms.model.Faculty;
import com.cms.model.Floor;
import com.cms.model.HostelRoom;
import com.cms.model.HostelRoomType;
import com.cms.model.Organization;
import com.cms.model.Room;
import com.cms.model.RoomPurposeCategory;
import com.cms.model.RoomSubType;
import com.cms.model.Zone;
import com.cms.repository.BlockRepository;
import com.cms.repository.BranchRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.FloorRepository;
import com.cms.repository.HostelRoomRepository;
import com.cms.repository.HostelRoomTypeRepository;
import com.cms.repository.OrganizationRepository;
import com.cms.repository.RoomPurposeCategoryRepository;
import com.cms.repository.RoomRepository;
import com.cms.repository.RoomSubTypeRepository;
import com.cms.repository.ZoneRepository;

@Service
@Transactional(readOnly = true)
public class CampusInfrastructureService {

    /** Room number auto-assigned when a Zone is created — every Zone always has >=1 Room, so
     *  Stores/inventory attachment can always target a room_id uniformly, even for undivided
     *  spaces (e.g. a lab spanning a whole zone with no internal partitions). */
    private static final String DEFAULT_ROOM_NUMBER = "Main";

    /** Every Floor always has >=1 Zone, and every Block always has >=1 Floor — same rationale as
     *  DEFAULT_ROOM_NUMBER above: the Campus Setup builder screen assumes a Block/Floor is never
     *  structurally empty, so it can always render at least one Floor/Zone/Room without the admin
     *  having to add one manually for the common single-floor, single-zone building. */
    private static final String DEFAULT_FLOOR_NAME = "Ground Floor";
    private static final int DEFAULT_FLOOR_NUMBER = 0;
    private static final String DEFAULT_ZONE_NAME = "Main Zone";

    private final OrganizationRepository organizationRepository;
    private final BranchRepository branchRepository;
    private final BlockRepository blockRepository;
    private final FloorRepository floorRepository;
    private final ZoneRepository zoneRepository;
    private final RoomRepository roomRepository;
    private final HostelRoomRepository hostelRoomRepository;
    private final FacultyRepository facultyRepository;
    private final HostelRoomTypeRepository hostelRoomTypeRepository;
    private final RoomPurposeCategoryRepository roomPurposeCategoryRepository;
    private final RoomSubTypeRepository roomSubTypeRepository;

    public CampusInfrastructureService(OrganizationRepository organizationRepository,
                                        BranchRepository branchRepository,
                                        BlockRepository blockRepository,
                                        FloorRepository floorRepository,
                                        ZoneRepository zoneRepository,
                                        RoomRepository roomRepository,
                                        HostelRoomRepository hostelRoomRepository,
                                        FacultyRepository facultyRepository,
                                        HostelRoomTypeRepository hostelRoomTypeRepository,
                                        RoomPurposeCategoryRepository roomPurposeCategoryRepository,
                                        RoomSubTypeRepository roomSubTypeRepository) {
        this.organizationRepository = organizationRepository;
        this.branchRepository = branchRepository;
        this.blockRepository = blockRepository;
        this.floorRepository = floorRepository;
        this.zoneRepository = zoneRepository;
        this.roomRepository = roomRepository;
        this.hostelRoomRepository = hostelRoomRepository;
        this.facultyRepository = facultyRepository;
        this.hostelRoomTypeRepository = hostelRoomTypeRepository;
        this.roomPurposeCategoryRepository = roomPurposeCategoryRepository;
        this.roomSubTypeRepository = roomSubTypeRepository;
    }

    // ─── Organizations ───────────────────────────────────────────────────────

    public List<OrganizationResponse> findAllOrganizations() {
        return organizationRepository.findAllByOrderByNameAsc().stream().map(this::toOrganizationResponse).toList();
    }

    public List<OrganizationResponse> findActiveOrganizations() {
        return organizationRepository.findByIsActiveTrueOrderByNameAsc().stream().map(this::toOrganizationResponse).toList();
    }

    public OrganizationResponse findOrganizationById(Long id) {
        return toOrganizationResponse(fetchOrganization(id));
    }

    @Transactional
    public OrganizationResponse createOrganization(OrganizationRequest request) {
        String name = requireTrimmed(request.name(), "Organization name is required");
        String code = requireTrimmed(request.code(), "Organization code is required");
        if (organizationRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("An organization with the name '" + name + "' already exists");
        }
        if (organizationRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("An organization with the code '" + code + "' already exists");
        }
        Organization organization = new Organization(name, code.toUpperCase(), trim(request.description()));
        if (request.isActive() != null) organization.setIsActive(request.isActive());
        return toOrganizationResponse(organizationRepository.save(organization));
    }

    @Transactional
    public OrganizationResponse updateOrganization(Long id, OrganizationRequest request) {
        Organization organization = fetchOrganization(id);
        String name = requireTrimmed(request.name(), "Organization name is required");
        String code = requireTrimmed(request.code(), "Organization code is required");
        if (organizationRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException("An organization with the name '" + name + "' already exists");
        }
        if (organizationRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new IllegalArgumentException("An organization with the code '" + code + "' already exists");
        }
        organization.setName(name);
        organization.setCode(code.toUpperCase());
        organization.setDescription(trim(request.description()));
        if (request.isActive() != null) organization.setIsActive(request.isActive());
        return toOrganizationResponse(organizationRepository.save(organization));
    }

    @Transactional
    public void deleteOrganization(Long id) {
        if (!organizationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Organization not found with id: " + id);
        }
        organizationRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateOrganizationStatus(Long id, ActiveStatusUpdateRequest request) {
        Organization organization = fetchOrganization(id);
        organization.setIsActive(Boolean.TRUE.equals(request.isActive()));
        Organization saved = organizationRepository.save(organization);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    public boolean organizationNameExists(String name, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        if (excludeId != null) return organizationRepository.existsByNameIgnoreCaseAndIdNot(trimmed, excludeId);
        return organizationRepository.existsByNameIgnoreCase(trimmed);
    }

    public boolean organizationCodeExists(String code, Long excludeId) {
        String trimmed = code == null ? "" : code.trim();
        if (excludeId != null) return organizationRepository.existsByCodeIgnoreCaseAndIdNot(trimmed, excludeId);
        return organizationRepository.existsByCodeIgnoreCase(trimmed);
    }

    // ─── Branches ────────────────────────────────────────────────────────────

    public List<BranchResponse> findBranchesByOrganization(Long organizationId) {
        fetchOrganization(organizationId);
        return branchRepository.findByOrganizationIdOrderByNameAsc(organizationId).stream().map(this::toBranchResponse).toList();
    }

    public List<BranchResponse> findActiveBranchesByOrganization(Long organizationId) {
        fetchOrganization(organizationId);
        return branchRepository.findByOrganizationIdAndIsActiveTrueOrderByNameAsc(organizationId).stream().map(this::toBranchResponse).toList();
    }

    public BranchResponse findBranchById(Long id) {
        return toBranchResponse(fetchBranch(id));
    }

    @Transactional
    public BranchResponse createBranch(BranchRequest request) {
        Organization organization = resolveOrganization(request.organizationId());
        String name = requireTrimmed(request.name(), "Branch name is required");
        String code = requireTrimmed(request.code(), "Branch code is required");
        if (branchRepository.existsByOrganizationIdAndNameIgnoreCase(organization.getId(), name)) {
            throw new IllegalArgumentException("A branch with the name '" + name + "' already exists in this organization");
        }
        if (branchRepository.existsByOrganizationIdAndCodeIgnoreCase(organization.getId(), code)) {
            throw new IllegalArgumentException("A branch with the code '" + code + "' already exists in this organization");
        }
        Branch branch = new Branch(organization, name, code.toUpperCase(), trim(request.description()));
        if (request.isActive() != null) branch.setIsActive(request.isActive());
        return toBranchResponse(branchRepository.save(branch));
    }

    @Transactional
    public BranchResponse updateBranch(Long id, BranchRequest request) {
        Branch branch = fetchBranch(id);
        Organization organization = resolveOrganization(request.organizationId() != null ? request.organizationId() : branch.getOrganization().getId());
        String name = requireTrimmed(request.name(), "Branch name is required");
        String code = requireTrimmed(request.code(), "Branch code is required");
        if (branchRepository.existsByOrganizationIdAndNameIgnoreCaseAndIdNot(organization.getId(), name, id)) {
            throw new IllegalArgumentException("A branch with the name '" + name + "' already exists in this organization");
        }
        if (branchRepository.existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(organization.getId(), code, id)) {
            throw new IllegalArgumentException("A branch with the code '" + code + "' already exists in this organization");
        }
        branch.setOrganization(organization);
        branch.setName(name);
        branch.setCode(code.toUpperCase());
        branch.setDescription(trim(request.description()));
        if (request.isActive() != null) branch.setIsActive(request.isActive());
        return toBranchResponse(branchRepository.save(branch));
    }

    @Transactional
    public void deleteBranch(Long id) {
        if (!branchRepository.existsById(id)) {
            throw new ResourceNotFoundException("Branch not found with id: " + id);
        }
        branchRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateBranchStatus(Long id, ActiveStatusUpdateRequest request) {
        Branch branch = fetchBranch(id);
        branch.setIsActive(Boolean.TRUE.equals(request.isActive()));
        Branch saved = branchRepository.save(branch);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    // ─── Blocks ──────────────────────────────────────────────────────────────

    public List<BlockResponse> findBlocksByBranch(Long branchId) {
        fetchBranch(branchId);
        return blockRepository.findByBranchIdOrderByOrderIndexAsc(branchId).stream().map(this::toBlockResponse).toList();
    }

    public List<BlockResponse> findActiveBlocksByBranch(Long branchId) {
        fetchBranch(branchId);
        return blockRepository.findByBranchIdAndIsActiveTrueOrderByOrderIndexAsc(branchId).stream().map(this::toBlockResponse).toList();
    }

    public BlockResponse findBlockById(Long id) {
        return toBlockResponse(fetchBlock(id));
    }

    @Transactional
    public BlockResponse createBlock(BlockRequest request) {
        Branch branch = resolveBranch(request.branchId());
        String name = requireTrimmed(request.name(), "Block name is required");
        String code = requireTrimmed(request.code(), "Block code is required");
        if (blockRepository.existsByBranchIdAndNameIgnoreCase(branch.getId(), name)) {
            throw new IllegalArgumentException("A block with the name '" + name + "' already exists in this branch");
        }
        if (blockRepository.existsByBranchIdAndCodeIgnoreCase(branch.getId(), code)) {
            throw new IllegalArgumentException("A block with the code '" + code + "' already exists in this branch");
        }
        Block block = new Block(branch, name, code.toUpperCase(), trim(request.description()));
        block.setIsHostel(Boolean.TRUE.equals(request.isHostel()));
        block.setGenderRestriction(request.genderRestriction());
        block.setOrderIndex(nextOrderIndex(blockRepository.findByBranchIdOrderByOrderIndexAsc(branch.getId()), Block::getOrderIndex));
        if (request.isActive() != null) block.setIsActive(request.isActive());
        Block saved = blockRepository.save(block);
        createDefaultFloor(saved);
        return toBlockResponse(saved);
    }

    /** Reorders the Blocks in a Branch to match `orderedIds` — must be exactly the set of Block ids
     *  currently in that Branch, just in the desired sequence (drag-to-reorder in Campus Setup's
     *  skyline view never moves a Block to a different Branch, only reorders siblings). */
    @Transactional
    public void reorderBlocks(Long branchId, ReorderRequest request) {
        fetchBranch(branchId);
        List<Block> blocks = blockRepository.findByBranchIdOrderByOrderIndexAsc(branchId);
        applyOrder(blocks, Block::getId, Block::setOrderIndex, request.orderedIds(), blockRepository::save,
            "Reorder list must contain exactly the blocks currently in this branch");
    }

    @Transactional
    public BlockResponse updateBlock(Long id, BlockRequest request) {
        Block block = fetchBlock(id);
        Branch branch = resolveBranch(request.branchId() != null ? request.branchId() : block.getBranch().getId());
        String name = requireTrimmed(request.name(), "Block name is required");
        String code = requireTrimmed(request.code(), "Block code is required");
        if (blockRepository.existsByBranchIdAndNameIgnoreCaseAndIdNot(branch.getId(), name, id)) {
            throw new IllegalArgumentException("A block with the name '" + name + "' already exists in this branch");
        }
        if (blockRepository.existsByBranchIdAndCodeIgnoreCaseAndIdNot(branch.getId(), code, id)) {
            throw new IllegalArgumentException("A block with the code '" + code + "' already exists in this branch");
        }
        block.setBranch(branch);
        block.setName(name);
        block.setCode(code.toUpperCase());
        block.setDescription(trim(request.description()));
        block.setIsHostel(Boolean.TRUE.equals(request.isHostel()));
        block.setGenderRestriction(request.genderRestriction());
        if (request.isActive() != null) block.setIsActive(request.isActive());
        Block saved = blockRepository.save(block);
        cascadeBlockToChildren(saved);
        return toBlockResponse(saved);
    }

    @Transactional
    public void deleteBlock(Long id) {
        if (!blockRepository.existsById(id)) {
            throw new ResourceNotFoundException("Block not found with id: " + id);
        }
        blockRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateBlockStatus(Long id, ActiveStatusUpdateRequest request) {
        Block block = fetchBlock(id);
        block.setIsActive(Boolean.TRUE.equals(request.isActive()));
        Block saved = blockRepository.save(block);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    // ─── Floors ──────────────────────────────────────────────────────────────

    public List<FloorResponse> findFloorsByBlock(Long blockId) {
        fetchBlock(blockId);
        return floorRepository.findByBlockIdOrderByFloorNumberAsc(blockId).stream().map(this::toFloorResponse).toList();
    }

    public List<FloorResponse> findActiveFloorsByBlock(Long blockId) {
        fetchBlock(blockId);
        return floorRepository.findByBlockIdAndIsActiveTrueOrderByFloorNumberAsc(blockId).stream().map(this::toFloorResponse).toList();
    }

    public FloorResponse findFloorById(Long id) {
        return toFloorResponse(fetchFloor(id));
    }

    @Transactional
    public FloorResponse createFloor(FloorRequest request) {
        Block block = resolveBlock(request.blockId());
        String name = requireTrimmed(request.name(), "Floor name is required");
        if (request.floorNumber() == null) {
            throw new IllegalArgumentException("Floor number is required");
        }
        if (floorRepository.existsByBlockIdAndNameIgnoreCase(block.getId(), name)) {
            throw new IllegalArgumentException("A floor with the name '" + name + "' already exists in this block");
        }
        if (floorRepository.existsByBlockIdAndFloorNumber(block.getId(), request.floorNumber())) {
            throw new IllegalArgumentException("Floor number " + request.floorNumber() + " already exists in this block");
        }
        Floor floor = new Floor(block, name, request.floorNumber());
        floor.setIsHostel(Boolean.TRUE.equals(request.isHostel()));
        floor.setGenderRestriction(request.genderRestriction());
        floor.setIsBasement(Boolean.TRUE.equals(request.isBasement()));
        if (request.isActive() != null) floor.setIsActive(request.isActive());
        Floor saved = floorRepository.save(floor);
        createDefaultZone(saved);
        return toFloorResponse(saved);
    }

    @Transactional
    public FloorResponse updateFloor(Long id, FloorRequest request) {
        Floor floor = fetchFloor(id);
        Block block = resolveBlock(request.blockId() != null ? request.blockId() : floor.getBlock().getId());
        String name = requireTrimmed(request.name(), "Floor name is required");
        if (request.floorNumber() == null) {
            throw new IllegalArgumentException("Floor number is required");
        }
        if (floorRepository.existsByBlockIdAndNameIgnoreCaseAndIdNot(block.getId(), name, id)) {
            throw new IllegalArgumentException("A floor with the name '" + name + "' already exists in this block");
        }
        if (floorRepository.existsByBlockIdAndFloorNumberAndIdNot(block.getId(), request.floorNumber(), id)) {
            throw new IllegalArgumentException("Floor number " + request.floorNumber() + " already exists in this block");
        }
        floor.setBlock(block);
        floor.setName(name);
        floor.setFloorNumber(request.floorNumber());
        floor.setIsHostel(Boolean.TRUE.equals(request.isHostel()));
        floor.setGenderRestriction(request.genderRestriction());
        floor.setIsBasement(Boolean.TRUE.equals(request.isBasement()));
        if (request.isActive() != null) floor.setIsActive(request.isActive());
        Floor saved = floorRepository.save(floor);
        cascadeFloorToChildren(saved);
        return toFloorResponse(saved);
    }

    @Transactional
    public void deleteFloor(Long id) {
        if (!floorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Floor not found with id: " + id);
        }
        floorRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateFloorStatus(Long id, ActiveStatusUpdateRequest request) {
        Floor floor = fetchFloor(id);
        floor.setIsActive(Boolean.TRUE.equals(request.isActive()));
        Floor saved = floorRepository.save(floor);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    // ─── Zones ───────────────────────────────────────────────────────────────

    public List<ZoneResponse> findZonesByFloor(Long floorId) {
        fetchFloor(floorId);
        return zoneRepository.findByFloorIdOrderByOrderIndexAsc(floorId).stream().map(this::toZoneResponse).toList();
    }

    public List<ZoneResponse> findActiveZonesByFloor(Long floorId) {
        fetchFloor(floorId);
        return zoneRepository.findByFloorIdAndIsActiveTrueOrderByOrderIndexAsc(floorId).stream().map(this::toZoneResponse).toList();
    }

    /** Flat, campus-wide list of active zones — for pickers (e.g. Room Preference) that need a
     *  zone choice without walking the Organization/Branch/Block/Floor hierarchy first. */
    public List<ZoneResponse> findAllActiveZones() {
        return zoneRepository.findByIsActiveTrueOrderByNameAsc().stream().map(this::toZoneResponse).toList();
    }

    public ZoneResponse findZoneById(Long id) {
        return toZoneResponse(fetchZone(id));
    }

    /** Every Zone always has >=1 Room — creating a Zone auto-creates one default Room ("Main"),
     *  so Stores/inventory attachment always has a room_id to target, even for a lab or space that
     *  spans the whole zone with no internal partitions. The admin can add more rooms afterward if
     *  the zone turns out to be subdivided, or rename/repurpose the default one. */
    @Transactional
    public ZoneResponse createZone(ZoneRequest request) {
        Floor floor = resolveFloor(request.floorId());
        String name = requireTrimmed(request.name(), "Zone name is required");
        if (zoneRepository.existsByFloorIdAndNameIgnoreCase(floor.getId(), name)) {
            throw new IllegalArgumentException("A zone with the name '" + name + "' already exists on this floor");
        }
        Faculty warden = resolveWarden(request.wardenId());
        Zone zone = new Zone(floor, name, request.genderRestriction(), warden);
        zone.setIsHostel(Boolean.TRUE.equals(request.isHostel()));
        zone.setOrderIndex(nextOrderIndex(zoneRepository.findByFloorIdOrderByOrderIndexAsc(floor.getId()), Zone::getOrderIndex));
        if (request.isActive() != null) zone.setIsActive(request.isActive());
        Zone saved = zoneRepository.save(zone);
        createDefaultRoom(saved);
        return toZoneResponse(saved);
    }

    /** Reorders the Zones on a Floor to match `orderedIds` — must be exactly the set of Zone ids
     *  currently on that Floor, just in the desired sequence (drag-to-reorder in Campus Setup's
     *  skyline view never moves a Zone to a different Floor, only reorders siblings). */
    @Transactional
    public void reorderZones(Long floorId, ReorderRequest request) {
        fetchFloor(floorId);
        List<Zone> zones = zoneRepository.findByFloorIdOrderByOrderIndexAsc(floorId);
        applyOrder(zones, Zone::getId, Zone::setOrderIndex, request.orderedIds(), zoneRepository::save,
            "Reorder list must contain exactly the zones currently on this floor");
    }

    @Transactional
    public ZoneResponse updateZone(Long id, ZoneRequest request) {
        Zone zone = fetchZone(id);
        Floor floor = resolveFloor(request.floorId() != null ? request.floorId() : zone.getFloor().getId());
        String name = requireTrimmed(request.name(), "Zone name is required");
        if (zoneRepository.existsByFloorIdAndNameIgnoreCaseAndIdNot(floor.getId(), name, id)) {
            throw new IllegalArgumentException("A zone with the name '" + name + "' already exists on this floor");
        }
        zone.setFloor(floor);
        zone.setName(name);
        zone.setIsHostel(Boolean.TRUE.equals(request.isHostel()));
        zone.setGenderRestriction(request.genderRestriction());
        zone.setWarden(resolveWarden(request.wardenId()));
        if (request.isActive() != null) zone.setIsActive(request.isActive());
        return toZoneResponse(zoneRepository.save(zone));
    }

    @Transactional
    public void deleteZone(Long id) {
        if (!zoneRepository.existsById(id)) {
            throw new ResourceNotFoundException("Zone not found with id: " + id);
        }
        zoneRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateZoneStatus(Long id, ActiveStatusUpdateRequest request) {
        Zone zone = fetchZone(id);
        zone.setIsActive(Boolean.TRUE.equals(request.isActive()));
        Zone saved = zoneRepository.save(zone);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    // ─── Rooms ───────────────────────────────────────────────────────────────

    public List<RoomResponse> findRoomsByZone(Long zoneId) {
        fetchZone(zoneId);
        return roomRepository.findByZoneIdOrderByOrderIndexAsc(zoneId).stream().map(this::toRoomResponse).toList();
    }

    public List<RoomResponse> findActiveRoomsByZone(Long zoneId) {
        fetchZone(zoneId);
        return roomRepository.findByZoneIdAndIsActiveTrueOrderByOrderIndexAsc(zoneId).stream().map(this::toRoomResponse).toList();
    }

    public RoomResponse findRoomById(Long id) {
        return toRoomResponse(fetchRoom(id));
    }

    @Transactional
    public RoomResponse createRoom(RoomRequest request) {
        Zone zone = resolveZone(request.zoneId());
        String roomNumber = requireTrimmed(request.roomNumber(), "Room number is required");
        if (roomRepository.existsByZoneIdAndRoomNumberIgnoreCase(zone.getId(), roomNumber)) {
            throw new IllegalArgumentException("Room '" + roomNumber + "' already exists in this zone");
        }
        Room room = new Room(zone, roomNumber, request.capacity(), trim(request.description()));
        room.setOrderIndex(nextOrderIndex(roomRepository.findByZoneIdOrderByOrderIndexAsc(zone.getId()), Room::getOrderIndex));
        if (request.isActive() != null) room.setIsActive(request.isActive());
        applyPurposeClassification(room, request.purposeCategoryId(), request.subTypeId());
        return toRoomResponse(roomRepository.save(room));
    }

    /** Reorders the Rooms in a Zone to match `orderedIds` — must be exactly the set of Room ids
     *  currently in that Zone, just in the desired sequence (drag-to-reorder in Campus Setup's
     *  skyline view never moves a Room to a different Zone, only reorders siblings). */
    @Transactional
    public void reorderRooms(Long zoneId, ReorderRequest request) {
        fetchZone(zoneId);
        List<Room> rooms = roomRepository.findByZoneIdOrderByOrderIndexAsc(zoneId);
        applyOrder(rooms, Room::getId, Room::setOrderIndex, request.orderedIds(), roomRepository::save,
            "Reorder list must contain exactly the rooms currently in this zone");
    }

    @Transactional
    public RoomResponse updateRoom(Long id, RoomRequest request) {
        Room room = fetchRoom(id);
        Zone zone = resolveZone(request.zoneId() != null ? request.zoneId() : room.getZone().getId());
        String roomNumber = requireTrimmed(request.roomNumber(), "Room number is required");
        if (roomRepository.existsByZoneIdAndRoomNumberIgnoreCaseAndIdNot(zone.getId(), roomNumber, id)) {
            throw new IllegalArgumentException("Room '" + roomNumber + "' already exists in this zone");
        }
        room.setZone(zone);
        room.setRoomNumber(roomNumber);
        room.setCapacity(request.capacity());
        room.setDescription(trim(request.description()));
        if (request.isActive() != null) room.setIsActive(request.isActive());
        applyPurposeClassification(room, request.purposeCategoryId(), request.subTypeId());
        return toRoomResponse(roomRepository.save(room));
    }

    /** Sets/clears Room Purpose Classification, validating the sub-type (if given) actually belongs
     *  to the given category — both tiers are optional so unclassified rooms remain valid. */
    private void applyPurposeClassification(Room room, Long purposeCategoryId, Long subTypeId) {
        if (purposeCategoryId == null) {
            if (subTypeId != null) {
                throw new IllegalArgumentException("A purpose category is required when a sub-type is set");
            }
            room.setPurposeCategory(null);
            room.setSubType(null);
            return;
        }
        RoomPurposeCategory category = roomPurposeCategoryRepository.findById(purposeCategoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Room purpose category not found with id: " + purposeCategoryId));
        room.setPurposeCategory(category);

        if (subTypeId == null) {
            room.setSubType(null);
            return;
        }
        RoomSubType subType = roomSubTypeRepository.findById(subTypeId)
            .orElseThrow(() -> new ResourceNotFoundException("Room sub-type not found with id: " + subTypeId));
        if (!subType.getPurposeCategory().getId().equals(category.getId())) {
            throw new IllegalArgumentException("Sub-type '" + subType.getName() + "' does not belong to the selected purpose category");
        }
        room.setSubType(subType);
    }

    @Transactional
    public void deleteRoom(Long id) {
        if (!roomRepository.existsById(id)) {
            throw new ResourceNotFoundException("Room not found with id: " + id);
        }
        roomRepository.deleteById(id);
    }

    @Transactional
    public ActiveStatusUpdateResponse updateRoomStatus(Long id, ActiveStatusUpdateRequest request) {
        Room room = fetchRoom(id);
        room.setIsActive(Boolean.TRUE.equals(request.isActive()));
        Room saved = roomRepository.save(room);
        return new ActiveStatusUpdateResponse(saved.getId(), saved.getIsActive(), saved.getUpdatedAt());
    }

    // ─── Hostel Rooms (Room + HostelRoomType attachment) ────────────────────

    public HostelRoomResponse findHostelRoomByRoomId(Long roomId) {
        HostelRoom hostelRoom = hostelRoomRepository.findByRoomId(roomId)
            .orElseThrow(() -> new ResourceNotFoundException("Room " + roomId + " is not designated as a hostel room"));
        return toHostelRoomResponse(hostelRoom);
    }

    /** Designates a room as a hostel room (or updates its room type if already designated). */
    @Transactional
    public HostelRoomResponse assignHostelRoom(Long roomId, HostelRoomRequest request) {
        Room room = fetchRoom(roomId);
        if (room.getPurposeCategory() == null || !Boolean.TRUE.equals(room.getPurposeCategory().getIsResidential())) {
            throw new IllegalArgumentException(
                "Room must be classified under a Residential purpose category before it can be designated a hostel room");
        }
        HostelRoomType roomType = hostelRoomTypeRepository.findById(request.roomTypeId())
            .orElseThrow(() -> new ResourceNotFoundException("Hostel room type not found with id: " + request.roomTypeId()));

        HostelRoom hostelRoom = hostelRoomRepository.findByRoomId(roomId).orElse(null);
        if (hostelRoom == null) {
            hostelRoom = new HostelRoom(room, roomType);
        } else {
            hostelRoom.setRoomType(roomType);
        }
        if (request.isActive() != null) hostelRoom.setIsActive(request.isActive());
        return toHostelRoomResponse(hostelRoomRepository.save(hostelRoom));
    }

    /** Removes a room's hostel-room designation (the physical Room itself is untouched). */
    @Transactional
    public void unassignHostelRoom(Long roomId) {
        HostelRoom hostelRoom = hostelRoomRepository.findByRoomId(roomId)
            .orElseThrow(() -> new ResourceNotFoundException("Room " + roomId + " is not designated as a hostel room"));
        hostelRoomRepository.delete(hostelRoom);
    }

    // ─── Default structural cascade ──────────────────────────────────────────
    // A Block/Floor is never left structurally empty: creating a Block auto-creates one default
    // Floor, which auto-creates one default Zone, which auto-creates one default Room — so the
    // common case (a simple building with one floor, one zone, one room) needs no extra steps, and
    // the Campus Setup builder can always render at least one Floor/Zone/Room per Block/Floor/Zone.

    private void createDefaultFloor(Block block) {
        Floor floor = new Floor(block, DEFAULT_FLOOR_NAME, DEFAULT_FLOOR_NUMBER);
        Floor saved = floorRepository.save(floor);
        createDefaultZone(saved);
    }

    private void createDefaultZone(Floor floor) {
        Zone zone = new Zone(floor, DEFAULT_ZONE_NAME, null, null);
        Zone saved = zoneRepository.save(zone);
        createDefaultRoom(saved);
    }

    private void createDefaultRoom(Zone zone) {
        roomRepository.save(new Room(zone, DEFAULT_ROOM_NUMBER, null, "Auto-created default room"));
    }

    // ─── Hostel/gender cascade ───────────────────────────────────────────────
    // "Admin can choose at block or floor or zone, and the child under that also gets marked as
    // boys/girls respectively" — setting isHostel/genderRestriction on a Block or Floor overwrites
    // the same values on every level underneath. Not an enforced invariant: a child can be
    // independently re-edited afterward to differ from its parent's last cascade (the whole point
    // is "not permanent for a single gender").

    private void cascadeBlockToChildren(Block block) {
        for (Floor floor : floorRepository.findByBlockIdOrderByFloorNumberAsc(block.getId())) {
            floor.setIsHostel(block.getIsHostel());
            floor.setGenderRestriction(block.getGenderRestriction());
            floorRepository.save(floor);
            cascadeFloorToChildren(floor);
        }
    }

    private void cascadeFloorToChildren(Floor floor) {
        for (Zone zone : zoneRepository.findByFloorIdOrderByOrderIndexAsc(floor.getId())) {
            zone.setIsHostel(floor.getIsHostel());
            zone.setGenderRestriction(floor.getGenderRestriction());
            zoneRepository.save(zone);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Organization resolveOrganization(Long organizationId) {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization is required");
        }
        return fetchOrganization(organizationId);
    }

    private Branch resolveBranch(Long branchId) {
        if (branchId == null) {
            throw new IllegalArgumentException("Branch is required");
        }
        return fetchBranch(branchId);
    }

    private Block resolveBlock(Long blockId) {
        if (blockId == null) {
            throw new IllegalArgumentException("Block is required");
        }
        return fetchBlock(blockId);
    }

    private Floor resolveFloor(Long floorId) {
        if (floorId == null) {
            throw new IllegalArgumentException("Floor is required");
        }
        return fetchFloor(floorId);
    }

    private Zone resolveZone(Long zoneId) {
        if (zoneId == null) {
            throw new IllegalArgumentException("Zone is required");
        }
        return fetchZone(zoneId);
    }

    private Faculty resolveWarden(Long wardenId) {
        if (wardenId == null) return null;
        return facultyRepository.findById(wardenId)
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + wardenId));
    }

    /** Next append-at-end order index for a new sibling — one past the current max, not the list
     *  size, so it never collides with an existing index left non-contiguous by a prior delete. */
    private static <T> int nextOrderIndex(List<T> existing, ToIntFunction<T> orderIndexFn) {
        return existing.stream().mapToInt(orderIndexFn).max().orElse(-1) + 1;
    }

    /** Rewrites every item's order index to match its position in `orderedIds`, which must name
     *  exactly the ids in `current` (no partial list, no foreign id, no duplicates) — this is what
     *  keeps drag-to-reorder from silently moving a sibling out from under its actual parent. */
    private static <T> void applyOrder(List<T> current, Function<T, Long> idFn, BiConsumer<T, Integer> orderIndexSetter,
                                        List<Long> orderedIds, Consumer<T> saver, String errorMessage) {
        if (orderedIds == null || orderedIds.size() != current.size() || new HashSet<>(orderedIds).size() != orderedIds.size()) {
            throw new IllegalArgumentException(errorMessage);
        }
        Map<Long, T> byId = current.stream().collect(Collectors.toMap(idFn, item -> item));
        if (!byId.keySet().containsAll(orderedIds)) {
            throw new IllegalArgumentException(errorMessage);
        }
        for (int i = 0; i < orderedIds.size(); i++) {
            T item = byId.get(orderedIds.get(i));
            orderIndexSetter.accept(item, i);
            saver.accept(item);
        }
    }

    private Organization fetchOrganization(Long id) {
        return organizationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));
    }

    private Branch fetchBranch(Long id) {
        return branchRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + id));
    }

    private Block fetchBlock(Long id) {
        return blockRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Block not found with id: " + id));
    }

    private Floor fetchFloor(Long id) {
        return floorRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Floor not found with id: " + id));
    }

    private Zone fetchZone(Long id) {
        return zoneRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Zone not found with id: " + id));
    }

    private Room fetchRoom(Long id) {
        return roomRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));
    }

    private OrganizationResponse toOrganizationResponse(Organization o) {
        return new OrganizationResponse(o.getId(), o.getName(), o.getCode(), o.getDescription(),
            o.getIsActive(), o.getCreatedAt(), o.getUpdatedAt());
    }

    private BranchResponse toBranchResponse(Branch b) {
        Organization o = b.getOrganization();
        return new BranchResponse(b.getId(), b.getName(), b.getCode(), b.getDescription(),
            b.getIsActive(), b.getCreatedAt(), b.getUpdatedAt(),
            o != null ? o.getId() : null, o != null ? o.getName() : null);
    }

    private BlockResponse toBlockResponse(Block b) {
        Branch br = b.getBranch();
        return new BlockResponse(b.getId(), b.getName(), b.getCode(), b.getDescription(),
            b.getIsHostel(), b.getGenderRestriction(),
            b.getIsActive(), b.getCreatedAt(), b.getUpdatedAt(),
            br != null ? br.getId() : null, br != null ? br.getName() : null);
    }

    private FloorResponse toFloorResponse(Floor f) {
        Block b = f.getBlock();
        return new FloorResponse(f.getId(), f.getName(), f.getFloorNumber(),
            f.getIsHostel(), f.getGenderRestriction(), f.getIsBasement(), f.getIsActive(),
            f.getCreatedAt(), f.getUpdatedAt(),
            b != null ? b.getId() : null, b != null ? b.getName() : null);
    }

    private ZoneResponse toZoneResponse(Zone z) {
        Floor f = z.getFloor();
        Faculty w = z.getWarden();
        return new ZoneResponse(z.getId(), z.getName(), z.getIsHostel(), z.getGenderRestriction(),
            w != null ? w.getId() : null, w != null ? (w.getFirstName() + " " + w.getLastName()) : null,
            z.getIsActive(), z.getCreatedAt(), z.getUpdatedAt(),
            f != null ? f.getId() : null, f != null ? f.getName() : null);
    }

    private RoomResponse toRoomResponse(Room r) {
        Zone z = r.getZone();
        HostelRoom hostelRoom = hostelRoomRepository.findByRoomId(r.getId()).orElse(null);
        RoomPurposeCategory category = r.getPurposeCategory();
        RoomSubType subType = r.getSubType();
        return new RoomResponse(r.getId(), r.getRoomNumber(), r.getCapacity(), r.getDescription(),
            r.getIsActive(), r.getCreatedAt(), r.getUpdatedAt(),
            z != null ? z.getId() : null, z != null ? z.getName() : null,
            hostelRoom != null ? hostelRoom.getId() : null,
            hostelRoom != null ? hostelRoom.getRoomType().getId() : null,
            hostelRoom != null ? hostelRoom.getRoomType().getName() : null,
            category != null ? category.getId() : null, category != null ? category.getName() : null,
            subType != null ? subType.getId() : null, subType != null ? subType.getName() : null);
    }

    private HostelRoomResponse toHostelRoomResponse(HostelRoom hr) {
        Room r = hr.getRoom();
        Zone z = r.getZone();
        HostelRoomType rt = hr.getRoomType();
        return new HostelRoomResponse(hr.getId(), r.getId(), r.getRoomNumber(),
            z != null ? z.getId() : null, z != null ? z.getName() : null,
            rt.getId(), rt.getName(), rt.getSharingCapacity(), rt.getIsAc(), rt.getFeeAmountPerYear(),
            hr.getIsActive(), hr.getCreatedAt(), hr.getUpdatedAt());
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String requireTrimmed(String s, String message) {
        String t = trim(s);
        if (t == null) throw new IllegalArgumentException(message);
        return t;
    }
}
