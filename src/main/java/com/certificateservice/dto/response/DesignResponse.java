package com.certificateservice.dto.response;

import com.certificateservice.model.Design;
import com.certificateservice.model.DesignStatus;

import java.time.Instant;
import java.util.UUID;

public class DesignResponse {

    private UUID id;
    private String name;
    private String content;
    private DesignStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public DesignResponse() {
    }

    public DesignResponse(UUID id, String name, String content, DesignStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.content = content;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static DesignResponse fromEntity(Design design) {
        if (design == null) {
            return null;
        }
        return new DesignResponse(
                design.getId(),
                design.getName(),
                design.getContent(),
                design.getStatus(),
                design.getCreatedAt(),
                design.getUpdatedAt()
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public DesignStatus getStatus() {
        return status;
    }

    public void setStatus(DesignStatus status) {
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
