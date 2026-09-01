package com.certificateservice.dto.response;

import com.certificateservice.model.Programme;
import com.certificateservice.model.ProgrammeStatus;

import java.time.Instant;
import java.util.UUID;

public class ProgrammeResponse {

    private UUID id;
    private String name;
    private String description;
    private ProgrammeStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public ProgrammeResponse() {
    }

    public ProgrammeResponse(UUID id, String name, String description, ProgrammeStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ProgrammeResponse fromEntity(Programme programme) {
        if (programme == null) {
            return null;
        }
        return new ProgrammeResponse(
                programme.getId(),
                programme.getName(),
                programme.getDescription(),
                programme.getStatus(),
                programme.getCreatedAt(),
                programme.getUpdatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProgrammeStatus getStatus() {
        return status;
    }

    public void setStatus(ProgrammeStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
