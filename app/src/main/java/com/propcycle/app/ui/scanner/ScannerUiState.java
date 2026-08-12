package com.propcycle.app.ui.scanner;

import androidx.annotation.NonNull;

/** Immutable rendering state for the scanner screen. */
public final class ScannerUiState {

    public enum Kind {
        IDLE,
        CAMERA_STARTING,
        CAMERA_READY,
        CAPTURING,
        PROCESSING_IMAGE,
        IMAGE_READY,
        ANALYZING,
        ERROR,
        CONFIGURATION_REQUIRED,
        AUTHENTICATION_REQUIRED
    }

    private final Kind kind;
    private final String message;
    private final boolean hasImage;

    private ScannerUiState(
            @NonNull Kind kind,
            @NonNull String message,
            boolean hasImage) {
        this.kind = kind;
        this.message = message;
        this.hasImage = hasImage;
    }

    @NonNull
    static ScannerUiState of(
            @NonNull Kind kind,
            @NonNull String message,
            boolean hasImage) {
        return new ScannerUiState(kind, message, hasImage);
    }

    @NonNull
    static ScannerUiState idle() {
        return of(Kind.IDLE, "Take a photo or choose one from your device.", false);
    }

    @NonNull
    public Kind getKind() {
        return kind;
    }

    @NonNull
    public String getMessage() {
        return message;
    }

    public boolean hasImage() {
        return hasImage;
    }

    public boolean isBusy() {
        return kind == Kind.CAPTURING
                || kind == Kind.PROCESSING_IMAGE
                || kind == Kind.ANALYZING;
    }
}
