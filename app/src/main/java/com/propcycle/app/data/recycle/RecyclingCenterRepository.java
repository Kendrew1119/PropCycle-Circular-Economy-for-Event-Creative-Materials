package com.propcycle.app.data.recycle;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.CircularBounds;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchByTextRequest;
import com.propcycle.app.core.maps.MapsEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Executes one bounded Places Text Search at a time. */
public final class RecyclingCenterRepository {

    public interface Callback {
        void onSuccess(@NonNull List<RecyclingCenter> values);

        void onFailure(@NonNull Exception error);
    }

    private static final List<Place.Field> PLACE_FIELDS = Arrays.asList(
            Place.Field.ID,
            Place.Field.DISPLAY_NAME,
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.LOCATION,
            Place.Field.RATING);

    private final PlacesClient placesClient;
    @Nullable
    private CancellationTokenSource activeSearch;

    public RecyclingCenterRepository(@NonNull Context context) {
        Context applicationContext = context.getApplicationContext();
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(
                    applicationContext,
                    MapsEnvironment.apiKey(),
                    Locale.getDefault());
        }
        placesClient = Places.createClient(applicationContext);
    }

    public void search(
            @Nullable String area,
            @Nullable GeoPoint locationBias,
            @NonNull Callback callback) {
        cancel();
        CancellationTokenSource cancellation = new CancellationTokenSource();
        activeSearch = cancellation;

        String query = locationBias == null
                ? RecyclingCenterPolicy.buildManualQuery(area)
                : "recycling centre";
        SearchByTextRequest.Builder builder = SearchByTextRequest.builder(query, PLACE_FIELDS)
                .setMaxResultCount(RecyclingCenterPolicy.MAX_RESULTS)
                .setRegionCode("MY")
                .setCancellationToken(cancellation.getToken());

        if (locationBias != null) {
            builder.setLocationBias(CircularBounds.newInstance(
                    new LatLng(
                            locationBias.getLatitude(),
                            locationBias.getLongitude()),
                    RecyclingCenterPolicy.SEARCH_RADIUS_METRES));
            builder.setRankPreference(SearchByTextRequest.RankPreference.DISTANCE);
        }

        placesClient.searchByText(builder.build())
                .addOnSuccessListener(response -> {
                    if (cancellation.getToken().isCancellationRequested()) {
                        return;
                    }
                    callback.onSuccess(RecyclingCenterPolicy.prepareResults(
                            toCenters(response.getPlaces()),
                            locationBias));
                })
                .addOnFailureListener(error -> {
                    if (!cancellation.getToken().isCancellationRequested()) {
                        callback.onFailure(error);
                    }
                });
    }

    public void cancel() {
        if (activeSearch != null) {
            activeSearch.cancel();
            activeSearch = null;
        }
    }

    @NonNull
    private static List<RecyclingCenter> toCenters(@NonNull List<Place> places) {
        List<RecyclingCenter> values = new ArrayList<>();
        for (Place place : places) {
            if (place == null || place.getLocation() == null) {
                continue;
            }
            String id = clean(place.getId());
            String name = clean(place.getDisplayName());
            if (id.isEmpty() || name.isEmpty()) {
                continue;
            }
            String address = clean(place.getFormattedAddress());
            LatLng location = place.getLocation();
            values.add(new RecyclingCenter(
                    id,
                    name,
                    address.isEmpty() ? "Address unavailable" : address,
                    new GeoPoint(location.latitude, location.longitude),
                    place.getRating(),
                    null));
        }
        return values;
    }

    @NonNull
    private static String clean(@Nullable String value) {
        return value == null ? "" : value.trim();
    }
}
