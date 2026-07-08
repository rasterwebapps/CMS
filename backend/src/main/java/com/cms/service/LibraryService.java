package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.LibraryResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Library;
import com.cms.repository.LibraryRepository;

/**
 * Only one Library row exists today; this service is intentionally minimal (no create/update/
 * delete) since there's no UI for managing libraries yet — full CRUD can be added when a second
 * physical library is actually confirmed.
 */
@Service
@Transactional(readOnly = true)
public class LibraryService {

    private final LibraryRepository libraryRepository;

    public LibraryService(LibraryRepository libraryRepository) {
        this.libraryRepository = libraryRepository;
    }

    public List<LibraryResponse> findAll() {
        return libraryRepository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    public Library getDefault() {
        return libraryRepository.findByCode("MAIN")
            .orElseThrow(() -> new ResourceNotFoundException("Default library not found"));
    }

    private LibraryResponse toResponse(Library l) {
        return new LibraryResponse(l.getId(), l.getName(), l.getCode(), l.getAddress(),
            l.getIsActive(), l.getCreatedAt(), l.getUpdatedAt());
    }
}
