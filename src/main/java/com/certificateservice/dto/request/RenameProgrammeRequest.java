package com.certificateservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RenameProgrammeRequest {

    @NotBlank(message = "Programme name must not be blank")
    @Size(max = 255, message = "Programme name must not exceed 255 characters")
    private String name;

    public RenameProgrammeRequest() {
    }

    public RenameProgrammeRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
