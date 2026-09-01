package com.certificateservice.integration;

import com.certificateservice.dto.request.AssignDesignRequest;
import com.certificateservice.dto.request.CreateDesignRequest;
import com.certificateservice.dto.request.CreateProgrammeRequest;
import com.certificateservice.dto.request.IssueCertificateRequest;
import com.certificateservice.dto.request.RenameProgrammeRequest;
import com.certificateservice.dto.response.CertificateResponse;
import com.certificateservice.dto.response.DesignResponse;
import com.certificateservice.dto.response.ProgrammeResponse;
import com.certificateservice.model.CertificateStatus;
import com.certificateservice.model.DesignStatus;
import com.certificateservice.model.ProgrammeStatus;
import com.certificateservice.service.CertificateService;
import com.certificateservice.service.DesignService;
import com.certificateservice.service.ProgrammeDesignService;
import com.certificateservice.service.ProgrammeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CertificateImmutabilityIntegrationTest {

    @Autowired
    private DesignService designService;

    @Autowired
    private ProgrammeService programmeService;

    @Autowired
    private ProgrammeDesignService programmeDesignService;

    @Autowired
    private CertificateService certificateService;

    @Test
    @DisplayName("Verify certificate immutability after programme rename, design change, programme disabling, and design disabling")
    void testCertificateImmutability() {
        // Step 1: Create initial design (Gold Border v1)
        DesignResponse goldV1 = designService.createDesign(new CreateDesignRequest(
                "Gold Border v1",
                "<template font=\"serif\">Original Gold Border v1 Template</template>"
        ));

        // Step 2: Create initial programme (Advanced SQL)
        ProgrammeResponse programme = programmeService.createProgramme(new CreateProgrammeRequest(
                "Advanced SQL",
                "Original SQL curriculum description"
        ));

        // Step 3: Attach Gold Border v1 to Advanced SQL
        programmeDesignService.assignDesign(programme.getId(), new AssignDesignRequest(
                goldV1.getId(), Instant.now()
        ));

        // Step 4: Issue certificate to Priya Sharma
        CertificateResponse issuedCert = certificateService.issueCertificate(new IssueCertificateRequest(
                programme.getId(),
                "Priya Sharma",
                "priya.sharma@example.com"
        ));

        assertThat(issuedCert).isNotNull();
        assertThat(issuedCert.getProgrammeNameSnapshot()).isEqualTo("Advanced SQL");
        assertThat(issuedCert.getDesignNameSnapshot()).isEqualTo("Gold Border v1");
        assertThat(issuedCert.getDesignContentSnapshot()).isEqualTo("<template font=\"serif\">Original Gold Border v1 Template</template>");
        assertThat(issuedCert.getStatus()).isEqualTo(CertificateStatus.ACTIVE);

        // Step 5: Perform all mutations
        // 1) Rename the programme
        programmeService.renameProgramme(programme.getId(), new RenameProgrammeRequest("Mastering Enterprise SQL"));

        // 2) Create a new design (Gold Border v2) and attach it to the programme
        DesignResponse goldV2 = designService.createDesign(new CreateDesignRequest(
                "Gold Border v2",
                "<template font=\"sans-serif\">Modern Gold Border v2 Template</template>"
        ));
        programmeDesignService.assignDesign(programme.getId(), new AssignDesignRequest(
                goldV2.getId(), Instant.now()
        ));

        // 3) Disable the programme
        programmeService.disableProgramme(programme.getId());

        // 4) Disable the original design (Gold Border v1)
        designService.disableDesign(goldV1.getId());

        // Verify live entities reflect the mutations
        ProgrammeResponse mutatedProgramme = programmeService.getProgrammeById(programme.getId());
        assertThat(mutatedProgramme.getName()).isEqualTo("Mastering Enterprise SQL");
        assertThat(mutatedProgramme.getStatus()).isEqualTo(ProgrammeStatus.DISABLED);

        DesignResponse mutatedDesign = designService.getDesignById(goldV1.getId());
        assertThat(mutatedDesign.getStatus()).isEqualTo(DesignStatus.DISABLED);

        // Step 6: Public Lookup the previously issued certificate
        CertificateResponse lookupCert = certificateService.getCertificateById(issuedCert.getId());

        // Step 7: Verify that the certificate STILL contains EXACTLY the original snapshot data!
        assertThat(lookupCert).isNotNull();
        assertThat(lookupCert.getId()).isEqualTo(issuedCert.getId());
        assertThat(lookupCert.getPersonName()).isEqualTo("Priya Sharma");
        assertThat(lookupCert.getPersonEmail()).isEqualTo("priya.sharma@example.com");
        assertThat(lookupCert.getProgrammeNameSnapshot()).isEqualTo("Advanced SQL");
        assertThat(lookupCert.getDesignNameSnapshot()).isEqualTo("Gold Border v1");
        assertThat(lookupCert.getDesignContentSnapshot()).isEqualTo("<template font=\"serif\">Original Gold Border v1 Template</template>");
        assertThat(lookupCert.getStatus()).isEqualTo(CertificateStatus.ACTIVE);
    }
}
