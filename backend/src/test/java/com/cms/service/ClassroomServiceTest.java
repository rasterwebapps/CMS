package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.ClassroomRequest;
import com.cms.model.Classroom;
import com.cms.model.Room;
import com.cms.model.RoomPurposeCategory;
import com.cms.model.Zone;
import com.cms.model.enums.RoomPurposeCategoryCode;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.RoomRepository;

@ExtendWith(MockitoExtension.class)
class ClassroomServiceTest {

    @Mock
    private ClassroomRepository classroomRepository;
    @Mock
    private RoomRepository roomRepository;

    private ClassroomService newService() {
        return new ClassroomService(classroomRepository, roomRepository);
    }

    private Room room(Long id, RoomPurposeCategoryCode code) {
        Room room = new Room();
        room.setId(id);
        room.setRoomNumber("R-" + id);
        room.setCapacity(50);
        Zone zone = new Zone();
        zone.setName("Main Zone");
        room.setZone(zone);
        RoomPurposeCategory category = new RoomPurposeCategory();
        category.setCode(code);
        room.setPurposeCategory(category);
        return room;
    }

    @Test
    void shouldAllowLinkingToAnAcademicRoom() {
        ClassroomService svc = newService();
        Room academicRoom = room(1L, RoomPurposeCategoryCode.ACADEMIC);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(academicRoom));
        when(classroomRepository.existsByNameIgnoreCase("Lecture Hall A")).thenReturn(false);
        when(classroomRepository.save(any(Classroom.class))).thenAnswer(inv -> inv.getArgument(0));

        ClassroomRequest request = new ClassroomRequest("Lecture Hall A", null, null, null, true, 1L, false);

        assertThat(svc.create(request).name()).isEqualTo("Lecture Hall A");
    }

    /** OC-248: a Sports & Recreation room must also be linkable -- TimetableGlobalAutoScheduleService
     *  #fillSportsGaps queries for exactly this (Classroom whose Room is Sports & Recreation), so
     *  rejecting it here made Sports auto-fill structurally impossible to set up through any real
     *  admin flow, in any environment. */
    @Test
    void shouldAllowLinkingToASportsRoom() {
        ClassroomService svc = newService();
        Room sportsRoom = room(2L, RoomPurposeCategoryCode.SPORTS);
        when(roomRepository.findById(2L)).thenReturn(Optional.of(sportsRoom));
        when(classroomRepository.existsByNameIgnoreCase("Sports Hall 1")).thenReturn(false);
        when(classroomRepository.save(any(Classroom.class))).thenAnswer(inv -> inv.getArgument(0));

        ClassroomRequest request = new ClassroomRequest("Sports Hall 1", null, null, null, true, 2L, false);

        assertThat(svc.create(request).name()).isEqualTo("Sports Hall 1");
    }

    @Test
    void shouldStillRejectLinkingToAResidentialRoom() {
        ClassroomService svc = newService();
        Room dormRoom = room(3L, RoomPurposeCategoryCode.RESIDENTIAL);
        when(roomRepository.findById(3L)).thenReturn(Optional.of(dormRoom));
        when(classroomRepository.existsByNameIgnoreCase("Not A Classroom")).thenReturn(false);

        ClassroomRequest request = new ClassroomRequest("Not A Classroom", null, null, null, true, 3L, false);

        assertThatThrownBy(() -> svc.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Academic or Sports & Recreation");
    }
}
