package com.certificateservice.service;

import com.certificateservice.dto.request.CreateDesignRequest;
import com.certificateservice.dto.response.DesignResponse;
import com.certificateservice.exception.ResourceNotFoundException;
import com.certificateservice.model.Design;
import com.certificateservice.model.DesignStatus;
import com.certificateservice.repository.DesignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class DesignService {

    private final DesignRepository designRepository;

    public DesignService(DesignRepository designRepository) {
        this.designRepository = designRepository;
    }

    public DesignResponse createDesign(CreateDesignRequest request) {
        Design design = new Design(
                null,
                request.getName().trim(),
                request.getContent(),
                DesignStatus.ACTIVE
        );
        Design saved = designRepository.save(design);
        return DesignResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<DesignResponse> listDesigns(DesignStatus status) {
        List<Design> designs = (status != null)
                ? designRepository.findByStatusOrderByCreatedAtDesc(status)
                : designRepository.findAllByOrderByCreatedAtDesc();

        return designs.stream()
                .map(DesignResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DesignResponse getDesignById(UUID id) {
        Design design = findDesignOrThrow(id);
        return DesignResponse.fromEntity(design);
    }

    public DesignResponse disableDesign(UUID id) {
        Design design = findDesignOrThrow(id);
        design.setStatus(DesignStatus.DISABLED);
        Design updated = designRepository.save(design);
        return DesignResponse.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public Design findDesignOrThrow(UUID id) {
        return designRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Design not found with ID: " + id));
    }
}
