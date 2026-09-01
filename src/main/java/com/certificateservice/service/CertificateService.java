package com.certificateservice.service;

import com.certificateservice.dto.request.CancelCertificateRequest;
import com.certificateservice.dto.request.IssueCertificateRequest;
import com.certificateservice.dto.response.CertificateResponse;
import com.certificateservice.dto.response.PageResponse;
import com.certificateservice.exception.DuplicateCertificateException;
import com.certificateservice.exception.InvalidOperationException;
import com.certificateservice.exception.ResourceNotFoundException;
import com.certificateservice.model.Certificate;
import com.certificateservice.model.CertificateStatus;
import com.certificateservice.model.Design;
import com.certificateservice.model.Programme;
import com.certificateservice.model.ProgrammeDesignAssignment;
import com.certificateservice.repository.CertificateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

@Service
public class CertificateService {

    private static final Logger log = LoggerFactory.getLogger(CertificateService.class);

    private final CertificateRepository certificateRepository;
    private final ProgrammeService programmeService;
    private final ProgrammeDesignService programmeDesignService;
    private final TransactionTemplate transactionTemplate;

    public CertificateService(CertificateRepository certificateRepository,
                              ProgrammeService programmeService,
                              ProgrammeDesignService programmeDesignService,
                              PlatformTransactionManager transactionManager) {
        this.certificateRepository = certificateRepository;
        this.programmeService = programmeService;
        this.programmeDesignService = programmeDesignService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public CertificateResponse issueCertificate(IssueCertificateRequest request) {
        Programme programme = programmeService.findProgrammeOrThrow(request.getProgrammeId());
        if (programme.isDisabled()) {
            throw new InvalidOperationException("Cannot issue certificate for a disabled programme: " + programme.getName());
        }

        String normalizedEmail = request.getPersonEmail().trim().toLowerCase();
        String personName = request.getPersonName().trim();
        Instant issueTimestamp = request.getIssuedAt() != null ? request.getIssuedAt() : Instant.now();

        // Lock on the specific programme + person email tuple to serialize concurrent requests within the JVM
        String lockKey = ("CERT_LOCK:" + programme.getId() + ":" + normalizedEmail).intern();

        synchronized (lockKey) {
            return transactionTemplate.execute(txStatus -> {
                // Pre-check for existing active certificate
                if (certificateRepository.existsByProgrammeIdAndPersonEmailIgnoreCaseAndStatus(
                        programme.getId(), normalizedEmail, CertificateStatus.ACTIVE)) {
                    throw new DuplicateCertificateException(String.format(
                            "A live certificate already exists for person '%s' (%s) in programme '%s'",
                            personName, normalizedEmail, programme.getName()));
                }

                // Determine active design for the programme at the issue timestamp
                ProgrammeDesignAssignment assignment = programmeDesignService.findDesignAssignmentEntityAt(
                        programme.getId(), issueTimestamp);
                Design design = assignment.getDesign();

                // Create certificate with immutable snapshots of programme name and design template
                Certificate certificate = new Certificate(
                        null,
                        personName,
                        normalizedEmail,
                        programme,
                        programme.getName(),
                        design,
                        design.getName(),
                        design.getContent(),
                        issueTimestamp
                );

                try {
                    Certificate saved = certificateRepository.saveAndFlush(certificate);
                    log.info("Issued certificate {} to {} for programme {}", saved.getId(), normalizedEmail, programme.getName());
                    return CertificateResponse.fromEntity(saved);
                } catch (DataIntegrityViolationException ex) {
                    log.warn("Database constraint violation during certificate issuance for {} in {}: {}",
                            normalizedEmail, programme.getId(), ex.getMessage());
                    throw new DuplicateCertificateException(String.format(
                            "A live certificate already exists for person '%s' (%s) in programme '%s'",
                            personName, normalizedEmail, programme.getName()), ex);
                }
            });
        }
    }

    @Transactional(readOnly = true)
    public CertificateResponse getCertificateById(UUID certificateId) {
        Certificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found with ID: " + certificateId));
        return CertificateResponse.fromEntity(certificate);
    }

    @Transactional(readOnly = true)
    public PageResponse<CertificateResponse> listCertificatesByPerson(
            String personEmail, CertificateStatus status, Pageable pageable) {
        String normalizedEmail = personEmail.trim().toLowerCase();
        Page<Certificate> page;

        if (status != null) {
            page = certificateRepository.findByPersonEmailIgnoreCaseAndStatus(normalizedEmail, status, pageable);
        } else {
            page = certificateRepository.findByPersonEmailIgnoreCase(normalizedEmail, pageable);
        }

        Page<CertificateResponse> responsePage = page.map(CertificateResponse::fromEntity);
        return PageResponse.fromPage(responsePage);
    }

    @Transactional
    public CertificateResponse cancelCertificate(UUID certificateId, CancelCertificateRequest request) {
        Certificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found with ID: " + certificateId));

        if (certificate.isCancelled()) {
            throw new InvalidOperationException("Certificate with ID " + certificateId + " is already cancelled");
        }

        certificate.cancel(request.getReason().trim());
        Certificate updated = certificateRepository.save(certificate);
        log.info("Cancelled certificate {} with reason: {}", certificateId, request.getReason());
        return CertificateResponse.fromEntity(updated);
    }
}
