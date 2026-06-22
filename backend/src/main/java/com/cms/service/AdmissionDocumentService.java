package com.cms.service;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cms.config.PermSecurityBean;
import com.cms.dto.AdmissionDocumentResponse;
import com.cms.dto.DocumentChecklistResponse;
import com.cms.dto.DocumentFileDownload;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Admission;
import com.cms.model.EnquiryDocument;
import com.cms.model.EnquiryDocumentHistory;
import com.cms.model.Program;
import com.cms.model.Student;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.DocumentVerificationStatus;
import com.cms.repository.AdmissionRepository;
import com.cms.repository.EnquiryDocumentHistoryRepository;
import com.cms.repository.EnquiryDocumentRepository;
import com.cms.util.CurrentUserResolver;

@Service
@Transactional(readOnly = true)
public class AdmissionDocumentService {

    public static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L;

    private final EnquiryDocumentRepository documentRepository;
    private final EnquiryDocumentHistoryRepository historyRepository;
    private final AdmissionRepository admissionRepository;
    private final CurrentUserResolver currentUserResolver;
    private final PermSecurityBean permSecurityBean;

    public AdmissionDocumentService(EnquiryDocumentRepository documentRepository,
                                    EnquiryDocumentHistoryRepository historyRepository,
                                    AdmissionRepository admissionRepository,
                                    CurrentUserResolver currentUserResolver,
                                    PermSecurityBean permSecurityBean) {
        this.documentRepository = documentRepository;
        this.historyRepository = historyRepository;
        this.admissionRepository = admissionRepository;
        this.currentUserResolver = currentUserResolver;
        this.permSecurityBean = permSecurityBean;
    }

    public List<AdmissionDocumentResponse> findByAdmissionId(Long admissionId) {
        return documentRepository.findByAdmission_Id(admissionId).stream()
            .map(doc -> toResponse(doc, admissionId))
            .toList();
    }

    public AdmissionDocumentResponse uploadFile(Long admissionId, DocumentType documentType,
                                                String remarks, MultipartFile file) {
        return uploadFile(admissionId, documentType, remarks, file, false);
    }

    @Transactional
    public AdmissionDocumentResponse uploadFile(Long admissionId, DocumentType documentType,
                                                String remarks, MultipartFile file, boolean forceOverride) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File exceeds the 10 MB maximum");
        }

        Admission admission = admissionRepository.findById(admissionId)
            .orElseThrow(() -> new ResourceNotFoundException("Admission not found with id: " + admissionId));

        EnquiryDocument document = documentRepository.findByAdmission_Id(admissionId).stream()
            .filter(d -> d.getDocumentType() == documentType)
            .findFirst()
            .orElseGet(() -> {
                EnquiryDocument d = new EnquiryDocument(null, documentType, DocumentVerificationStatus.NOT_UPLOADED);
                d.setAdmission(admission);
                return d;
            });

        boolean wasVerified = document.getId() != null && document.getStatus() == DocumentVerificationStatus.VERIFIED;
        if (wasVerified) {
            if (!forceOverride) {
                throw new IllegalStateException("Cannot replace a verified document");
            }
            if (!permSecurityBean.has("DOCUMENT_VERIFIED_OVERRIDE")) {
                throw new AccessDeniedException("Replacing a verified document requires override permission");
            }
        }

        DocumentVerificationStatus previousStatus = document.getId() != null ? document.getStatus() : null;

        try {
            document.setFileData(file.getBytes());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read uploaded file", ex);
        }
        document.setFileName(sanitizeFileName(file.getOriginalFilename()));
        document.setContentType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setUploadedAt(Instant.now());
        document.setStatus(DocumentVerificationStatus.UPLOADED);
        if (wasVerified) {
            document.setVerifiedBy(null);
            document.setVerifiedAt(null);
        }
        if (remarks != null && !remarks.isBlank()) {
            document.setRemarks(remarks.trim());
        }

        EnquiryDocument saved = documentRepository.save(document);
        recordHistory(saved, previousStatus, DocumentVerificationStatus.UPLOADED);

        return toResponse(saved, admissionId);
    }

    public DocumentFileDownload getFileForDownload(Long documentId) {
        EnquiryDocument document = documentRepository.findById(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));
        byte[] data = document.getFileData();
        if (data == null || data.length == 0) {
            throw new ResourceNotFoundException("No file uploaded for document id: " + documentId);
        }
        String fileName = document.getFileName() != null
            ? document.getFileName() : document.getDocumentType().name();
        String contentType = document.getContentType() != null
            ? document.getContentType() : "application/octet-stream";
        return new DocumentFileDownload(fileName, contentType, data);
    }

    @Transactional
    public AdmissionDocumentResponse updateVerification(Long docId, DocumentVerificationStatus status,
                                                        String verifiedBy) {
        EnquiryDocument document = documentRepository.findById(docId)
            .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + docId));

        if (document.getStatus() == DocumentVerificationStatus.VERIFIED) {
            throw new IllegalStateException("Document is already verified and cannot be changed");
        }

        DocumentVerificationStatus previousStatus = document.getStatus();

        document.setStatus(status);
        document.setVerifiedBy(verifiedBy != null ? verifiedBy : currentUserResolver.resolve());
        document.setVerifiedAt(Instant.now());
        document.setRemarks(null);

        EnquiryDocument updated = documentRepository.save(document);
        recordHistory(updated, previousStatus, status);

        Long admissionId = updated.getAdmission() != null ? updated.getAdmission().getId() : null;
        return toResponse(updated, admissionId);
    }

    @Transactional
    public void deleteDocument(Long documentId) {
        EnquiryDocument document = documentRepository.findById(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));
        if (document.getStatus() == DocumentVerificationStatus.VERIFIED) {
            throw new IllegalStateException("Verified documents cannot be deleted");
        }
        documentRepository.deleteById(documentId);
    }

    public DocumentChecklistResponse getChecklist(Long admissionId) {
        Admission admission = admissionRepository.findById(admissionId)
            .orElseThrow(() -> new ResourceNotFoundException("Admission not found with id: " + admissionId));

        Set<DocumentType> mandatory = resolveMandatoryTypes(admission);
        Set<DocumentType> optional  = resolveOptionalTypes(admission);

        Map<String, String> mandatoryMap = new LinkedHashMap<>();
        Map<String, String> optionalMap  = new LinkedHashMap<>();

        for (DocumentType type : mandatory) {
            mandatoryMap.put(type.name(), DocumentVerificationStatus.NOT_UPLOADED.name());
        }
        for (DocumentType type : optional) {
            optionalMap.put(type.name(), DocumentVerificationStatus.NOT_UPLOADED.name());
        }

        for (EnquiryDocument doc : documentRepository.findByAdmission_Id(admissionId)) {
            String key = doc.getDocumentType().name();
            String status = doc.getStatus().name();
            if (mandatoryMap.containsKey(key)) {
                mandatoryMap.put(key, status);
            } else if (optionalMap.containsKey(key)) {
                optionalMap.put(key, status);
            }
        }

        return new DocumentChecklistResponse(mandatoryMap, optionalMap);
    }

    private void recordHistory(EnquiryDocument doc, DocumentVerificationStatus previous,
                               DocumentVerificationStatus next) {
        EnquiryDocumentHistory history = new EnquiryDocumentHistory();
        history.setEnquiryDocument(doc);
        history.setEnquiry(doc.getEnquiry());
        history.setAdmission(doc.getAdmission());
        history.setDocumentType(doc.getDocumentType());
        history.setPreviousStatus(previous);
        history.setNewStatus(next);
        history.setFileName(doc.getFileName());
        history.setFileSize(doc.getFileSize());
        history.setContentType(doc.getContentType());
        history.setRemarks(doc.getRemarks());
        history.setChangedBy(currentUserResolver.resolve());
        historyRepository.save(history);
    }

    private Set<DocumentType> resolveMandatoryTypes(Admission admission) {
        Student student = admission.getStudent();
        if (student != null && student.getProgram() != null) {
            return new HashSet<>(student.getProgram().getMandatoryDocumentTypes());
        }
        return new HashSet<>();
    }

    private Set<DocumentType> resolveOptionalTypes(Admission admission) {
        Student student = admission.getStudent();
        if (student != null && student.getProgram() != null) {
            return new HashSet<>(student.getProgram().getOptionalDocumentTypes());
        }
        return new HashSet<>();
    }

    private String sanitizeFileName(String original) {
        if (original == null) return null;
        String name = original.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) name = name.substring(slash + 1);
        return name.isBlank() ? null : name;
    }

    private AdmissionDocumentResponse toResponse(EnquiryDocument document, Long admissionId) {
        Long resolvedAdmissionId = admissionId != null ? admissionId
            : (document.getAdmission() != null ? document.getAdmission().getId() : null);
        return new AdmissionDocumentResponse(
            document.getId(),
            resolvedAdmissionId,
            document.getDocumentType(),
            document.getFileName(),
            null,
            toUtcLocalDateTime(document.getUploadedAt()),
            null,
            document.getVerifiedBy(),
            toUtcLocalDateTime(document.getVerifiedAt()),
            document.getStatus(),
            document.getCreatedAt(),
            document.getUpdatedAt(),
            document.getFileName() != null,
            document.getRemarks(),
            document.getContentType(),
            document.getFileSize()
        );
    }

    private LocalDateTime toUtcLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
