package com.propcycle.app.ui.recycle;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.libraries.places.api.net.PlacesStatusCodes;
import com.propcycle.app.core.maps.MapsEnvironment;
import com.propcycle.app.data.recycle.GeoPoint;
import com.propcycle.app.data.recycle.RecyclingCenter;
import com.propcycle.app.data.recycle.RecyclingCenterRepository;
import com.propcycle.app.data.activity.ActivityLogRepository;

import java.util.List;

/** Owns one-time location and bounded Places search state. */
public final class RecycleCenterViewModel extends AndroidViewModel {

    private final MutableLiveData<RecycleCenterUiState> state = new MutableLiveData<>();
    private final FusedLocationProviderClient locationClient;
    private final ActivityLogRepository activityLog;
    private RecyclingCenterRepository repository;
    private CancellationTokenSource locationCancellation;
    private int operationGeneration;
    private boolean inAppSearchAvailable;

    public RecycleCenterViewModel(@NonNull Application application) {
        super(application);
        locationClient = LocationServices.getFusedLocationProviderClient(application);
        activityLog = new ActivityLogRepository(application);
        initialise(application);
    }

    @NonNull
    public LiveData<RecycleCenterUiState> getState() {
        return state;
    }

    public boolean isInAppSearchAvailable() {
        return inAppSearchAvailable;
    }

    public void searchArea(@NonNull String area) {
        if (!inAppSearchAvailable) {
            return;
        }
        int generation = beginOperation();
        if (!hasNetwork()) {
            state.setValue(RecycleCenterUiState.message(
                    RecycleCenterUiState.Kind.ERROR,
                    "You appear to be offline. Connect to the internet, then try again."));
            return;
        }
        state.setValue(RecycleCenterUiState.message(
                RecycleCenterUiState.Kind.SEARCHING,
                "Searching recycling centres near " + area + "…"));
        repository.search(area, null, callbackFor(
                generation, null, "Recycling centres searched near " + area.trim()));
    }

    @SuppressLint("MissingPermission")
    public void searchUsingCurrentLocation() {
        if (!inAppSearchAvailable) {
            return;
        }
        int generation = beginOperation();
        if (!hasNetwork()) {
            state.setValue(RecycleCenterUiState.message(
                    RecycleCenterUiState.Kind.ERROR,
                    "You appear to be offline. Connect to the internet, then try again."));
            return;
        }
        state.setValue(RecycleCenterUiState.message(
                RecycleCenterUiState.Kind.LOCATING,
                "Getting your current approximate location…"));
        CancellationTokenSource cancellation = new CancellationTokenSource();
        locationCancellation = cancellation;
        locationClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cancellation.getToken())
                .addOnSuccessListener(location -> {
                    if (!isCurrent(generation) || cancellation.getToken().isCancellationRequested()) {
                        return;
                    }
                    if (location == null) {
                        state.setValue(RecycleCenterUiState.message(
                                RecycleCenterUiState.Kind.ERROR,
                                "Your current location is unavailable. Search by city or area instead."));
                        return;
                    }
                    GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
                    state.setValue(RecycleCenterUiState.message(
                            RecycleCenterUiState.Kind.SEARCHING,
                            "Searching within about 25 km of your location…"));
                    repository.search(null, point, callbackFor(
                            generation, point, "Recycling centres searched near current area"));
                })
                .addOnFailureListener(error -> {
                    if (isCurrent(generation)
                            && !cancellation.getToken().isCancellationRequested()) {
                        state.setValue(RecycleCenterUiState.message(
                                RecycleCenterUiState.Kind.ERROR,
                                "Your current location is unavailable. Search by city or area instead."));
                    }
                });
    }

    public void showPermissionDenied() {
        beginOperation();
        state.setValue(RecycleCenterUiState.message(
                RecycleCenterUiState.Kind.PERMISSION_DENIED,
                "Location permission was not allowed. You can still search by city or area."));
    }

    private void initialise(@NonNull Context context) {
        if (!MapsEnvironment.hasApiKey()) {
            inAppSearchAvailable = false;
            state.setValue(RecycleCenterUiState.message(
                    RecycleCenterUiState.Kind.SETUP_REQUIRED,
                    "Maps setup is required for the in-app map. Add your restricted key to "
                            + "secrets.properties. You can still search in an installed map app."));
            return;
        }
        if (!MapsEnvironment.hasGooglePlayServices(context)) {
            inAppSearchAvailable = false;
            state.setValue(RecycleCenterUiState.message(
                    RecycleCenterUiState.Kind.PLAY_SERVICES_UNAVAILABLE,
                    "Google Play services is unavailable on this device. You can still search "
                            + "in an installed map app."));
            return;
        }
        try {
            repository = new RecyclingCenterRepository(context);
            inAppSearchAvailable = true;
            state.setValue(RecycleCenterUiState.message(
                    RecycleCenterUiState.Kind.READY,
                    "Search by city or area, or use your current approximate location."));
        } catch (RuntimeException error) {
            inAppSearchAvailable = false;
            state.setValue(RecycleCenterUiState.message(
                    RecycleCenterUiState.Kind.SETUP_REQUIRED,
                    "Maps could not start. Check the API key, enabled APIs, billing, and key restrictions."));
        }
    }

    private int beginOperation() {
        operationGeneration++;
        if (repository != null) {
            repository.cancel();
        }
        if (locationCancellation != null) {
            locationCancellation.cancel();
            locationCancellation = null;
        }
        return operationGeneration;
    }

    @NonNull
    private RecyclingCenterRepository.Callback callbackFor(
            int generation,
            GeoPoint origin,
            @NonNull String activityTitle) {
        return new RecyclingCenterRepository.Callback() {
            @Override
            public void onSuccess(@NonNull List<RecyclingCenter> values) {
                if (!isCurrent(generation)) {
                    return;
                }
                activityLog.record(
                        ActivityLogRepository.TYPE_RECYCLE_SEARCH,
                        activityTitle,
                        values.isEmpty()
                                ? "No matching centre was found."
                                : values.size() + " result(s) found. Confirm accepted materials before travelling.",
                        ActivityLogRepository.DESTINATION_RECYCLE,
                        "");
                state.setValue(values.isEmpty()
                        ? RecycleCenterUiState.empty(origin)
                        : RecycleCenterUiState.content(values, origin));
            }

            @Override
            public void onFailure(@NonNull Exception error) {
                if (isCurrent(generation)) {
                    state.setValue(RecycleCenterUiState.message(
                            RecycleCenterUiState.Kind.ERROR,
                            userMessage(error)));
                }
            }
        };
    }

    private boolean isCurrent(int generation) {
        return generation == operationGeneration;
    }

    private boolean hasNetwork() {
        ConnectivityManager manager = (ConnectivityManager) getApplication()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return true;
        }
        Network network = manager.getActiveNetwork();
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    @NonNull
    private static String userMessage(@NonNull Exception error) {
        if (error instanceof ApiException apiError) {
            int code = apiError.getStatusCode();
            if (code == PlacesStatusCodes.OVER_QUERY_LIMIT) {
                return "The Places search limit was reached. Check quota and billing, then retry.";
            }
            if (code == PlacesStatusCodes.REQUEST_DENIED) {
                return "Places denied the search. Check the key, package/SHA-1 restrictions, "
                        + "enabled APIs, and billing.";
            }
            if (code == PlacesStatusCodes.INVALID_REQUEST) {
                return "Places could not understand this search. Try a nearby city or area.";
            }
            if (code == CommonStatusCodes.NETWORK_ERROR) {
                return "The search could not reach Places. Check the internet connection and retry.";
            }
        }
        return "The recycling-centre search failed. Check the internet, API setup, quota, and key restrictions, then retry.";
    }

    @Override
    protected void onCleared() {
        beginOperation();
    }
}
