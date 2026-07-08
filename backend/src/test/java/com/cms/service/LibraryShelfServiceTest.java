package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.LibraryShelfRequest;
import com.cms.model.Library;
import com.cms.model.LibraryRack;
import com.cms.repository.LibraryRackRepository;
import com.cms.repository.LibraryShelfRepository;

@ExtendWith(MockitoExtension.class)
class LibraryShelfServiceTest {

    @Mock private LibraryShelfRepository shelfRepository;
    @Mock private LibraryRackRepository rackRepository;

    private LibraryShelfService service;
    private LibraryRack rackA;
    private LibraryRack rackB;

    @BeforeEach
    void setUp() {
        service = new LibraryShelfService(shelfRepository, rackRepository);

        Library library = new Library();
        library.setId(1L);
        library.setName("Main Library");

        rackA = new LibraryRack();
        rackA.setId(5L);
        rackA.setName("Rack A");
        rackA.setLibrary(library);

        rackB = new LibraryRack();
        rackB.setId(6L);
        rackB.setName("Rack B");
        rackB.setLibrary(library);

        lenient().when(rackRepository.findById(5L)).thenReturn(Optional.of(rackA));
        lenient().when(rackRepository.findById(6L)).thenReturn(Optional.of(rackB));
    }

    @Test
    void create_duplicateNameOnSameRack_throws() {
        when(shelfRepository.existsByNameIgnoreCaseAndRackId("Tier 1", 5L)).thenReturn(true);

        var request = new LibraryShelfRequest(5L, "Tier 1", "TIER1", null, null);

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists on this rack");
        verify(shelfRepository, never()).save(any());
    }

    @Test
    void create_sameNameOnDifferentRack_isAllowed() {
        when(shelfRepository.existsByNameIgnoreCaseAndRackId("Tier 1", 6L)).thenReturn(false);
        when(shelfRepository.existsByCodeIgnoreCaseAndRackId("TIER1", 6L)).thenReturn(false);
        when(shelfRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new LibraryShelfRequest(6L, "Tier 1", "TIER1", null, null);
        var response = service.create(request);

        assertThat(response.name()).isEqualTo("Tier 1");
        assertThat(response.rackId()).isEqualTo(6L);
        verify(shelfRepository).save(any());
    }
}
