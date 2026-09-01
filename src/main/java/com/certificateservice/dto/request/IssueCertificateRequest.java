package com.certificateservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public class IssueCertificateRequest {

    @NotNull(message = "Programme ID must not be null")
    private UUID programmeId;

    @NotBlank(message = "Person name must not be blank")
    @Size(max = 255, message = "Person name must not exceed 255 characters")
    private String personName;

    @NotBlank(message = "Person email must not be blank")
    @Email(message = "Person email must be a valid email address")
    @Size(max = 255, message = "Person email must not exceed 255 characters")
    private String personEmail;

    private Instant issuedAt;

    public IssueCertificateRequest() {
    }

    public IssueCertificateRequest(UUID programmeId, String personName, String personEmail) {
        this.programmeId = programmeId;
        this.personName = personName;
        this.personEmail = personEmail;
    }

    public IssueCertificateRequest(UUID programmeId, String personName, String personEmail, Instant issuedAt) {
        this.programmeId = programmeId;
        this.personName = personName;
        this.personEmail = personEmail;
        this.issuedAt = issuedAt;
    }

    public UUID getProgrammeId() {
        return programmeId;
    }

    public void setProgrammeId(UUID programmeId) {
        this.programmeId = programmeId;
    }

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public String getPersonEmail() {
        return personEmail;
    }

    public void setPersonEmail(String personEmail) {
        this.personEmail = personEmail;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }
}
