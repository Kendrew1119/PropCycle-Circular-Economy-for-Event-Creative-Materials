package com.propcycle.app.ui.recycle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.propcycle.app.data.recycle.GeoPoint;
import com.propcycle.app.data.recycle.RecyclingCenter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable Recycling Centre screen state. */
public final class RecycleCenterUiState {

    public enum Kind {
        READY,
        SETUP_REQUIRED,
        PLAY_SERVICES_UNAVAILABLE,
        PERMISSION_DENIED,
        LOCATING,
        SEARCHING,
        CONTENT,
        EMPTY,
        ERROR
    }

    private final Kind kind;
    private final String message;
    private final List<RecyclingCenter> centers;
    @Nullable
    private final GeoPoint userLocation;

    private RecycleCenterUiState(
            @NonNull Kind kind,
            @NonNull String message,
            @NonNull List<RecyclingCenter> centers,
            @Nullable GeoPoint userLocation) {
        this.kind = kind;
        this.message = message;
        this.centers = Collections.unmodifiableList(new ArrayList<>(centers));
        this.userLocation = userLocation;
    }

    @NonNull
    public static RecycleCenterUiState message(@NonNull Kind kind, @NonNull String message) {
        return new RecycleCenterUiState(kind, message, Collections.emptyList(), null);
    }

    @NonNull
    public static RecycleCenterUiState content(
            @NonNull List<RecyclingCenter> centers,
            @Nullable GeoPoint userLocation) {
        return new RecycleCenterUiState(
                Kind.CONTENT,
                "Nearby recycling centres",
                centers,
                userLocation);
    }

    @NonNull
    public static RecycleCenterUiState empty(@Nullable GeoPoint userLocation) {
        return new RecycleCenterUiState(
                Kind.EMPTY,
                "No recycling centres were found. Try a wider area or a nearby city.",
                Collections.emptyList(),
                userLocation);
    }

    @NonNull
    public Kind getKind() {
        return kind;
    }

    @NonNull
    public String getMessage() {
        return message;
    }

    @NonNull
    public List<RecyclingCenter> getCenters() {
        return centers;
    }

    @Nullable
    public GeoPoint getUserLocation() {
        return userLocation;
    }
}
