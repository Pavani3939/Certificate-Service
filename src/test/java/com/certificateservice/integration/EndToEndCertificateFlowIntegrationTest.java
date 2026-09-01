package com.certificateservice.integration;

import com.certificateservice.dto.request.AssignDesignRequest;
import com.certificateservice.dto.request.CancelCertificateRequest;
import com.certificateservice.dto.request.CreateDesignRequest;
import com.certificateservice.dto.request.CreateProgrammeRequest;
import com.certificateservice.dto.request.IssueCertificateRequest;
import com.certificateservice.dto.request.RenameProgrammeRequest;
import com.certificateservice.dto.response.CertificateResponse;
import com.certificateservice.dto.response.DesignResponse;
import com.certificateservice.dto.response.PageResponse;
import com.certificateservice.dto.response.ProgrammeResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EndToEndCertificateFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("End-to-End lifecycle: Create design -> Create programme -> Assign design -> Issue cert -> Lookup cert -> Cancel cert -> Re-issue cert")
    void testFullEndToEndLifecycle() throws Exception {
        // 1. Create Design
        CreateDesignRequest designReq = new CreateDesignRequest(
                "Modern Emerald", "<template font='sans'>Emerald Template</template>");
        MvcResult designResult = mockMvc.perform(post("/api/designs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(designReq)))
                .andExpect(status().isCreated())
                .andReturn();
        DesignResponse design = objectMapper.readValue(designResult.getResponse().getContentAsString(), DesignResponse.class);
        assertThat(design.getId()).isNotNull();

        // 2. Create Programme
        CreateProgrammeRequest progReq = new CreateProgrammeRequest(
                "Distributed Systems 101", "Learn Raft and Paxos");
        MvcResult progResult = mockMvc.perform(post("/api/programmes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(progReq)))
                .andExpect(status().isCreated())
                .andReturn();
        ProgrammeResponse programme = objectMapper.readValue(progResult.getResponse().getContentAsString(), ProgrammeResponse.class);
        assertThat(programme.getId()).isNotNull();

        // 3. Assign Design to Programme
        AssignDesignRequest assignReq = new AssignDesignRequest(design.getId(), Instant.now());
        mockMvc.perform(post("/api/programmes/{id}/designs", programme.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.designName").value("Modern Emerald"));

        // 4. Issue Certificate
        IssueCertificateRequest issueReq = new IssueCertificateRequest(
                programme.getId(), "Rahul Verma", "rahul.verma@example.com");
        MvcResult issueResult = mockMvc.perform(post("/api/certificates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(issueReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.personName").value("Rahul Verma"))
                .andExpect(jsonPath("$.programmeNameSnapshot").value("Distributed Systems 101"))
                .andExpect(jsonPath("$.designNameSnapshot").value("Modern Emerald"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();
        CertificateResponse certificate = objectMapper.readValue(issueResult.getResponse().getContentAsString(), CertificateResponse.class);

        // 5. Lookup Certificate by ID (Public Endpoint)
        mockMvc.perform(get("/api/certificates/{id}", certificate.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(certificate.getId().toString()))
                .andExpect(jsonPath("$.programmeNameSnapshot").value("Distributed Systems 101"));

        // 6. List Person's Certificates with Pagination
        MvcResult listResult = mockMvc.perform(get("/api/certificates")
                        .param("personEmail", "rahul.verma@example.com")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].personName").value("Rahul Verma"))
                .andReturn();

        // 7. Attempting duplicate issuance fails with 409 Conflict
        mockMvc.perform(post("/api/certificates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(issueReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // 8. Cancel Certificate
        CancelCertificateRequest cancelReq = new CancelCertificateRequest("User requested name update");
        mockMvc.perform(post("/api/certificates/{id}/cancel", certificate.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("User requested name update"));

        // 9. Re-issue Certificate after cancellation succeeds
        IssueCertificateRequest reissueReq = new IssueCertificateRequest(
                programme.getId(), "Rahul R. Verma", "rahul.verma@example.com");
        mockMvc.perform(post("/api/certificates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reissueReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.personName").value("Rahul R. Verma"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // 10. List person certificates now returns 2 (1 CANCELLED, 1 ACTIVE)
        mockMvc.perform(get("/api/certificates")
                        .param("personEmail", "rahul.verma@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }
}
