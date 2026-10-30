package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.dto.LibrarySubjectCategoryRequest;
import com.cms.dto.LibrarySubjectCategoryResponse;
import com.cms.model.LibrarySubjectCategory;
import com.cms.repository.LibraryBookRepository;
import com.cms.repository.LibrarySubjectCategoryRepository;

@ExtendWith(MockitoExtension.class)
class LibrarySubjectCategoryServiceTest {

    @Mock private LibrarySubjectCategoryRepository categoryRepository;
    @Mock private LibraryBookRepository bookRepository;

    private LibrarySubjectCategoryService service;

    @BeforeEach
    void setUp() {
        service = new LibrarySubjectCategoryService(categoryRepository, bookRepository);
    }

    @Test
    void create_happyPath_savesCategory() {
        var request = new LibrarySubjectCategoryRequest("Pharmacology", "PHARM", "Drugs & Medicine", true);
        when(categoryRepository.existsByNameIgnoreCase("Pharmacology")).thenReturn(false);
        when(categoryRepository.existsByCodeIgnoreCase("PHARM")).thenReturn(false);

        LibrarySubjectCategory saved = new LibrarySubjectCategory("Pharmacology", "PHARM");
        saved.setId(10L);
        saved.setDescription("Drugs & Medicine");
        saved.setIsActive(true);
        when(categoryRepository.save(any())).thenReturn(saved);

        LibrarySubjectCategoryResponse res = service.create(request);

        assertThat(res.id()).isEqualTo(10L);
        assertThat(res.name()).isEqualTo("Pharmacology");
        assertThat(res.code()).isEqualTo("PHARM");
        verify(categoryRepository).save(any());
    }

    @Test
    void create_duplicateName_throws() {
        var request = new LibrarySubjectCategoryRequest("Pharmacology", "PHARM", null, true);
        when(categoryRepository.existsByNameIgnoreCase("Pharmacology")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void create_autoGeneratesCode_whenBlank() {
        var request = new LibrarySubjectCategoryRequest("Anatomy & Physiology", null, null, true);
        when(categoryRepository.existsByNameIgnoreCase("Anatomy & Physiology")).thenReturn(false);
        when(categoryRepository.existsByCodeIgnoreCase(any())).thenReturn(false);

        LibrarySubjectCategory saved = new LibrarySubjectCategory("Anatomy & Physiology", "ANATOMY_PHYSIOLOGY");
        saved.setId(11L);
        when(categoryRepository.save(any())).thenReturn(saved);

        LibrarySubjectCategoryResponse res = service.create(request);
        assertThat(res).isNotNull();
        verify(categoryRepository).save(any());
    }

    @Test
    void findAll_activeOnly_returnsActive() {
        LibrarySubjectCategory c1 = new LibrarySubjectCategory("Biochemistry", "BIO");
        c1.setId(1L);
        c1.setIsActive(true);
        when(categoryRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(c1));

        List<LibrarySubjectCategoryResponse> list = service.findAll(true);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).name()).isEqualTo("Biochemistry");
    }

    @Test
    void delete_whenBooksExist_throws() {
        when(categoryRepository.existsById(5L)).thenReturn(true);
        when(bookRepository.existsBySubjectCategoryMasterId(5L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(5L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("associated books");
        verify(categoryRepository, never()).deleteById(any());
    }

    @Test
    void delete_whenNoBooks_succeeds() {
        when(categoryRepository.existsById(5L)).thenReturn(true);
        when(bookRepository.existsBySubjectCategoryMasterId(5L)).thenReturn(false);

        service.delete(5L);
        verify(categoryRepository).deleteById(5L);
    }

    @Test
    void updateStatus_togglesIsActive() {
        LibrarySubjectCategory cat = new LibrarySubjectCategory("General", "GEN");
        cat.setId(1L);
        cat.setIsActive(true);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ActiveStatusUpdateResponse res = service.updateStatus(1L, new ActiveStatusUpdateRequest(false, null));
        assertThat(res.isActive()).isFalse();
        assertThat(cat.getIsActive()).isFalse();
    }

    @Test
    void resolveOrCreate_whenExists_returnsExisting() {
        LibrarySubjectCategory existing = new LibrarySubjectCategory("Microbiology", "MICRO");
        existing.setId(20L);
        when(categoryRepository.findByNameIgnoreCase("Microbiology")).thenReturn(Optional.of(existing));

        LibrarySubjectCategory res = service.resolveOrCreate("Microbiology");
        assertThat(res.getId()).isEqualTo(20L);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void resolveOrCreate_whenMissing_createsNew() {
        when(categoryRepository.findByNameIgnoreCase("Genetics")).thenReturn(Optional.empty());
        when(categoryRepository.existsByCodeIgnoreCase(any())).thenReturn(false);

        LibrarySubjectCategory created = new LibrarySubjectCategory("Genetics", "GENETICS");
        created.setId(30L);
        when(categoryRepository.save(any())).thenReturn(created);

        LibrarySubjectCategory res = service.resolveOrCreate("Genetics");
        assertThat(res.getId()).isEqualTo(30L);
        verify(categoryRepository).save(any());
    }
}
