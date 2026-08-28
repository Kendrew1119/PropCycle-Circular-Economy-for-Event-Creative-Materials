package com.propcycle.app.ui.lending;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.propcycle.app.data.lending.FirestoreLendingRepository;
import com.propcycle.app.data.lending.LendingItem;
import com.propcycle.app.data.lending.LendingPolicy;

import java.util.Collections;
import java.util.List;

/** Real-time lending map/list state; distance sorting stays local to the device. */
public final class LendingMapViewModel extends AndroidViewModel {

    public static final class State {
        private final boolean loading;
        private final boolean fromCache;
        private final String message;
        private final List<LendingItem> items;
        @Nullable private final Double latitude;
        @Nullable private final Double longitude;

        private State(
                boolean loading,
                boolean fromCache,
                @Nullable String message,
                @NonNull List<LendingItem> items,
                @Nullable Double latitude,
                @Nullable Double longitude) {
            this.loading = loading;
            this.fromCache = fromCache;
            this.message = message;
            this.items = items;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public boolean isLoading() { return loading; }
        public boolean isFromCache() { return fromCache; }
        @Nullable public String getMessage() { return message; }
        @NonNull public List<LendingItem> getItems() { return items; }
        @Nullable public Double getLatitude() { return latitude; }
        @Nullable public Double getLongitude() { return longitude; }
    }

    private final FirestoreLendingRepository repository;
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(
            true, false, null, Collections.emptyList(), null, null));
    private FirestoreLendingRepository.Subscription subscription =
            FirestoreLendingRepository.Subscription.NONE;
    private List<LendingItem> source = Collections.emptyList();
    private String query = "";
    private String category = "all";
    @Nullable private Double latitude;
    @Nullable private Double longitude;
    private boolean fromCache;

    public LendingMapViewModel(@NonNull Application application) {
        super(application);
        repository = new FirestoreLendingRepository(application);
    }

    @NonNull public LiveData<State> getState() { return state; }

    public void start() {
        subscription.remove();
        state.setValue(new State(true, false, null,
                Collections.emptyList(), latitude, longitude));
        subscription = repository.observeAvailableItems(
                new FirestoreLendingRepository.SnapshotCallback<>() {
                    @Override
                    public void onData(@NonNull List<LendingItem> value, boolean cached) {
                        source = value;
                        fromCache = cached;
                        publish(cached
                                ? "Offline - showing cached lending items."
                                : null);
                    }

                    @Override
                    public void onError(@NonNull Exception error) {
                        state.setValue(new State(false, false,
                                LendingListViewModel.safeMessage(
                                        error, "Nearby lending items could not be loaded."),
                                Collections.emptyList(), latitude, longitude));
                    }
                });
    }

    public void setQuery(@Nullable String value) {
        query = value == null ? "" : value;
        publish(null);
    }

    public void setCategory(@Nullable String value) {
        category = value == null ? "all" : value;
        publish(null);
    }

    @NonNull public String getQuery() { return query; }
    @NonNull public String getCategory() { return category; }

    public void setLocation(double rawLatitude, double rawLongitude) {
        latitude = LendingPolicy.roundLatitude(rawLatitude);
        longitude = LendingPolicy.roundLongitude(rawLongitude);
        publish("Sorted by approximate straight-line distance.");
    }

    public void showLocationMessage(@NonNull String message) {
        publish(message);
    }

    private void publish(@Nullable String message) {
        state.setValue(new State(false, fromCache, message,
                LendingPolicy.filterAndSort(
                        source, query, category, latitude, longitude),
                latitude, longitude));
    }

    public void stop() {
        subscription.remove();
        subscription = FirestoreLendingRepository.Subscription.NONE;
    }

    @Override protected void onCleared() { stop(); }
}
