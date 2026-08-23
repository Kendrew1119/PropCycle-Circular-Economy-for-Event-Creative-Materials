package com.propcycle.app.data.recycle;

/** In-memory latitude/longitude value. Phase 2D never persists this value. */
public final class GeoPoint {

    private final double latitude;
    private final double longitude;

    public GeoPoint(double latitude, double longitude) {
        if (latitude < -90d || latitude > 90d) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (longitude < -180d || longitude > 180d) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
