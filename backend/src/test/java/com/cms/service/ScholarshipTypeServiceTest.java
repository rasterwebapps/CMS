package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.ScholarshipTypeRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.ScholarshipType;
import com.cms.model.enums.DiscountType;
import com.cms.repository.ScholarshipTypeRepository;

@ExtendWith(MockitoExtension.class)
class ScholarshipTypeServiceTest {

    @Mock private ScholarshipTypeRepository repository;
    private ScholarshipTypeService service;

    @BeforeEach
    void setUp() {
        service = new ScholarshipTypeService(repository);
    }

    @Test
    void shouldListActiveScholarships() {
        when(repository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(type(1L, "SC_GOVT")));
        assertThat(service.getAllActive()).singleElement().satisfies(r -> assertThat(r.code()).isEqualTo("SC_GOVT"));
    }

    @Test
    void shouldCreateScholarshipType() {
        when(repository.existsByCode("TEST")).thenReturn(false);
        when(repository.save(any(ScholarshipType.class))).thenAnswer(inv -> { ScholarshipType t = inv.getArgument(0); t.setId(1L); return t; });
        var req = new ScholarshipTypeRequest("test", "Test", "Desc", true, "S1", DiscountType.FIXED_AMOUNT,
            new BigDecimal("1000"), new BigDecimal("1000"), true, true,
            com.cms.model.enums.ScholarshipApplicationMode.GOVT_PORTAL, "NSP", "https://scholarships.gov.in", 1, 4);
        var res = service.create(req, "admin");
        assertThat(res.code()).isEqualTo("TEST");
        assertThat(res.govtScheme()).isTrue();
        assertThat(res.renewalRequired()).isTrue();
        assertThat(res.applicationMode()).isEqualTo(com.cms.model.enums.ScholarshipApplicationMode.GOVT_PORTAL);
        assertThat(res.portalName()).isEqualTo("NSP");
        assertThat(res.eligibleFromYear()).isEqualTo(1);
    }

    @Test
    void shouldRejectDuplicateCodeOnCreate() {
        when(repository.existsByCode("TEST")).thenReturn(true);
        var req = new ScholarshipTypeRequest("TEST", "Test", null, false, null, DiscountType.FIXED_AMOUNT,
            BigDecimal.ZERO, null, false, true, null, null, null, null, null);
        assertThatThrownBy(() -> service.create(req, "admin")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldUpdateAndDeactivate() {
        ScholarshipType existing = type(1L, "OLD");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByCodeAndIdNot("NEW", 1L)).thenReturn(false);
        when(repository.save(any(ScholarshipType.class))).thenAnswer(inv -> inv.getArgument(0));
        var req = new ScholarshipTypeRequest("NEW", "New", null, false, null, DiscountType.PERCENTAGE,
            new BigDecimal("50"), null, false, true, null, null, null, null, null);
        assertThat(service.update(1L, req, "admin").code()).isEqualTo("NEW");
        service.deactivate(1L, "admin");
        assertThat(existing.isActive()).isFalse();
    }

    @Test
    void shouldRejectInvalidYearRangeOnCreate() {
        when(repository.existsByCode("BADRANGE")).thenReturn(false);
        var req = new ScholarshipTypeRequest("BADRANGE", "Bad Range", null, false, null, DiscountType.FIXED_AMOUNT,
            BigDecimal.ZERO, null, false, true, null, null, null, 4, 1);
        assertThatThrownBy(() -> service.create(req, "admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Eligible from year");
    }

    @Test
    void shouldThrowWhenNotFound() {
        when(repository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    private ScholarshipType type(Long id, String code) {
        ScholarshipType t = new ScholarshipType();
        t.setId(id);
        t.setCode(code);
        t.setName(code);
        t.setDiscountType(DiscountType.FIXED_AMOUNT);
        t.setDiscountValue(new BigDecimal("1000"));
        t.setActive(true);
        return t;
    }
}

