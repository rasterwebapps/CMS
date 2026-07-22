package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.RoomAllocationRequest;
import com.cms.dto.RoomAllocationResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.HostelRoom;
import com.cms.model.HostelRoomType;
import com.cms.model.Room;
import com.cms.model.RoomAllocation;
import com.cms.model.Student;
import com.cms.model.Zone;
import com.cms.model.enums.RoomAllocationStatus;
import com.cms.model.enums.StudentType;
import com.cms.repository.HostelRoomRepository;
import com.cms.repository.RoomAllocationRepository;
import com.cms.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class RoomAllocationServiceTest {

    @Mock
    private RoomAllocationRepository roomAllocationRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private HostelRoomRepository hostelRoomRepository;

    private RoomAllocationService roomAllocationService;

    @BeforeEach
    void setUp() {
        roomAllocationService = new RoomAllocationService(roomAllocationRepository, studentRepository, hostelRoomRepository);
    }

    @Test
    void shouldCreateAllocation() {
        Student student = student(1L, StudentType.HOSTELER);
        HostelRoom hostelRoom = hostelRoom(1L, 2);
        RoomAllocationRequest request = new RoomAllocationRequest(1L, 1L, LocalDate.of(2026, 6, 1), null, null, null);
        RoomAllocation saved = allocation(1L, student, hostelRoom, LocalDate.of(2026, 6, 1));

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(roomAllocationRepository.existsByStudentIdAndStatus(1L, RoomAllocationStatus.ACTIVE)).thenReturn(false);
        when(hostelRoomRepository.findById(1L)).thenReturn(Optional.of(hostelRoom));
        when(roomAllocationRepository.countByHostelRoomIdAndStatus(1L, RoomAllocationStatus.ACTIVE)).thenReturn(0L);
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenReturn(saved);

        RoomAllocationResponse response = roomAllocationService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(RoomAllocationStatus.ACTIVE);
        verify(roomAllocationRepository).save(any(RoomAllocation.class));
    }

    @Test
    void shouldRejectWhenStudentNotHosteler() {
        Student student = student(1L, StudentType.DAY_SCHOLAR);
        RoomAllocationRequest request = new RoomAllocationRequest(1L, 1L, LocalDate.of(2026, 6, 1), null, null, null);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> roomAllocationService.create(request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Hosteler");

        verify(roomAllocationRepository, never()).save(any());
    }

    @Test
    void shouldRejectWhenStudentAlreadyHasActiveAllocation() {
        Student student = student(1L, StudentType.HOSTELER);
        RoomAllocationRequest request = new RoomAllocationRequest(1L, 1L, LocalDate.of(2026, 6, 1), null, null, null);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(roomAllocationRepository.existsByStudentIdAndStatus(1L, RoomAllocationStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> roomAllocationService.create(request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already has an active room allocation");

        verify(roomAllocationRepository, never()).save(any());
    }

    @Test
    void shouldRejectWhenRoomAtFullCapacity() {
        Student student = student(1L, StudentType.HOSTELER);
        HostelRoom hostelRoom = hostelRoom(1L, 2);
        RoomAllocationRequest request = new RoomAllocationRequest(1L, 1L, LocalDate.of(2026, 6, 1), null, null, null);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(roomAllocationRepository.existsByStudentIdAndStatus(1L, RoomAllocationStatus.ACTIVE)).thenReturn(false);
        when(hostelRoomRepository.findById(1L)).thenReturn(Optional.of(hostelRoom));
        when(roomAllocationRepository.countByHostelRoomIdAndStatus(1L, RoomAllocationStatus.ACTIVE)).thenReturn(2L);

        assertThatThrownBy(() -> roomAllocationService.create(request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("full capacity");

        verify(roomAllocationRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenStudentNotFoundOnCreate() {
        RoomAllocationRequest request = new RoomAllocationRequest(999L, 1L, LocalDate.of(2026, 6, 1), null, null, null);
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomAllocationService.create(request))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldUpdateStatus() {
        Student student = student(1L, StudentType.HOSTELER);
        HostelRoom hostelRoom = hostelRoom(1L, 2);
        RoomAllocation existing = allocation(1L, student, hostelRoom, LocalDate.of(2026, 6, 1));
        RoomAllocation cancelled = allocation(1L, student, hostelRoom, LocalDate.of(2026, 6, 1));
        cancelled.setStatus(RoomAllocationStatus.CANCELLED);

        when(roomAllocationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenReturn(cancelled);

        RoomAllocationResponse response = roomAllocationService.updateStatus(1L, RoomAllocationStatus.CANCELLED);

        assertThat(response.status()).isEqualTo(RoomAllocationStatus.CANCELLED);
    }

    @Test
    void shouldDelete() {
        when(roomAllocationRepository.existsById(1L)).thenReturn(true);

        roomAllocationService.delete(1L);

        verify(roomAllocationRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeletingNonExistent() {
        when(roomAllocationRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> roomAllocationService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class);

        verify(roomAllocationRepository, never()).deleteById(any());
    }

    @Test
    void shouldComputeOccupancy() {
        Student student = student(1L, StudentType.HOSTELER);
        HostelRoom hostelRoom = hostelRoom(1L, 2);
        RoomAllocation active = allocation(1L, student, hostelRoom, LocalDate.of(2026, 6, 1));

        when(hostelRoomRepository.findAll()).thenReturn(List.of(hostelRoom));
        when(roomAllocationRepository.findByHostelRoomIdAndStatus(1L, RoomAllocationStatus.ACTIVE)).thenReturn(List.of(active));

        var occupancy = roomAllocationService.findOccupancy();

        assertThat(occupancy).hasSize(1);
        assertThat(occupancy.get(0).occupiedCount()).isEqualTo(1);
        assertThat(occupancy.get(0).sharingCapacity()).isEqualTo(2);
        assertThat(occupancy.get(0).occupants()).hasSize(1);
    }

    private Student student(Long id, StudentType type) {
        Student student = new Student();
        student.setId(id);
        student.setFirstName("Test");
        student.setLastName("Student");
        student.setStudentType(type);
        return student;
    }

    private HostelRoom hostelRoom(Long id, int sharingCapacity) {
        Zone zone = new Zone();
        zone.setId(1L);
        zone.setName("North Wing");

        Room room = new Room(zone, "R101", null, null);
        room.setId(1L);

        HostelRoomType roomType = new HostelRoomType("Double Sharing", "DBL", sharingCapacity, false, java.math.BigDecimal.TEN, null);
        roomType.setId(1L);

        HostelRoom hostelRoom = new HostelRoom(room, roomType);
        hostelRoom.setId(id);
        hostelRoom.setIsActive(true);
        return hostelRoom;
    }

    private RoomAllocation allocation(Long id, Student student, HostelRoom hostelRoom, LocalDate startDate) {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(id);
        allocation.setStudent(student);
        allocation.setHostelRoom(hostelRoom);
        allocation.setStartDate(startDate);
        Instant now = Instant.now();
        allocation.setCreatedAt(now);
        allocation.setUpdatedAt(now);
        return allocation;
    }
}
