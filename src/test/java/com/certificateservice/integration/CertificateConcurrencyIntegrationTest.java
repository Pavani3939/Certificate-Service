package com.certificateservice.integration;

import com.certificateservice.dto.request.AssignDesignRequest;
import com.certificateservice.dto.request.CancelCertificateRequest;
import com.certificateservice.dto.request.CreateDesignRequest;
import com.certificateservice.dto.request.CreateProgrammeRequest;
import com.certificateservice.dto.request.IssueCertificateRequest;
import com.certificateservice.dto.response.CertificateResponse;
import com.certificateservice.dto.response.DesignResponse;
import com.certificateservice.dto.response.ProgrammeResponse;
import com.certificateservice.exception.DuplicateCertificateException;
import com.certificateservice.model.Certificate;
import com.certificateservice.model.CertificateStatus;
import com.certificateservice.repository.CertificateRepository;
import com.certificateservice.service.CertificateService;
import com.certificateservice.service.DesignService;
import com.certificateservice.service.ProgrammeDesignService;
import com.certificateservice.service.ProgrammeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CertificateConcurrencyIntegrationTest {

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private DesignService designService;

    @Autowired
    private ProgrammeService programmeService;

    @Autowired
    private ProgrammeDesignService programmeDesignService;

    @Autowired
    private CertificateRepository certificateRepository;

    private UUID programmeId;
    private final String personEmail = "priya.concurrency@example.com";
    private final String personName = "Priya Sharma";

    @BeforeEach
    void setUp() {
        certificateRepository.deleteAll();

        DesignResponse design = designService.createDesign(new CreateDesignRequest(
                "Gold Border v1", "<template>Gold</template>"));
        ProgrammeResponse programme = programmeService.createProgramme(new CreateProgrammeRequest(
                "Advanced SQL - " + UUID.randomUUID(), "SQL Course"));
        programmeDesignService.assignDesign(programme.getId(), new AssignDesignRequest(
                design.getId(), Instant.now()));

        programmeId = programme.getId();
    }

    @Test
    @DisplayName("Concurrent issuance for same person and programme creates exactly ONE live certificate and rejects the rest")
    void testConcurrentCertificateIssuance() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<CertificateResponse> createdCertificates = Collections.synchronizedList(new ArrayList<>());
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Wait for all threads to be ready to fire simultaneously
                    CertificateResponse response = certificateService.issueCertificate(
                            new IssueCertificateRequest(programmeId, personName, personEmail));
                    successCount.incrementAndGet();
                    createdCertificates.add(response);
                } catch (DuplicateCertificateException ex) {
                    conflictCount.incrementAndGet();
                } catch (Throwable ex) {
                    errors.add(ex);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Fire all threads simultaneously
        boolean finished = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(errors).isEmpty();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(numberOfThreads - 1);
        assertThat(createdCertificates).hasSize(1);

        // Verify in database: exactly 1 active certificate exists
        List<Certificate> certsInDb = certificateRepository.findAll();
        assertThat(certsInDb).hasSize(1);
        assertThat(certsInDb.get(0).getStatus()).isEqualTo(CertificateStatus.ACTIVE);
        assertThat(certsInDb.get(0).getPersonEmail()).isEqualTo(personEmail);
    }

    @Test
    @DisplayName("Re-issuing certificate after cancellation succeeds without conflict")
    void testIssueCertificateAfterCancellation() {
        // 1. Issue first certificate
        CertificateResponse firstCert = certificateService.issueCertificate(
                new IssueCertificateRequest(programmeId, personName, personEmail));
        assertThat(firstCert.getStatus()).isEqualTo(CertificateStatus.ACTIVE);

        // 2. Attempting to issue a duplicate active certificate immediately fails
        assertThatThrownBy(() -> certificateService.issueCertificate(
                new IssueCertificateRequest(programmeId, personName, personEmail)))
                .isInstanceOf(DuplicateCertificateException.class);

        // 3. Cancel the first certificate
        CertificateResponse cancelledCert = certificateService.cancelCertificate(
                firstCert.getId(), new CancelCertificateRequest("Student requested re-issuance"));
        assertThat(cancelledCert.getStatus()).isEqualTo(CertificateStatus.CANCELLED);

        // 4. Issue a new certificate for the same person & programme
        CertificateResponse secondCert = certificateService.issueCertificate(
                new IssueCertificateRequest(programmeId, personName, personEmail));
        assertThat(secondCert.getStatus()).isEqualTo(CertificateStatus.ACTIVE);
        assertThat(secondCert.getId()).isNotEqualTo(firstCert.getId());

        // 5. Total certificates for person is 2 (1 CANCELLED, 1 ACTIVE)
        var personCerts = certificateService.listCertificatesByPerson(personEmail, null, Pageable.unpaged());
        assertThat(personCerts.getContent()).hasSize(2);

        // 6. Active certificates for person is exactly 1
        var activeCerts = certificateService.listCertificatesByPerson(personEmail, CertificateStatus.ACTIVE, Pageable.unpaged());
        assertThat(activeCerts.getContent()).hasSize(1);
        assertThat(activeCerts.getContent().get(0).getId()).isEqualTo(secondCert.getId());
    }
}
