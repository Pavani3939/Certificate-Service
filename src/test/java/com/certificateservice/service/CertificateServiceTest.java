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
import com.certificateservice.model.DesignStatus;
import com.certificateservice.model.Programme;
import com.certificateservice.model.ProgrammeDesignAssignment;
import com.certificateservice.model.ProgrammeStatus;
import com.certificateservice.repository.CertificateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    @Mock
    private CertificateRepository certificateRepository;

    @Mock
    private ProgrammeService programmeService;

    @Mock
    private ProgrammeDesignService programmeDesignService;

    private CertificateService certificateService;

    private Programme programme;
    private Design design;
    private ProgrammeDesignAssignment assignment;
    private Certificate certificate;
    private UUID certId;
    private UUID programmeId;
    private UUID designId;

    @BeforeEach
    void setUp() {
        programmeId = UUID.randomUUID();
        designId = UUID.randomUUID();
        certId = UUID.randomUUID();

        PlatformTransactionManager mockTxManager = new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
        };

        certificateService = new CertificateService(
                certificateRepository,
                programmeService,
                programmeDesignService,
                mockTxManager
        );

        programme = new Programme(programmeId, "Advanced SQL", "SQL", ProgrammeStatus.ACTIVE);
        design = new Design(designId, "Gold Border v1", "<template>Gold</template>", DesignStatus.ACTIVE);
        assignment = new ProgrammeDesignAssignment(UUID.randomUUID(), programme, design, Instant.now());
        certificate = new Certificate(
                certId,
                "Priya Sharma",
                "priya@example.com",
                programme,
                "Advanced SQL",
                design,
                "Gold Border v1",
                "<template>Gold</template>",
                Instant.now()
        );
    }

    @Test
    @DisplayName("Issue certificate creates immutable snapshot record")
    void testIssueCertificateSuccess() {
        IssueCertificateRequest request = new IssueCertificateRequest(programmeId, "Priya Sharma", "priya@example.com");

        when(programmeService.findProgrammeOrThrow(programmeId)).thenReturn(programme);
        when(certificateRepository.existsByProgrammeIdAndPersonEmailIgnoreCaseAndStatus(
                programmeId, "priya@example.com", CertificateStatus.ACTIVE)).thenReturn(false);
        when(programmeDesignService.findDesignAssignmentEntityAt(eq(programmeId), any(Instant.class))).thenReturn(assignment);
        when(certificateRepository.saveAndFlush(any(Certificate.class))).thenAnswer(i -> i.getArgument(0));

        CertificateResponse response = certificateService.issueCertificate(request);

        assertThat(response).isNotNull();
        assertThat(response.getPersonName()).isEqualTo("Priya Sharma");
        assertThat(response.getPersonEmail()).isEqualTo("priya@example.com");
        assertThat(response.getProgrammeNameSnapshot()).isEqualTo("Advanced SQL");
        assertThat(response.getDesignNameSnapshot()).isEqualTo("Gold Border v1");
        assertThat(response.getDesignContentSnapshot()).isEqualTo("<template>Gold</template>");
        assertThat(response.getStatus()).isEqualTo(CertificateStatus.ACTIVE);
    }

    @Test
    @DisplayName("Issue certificate fails when programme is disabled")
    void testIssueCertificateProgrammeDisabled() {
        programme.setStatus(ProgrammeStatus.DISABLED);
        when(programmeService.findProgrammeOrThrow(programmeId)).thenReturn(programme);

        IssueCertificateRequest request = new IssueCertificateRequest(programmeId, "Priya Sharma", "priya@example.com");

        assertThatThrownBy(() -> certificateService.issueCertificate(request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot issue certificate for a disabled programme");
    }

    @Test
    @DisplayName("Issue certificate fails when a live certificate already exists for person & programme")
    void testIssueCertificateDuplicateLiveCertificate() {
        IssueCertificateRequest request = new IssueCertificateRequest(programmeId, "Priya Sharma", "priya@example.com");

        when(programmeService.findProgrammeOrThrow(programmeId)).thenReturn(programme);
        when(certificateRepository.existsByProgrammeIdAndPersonEmailIgnoreCaseAndStatus(
                programmeId, "priya@example.com", CertificateStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> certificateService.issueCertificate(request))
                .isInstanceOf(DuplicateCertificateException.class)
                .hasMessageContaining("A live certificate already exists");
    }

    @Test
    @DisplayName("Get certificate by ID returns snapshot record")
    void testGetCertificateByIdFound() {
        when(certificateRepository.findById(certId)).thenReturn(Optional.of(certificate));

        CertificateResponse response = certificateService.getCertificateById(certId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(certId);
        assertThat(response.getProgrammeNameSnapshot()).isEqualTo("Advanced SQL");
    }

    @Test
    @DisplayName("Get certificate by ID throws ResourceNotFoundException if not found")
    void testGetCertificateByIdNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(certificateRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> certificateService.getCertificateById(unknownId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Certificate not found with ID: " + unknownId);
    }

    @Test
    @DisplayName("List certificates by person with pagination")
    void testListCertificatesByPerson() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Certificate> page = new PageImpl<>(List.of(certificate), pageable, 1);

        when(certificateRepository.findByPersonEmailIgnoreCase("priya@example.com", pageable))
                .thenReturn(page);

        PageResponse<CertificateResponse> response = certificateService.listCertificatesByPerson(
                "priya@example.com", null, pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getPageNumber()).isZero();
    }

    @Test
    @DisplayName("Cancel certificate marks status as CANCELLED and sets reason and timestamp")
    void testCancelCertificateSuccess() {
        CancelCertificateRequest request = new CancelCertificateRequest("Invalid exam submission");
        when(certificateRepository.findById(certId)).thenReturn(Optional.of(certificate));
        when(certificateRepository.save(any(Certificate.class))).thenAnswer(i -> i.getArgument(0));

        CertificateResponse response = certificateService.cancelCertificate(certId, request);

        assertThat(response.getStatus()).isEqualTo(CertificateStatus.CANCELLED);
        assertThat(response.getCancellationReason()).isEqualTo("Invalid exam submission");
        assertThat(response.getCancelledAt()).isNotNull();
        assertThat(certificate.isCancelled()).isTrue();
    }

    @Test
    @DisplayName("Cancel certificate fails when certificate is already cancelled")
    void testCancelCertificateAlreadyCancelled() {
        certificate.cancel("Already cancelled once");
        when(certificateRepository.findById(certId)).thenReturn(Optional.of(certificate));

        CancelCertificateRequest request = new CancelCertificateRequest("Second cancel attempt");

        assertThatThrownBy(() -> certificateService.cancelCertificate(certId, request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("is already cancelled");
    }
}
