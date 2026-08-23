package com.propcycle.app.data.recycle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Pure validation, distance, ordering, and display rules for Phase 2D. */
public final class RecyclingCenterPolicy {

    public static final int MAX_RESULTS = 10;
    public static final double SEARCH_RADIUS_METRES = 25_000d;
    public static final int MAX_AREA_LENGTH = 80;
    private static final double EARTH_RADIUS_KM = 6_371.0088d;

    private RecyclingCenterPolicy() {
    }

    @NonNull
    public static String normalizeArea(@Nullable String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    public static boolean isValidArea(@Nullable String value) {
        int length = normalizeArea(value).length();
        return length >= 2 && length <= MAX_AREA_LENGTH;
    }

    @NonNull
    public static String buildManualQuery(@Nullable String area) {
        String normalized = normalizeArea(area);
        if (!isValidArea(normalized)) {
            throw new IllegalArgumentException("Area must contain 2 to 80 characters");
        }
        String suffix = normalized.toLowerCase(Locale.ROOT).contains("malaysia")
                ? ""
                : ", Malaysia";
        return "recycling centre near " + normalized + suffix;
    }

    @Nullable
    static Double validRatingOrNull(@Nullable Double rating) {
        if (rating == null || !Double.isFinite(rating) || rating < 0d || rating > 5d) {
            return null;
        }
        return rating;
    }

    public static double distanceKm(@NonNull GeoPoint from, @NonNull GeoPoint to) {
        double lat1 = Math.toRadians(from.getLatitude());
        double lat2 = Math.toRadians(to.getLatitude());
        double deltaLat = lat2 - lat1;
        double deltaLon = Math.toRadians(to.getLongitude() - from.getLongitude());
        double sinLat = Math.sin(deltaLat / 2d);
        double sinLon = Math.sin(deltaLon / 2d);
        double a = sinLat * sinLat
                + Math.cos(lat1) * Math.cos(lat2) * sinLon * sinLon;
        double bounded = Math.min(1d, Math.max(0d, a));
        return EARTH_RADIUS_KM * 2d * Math.atan2(Math.sqrt(bounded), Math.sqrt(1d - bounded));
    }

    @NonNull
    public static List<RecyclingCenter> prepareResults(
            @NonNull List<RecyclingCenter> values,
            @Nullable GeoPoint origin) {
        List<RecyclingCenter> prepared = new ArrayList<>();
        for (RecyclingCenter value : values) {
            if (value == null) {
                continue;
            }
            prepared.add(origin == null
                    ? value
                    : value.withDistance(distanceKm(origin, value.getLocation())));
        }
        if (origin != null) {
            prepared.sort(Comparator.comparingDouble(value -> value.getDistanceKm() == null
                    ? Double.MAX_VALUE
                    : value.getDistanceKm()));
        }
        if (prepared.size() > MAX_RESULTS) {
            return new ArrayList<>(prepared.subList(0, MAX_RESULTS));
        }
        return prepared;
    }

    @NonNull
    public static String formatDistance(@Nullable Double distanceKm) {
        if (distanceKm == null || !Double.isFinite(distanceKm) || distanceKm < 0d) {
            return "Distance unavailable";
        }
        if (distanceKm < 1d) {
            return String.format(Locale.getDefault(), "Approx. %.0f m", distanceKm * 1_000d);
        }
        return String.format(Locale.getDefault(), "Approx. %.1f km", distanceKm);
    }

    @NonNull
    public static String formatRating(@Nullable Double rating) {
        Double valid = validRatingOrNull(rating);
        return valid == null
                ? "No rating"
                : String.format(Locale.getDefault(), "%.1f ★", valid);
    }
}
