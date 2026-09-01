package com.certificateservice.service;

import com.certificateservice.dto.request.AssignDesignRequest;
import com.certificateservice.dto.response.ProgrammeDesignAssignmentResponse;
import com.certificateservice.exception.InvalidOperationException;
import com.certificateservice.exception.ResourceNotFoundException;
import com.certificateservice.model.Design;
import com.certificateservice.model.DesignStatus;
import com.certificateservice.model.Programme;
import com.certificateservice.model.ProgrammeDesignAssignment;
import com.certificateservice.model.ProgrammeStatus;
import com.certificateservice.repository.ProgrammeDesignAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgrammeDesignServiceTest {

    @Mock
    private ProgrammeDesignAssignmentRepository assignmentRepository;

    @Mock
    private ProgrammeService programmeService;

    @Mock
    private DesignService designService;

    @InjectMocks
    private ProgrammeDesignService programmeDesignService;

    private Programme programme;
    private Design design;
    private UUID programmeId;
    private UUID designId;

    @BeforeEach
    void setUp() {
        programmeId = UUID.randomUUID();
        designId = UUID.randomUUID();
        programme = new Programme(programmeId, "Advanced SQL", "SQL", ProgrammeStatus.ACTIVE);
        design = new Design(designId, "Gold Border v1", "<template>Gold</template>", DesignStatus.ACTIVE);
    }

    @Test
    @DisplayName("Assign design successfully when programme and design are active")
    void testAssignDesignSuccess() {
        AssignDesignRequest request = new AssignDesignRequest(designId, Instant.now());
        when(programmeService.findProgrammeOrThrow(programmeId)).thenReturn(programme);
        when(designService.findDesignOrThrow(designId)).thenReturn(design);
        when(assignmentRepository.save(any(ProgrammeDesignAssignment.class))).thenAnswer(i -> {
            ProgrammeDesignAssignment assignment = i.getArgument(0);
            assignment.setId(UUID.randomUUID());
            return assignment;
        });

        ProgrammeDesignAssignmentResponse response = programmeDesignService.assignDesign(programmeId, request);

        assertThat(response).isNotNull();
        assertThat(response.getProgrammeId()).isEqualTo(programmeId);
        assertThat(response.getDesignId()).isEqualTo(designId);
        verify(assignmentRepository).save(any(ProgrammeDesignAssignment.class));
    }

    @Test
    @DisplayName("Assign design fails if programme is disabled")
    void testAssignDesignFailsWhenProgrammeDisabled() {
        programme.setStatus(ProgrammeStatus.DISABLED);
        when(programmeService.findProgrammeOrThrow(programmeId)).thenReturn(programme);

        AssignDesignRequest request = new AssignDesignRequest(designId, Instant.now());

        assertThatThrownBy(() -> programmeDesignService.assignDesign(programmeId, request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot assign design to a disabled programme");
    }

    @Test
    @DisplayName("Assign design fails if design is disabled")
    void testAssignDesignFailsWhenDesignDisabled() {
        design.setStatus(DesignStatus.DISABLED);
        when(programmeService.findProgrammeOrThrow(programmeId)).thenReturn(programme);
        when(designService.findDesignOrThrow(designId)).thenReturn(design);

        AssignDesignRequest request = new AssignDesignRequest(designId, Instant.now());

        assertThatThrownBy(() -> programmeDesignService.assignDesign(programmeId, request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot assign a disabled design");
    }

    @Test
    @DisplayName("Get design assignment at specific point in time returns active design")
    void testGetDesignAssignmentAtTimestamp() {
        Instant timestamp = Instant.parse("2026-03-05T10:00:00Z");
        ProgrammeDesignAssignment assignment = new ProgrammeDesignAssignment(
                UUID.randomUUID(), programme, design, Instant.parse("2026-03-01T00:00:00Z"));

        when(programmeService.findProgrammeOrThrow(programmeId)).thenReturn(programme);
        when(assignmentRepository.findFirstByProgrammeIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                eq(programmeId), eq(timestamp))).thenReturn(Optional.of(assignment));

        ProgrammeDesignAssignmentResponse response = programmeDesignService.getDesignAssignmentAt(programmeId, timestamp);

        assertThat(response).isNotNull();
        assertThat(response.getDesignName()).isEqualTo("Gold Border v1");
    }

    @Test
    @DisplayName("Get design assignment at timestamp throws ResourceNotFoundException if no assignment existed")
    void testGetDesignAssignmentAtTimestampNotFound() {
        Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
        when(programmeService.findProgrammeOrThrow(programmeId)).thenReturn(programme);
        when(assignmentRepository.findFirstByProgrammeIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                eq(programmeId), eq(timestamp))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> programmeDesignService.getDesignAssignmentAt(programmeId, timestamp))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No design assignment found for programme");
    }

    @Test
    @DisplayName("Get full assignment history for programme")
    void testGetAssignmentHistory() {
        ProgrammeDesignAssignment a1 = new ProgrammeDesignAssignment(
                UUID.randomUUID(), programme, design, Instant.parse("2026-03-01T00:00:00Z"));
        when(programmeService.findProgrammeOrThrow(programmeId)).thenReturn(programme);
        when(assignmentRepository.findByProgrammeIdOrderByEffectiveFromDesc(programmeId)).thenReturn(List.of(a1));

        List<ProgrammeDesignAssignmentResponse> history = programmeDesignService.getAssignmentHistory(programmeId);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getDesignName()).isEqualTo("Gold Border v1");
    }
}
