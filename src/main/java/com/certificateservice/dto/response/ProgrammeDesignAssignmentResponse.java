package com.certificateservice.dto.response;

import com.certificateservice.model.ProgrammeDesignAssignment;

import java.time.Instant;
import java.util.UUID;

public class ProgrammeDesignAssignmentResponse {

    private UUID id;
    private UUID programmeId;
    private String programmeName;
    private UUID designId;
    private String designName;
    private String designContent;
    private Instant effectiveFrom;
    private Instant createdAt;

    public ProgrammeDesignAssignmentResponse() {
    }

    public ProgrammeDesignAssignmentResponse(UUID id, UUID programmeId, String programmeName,
                                           UUID designId, String designName, String designContent,
                                           Instant effectiveFrom, Instant createdAt) {
        this.id = id;
        this.programmeId = programmeId;
        this.programmeName = programmeName;
        this.designId = designId;
        this.designName = designName;
        this.designContent = designContent;
        this.effectiveFrom = effectiveFrom;
        this.createdAt = createdAt;
    }

    public static ProgrammeDesignAssignmentResponse fromEntity(ProgrammeDesignAssignment assignment) {
        if (assignment == null) {
            return null;
        }
        return new ProgrammeDesignAssignmentResponse(
                assignment.getId(),
                assignment.getProgramme() != null ? assignment.getProgramme().getId() : null,
                assignment.getProgramme() != null ? assignment.getProgramme().getName() : null,
                assignment.getDesign() != null ? assignment.getDesign().getId() : null,
                assignment.getDesign() != null ? assignment.getDesign().getName() : null,
                assignment.getDesign() != null ? assignment.getDesign().getContent() : null,
                assignment.getEffectiveFrom(),
                assignment.getCreatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProgrammeId() {
        return programmeId;
    }

    public void setProgrammeId(UUID programmeId) {
        this.programmeId = programmeId;
    }

    public String getProgrammeName() {
        return programmeName;
    }

    public void setProgrammeName(String programmeName) {
        this.programmeName = programmeName;
    }

    public UUID getDesignId() {
        return designId;
    }

    public void setDesignId(UUID designId) {
        this.designId = designId;
    }

    public String getDesignName() {
        return designName;
    }

    public void setDesignName(String designName) {
        this.designName = designName;
    }

    public String getDesignContent() {
        return designContent;
    }

    public void setDesignContent(String designContent) {
        this.designContent = designContent;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Instant effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
