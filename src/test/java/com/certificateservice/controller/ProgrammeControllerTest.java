package com.certificateservice.controller;

import com.certificateservice.dto.request.AssignDesignRequest;
import com.certificateservice.dto.request.CreateProgrammeRequest;
import com.certificateservice.dto.request.RenameProgrammeRequest;
import com.certificateservice.dto.response.ProgrammeDesignAssignmentResponse;
import com.certificateservice.dto.response.ProgrammeResponse;
import com.certificateservice.exception.GlobalExceptionHandler;
import com.certificateservice.exception.ResourceNotFoundException;
import com.certificateservice.model.ProgrammeStatus;
import com.certificateservice.service.ProgrammeDesignService;
import com.certificateservice.service.ProgrammeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProgrammeController.class)
@Import(GlobalExceptionHandler.class)
class ProgrammeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProgrammeService programmeService;

    @MockBean
    private ProgrammeDesignService programmeDesignService;

    @Test
    @DisplayName("POST /api/programmes creates programme and returns 201 Created")
    void testCreateProgramme() throws Exception {
        CreateProgrammeRequest request = new CreateProgrammeRequest("Advanced SQL", "SQL Course");
        UUID id = UUID.randomUUID();
        ProgrammeResponse response = new ProgrammeResponse(id, "Advanced SQL", "SQL Course",
                ProgrammeStatus.ACTIVE, Instant.now(), Instant.now());

        when(programmeService.createProgramme(any(CreateProgrammeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/programmes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Advanced SQL"));
    }

    @Test
    @DisplayName("PATCH /api/programmes/{id}/name renames programme")
    void testRenameProgramme() throws Exception {
        UUID id = UUID.randomUUID();
        RenameProgrammeRequest request = new RenameProgrammeRequest("Mastering SQL");
        ProgrammeResponse response = new ProgrammeResponse(id, "Mastering SQL", "SQL Course",
                ProgrammeStatus.ACTIVE, Instant.now(), Instant.now());

        when(programmeService.renameProgramme(eq(id), any(RenameProgrammeRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/programmes/{id}/name", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mastering SQL"));
    }

    @Test
    @DisplayName("POST /api/programmes/{id}/disable disables programme")
    void testDisableProgramme() throws Exception {
        UUID id = UUID.randomUUID();
        ProgrammeResponse response = new ProgrammeResponse(id, "Advanced SQL", "SQL Course",
                ProgrammeStatus.DISABLED, Instant.now(), Instant.now());

        when(programmeService.disableProgramme(eq(id))).thenReturn(response);

        mockMvc.perform(post("/api/programmes/{id}/disable", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));
    }

    @Test
    @DisplayName("POST /api/programmes/{id}/designs attaches design to programme")
    void testAssignDesign() throws Exception {
        UUID progId = UUID.randomUUID();
        UUID designId = UUID.randomUUID();
        AssignDesignRequest request = new AssignDesignRequest(designId, Instant.now());
        ProgrammeDesignAssignmentResponse response = new ProgrammeDesignAssignmentResponse(
                UUID.randomUUID(), progId, "Advanced SQL", designId, "Gold Border v1", "<content>",
                Instant.now(), Instant.now()
        );

        when(programmeDesignService.assignDesign(eq(progId), any(AssignDesignRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/programmes/{id}/designs", progId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.designName").value("Gold Border v1"));
    }

    @Test
    @DisplayName("GET /api/programmes/{id}/designs/historical retrieves historical design at timestamp")
    void testGetHistoricalDesign() throws Exception {
        UUID progId = UUID.randomUUID();
        UUID designId = UUID.randomUUID();
        Instant at = Instant.parse("2026-03-05T00:00:00Z");

        ProgrammeDesignAssignmentResponse response = new ProgrammeDesignAssignmentResponse(
                UUID.randomUUID(), progId, "Advanced SQL", designId, "Gold Border v1", "<content>",
                Instant.parse("2026-03-01T00:00:00Z"), Instant.now()
        );

        when(programmeDesignService.getDesignAssignmentAt(eq(progId), eq(at))).thenReturn(response);

        mockMvc.perform(get("/api/programmes/{id}/designs/historical", progId)
                        .param("at", "2026-03-05T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.designName").value("Gold Border v1"));
    }

    @Test
    @DisplayName("GET /api/programmes/{id}/designs/history returns assignment history")
    void testGetDesignHistory() throws Exception {
        UUID progId = UUID.randomUUID();
        ProgrammeDesignAssignmentResponse response = new ProgrammeDesignAssignmentResponse(
                UUID.randomUUID(), progId, "Advanced SQL", UUID.randomUUID(), "Gold Border v1", "<content>",
                Instant.parse("2026-03-01T00:00:00Z"), Instant.now()
        );

        when(programmeDesignService.getAssignmentHistory(eq(progId))).thenReturn(List.of(response));

        mockMvc.perform(get("/api/programmes/{id}/designs/history", progId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].designName").value("Gold Border v1"));
    }
}
