package com.certificateservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "certificates")
public class Certificate {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "person_name", nullable = false)
    private String personName;

    @Column(name = "person_email", nullable = false)
    private String personEmail;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "programme_id", nullable = false)
    private Programme programme;

    @Column(name = "programme_name_snapshot", nullable = false)
    private String programmeNameSnapshot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "design_id", nullable = false)
    private Design design;

    @Column(name = "design_name_snapshot", nullable = false)
    private String designNameSnapshot;

    @Column(name = "design_content_snapshot", nullable = false, columnDefinition = "TEXT")
    private String designContentSnapshot;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private CertificateStatus status = CertificateStatus.ACTIVE;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    public Certificate() {
    }

    public Certificate(UUID id, String personName, String personEmail,
                       Programme programme, String programmeNameSnapshot,
                       Design design, String designNameSnapshot, String designContentSnapshot,
                       Instant issuedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.personName = personName;
        this.personEmail = personEmail;
        this.programme = programme;
        this.programmeNameSnapshot = programmeNameSnapshot;
        this.design = design;
        this.designNameSnapshot = designNameSnapshot;
        this.designContentSnapshot = designContentSnapshot;
        this.issuedAt = issuedAt != null ? issuedAt : Instant.now();
        this.status = CertificateStatus.ACTIVE;
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.issuedAt == null) {
            this.issuedAt = Instant.now();
        }
        if (this.status == null) {
            this.status = CertificateStatus.ACTIVE;
        }
    }

    public boolean isLive() {
        return this.status == CertificateStatus.ACTIVE;
    }

    public boolean isCancelled() {
        return this.status == CertificateStatus.CANCELLED;
    }

    public void cancel(String reason) {
        this.status = CertificateStatus.CANCELLED;
        this.cancellationReason = reason;
        this.cancelledAt = Instant.now();
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

    public Programme getProgramme() {
        return programme;
    }

    public void setProgramme(Programme programme) {
        this.programme = programme;
    }

    public String getProgrammeNameSnapshot() {
        return programmeNameSnapshot;
    }

    public void setProgrammeNameSnapshot(String programmeNameSnapshot) {
        this.programmeNameSnapshot = programmeNameSnapshot;
    }

    public Design getDesign() {
        return design;
    }

    public void setDesign(Design design) {
        this.design = design;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Certificate that = (Certificate) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
