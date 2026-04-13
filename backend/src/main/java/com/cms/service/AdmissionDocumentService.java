package com.cms.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AdmissionDocumentResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AdmissionDocument;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.DocumentVerificationStatus;
import com.cms.repository.AdmissionDocumentRepository;

@Service
@Transactional(readOnly = true)
public class AdmissionDocumentService {

    private final AdmissionDocumentRepository admissionDocumentRepository;

    public AdmissionDocumentService(AdmissionDocumentRepository admissionDocumentRepository) {
        this.admissionDocumentRepository = admissionDocumentRepository;
    }

    public List<AdmissionDocumentResponse> findByAdmissionId(Long admissionId) {
        return admissionDocumentRepository.findByAdmissionId(admissionId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public AdmissionDocumentResponse updateVerification(Long docId, DocumentVerificationStatus status, String verifiedBy) {
        AdmissionDocument document = admissionDocumentRepository.findById(docId)
            .orElseThrow(() -> new ResourceNotFoundException("Admission document not found with id: " + docId));
        document.setVerificationStatus(status);
        document.setVerifiedBy(verifiedBy);
        document.setVerifiedAt(LocalDateTime.now(ZoneOffset.UTC));
        AdmissionDocument updated = admissionDocumentRepository.save(document);
        return toResponse(updated);
    }

    public Map<DocumentType, DocumentVerificationStatus> getChecklist(Long admissionId) {
        Map<DocumentType, DocumentVerificationStatus> checklist = new EnumMap<>(DocumentType.class);
        for (DocumentType type : DocumentType.values()) {
            checklist.put(type, DocumentVerificationStatus.NOT_UPLOADED);
        }
        List<AdmissionDocument> documents = admissionDocumentRepository.findByAdmissionId(admissionId);
        for (AdmissionDocument doc : documents) {
            checklist.put(doc.getDocumentType(), doc.getVerificationStatus());
        }
        return checklist;
    }

    private AdmissionDocumentResponse toResponse(AdmissionDocument document) {
        return new AdmissionDocumentResponse(
            document.getId(),
            document.getAdmission().getId(),
            document.getDocumentType(),
            document.getFileName(),
            document.getStorageKey(),
            document.getUploadedAt(),
            document.getOriginalSubmitted(),
            document.getVerifiedBy(),
            document.getVerifiedAt(),
            document.getVerificationStatus(),
            document.getCreatedAt(),
            document.getUpdatedAt()
        );
    }
}
