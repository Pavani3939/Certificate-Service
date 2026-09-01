package com.certificateservice.dto.response;

import com.certificateservice.model.Certificate;
import com.certificateservice.model.CertificateStatus;

import java.time.Instant;
import java.util.UUID;

public class CertificateResponse {

    private UUID id;
    private String personName;
    private String personEmail;
    private UUID programmeId;
    private String programmeNameSnapshot;
    private UUID designId;
    private String designNameSnapshot;
    private String designContentSnapshot;
    private Instant issuedAt;
    private CertificateStatus status;
    private String cancellationReason;
    private Instant cancelledAt;

    public CertificateResponse() {
    }

    public CertificateResponse(UUID id, String personName, String personEmail,
                               UUID programmeId, String programmeNameSnapshot,
                               UUID designId, String designNameSnapshot, String designContentSnapshot,
                               Instant issuedAt, CertificateStatus status,
                               String cancellationReason, Instant cancelledAt) {
        this.id = id;
        this.personName = personName;
        this.personEmail = personEmail;
        this.programmeId = programmeId;
        this.programmeNameSnapshot = programmeNameSnapshot;
        this.designId = designId;
        this.designNameSnapshot = designNameSnapshot;
        this.designContentSnapshot = designContentSnapshot;
        this.issuedAt = issuedAt;
        this.status = status;
        this.cancellationReason = cancellationReason;
        this.cancelledAt = cancelledAt;
    }

    public static CertificateResponse fromEntity(Certificate certificate) {
        if (certificate == null) {
            return null;
        }
        return new CertificateResponse(
                certificate.getId(),
                certificate.getPersonName(),
                certificate.getPersonEmail(),
                certificate.getProgramme() != null ? certificate.getProgramme().getId() : null,
                certificate.getProgrammeNameSnapshot(),
                certificate.getDesign() != null ? certificate.getDesign().getId() : null,
                certificate.getDesignNameSnapshot(),
                certificate.getDesignContentSnapshot(),
                certificate.getIssuedAt(),
                certificate.getStatus(),
                certificate.getCancellationReason(),
                certificate.getCancelledAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public UUID getProgrammeId() {
        return programmeId;
    }

    public void setProgrammeId(UUID programmeId) {
        this.programmeId = programmeId;
    }

    public String getProgrammeNameSnapshot() {
        return programmeNameSnapshot;
    }

    public void setProgrammeNameSnapshot(String programmeNameSnapshot) {
        this.programmeNameSnapshot = programmeNameSnapshot;
    }

    public UUID getDesignId() {
        return designId;
    }

    public void setDesignId(UUID designId) {
        this.designId = designId;
    }

    public String getDesignNameSnapshot() {
        return designNameSnapshot;
    }

    public void setDesignNameSnapshot(String designNameSnapshot) {
        this.designNameSnapshot = designNameSnapshot;
    }

    public String getDesignContentSnapshot() {
        return designContentSnapshot;
    }

    public void setDesignContentSnapshot(String designContentSnapshot) {
        this.designContentSnapshot = designContentSnapshot;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public CertificateStatus getStatus() {
        return status;
    }

    public void setStatus(CertificateStatus status) {
        this.status = status;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
}
