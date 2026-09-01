package com.certificateservice.exception;

public class DuplicateCertificateException extends RuntimeException {

    public DuplicateCertificateException(String message) {
        super(message);
    }

    public DuplicateCertificateException(String message, Throwable cause) {
        super(message, cause);
    }
}
