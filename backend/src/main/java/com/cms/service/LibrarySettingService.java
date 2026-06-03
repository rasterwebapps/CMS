package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.LibrarySettingResponse;
import com.cms.dto.LibrarySettingUpdateRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.LibrarySetting;
import com.cms.repository.LibrarySettingRepository;

@Service
@Transactional(readOnly = true)
public class LibrarySettingService {

    private final LibrarySettingRepository repository;

    public LibrarySettingService(LibrarySettingRepository repository) {
        this.repository = repository;
    }

    public List<LibrarySettingResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public LibrarySettingResponse updateByKey(String key, LibrarySettingUpdateRequest request) {
        LibrarySetting setting = repository.findBySettingKey(key)
            .orElseThrow(() -> new ResourceNotFoundException("Library setting not found: " + key));
        setting.setSettingValue(request.settingValue().trim());
        return toResponse(repository.save(setting));
    }

    private LibrarySettingResponse toResponse(LibrarySetting s) {
        return new LibrarySettingResponse(
            s.getId(),
            s.getSettingKey(),
            s.getSettingValue(),
            s.getDisplayName(),
            s.getDescription(),
            s.getDataType(),
            s.getUpdatedAt()
        );
    }
}
