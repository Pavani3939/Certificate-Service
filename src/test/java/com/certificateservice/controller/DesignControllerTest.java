package com.certificateservice.controller;

import com.certificateservice.dto.request.CreateDesignRequest;
import com.certificateservice.dto.response.DesignResponse;
import com.certificateservice.exception.GlobalExceptionHandler;
import com.certificateservice.exception.ResourceNotFoundException;
import com.certificateservice.model.DesignStatus;
import com.certificateservice.service.DesignService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DesignController.class)
@Import(GlobalExceptionHandler.class)
class DesignControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DesignService designService;

    @Test
    @DisplayName("POST /api/designs creates design and returns 201 Created")
    void testCreateDesignSuccess() throws Exception {
        CreateDesignRequest request = new CreateDesignRequest("Gold Border v1", "<template>Gold</template>");
        UUID id = UUID.randomUUID();
        DesignResponse response = new DesignResponse(id, "Gold Border v1", "<template>Gold</template>",
                DesignStatus.ACTIVE, Instant.now(), Instant.now());

        when(designService.createDesign(any(CreateDesignRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/designs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Gold Border v1"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/designs with blank name returns 400 Bad Request")
    void testCreateDesignValidationFailure() throws Exception {
        CreateDesignRequest request = new CreateDesignRequest("", "<template></template>");

        mockMvc.perform(post("/api/designs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @DisplayName("GET /api/designs returns list of designs with 200 OK")
    void testListDesigns() throws Exception {
        UUID id = UUID.randomUUID();
        DesignResponse response = new DesignResponse(id, "Gold Border v1", "<template>Gold</template>",
                DesignStatus.ACTIVE, Instant.now(), Instant.now());

        when(designService.listDesigns(null)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/designs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Gold Border v1"));
    }

    @Test
    @DisplayName("POST /api/designs/{id}/disable returns 200 OK and disabled design")
    void testDisableDesign() throws Exception {
        UUID id = UUID.randomUUID();
        DesignResponse response = new DesignResponse(id, "Gold Border v1", "<template>Gold</template>",
                DesignStatus.DISABLED, Instant.now(), Instant.now());

        when(designService.disableDesign(eq(id))).thenReturn(response);

        mockMvc.perform(post("/api/designs/{id}/disable", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));
    }

    @Test
    @DisplayName("GET /api/designs/{id} with unknown ID returns 404 Not Found")
    void testGetDesignNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(designService.getDesignById(id)).thenThrow(new ResourceNotFoundException("Design not found with ID: " + id));

        mockMvc.perform(get("/api/designs/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Design not found with ID: " + id));
    }
}
