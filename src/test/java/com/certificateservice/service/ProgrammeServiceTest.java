package com.certificateservice.service;

import com.certificateservice.dto.request.CreateProgrammeRequest;
import com.certificateservice.dto.request.RenameProgrammeRequest;
import com.certificateservice.dto.response.ProgrammeResponse;
import com.certificateservice.exception.ResourceNotFoundException;
import com.certificateservice.model.Programme;
import com.certificateservice.model.ProgrammeStatus;
import com.certificateservice.repository.ProgrammeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgrammeServiceTest {

    @Mock
    private ProgrammeRepository programmeRepository;

    @InjectMocks
    private ProgrammeService programmeService;

    private Programme activeProgramme;
    private UUID programmeId;

    @BeforeEach
    void setUp() {
        programmeId = UUID.randomUUID();
        activeProgramme = new Programme(programmeId, "Advanced SQL", "SQL Course", ProgrammeStatus.ACTIVE);
    }

    @Test
    @DisplayName("Create programme creates active programme")
    void testCreateProgramme() {
        CreateProgrammeRequest request = new CreateProgrammeRequest("Advanced SQL", "SQL Course");
        when(programmeRepository.save(any(Programme.class))).thenReturn(activeProgramme);

        ProgrammeResponse response = programmeService.createProgramme(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Advanced SQL");
        assertThat(response.getStatus()).isEqualTo(ProgrammeStatus.ACTIVE);
        verify(programmeRepository).save(any(Programme.class));
    }

    @Test
    @DisplayName("List programmes returns all programmes")
    void testListProgrammes() {
        when(programmeRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(activeProgramme));

        List<ProgrammeResponse> result = programmeService.listProgrammes(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Advanced SQL");
    }

    @Test
    @DisplayName("List programmes with status filter")
    void testListProgrammesFiltered() {
        when(programmeRepository.findByStatusOrderByCreatedAtDesc(ProgrammeStatus.ACTIVE)).thenReturn(List.of(activeProgramme));

        List<ProgrammeResponse> result = programmeService.listProgrammes(ProgrammeStatus.ACTIVE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(ProgrammeStatus.ACTIVE);
    }

    @Test
    @DisplayName("Rename programme updates name")
    void testRenameProgramme() {
        when(programmeRepository.findById(programmeId)).thenReturn(Optional.of(activeProgramme));
        when(programmeRepository.save(any(Programme.class))).thenAnswer(i -> i.getArgument(0));

        RenameProgrammeRequest request = new RenameProgrammeRequest("Mastering SQL & Optimization");
        ProgrammeResponse response = programmeService.renameProgramme(programmeId, request);

        assertThat(response.getName()).isEqualTo("Mastering SQL & Optimization");
        assertThat(activeProgramme.getName()).isEqualTo("Mastering SQL & Optimization");
    }

    @Test
    @DisplayName("Disable programme changes status to DISABLED")
    void testDisableProgramme() {
        when(programmeRepository.findById(programmeId)).thenReturn(Optional.of(activeProgramme));
        when(programmeRepository.save(any(Programme.class))).thenAnswer(i -> i.getArgument(0));

        ProgrammeResponse response = programmeService.disableProgramme(programmeId);

        assertThat(response.getStatus()).isEqualTo(ProgrammeStatus.DISABLED);
        assertThat(activeProgramme.getStatus()).isEqualTo(ProgrammeStatus.DISABLED);
    }

    @Test
    @DisplayName("Find unknown programme throws ResourceNotFoundException")
    void testFindUnknownProgramme() {
        UUID unknownId = UUID.randomUUID();
        when(programmeRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> programmeService.getProgrammeById(unknownId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Programme not found with ID: " + unknownId);
    }
}
