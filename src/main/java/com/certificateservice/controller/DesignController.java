package com.certificateservice.controller;

import com.certificateservice.dto.request.CreateDesignRequest;
import com.certificateservice.dto.response.DesignResponse;
import com.certificateservice.model.DesignStatus;
import com.certificateservice.service.DesignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/designs")
@Tag(name = "Designs", description = "Endpoints for managing certificate designs and templates")
public class DesignController {

    private final DesignService designService;

    public DesignController(DesignService designService) {
        this.designService = designService;
    }

    @PostMapping
    @Operation(summary = "Create a new certificate design")
    public ResponseEntity<DesignResponse> createDesign(@Valid @RequestBody CreateDesignRequest request) {
        DesignResponse response = designService.createDesign(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List designs, optionally filtered by status")
    public ResponseEntity<List<DesignResponse>> listDesigns(
            @RequestParam(required = false) DesignStatus status) {
        List<DesignResponse> designs = designService.listDesigns(status);
        return ResponseEntity.ok(designs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a design by ID")
    public ResponseEntity<DesignResponse> getDesignById(@PathVariable UUID id) {
        DesignResponse design = designService.getDesignById(id);
        return ResponseEntity.ok(design);
    }

    @PostMapping("/{id}/disable")
    @Operation(summary = "Disable a design")
    public ResponseEntity<DesignResponse> disableDesign(@PathVariable UUID id) {
        DesignResponse response = designService.disableDesign(id);
        return ResponseEntity.ok(response);
    }
}
