package com.propcycle.app.ui.lending;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.propcycle.app.data.lending.FirestoreLendingRepository;
import com.propcycle.app.data.lending.LendingItem;
import com.propcycle.app.data.lending.LendingPolicy;

import java.util.Collections;
import java.util.List;

/** Real-time lending browse state with local bounded filtering. */
public final class LendingListViewModel extends AndroidViewModel {

    public static final class State {
        private final boolean loading;
        private final boolean fromCache;
        private final String message;
        private final List<LendingItem> items;

        private State(boolean loading, boolean fromCache, String message, List<LendingItem> items) {
            this.loading = loading;
            this.fromCache = fromCache;
            this.message = message;
            this.items = items;
        }

        public boolean isLoading() { return loading; }
        public boolean isFromCache() { return fromCache; }
        public String getMessage() { return message; }
        public List<LendingItem> getItems() { return items; }
    }

    private final FirestoreLendingRepository repository;
    private final MutableLiveData<State> state = new MutableLiveData<>(
            new State(true, false, null, Collections.emptyList()));
    private FirestoreLendingRepository.Subscription subscription =
            FirestoreLendingRepository.Subscription.NONE;
    private List<LendingItem> source = Collections.emptyList();
    private String query = "";
    private String category = "all";

    public LendingListViewModel(@NonNull Application application) {
        super(application);
        repository = new FirestoreLendingRepository(application);
    }

    @NonNull
    public LiveData<State> getState() { return state; }

    public void start() {
        stop();
        state.setValue(new State(true, false, null, Collections.emptyList()));
        subscription = repository.observeAvailableItems(
                new FirestoreLendingRepository.SnapshotCallback<>() {
                    @Override
                    public void onData(@NonNull List<LendingItem> value, boolean fromCache) {
                        source = value;
                        publish(fromCache, fromCache ? "Offline - showing cached lending items." : null);
                    }

                    @Override
                    public void onError(@NonNull Exception error) {
                        state.setValue(new State(
                                false,
                                false,
                                safeMessage(error, "Lending items could not be loaded."),
                                Collections.emptyList()));
                    }
                });
    }

    public void setQuery(String value) {
        query = value == null ? "" : value;
        publish(false, null);
    }

    public void setCategory(String value) {
        category = value == null ? "all" : value;
        publish(false, null);
    }

    @NonNull public String getQuery() { return query; }
    @NonNull public String getCategory() { return category; }

    public void stop() {
        subscription.remove();
        subscription = FirestoreLendingRepository.Subscription.NONE;
    }

    private void publish(boolean fromCache, String message) {
        state.setValue(new State(
                false,
                fromCache,
                message,
                LendingPolicy.filterAndSort(source, query, category, null, null)));
    }

    @Override
    protected void onCleared() {
        stop();
    }

    @NonNull
    static String safeMessage(@NonNull Exception error, @NonNull String fallback) {
        String message = error.getMessage();
        return message == null || message.trim().isEmpty() ? fallback : message;
    }
}
