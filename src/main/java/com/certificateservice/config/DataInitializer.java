package com.certificateservice.config;

import com.certificateservice.dto.request.AssignDesignRequest;
import com.certificateservice.dto.request.CancelCertificateRequest;
import com.certificateservice.dto.request.CreateDesignRequest;
import com.certificateservice.dto.request.CreateProgrammeRequest;
import com.certificateservice.dto.request.IssueCertificateRequest;
import com.certificateservice.dto.response.CertificateResponse;
import com.certificateservice.dto.response.DesignResponse;
import com.certificateservice.dto.response.ProgrammeResponse;
import com.certificateservice.repository.CertificateRepository;
import com.certificateservice.repository.DesignRepository;
import com.certificateservice.repository.ProgrammeRepository;
import com.certificateservice.service.CertificateService;
import com.certificateservice.service.DesignService;
import com.certificateservice.service.ProgrammeDesignService;
import com.certificateservice.service.ProgrammeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final boolean seedDataEnabled;
    private final DesignRepository designRepository;
    private final ProgrammeRepository programmeRepository;
    private final CertificateRepository certificateRepository;
    private final DesignService designService;
    private final ProgrammeService programmeService;
    private final ProgrammeDesignService programmeDesignService;
    private final CertificateService certificateService;

    public DataInitializer(
            @Value("${app.seed-data.enabled:false}") boolean seedDataEnabled,
            DesignRepository designRepository,
            ProgrammeRepository programmeRepository,
            CertificateRepository certificateRepository,
            DesignService designService,
            ProgrammeService programmeService,
            ProgrammeDesignService programmeDesignService,
            CertificateService certificateService) {
        this.seedDataEnabled = seedDataEnabled;
        this.designRepository = designRepository;
        this.programmeRepository = programmeRepository;
        this.certificateRepository = certificateRepository;
        this.designService = designService;
        this.programmeService = programmeService;
        this.programmeDesignService = programmeDesignService;
        this.certificateService = certificateService;
    }

    @Override
    public void run(String... args) {
        if (!seedDataEnabled) {
            log.info("Seed data initialization is disabled.");
            return;
        }

        if (designRepository.count() > 0 || programmeRepository.count() > 0) {
            log.info("Database already contains data. Skipping seed data generation.");
            return;
        }

        log.info("Populating database with seed test data...");

        Instant now = Instant.now();
        Instant march1 = now.minus(30, ChronoUnit.DAYS);
        Instant march5 = now.minus(25, ChronoUnit.DAYS);
        Instant march12 = now.minus(18, ChronoUnit.DAYS);

        // 1. Create Designs
        DesignResponse goldV1 = designService.createDesign(new CreateDesignRequest(
                "Gold Border v1",
                "<svg class=\"cert-gold-v1\"><border color=\"gold\" width=\"4\"/><title>Certificate of Completion</title><body font=\"Georgia\">{{personName}} has completed {{programmeName}}</body></svg>"
        ));

        DesignResponse goldV2 = designService.createDesign(new CreateDesignRequest(
                "Gold Border v2",
                "<svg class=\"cert-gold-v2\"><border color=\"#FFD700\" width=\"6\"/><title>Official Certificate of Mastery</title><body font=\"Helvetica\">Presented to {{personName}} for {{programmeName}}</body></svg>"
        ));

        DesignResponse modernMinimalist = designService.createDesign(new CreateDesignRequest(
                "Modern Minimalist",
                "<div class=\"minimal-cert\"><h1>Certificate</h1><p>{{personName}} - {{programmeName}}</p></div>"
        ));

        DesignResponse classicVintage = designService.createDesign(new CreateDesignRequest(
                "Classic Vintage",
                "<template name=\"classic\"><header>HONORARY DIPLOMA</header><content>{{personName}} achieved excellence in {{programmeName}}</content></template>"
        ));

        // Disable one legacy design for testing
        designService.disableDesign(classicVintage.getId());

        // 2. Create Programmes
        ProgrammeResponse advSql = programmeService.createProgramme(new CreateProgrammeRequest(
                "Advanced SQL",
                "Deep dive into SQL optimization, indexing, query planning, and window functions."
        ));

        ProgrammeResponse distSys = programmeService.createProgramme(new CreateProgrammeRequest(
                "Distributed Systems",
                "Consensus protocols, Raft, Paxos, distributed transactions, and event-driven architecture."
        ));

        ProgrammeResponse cloudArch = programmeService.createProgramme(new CreateProgrammeRequest(
                "Cloud Native Architecture",
                "Kubernetes, microservices, service mesh, and observability patterns."
        ));

        // 3. Programme-Design Historical Assignments
        // Advanced SQL: March 1 -> Gold Border v1
        programmeDesignService.assignDesign(advSql.getId(), new AssignDesignRequest(goldV1.getId(), march1));
        // Advanced SQL: March 12 -> Gold Border v2
        programmeDesignService.assignDesign(advSql.getId(), new AssignDesignRequest(goldV2.getId(), march12));

        // Distributed Systems: Modern Minimalist
        programmeDesignService.assignDesign(distSys.getId(), new AssignDesignRequest(modernMinimalist.getId(), march1));

        // Cloud Native Architecture: Gold Border v2
        programmeDesignService.assignDesign(cloudArch.getId(), new AssignDesignRequest(goldV2.getId(), march1));

        // 4. Issue Historical and Current Certificates
        // Certificate 1: Issued on March 5 (should snapshot Gold Border v1)
        CertificateResponse certPriyaSql = certificateService.issueCertificate(new IssueCertificateRequest(
                advSql.getId(),
                "Priya Sharma",
                "priya.sharma@example.com",
                march5
        ));

        // Certificate 2: Issued currently for Distributed Systems
        certificateService.issueCertificate(new IssueCertificateRequest(
                distSys.getId(),
                "Priya Sharma",
                "priya.sharma@example.com",
                now.minus(5, ChronoUnit.DAYS)
        ));

        // Certificate 3: Issued to John Doe, then cancelled
        CertificateResponse certJohn = certificateService.issueCertificate(new IssueCertificateRequest(
                advSql.getId(),
                "John Doe",
                "john.doe@example.com",
                now.minus(10, ChronoUnit.DAYS)
        ));
        certificateService.cancelCertificate(certJohn.getId(), new CancelCertificateRequest(
                "Candidate requested re-examination due to updated identity record."
        ));

        // Certificate 4: Issued to Alice Smith for Cloud Architecture
        certificateService.issueCertificate(new IssueCertificateRequest(
                cloudArch.getId(),
                "Alice Smith",
                "alice.smith@example.com",
                now.minus(2, ChronoUnit.DAYS)
        ));

        log.info("Seed test data successfully initialized! Priya's March 5 certificate ID: {}", certPriyaSql.getId());
    }
}
