package com.certificateservice.controller;

import com.certificateservice.dto.request.CancelCertificateRequest;
import com.certificateservice.dto.request.IssueCertificateRequest;
import com.certificateservice.dto.response.CertificateResponse;
import com.certificateservice.dto.response.PageResponse;
import com.certificateservice.model.CertificateStatus;
import com.certificateservice.service.CertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/certificates")
@Tag(name = "Certificates", description = "Endpoints for certificate issuance, immutable lookup, listing, and cancellation")
public class CertificateController {

    private final CertificateService certificateService;

    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @PostMapping
    @Operation(summary = "Issue a new immutable certificate for a programme")
    public ResponseEntity<CertificateResponse> issueCertificate(
            @Valid @RequestBody IssueCertificateRequest request) {
        CertificateResponse response = certificateService.issueCertificate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{certificateId}")
    @Operation(summary = "Public lookup of an immutable certificate by ID")
    public ResponseEntity<CertificateResponse> getCertificateById(
            @PathVariable UUID certificateId) {
        CertificateResponse response = certificateService.getCertificateById(certificateId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List certificates belonging to a person with pagination")
    public ResponseEntity<PageResponse<CertificateResponse>> listCertificatesByPerson(
            @RequestParam String personEmail,
            @RequestParam(required = false) CertificateStatus status,
            @ParameterObject @PageableDefault(sort = "issuedAt", direction = Sort.Direction.DESC, size = 10) Pageable pageable) {
        PageResponse<CertificateResponse> response = certificateService.listCertificatesByPerson(
                personEmail, status, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/person/{personEmail}")
    @Operation(summary = "List certificates belonging to a person by email path variable")
    public ResponseEntity<PageResponse<CertificateResponse>> listCertificatesByPersonPath(
            @PathVariable String personEmail,
            @RequestParam(required = false) CertificateStatus status,
            @ParameterObject @PageableDefault(sort = "issuedAt", direction = Sort.Direction.DESC, size = 10) Pageable pageable) {
        return listCertificatesByPerson(personEmail, status, pageable);
    }

    @PostMapping("/{certificateId}/cancel")
    @Operation(summary = "Cancel an issued certificate with a reason")
    public ResponseEntity<CertificateResponse> cancelCertificate(
            @PathVariable UUID certificateId,
            @Valid @RequestBody CancelCertificateRequest request) {
        CertificateResponse response = certificateService.cancelCertificate(certificateId, request);
        return ResponseEntity.ok(response);
    }
}
