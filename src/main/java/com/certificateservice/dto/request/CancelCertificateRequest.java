package com.certificateservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CancelCertificateRequest {

    @NotBlank(message = "Cancellation reason must not be blank")
    @Size(max = 1000, message = "Cancellation reason must not exceed 1000 characters")
    private String reason;

    public CancelCertificateRequest() {
    }

    public CancelCertificateRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
