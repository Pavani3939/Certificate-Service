package com.certificateservice.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public class AssignDesignRequest {

    @NotNull(message = "Design ID must not be null")
    private UUID designId;

    private Instant effectiveFrom;

    public AssignDesignRequest() {
    }

    public AssignDesignRequest(UUID designId, Instant effectiveFrom) {
        this.designId = designId;
        this.effectiveFrom = effectiveFrom;
    }

    public UUID getDesignId() {
        return designId;
    }

    public void setDesignId(UUID designId) {
        this.designId = designId;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Instant effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }
}
