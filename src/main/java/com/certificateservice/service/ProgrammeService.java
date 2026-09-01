package com.certificateservice.service;

import com.certificateservice.dto.request.CreateProgrammeRequest;
import com.certificateservice.dto.request.RenameProgrammeRequest;
import com.certificateservice.dto.response.ProgrammeResponse;
import com.certificateservice.exception.ResourceNotFoundException;
import com.certificateservice.model.Programme;
import com.certificateservice.model.ProgrammeStatus;
import com.certificateservice.repository.ProgrammeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProgrammeService {

    private final ProgrammeRepository programmeRepository;

    public ProgrammeService(ProgrammeRepository programmeRepository) {
        this.programmeRepository = programmeRepository;
    }

    public ProgrammeResponse createProgramme(CreateProgrammeRequest request) {
        Programme programme = new Programme(
                null,
                request.getName().trim(),
                request.getDescription() != null ? request.getDescription().trim() : null,
                ProgrammeStatus.ACTIVE
        );
        Programme saved = programmeRepository.save(programme);
        return ProgrammeResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ProgrammeResponse> listProgrammes(ProgrammeStatus status) {
        List<Programme> programmes = (status != null)
                ? programmeRepository.findByStatusOrderByCreatedAtDesc(status)
                : programmeRepository.findAllByOrderByCreatedAtDesc();

        return programmes.stream()
                .map(ProgrammeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProgrammeResponse getProgrammeById(UUID id) {
        Programme programme = findProgrammeOrThrow(id);
        return ProgrammeResponse.fromEntity(programme);
    }

    public ProgrammeResponse renameProgramme(UUID id, RenameProgrammeRequest request) {
        Programme programme = findProgrammeOrThrow(id);
        programme.setName(request.getName().trim());
        Programme updated = programmeRepository.save(programme);
        return ProgrammeResponse.fromEntity(updated);
    }

    public ProgrammeResponse disableProgramme(UUID id) {
        Programme programme = findProgrammeOrThrow(id);
        programme.setStatus(ProgrammeStatus.DISABLED);
        Programme updated = programmeRepository.save(programme);
        return ProgrammeResponse.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public Programme findProgrammeOrThrow(UUID id) {
        return programmeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Programme not found with ID: " + id));
    }
}
