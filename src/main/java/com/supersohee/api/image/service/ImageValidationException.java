package com.supersohee.api.image.service;

public class ImageValidationException extends IllegalArgumentException {
    public enum Reason {
        MISSING_FILE,
        TOO_LARGE,
        INVALID_FILENAME,
        INVALID_CONTENT,
        UNSUPPORTED_FORMAT
    }

    private final Reason reason;

    public ImageValidationException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
