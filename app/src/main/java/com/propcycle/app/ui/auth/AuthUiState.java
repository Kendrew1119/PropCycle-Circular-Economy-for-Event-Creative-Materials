package com.propcycle.app.ui.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Immutable render state shared by the email/password authentication screens. */
public final class AuthUiState {

    public enum Status {
        IDLE,
        LOADING,
        ERROR,
        CONFIGURATION_REQUIRED,
        SUCCESS
    }

    private final Status status;
    private final String message;

    private AuthUiState(@NonNull Status status, @Nullable String message) {
        this.status = status;
        this.message = message;
    }

    public static AuthUiState idle() {
        return new AuthUiState(Status.IDLE, null);
    }

    public static AuthUiState loading(@NonNull String message) {
        return new AuthUiState(Status.LOADING, message);
    }

    public static AuthUiState error(@NonNull String message) {
        return new AuthUiState(Status.ERROR, message);
    }

    public static AuthUiState configurationRequired(@NonNull String message) {
        return new AuthUiState(Status.CONFIGURATION_REQUIRED, message);
    }

    public static AuthUiState success() {
        return new AuthUiState(Status.SUCCESS, null);
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    @Nullable
    public String getMessage() {
        return message;
    }
}
