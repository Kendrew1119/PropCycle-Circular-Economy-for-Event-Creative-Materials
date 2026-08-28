package com.propcycle.app.ui.lending;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.propcycle.app.data.lending.FirestoreLendingRepository;
import com.propcycle.app.data.activity.ActivityLogRepository;
import com.propcycle.app.data.lending.LendingItem;
import com.propcycle.app.data.lending.LendingRating;
import com.propcycle.app.ui.common.OneTimeEvent;

import java.util.Collections;
import java.util.List;

/** One lending detail snapshot plus request and owner actions. */
public final class LendingDetailViewModel extends AndroidViewModel {

    public static final class State {
        private final boolean loading;
        private final boolean busy;
        private final boolean fromCache;
        private final String message;
        private final LendingItem item;
        private final List<LendingRating> ratings;

        private State(
                boolean loading,
                boolean busy,
                boolean fromCache,
                String message,
                LendingItem item,
                List<LendingRating> ratings) {
            this.loading = loading;
            this.busy = busy;
            this.fromCache = fromCache;
            this.message = message;
            this.item = item;
            this.ratings = ratings;
        }

        public boolean isLoading() { return loading; }
        public boolean isBusy() { return busy; }
        public boolean isFromCache() { return fromCache; }
        public String getMessage() { return message; }
        public LendingItem getItem() { return item; }
        public List<LendingRating> getRatings() { return ratings; }
    }

    private final FirestoreLendingRepository repository;
    private final ActivityLogRepository activityLog;
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(
            true, false, false, null, null, Collections.emptyList()));
    private final MutableLiveData<OneTimeEvent<String>> requestCreated =
            new MutableLiveData<>();
    private FirestoreLendingRepository.Subscription itemSubscription =
            FirestoreLendingRepository.Subscription.NONE;
    private FirestoreLendingRepository.Subscription ratingSubscription =
            FirestoreLendingRepository.Subscription.NONE;
    private String activeItemId = "";
    private LendingItem item;
    private List<LendingRating> ratings = Collections.emptyList();
    private boolean itemFromCache;

    public LendingDetailViewModel(@NonNull Application application) {
        super(application);
        repository = new FirestoreLendingRepository(application);
        activityLog = new ActivityLogRepository(application);
    }

    @NonNull public LiveData<State> getState() { return state; }
    @NonNull public LiveData<OneTimeEvent<String>> getRequestCreated() {
        return requestCreated;
    }
    @Nullable public String currentUserId() { return repository.currentUserId(); }

    public void start(@Nullable String itemId) {
        String cleanId = itemId == null ? "" : itemId.trim();
        if (cleanId.equals(activeItemId) && itemSubscription != FirestoreLendingRepository.Subscription.NONE) {
            return;
        }
        stop();
        activeItemId = cleanId;
        item = null;
        ratings = Collections.emptyList();
        state.setValue(new State(true, false, false, null, null, ratings));
        itemSubscription = repository.observeItem(cleanId,
                new FirestoreLendingRepository.SnapshotCallback<>() {
                    @Override
                    public void onData(@NonNull LendingItem value, boolean fromCache) {
                        item = value;
                        itemFromCache = fromCache;
                        state.setValue(new State(
                                false, false, fromCache,
                                fromCache ? "Offline - showing cached item details." : null,
                                item, ratings));
                        observeRatings(value.getOwnerId());
                    }

                    @Override
                    public void onError(@NonNull Exception error) {
                        state.setValue(new State(false, false, false,
                                LendingListViewModel.safeMessage(
                                        error, "This lending item could not be loaded."),
                                null, ratings));
                    }
                });
    }

    public void request(@NonNull String startDate, @NonNull String endDate) {
        if (item == null) {
            return;
        }
        setBusy("Sending borrowing request...");
        repository.createRequest(item, startDate, endDate)
                .addOnSuccessListener(id -> {
                    activityLog.record(
                            ActivityLogRepository.TYPE_LENDING_REQUEST,
                            "Borrowing request sent",
                            item.getTitle() == null ? "Lending item" : item.getTitle(),
                            ActivityLogRepository.DESTINATION_LENDING_REQUESTS,
                            id);
                    requestCreated.setValue(new OneTimeEvent<>(id));
                    state.setValue(new State(false, false, itemFromCache,
                            "Request sent. Track it from Notifications.", item, ratings));
                })
                .addOnFailureListener(error -> state.setValue(new State(
                        false, false, itemFromCache,
                        LendingListViewModel.safeMessage(
                                asException(error), "The borrowing request failed."),
                        item, ratings)));
    }

    public void toggleStatus() {
        if (item == null) {
            return;
        }
        String next = "available".equals(item.getStatus()) ? "withdrawn" : "available";
        setBusy("Updating lending item...");
        repository.setItemStatus(item.getId(), next)
                .addOnSuccessListener(ignored -> activityLog.record(
                        ActivityLogRepository.TYPE_LENDING_STATUS,
                        "available".equals(next)
                                ? "Lending item made available"
                                : "Lending item withdrawn",
                        item.getTitle() == null ? "Lending item" : item.getTitle(),
                        ActivityLogRepository.DESTINATION_LENDING_ITEM,
                        item.getId()))
                .addOnFailureListener(error -> state.setValue(new State(
                        false, false, itemFromCache,
                        LendingListViewModel.safeMessage(
                                asException(error), "The lending status could not be changed."),
                        item, ratings)));
    }

    private void observeRatings(@NonNull String ownerUid) {
        ratingSubscription.remove();
        ratingSubscription = repository.observeRatings(ownerUid,
                new FirestoreLendingRepository.SnapshotCallback<>() {
                    @Override
                    public void onData(@NonNull List<LendingRating> value, boolean fromCache) {
                        ratings = value;
                        State current = state.getValue();
                        state.setValue(new State(
                                false,
                                current != null && current.isBusy(),
                                itemFromCache || fromCache,
                                current == null ? null : current.getMessage(),
                                item,
                                ratings));
                    }

                    @Override
                    public void onError(@NonNull Exception error) {
                        // Ratings are supplementary; item actions remain usable.
                    }
                });
    }

    private void setBusy(@NonNull String message) {
        state.setValue(new State(false, true, itemFromCache, message, item, ratings));
    }

    public void stop() {
        itemSubscription.remove();
        ratingSubscription.remove();
        itemSubscription = FirestoreLendingRepository.Subscription.NONE;
        ratingSubscription = FirestoreLendingRepository.Subscription.NONE;
        activeItemId = "";
    }

    @Override protected void onCleared() { stop(); }

    @NonNull
    private static Exception asException(@NonNull Exception error) {
        return error;
    }
}
