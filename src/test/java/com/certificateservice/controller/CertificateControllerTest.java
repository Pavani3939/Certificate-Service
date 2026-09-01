package com.certificateservice.controller;

import com.certificateservice.dto.request.CancelCertificateRequest;
import com.certificateservice.dto.request.IssueCertificateRequest;
import com.certificateservice.dto.response.CertificateResponse;
import com.certificateservice.dto.response.PageResponse;
import com.certificateservice.exception.DuplicateCertificateException;
import com.certificateservice.exception.GlobalExceptionHandler;
import com.certificateservice.exception.ResourceNotFoundException;
import com.certificateservice.model.CertificateStatus;
import com.certificateservice.service.CertificateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CertificateController.class)
@Import(GlobalExceptionHandler.class)
class CertificateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CertificateService certificateService;

    @Test
    @DisplayName("POST /api/certificates issues certificate and returns 201 Created")
    void testIssueCertificate() throws Exception {
        UUID progId = UUID.randomUUID();
        UUID certId = UUID.randomUUID();
        UUID designId = UUID.randomUUID();

        IssueCertificateRequest request = new IssueCertificateRequest(progId, "Priya Sharma", "priya@example.com");
        CertificateResponse response = new CertificateResponse(
                certId, "Priya Sharma", "priya@example.com",
                progId, "Advanced SQL", designId, "Gold Border v1", "<content>",
                Instant.now(), CertificateStatus.ACTIVE, null, null
        );

        when(certificateService.issueCertificate(any(IssueCertificateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/certificates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(certId.toString()))
                .andExpect(jsonPath("$.personName").value("Priya Sharma"))
                .andExpect(jsonPath("$.programmeNameSnapshot").value("Advanced SQL"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/certificates returns 409 Conflict when live certificate already exists")
    void testIssueCertificateConflict() throws Exception {
        UUID progId = UUID.randomUUID();
        IssueCertificateRequest request = new IssueCertificateRequest(progId, "Priya Sharma", "priya@example.com");

        when(certificateService.issueCertificate(any(IssueCertificateRequest.class)))
                .thenThrow(new DuplicateCertificateException("A live certificate already exists for person Priya"));

        mockMvc.perform(post("/api/certificates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("A live certificate already exists for person Priya"));
    }

    @Test
    @DisplayName("GET /api/certificates/{id} returns certificate snapshot with 200 OK")
    void testGetCertificateById() throws Exception {
        UUID certId = UUID.randomUUID();
        CertificateResponse response = new CertificateResponse(
                certId, "Priya Sharma", "priya@example.com",
                UUID.randomUUID(), "Advanced SQL", UUID.randomUUID(), "Gold Border v1", "<content>",
                Instant.now(), CertificateStatus.ACTIVE, null, null
        );

        when(certificateService.getCertificateById(certId)).thenReturn(response);

        mockMvc.perform(get("/api/certificates/{id}", certId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(certId.toString()))
                .andExpect(jsonPath("$.personName").value("Priya Sharma"));
    }

    @Test
    @DisplayName("GET /api/certificates?personEmail=... returns paginated list")
    void testListCertificatesByPerson() throws Exception {
        UUID certId = UUID.randomUUID();
        CertificateResponse response = new CertificateResponse(
                certId, "Priya Sharma", "priya@example.com",
                UUID.randomUUID(), "Advanced SQL", UUID.randomUUID(), "Gold Border v1", "<content>",
                Instant.now(), CertificateStatus.ACTIVE, null, null
        );
        PageResponse<CertificateResponse> pageResponse = new PageResponse<>(
                List.of(response), 0, 10, 1, 1, true, true, false, false
        );

        when(certificateService.listCertificatesByPerson(eq("priya@example.com"), any(), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/certificates")
                        .param("personEmail", "priya@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].personName").value("Priya Sharma"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.pageNumber").value(0));
    }

    @Test
    @DisplayName("POST /api/certificates/{id}/cancel cancels certificate")
    void testCancelCertificate() throws Exception {
        UUID certId = UUID.randomUUID();
        CancelCertificateRequest request = new CancelCertificateRequest("Administrative revocation");
        CertificateResponse response = new CertificateResponse(
                certId, "Priya Sharma", "priya@example.com",
                UUID.randomUUID(), "Advanced SQL", UUID.randomUUID(), "Gold Border v1", "<content>",
                Instant.now(), CertificateStatus.CANCELLED, "Administrative revocation", Instant.now()
        );

        when(certificateService.cancelCertificate(eq(certId), any(CancelCertificateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/certificates/{id}/cancel", certId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("Administrative revocation"));
    }
}
