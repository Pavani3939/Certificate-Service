package com.certificateservice.integration;

import com.certificateservice.dto.request.AssignDesignRequest;
import com.certificateservice.dto.request.CreateDesignRequest;
import com.certificateservice.dto.request.CreateProgrammeRequest;
import com.certificateservice.dto.response.DesignResponse;
import com.certificateservice.dto.response.ProgrammeDesignAssignmentResponse;
import com.certificateservice.dto.response.ProgrammeResponse;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProgrammeDesignHistoryIntegrationTest {

    @Autowired
    private DesignService designService;

    @Autowired
    private ProgrammeService programmeService;

    @Autowired
    private ProgrammeDesignService programmeDesignService;

    @Test
    @DisplayName("Verify temporal programme-design history lookup across multiple dates")
    void testTemporalDesignHistory() {
        // 1. Create Designs: Gold Border v1 and Gold Border v2
        DesignResponse goldV1 = designService.createDesign(new CreateDesignRequest(
                "Gold Border v1", "<template>Gold v1 content</template>"));
        DesignResponse goldV2 = designService.createDesign(new CreateDesignRequest(
                "Gold Border v2", "<template>Gold v2 content</template>"));

        // 2. Create Programme: Advanced SQL
        ProgrammeResponse programme = programmeService.createProgramme(new CreateProgrammeRequest(
                "Advanced SQL", "Master SQL"));

        Instant march1 = Instant.parse("2026-03-01T00:00:00Z");
        Instant march5 = Instant.parse("2026-03-05T12:00:00Z");
        Instant march12 = Instant.parse("2026-03-12T00:00:00Z");
        Instant march15 = Instant.parse("2026-03-15T12:00:00Z");

        // 3. Attach Gold Border v1 effective March 1
        programmeDesignService.assignDesign(programme.getId(), new AssignDesignRequest(goldV1.getId(), march1));

        // 4. Attach Gold Border v2 effective March 12
        programmeDesignService.assignDesign(programme.getId(), new AssignDesignRequest(goldV2.getId(), march12));

        // 5. Query active design on March 5 -> Must return Gold Border v1
        ProgrammeDesignAssignmentResponse march5Design = programmeDesignService.getDesignAssignmentAt(
                programme.getId(), march5);
        assertThat(march5Design).isNotNull();
        assertThat(march5Design.getDesignName()).isEqualTo("Gold Border v1");
        assertThat(march5Design.getDesignContent()).isEqualTo("<template>Gold v1 content</template>");

        // 6. Query active design on March 15 -> Must return Gold Border v2
        ProgrammeDesignAssignmentResponse march15Design = programmeDesignService.getDesignAssignmentAt(
                programme.getId(), march15);
        assertThat(march15Design).isNotNull();
        assertThat(march15Design.getDesignName()).isEqualTo("Gold Border v2");
        assertThat(march15Design.getDesignContent()).isEqualTo("<template>Gold v2 content</template>");

        // 7. Verify full chronological assignment history contains both records
        List<ProgrammeDesignAssignmentResponse> history = programmeDesignService.getAssignmentHistory(programme.getId());
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getDesignName()).isEqualTo("Gold Border v2"); // newest first
        assertThat(history.get(1).getDesignName()).isEqualTo("Gold Border v1");
    }
}
