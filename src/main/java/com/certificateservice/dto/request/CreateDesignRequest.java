package com.certificateservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateDesignRequest {

    @NotBlank(message = "Design name must not be blank")
    @Size(max = 255, message = "Design name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Design content must not be blank")
    private String content;

    public CreateDesignRequest() {
    }

    public CreateDesignRequest(String name, String content) {
        this.name = name;
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
