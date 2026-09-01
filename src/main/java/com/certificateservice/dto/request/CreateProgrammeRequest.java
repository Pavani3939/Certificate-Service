package com.certificateservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateProgrammeRequest {

    @NotBlank(message = "Programme name must not be blank")
    @Size(max = 255, message = "Programme name must not exceed 255 characters")
    private String name;

    private String description;

    public CreateProgrammeRequest() {
    }

    public CreateProgrammeRequest(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
