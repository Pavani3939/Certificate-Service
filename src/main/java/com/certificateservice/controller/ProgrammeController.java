package com.certificateservice.controller;

import com.certificateservice.dto.request.AssignDesignRequest;
import com.certificateservice.dto.request.CreateProgrammeRequest;
import com.certificateservice.dto.request.RenameProgrammeRequest;
import com.certificateservice.dto.response.ProgrammeDesignAssignmentResponse;
import com.certificateservice.dto.response.ProgrammeResponse;
import com.certificateservice.model.ProgrammeStatus;
import com.certificateservice.service.ProgrammeDesignService;
import com.certificateservice.service.ProgrammeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/programmes")
@Tag(name = "Programmes", description = "Endpoints for managing programmes and their design assignments")
public class ProgrammeController {

    private final ProgrammeService programmeService;
    private final ProgrammeDesignService programmeDesignService;

    public ProgrammeController(ProgrammeService programmeService,
                               ProgrammeDesignService programmeDesignService) {
        this.programmeService = programmeService;
        this.programmeDesignService = programmeDesignService;
    }

    @PostMapping
    @Operation(summary = "Create a new educational programme")
    public ResponseEntity<ProgrammeResponse> createProgramme(@Valid @RequestBody CreateProgrammeRequest request) {
        ProgrammeResponse response = programmeService.createProgramme(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List programmes, optionally filtered by status")
    public ResponseEntity<List<ProgrammeResponse>> listProgrammes(
            @RequestParam(required = false) ProgrammeStatus status) {
        List<ProgrammeResponse> programmes = programmeService.listProgrammes(status);
        return ResponseEntity.ok(programmes);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a programme by ID")
    public ResponseEntity<ProgrammeResponse> getProgrammeById(@PathVariable UUID id) {
        ProgrammeResponse programme = programmeService.getProgrammeById(id);
        return ResponseEntity.ok(programme);
    }

    @PatchMapping("/{id}/name")
    @Operation(summary = "Rename a programme")
    public ResponseEntity<ProgrammeResponse> renameProgramme(
            @PathVariable UUID id,
            @Valid @RequestBody RenameProgrammeRequest request) {
        ProgrammeResponse response = programmeService.renameProgramme(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/name")
    @Operation(summary = "Rename a programme (PUT alias)")
    public ResponseEntity<ProgrammeResponse> renameProgrammePut(
            @PathVariable UUID id,
            @Valid @RequestBody RenameProgrammeRequest request) {
        return renameProgramme(id, request);
    }

    @PostMapping("/{id}/disable")
    @Operation(summary = "Disable a programme")
    public ResponseEntity<ProgrammeResponse> disableProgramme(@PathVariable UUID id) {
        ProgrammeResponse response = programmeService.disableProgramme(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping(path = {"/{id}/designs", "/{id}/design"})
    @Operation(summary = "Attach or change design for a programme")
    public ResponseEntity<ProgrammeDesignAssignmentResponse> assignDesign(
            @PathVariable UUID id,
            @Valid @RequestBody AssignDesignRequest request) {
        ProgrammeDesignAssignmentResponse response = programmeDesignService.assignDesign(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(path = {"/{id}/designs/current", "/{id}/design"})
    @Operation(summary = "Get current active design assignment for a programme")
    public ResponseEntity<ProgrammeDesignAssignmentResponse> getCurrentDesign(@PathVariable UUID id) {
        ProgrammeDesignAssignmentResponse response = programmeDesignService.getCurrentDesignAssignment(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping(path = {"/{id}/designs/historical", "/{id}/design/historical"})
    @Operation(summary = "Get design active for a programme at a specific point in time")
    public ResponseEntity<ProgrammeDesignAssignmentResponse> getHistoricalDesign(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant at) {
        ProgrammeDesignAssignmentResponse response = programmeDesignService.getDesignAssignmentAt(id, at);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/designs/history")
    @Operation(summary = "Get complete design assignment history for a programme")
    public ResponseEntity<List<ProgrammeDesignAssignmentResponse>> getDesignHistory(@PathVariable UUID id) {
        List<ProgrammeDesignAssignmentResponse> history = programmeDesignService.getAssignmentHistory(id);
        return ResponseEntity.ok(history);
    }
}
