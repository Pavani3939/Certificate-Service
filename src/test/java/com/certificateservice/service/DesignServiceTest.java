package com.certificateservice.service;

import com.certificateservice.dto.request.CreateDesignRequest;
import com.certificateservice.dto.response.DesignResponse;
import com.certificateservice.exception.ResourceNotFoundException;
import com.certificateservice.model.Design;
import com.certificateservice.model.DesignStatus;
import com.certificateservice.repository.DesignRepository;
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
class DesignServiceTest {

    @Mock
    private DesignRepository designRepository;

    @InjectMocks
    private DesignService designService;

    private Design activeDesign;
    private UUID designId;

    @BeforeEach
    void setUp() {
        designId = UUID.randomUUID();
        activeDesign = new Design(designId, "Gold Border v1", "<template>Gold</template>", DesignStatus.ACTIVE);
    }

    @Test
    @DisplayName("Create design successfully returns active design response")
    void testCreateDesign() {
        CreateDesignRequest request = new CreateDesignRequest("Gold Border v1", "<template>Gold</template>");
        when(designRepository.save(any(Design.class))).thenReturn(activeDesign);

        DesignResponse response = designService.createDesign(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Gold Border v1");
        assertThat(response.getContent()).isEqualTo("<template>Gold</template>");
        assertThat(response.getStatus()).isEqualTo(DesignStatus.ACTIVE);
        verify(designRepository).save(any(Design.class));
    }

    @Test
    @DisplayName("List all designs without filter")
    void testListAllDesigns() {
        when(designRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(activeDesign));

        List<DesignResponse> result = designService.listDesigns(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Gold Border v1");
    }

    @Test
    @DisplayName("List designs filtered by status")
    void testListDesignsWithFilter() {
        when(designRepository.findByStatusOrderByCreatedAtDesc(DesignStatus.ACTIVE)).thenReturn(List.of(activeDesign));

        List<DesignResponse> result = designService.listDesigns(DesignStatus.ACTIVE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(DesignStatus.ACTIVE);
    }

    @Test
    @DisplayName("Get design by ID returns design when found")
    void testGetDesignByIdFound() {
        when(designRepository.findById(designId)).thenReturn(Optional.of(activeDesign));

        DesignResponse response = designService.getDesignById(designId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(designId);
    }

    @Test
    @DisplayName("Get design by ID throws ResourceNotFoundException when not found")
    void testGetDesignByIdNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(designRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> designService.getDesignById(unknownId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Design not found with ID: " + unknownId);
    }

    @Test
    @DisplayName("Disable design updates status to DISABLED")
    void testDisableDesign() {
        when(designRepository.findById(designId)).thenReturn(Optional.of(activeDesign));
        when(designRepository.save(any(Design.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DesignResponse response = designService.disableDesign(designId);

        assertThat(response.getStatus()).isEqualTo(DesignStatus.DISABLED);
        assertThat(activeDesign.getStatus()).isEqualTo(DesignStatus.DISABLED);
        verify(designRepository).save(activeDesign);
    }
}
