package com.cms.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.EnquiryDocumentRequest;
import com.cms.dto.EnquiryDocumentResponse;
import com.cms.dto.MissingDocumentsResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Enquiry;
import com.cms.model.EnquiryDocument;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.DocumentVerificationStatus;
import com.cms.repository.EnquiryDocumentRepository;
import com.cms.repository.EnquiryRepository;

@Service
@Transactional(readOnly = true)
public class EnquiryDocumentService {

    private static final Set<DocumentType> MANDATORY_DOCUMENTS = Set.of(
        DocumentType.TENTH_MARKSHEET,
        DocumentType.TWELFTH_MARKSHEET,
        DocumentType.TRANSFER_CERTIFICATE,
        DocumentType.AADHAR_CARD,
        DocumentType.PASSPORT_PHOTO
    );

    private final EnquiryDocumentRepository documentRepository;
    private final EnquiryRepository enquiryRepository;

    public EnquiryDocumentService(EnquiryDocumentRepository documentRepository,
                                   EnquiryRepository enquiryRepository) {
        this.documentRepository = documentRepository;
        this.enquiryRepository = enquiryRepository;
    }

    @Transactional
    public EnquiryDocumentResponse addDocument(Long enquiryId, EnquiryDocumentRequest request) {
        Enquiry enquiry = enquiryRepository.findById(enquiryId)
            .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + enquiryId));

        DocumentVerificationStatus status = request.status() != null
            ? request.status() : DocumentVerificationStatus.NOT_UPLOADED;

        EnquiryDocument document = new EnquiryDocument(enquiry, request.documentType(), status);
        document.setRemarks(request.remarks());

        EnquiryDocument saved = documentRepository.save(document);

        return toResponse(saved);
    }

    public MissingDocumentsResponse allMandatoryDocumentsSubmitted(Long enquiryId) {
        if (!enquiryRepository.existsById(enquiryId)) {
            throw new ResourceNotFoundException("Enquiry not found with id: " + enquiryId);
        }

        List<EnquiryDocument> documents = documentRepository.findByEnquiryId(enquiryId);

        Set<DocumentType> submittedTypes = documents.stream()
            .filter(d -> d.getStatus() == DocumentVerificationStatus.UPLOADED
                || d.getStatus() == DocumentVerificationStatus.VERIFIED)
            .map(EnquiryDocument::getDocumentType)
            .collect(Collectors.toSet());

        List<String> missing = new ArrayList<>();
        for (DocumentType mandatory : MANDATORY_DOCUMENTS) {
            if (!submittedTypes.contains(mandatory)) {
                missing.add(mandatory.name());
            }
        }

        return new MissingDocumentsResponse(missing.isEmpty(), missing);
    }

    public List<EnquiryDocumentResponse> findByEnquiryId(Long enquiryId) {
        if (!enquiryRepository.existsById(enquiryId)) {
            throw new ResourceNotFoundException("Enquiry not found with id: " + enquiryId);
        }
        return documentRepository.findByEnquiryId(enquiryId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public EnquiryDocumentResponse updateDocument(Long id, EnquiryDocumentRequest request) {
        EnquiryDocument document = documentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));

        document.setDocumentType(request.documentType());
        if (request.status() != null) {
            document.setStatus(request.status());
        }
        document.setRemarks(request.remarks());

        EnquiryDocument updated = documentRepository.save(document);
        return toResponse(updated);
    }

    @Transactional
    public void deleteDocument(Long id) {
        if (!documentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Document not found with id: " + id);
        }
        documentRepository.deleteById(id);
    }

    private EnquiryDocumentResponse toResponse(EnquiryDocument doc) {
        return new EnquiryDocumentResponse(
            doc.getId(),
            doc.getEnquiry().getId(),
            doc.getDocumentType(),
            doc.getStatus(),
            doc.getRemarks(),
            doc.getVerifiedBy(),
            doc.getVerifiedAt(),
            doc.getCreatedAt(),
            doc.getUpdatedAt()
        );
    }
}

