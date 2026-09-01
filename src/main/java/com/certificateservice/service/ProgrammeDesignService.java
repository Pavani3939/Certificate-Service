package com.certificateservice.service;

import com.certificateservice.dto.request.AssignDesignRequest;
import com.certificateservice.dto.response.ProgrammeDesignAssignmentResponse;
import com.certificateservice.exception.InvalidOperationException;
import com.certificateservice.exception.ResourceNotFoundException;
import com.certificateservice.model.Design;
import com.certificateservice.model.Programme;
import com.certificateservice.model.ProgrammeDesignAssignment;
import com.certificateservice.repository.ProgrammeDesignAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProgrammeDesignService {

    private final ProgrammeDesignAssignmentRepository assignmentRepository;
    private final ProgrammeService programmeService;
    private final DesignService designService;

    public ProgrammeDesignService(ProgrammeDesignAssignmentRepository assignmentRepository,
                                  ProgrammeService programmeService,
                                  DesignService designService) {
        this.assignmentRepository = assignmentRepository;
        this.programmeService = programmeService;
        this.designService = designService;
    }

    public ProgrammeDesignAssignmentResponse assignDesign(UUID programmeId, AssignDesignRequest request) {
        Programme programme = programmeService.findProgrammeOrThrow(programmeId);
        if (programme.isDisabled()) {
            throw new InvalidOperationException("Cannot assign design to a disabled programme: " + programme.getName());
        }

        Design design = designService.findDesignOrThrow(request.getDesignId());
        if (design.isDisabled()) {
            throw new InvalidOperationException("Cannot assign a disabled design: " + design.getName());
        }

        Instant effectiveFrom = request.getEffectiveFrom() != null ? request.getEffectiveFrom() : Instant.now();

        ProgrammeDesignAssignment assignment = new ProgrammeDesignAssignment(
                null,
                programme,
                design,
                effectiveFrom
        );

        ProgrammeDesignAssignment saved = assignmentRepository.save(assignment);
        return ProgrammeDesignAssignmentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public ProgrammeDesignAssignmentResponse getCurrentDesignAssignment(UUID programmeId) {
        return getDesignAssignmentAt(programmeId, Instant.now());
    }

    @Transactional(readOnly = true)
    public ProgrammeDesignAssignmentResponse getDesignAssignmentAt(UUID programmeId, Instant timestamp) {
        ProgrammeDesignAssignment assignment = findDesignAssignmentEntityAt(programmeId, timestamp);
        return ProgrammeDesignAssignmentResponse.fromEntity(assignment);
    }

    @Transactional(readOnly = true)
    public ProgrammeDesignAssignment findDesignAssignmentEntityAt(UUID programmeId, Instant timestamp) {
        // Ensure programme exists
        programmeService.findProgrammeOrThrow(programmeId);

        Instant searchTime = timestamp != null ? timestamp : Instant.now();
        return assignmentRepository
                .findFirstByProgrammeIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(programmeId, searchTime)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("No design assignment found for programme %s effective at %s", programmeId, searchTime)));
    }

    @Transactional(readOnly = true)
    public List<ProgrammeDesignAssignmentResponse> getAssignmentHistory(UUID programmeId) {
        programmeService.findProgrammeOrThrow(programmeId);
        List<ProgrammeDesignAssignment> history = assignmentRepository.findByProgrammeIdOrderByEffectiveFromDesc(programmeId);
        return history.stream()
                .map(ProgrammeDesignAssignmentResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
