package com.propcycle.app.data.recycle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Minimal Places result shown by the Recycling Centre screen. */
public final class RecyclingCenter {

    private final String id;
    private final String name;
    private final String address;
    private final GeoPoint location;
    @Nullable
    private final Double rating;
    @Nullable
    private final Double distanceKm;

    public RecyclingCenter(
            @NonNull String id,
            @NonNull String name,
            @NonNull String address,
            @NonNull GeoPoint location,
            @Nullable Double rating,
            @Nullable Double distanceKm) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.location = location;
        this.rating = RecyclingCenterPolicy.validRatingOrNull(rating);
        this.distanceKm = distanceKm != null && Double.isFinite(distanceKm) && distanceKm >= 0d
                ? distanceKm
                : null;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getAddress() {
        return address;
    }

    @NonNull
    public GeoPoint getLocation() {
        return location;
    }

    @Nullable
    public Double getRating() {
        return rating;
    }

    @Nullable
    public Double getDistanceKm() {
        return distanceKm;
    }

    @NonNull
    public RecyclingCenter withDistance(double value) {
        return new RecyclingCenter(id, name, address, location, rating, value);
    }
}
