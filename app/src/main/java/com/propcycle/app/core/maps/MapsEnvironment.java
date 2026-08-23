package com.propcycle.app.core.maps;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.propcycle.app.BuildConfig;

/** Local Maps configuration and Google Play services checks. */
public final class MapsEnvironment {

    private static final String SETUP_REQUIRED = "SETUP_REQUIRED";

    private MapsEnvironment() {
    }

    public static boolean hasApiKey() {
        String key = BuildConfig.MAPS_API_KEY;
        return key != null && !key.trim().isEmpty() && !SETUP_REQUIRED.equals(key.trim());
    }

    public static boolean hasGooglePlayServices(@NonNull Context context) {
        return GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
    }

    @NonNull
    public static String apiKey() {
        return BuildConfig.MAPS_API_KEY == null ? "" : BuildConfig.MAPS_API_KEY.trim();
    }
}
