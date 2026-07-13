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

    private static final List<String> VALID_PRINTER_MODES = List.of("BROWSER", "NETWORK", "LOCAL_AGENT");

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
        String value = request.settingValue().trim();

        if ("barcode_printer_mode".equals(key) && !VALID_PRINTER_MODES.contains(value)) {
            throw new IllegalArgumentException("barcode_printer_mode must be one of " + VALID_PRINTER_MODES);
        }
        if ("barcode_printer_ip".equals(key) && !value.isBlank() && !LibraryBarcodeService.isPrivateNetworkAddress(value)) {
            throw new IllegalArgumentException(
                "Printer IP must be a private/local network address (10.x, 172.16-31.x, 192.168.x, or loopback)");
        }

        setting.setSettingValue(value);
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
