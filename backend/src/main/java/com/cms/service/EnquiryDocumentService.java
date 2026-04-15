package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.EnquiryDocumentRequest;
import com.cms.dto.EnquiryDocumentResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Enquiry;
import com.cms.model.EnquiryDocument;
import com.cms.model.enums.DocumentVerificationStatus;
import com.cms.model.enums.EnquiryStatus;
import com.cms.repository.EnquiryDocumentRepository;
import com.cms.repository.EnquiryRepository;

@Service
@Transactional(readOnly = true)
public class EnquiryDocumentService {

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

        // Auto-update enquiry status to DOCUMENTS_SUBMITTED if applicable
        if (enquiry.getStatus() == EnquiryStatus.FEES_PAID
            || enquiry.getStatus() == EnquiryStatus.PARTIALLY_PAID) {
            enquiry.setStatus(EnquiryStatus.DOCUMENTS_SUBMITTED);
            enquiryRepository.save(enquiry);
        }

        return toResponse(saved);
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
