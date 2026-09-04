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
import com.propcycle.app.data.lending.LendingRequest;
import com.propcycle.app.data.lending.LendingRating;
import com.propcycle.app.data.chat.ChatRepository;
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
        private final LendingRequest request;
        private final List<LendingRating> ratings;

        private State(
                boolean loading,
                boolean busy,
                boolean fromCache,
                String message,
                LendingItem item,
                LendingRequest request,
                List<LendingRating> ratings) {
            this.loading = loading;
            this.busy = busy;
            this.fromCache = fromCache;
            this.message = message;
            this.item = item;
            this.request = request;
            this.ratings = ratings;
        }

        public boolean isLoading() { return loading; }
        public boolean isBusy() { return busy; }
        public boolean isFromCache() { return fromCache; }
        public String getMessage() { return message; }
        public LendingItem getItem() { return item; }
        @Nullable public LendingRequest getRequest() { return request; }
        public List<LendingRating> getRatings() { return ratings; }
    }

    private final FirestoreLendingRepository repository;
    private final ActivityLogRepository activityLog;
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(
            true, false, false, null, null, null, Collections.emptyList()));
    private final MutableLiveData<OneTimeEvent<String>> requestCreated =
            new MutableLiveData<>();
    private FirestoreLendingRepository.Subscription itemSubscription =
            FirestoreLendingRepository.Subscription.NONE;
    private FirestoreLendingRepository.Subscription ratingSubscription =
            FirestoreLendingRepository.Subscription.NONE;
    private FirestoreLendingRepository.Subscription requestSubscription =
            FirestoreLendingRepository.Subscription.NONE;
    private String activeItemId = "";
    private LendingItem item;
    private LendingRequest request;
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
        request = null;
        ratings = Collections.emptyList();
        state.setValue(new State(true, false, false, null, null, null, ratings));
        observeCurrentRequest(cleanId);
        itemSubscription = repository.observeItem(cleanId,
                new FirestoreLendingRepository.SnapshotCallback<>() {
                    @Override
                    public void onData(@NonNull LendingItem value, boolean fromCache) {
                        item = value;
                        itemFromCache = fromCache;
                        state.setValue(new State(
                                false, false, fromCache,
                                fromCache ? "Offline - showing cached item details." : null,
                                item, request, ratings));
                        observeRatings(value.getOwnerId());
                    }

                    @Override
                    public void onError(@NonNull Exception error) {
                        state.setValue(new State(false, false, false,
                                LendingListViewModel.safeMessage(
                                        error, "This lending item could not be loaded."),
                                null, request, ratings));
                    }
                });
    }

    public void request(@NonNull String startDate, @NonNull String endDate) {
        if (item == null) {
            return;
        }
        LendingItem requestedItem = item;
        setBusy("Sending borrowing request...");
        repository.createRequest(requestedItem, startDate, endDate)
                .addOnSuccessListener(id -> {
                    activityLog.record(
                            ActivityLogRepository.TYPE_LENDING_REQUEST,
                            "Borrowing request sent",
                            requestedItem.getTitle() == null
                                    ? "Lending item" : requestedItem.getTitle(),
                            ActivityLogRepository.DESTINATION_LENDING_REQUESTS,
                            id);
                    ChatRepository.createOrGetLendingRequestThread(
                                    getApplication(),
                                    requestedItem.getId(),
                                    requestedItem.getOwnerId(),
                                    requestedItem.getTitle(),
                                    id)
                            .addOnSuccessListener(threadId -> {
                                requestCreated.setValue(new OneTimeEvent<>(id));
                                state.setValue(new State(false, false, itemFromCache,
                                        "Request sent. Track it here, in Chat, or from Notifications.",
                                        item, request, ratings));
                            })
                            .addOnFailureListener(error -> {
                                requestCreated.setValue(new OneTimeEvent<>(id));
                                state.setValue(new State(false, false, itemFromCache,
                                        "Request sent, but its Chat card could not be added. Open Chat with Owner to retry.",
                                        item, request, ratings));
                            });
                })
                .addOnFailureListener(error -> state.setValue(new State(
                        false, false, itemFromCache,
                        LendingListViewModel.safeMessage(
                                asException(error), "The borrowing request failed."),
                        item, request, ratings)));
    }

    public void cancelRequest() {
        String uid = currentUserId();
        LendingRequest current = request;
        if (!isCancellable(current, uid)) {
            return;
        }
        setBusy("Cancelling request...");
        repository.cancel(current.getId())
                .addOnSuccessListener(ignored -> {
                    current.setStatus("cancelled");
                    activityLog.record(
                            ActivityLogRepository.TYPE_LENDING_STATUS,
                            "Borrowing request cancelled",
                            current.getItemTitle() == null
                                    ? "Lending item" : current.getItemTitle(),
                            ActivityLogRepository.DESTINATION_LENDING_REQUESTS,
                            current.getId());
                    state.setValue(new State(false, false, itemFromCache,
                            "Request cancelled.", item, current, ratings));
                })
                .addOnFailureListener(error -> state.setValue(new State(
                        false, false, itemFromCache,
                        LendingListViewModel.safeMessage(
                                asException(error), "The request could not be cancelled."),
                        item, request, ratings)));
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
                        item, request, ratings)));
    }

    private void observeCurrentRequest(@NonNull String itemId) {
        requestSubscription.remove();
        requestSubscription = repository.observeMyRequests(
                new FirestoreLendingRepository.SnapshotCallback<>() {
                    @Override
                    public void onData(
                            @NonNull List<LendingRequest> requests,
                            boolean fromCache) {
                        request = findBorrowerRequest(
                                requests, itemId, repository.currentUserId());
                        State current = state.getValue();
                        state.setValue(new State(
                                current == null || current.isLoading(),
                                current != null && current.isBusy(),
                                itemFromCache || fromCache,
                                current == null ? null : current.getMessage(),
                                item,
                                request,
                                ratings));
                    }

                    @Override
                    public void onError(@NonNull Exception error) {
                        State current = state.getValue();
                        state.setValue(new State(
                                current == null || current.isLoading(),
                                current != null && current.isBusy(),
                                itemFromCache,
                                LendingListViewModel.safeMessage(
                                        error, "Your lending request could not be loaded."),
                                item,
                                request,
                                ratings));
                    }
                });
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
                                request,
                                ratings));
                    }

                    @Override
                    public void onError(@NonNull Exception error) {
                        // Ratings are supplementary; item actions remain usable.
                    }
                });
    }

    private void setBusy(@NonNull String message) {
        state.setValue(new State(
                false, true, itemFromCache, message, item, request, ratings));
    }

    public void stop() {
        itemSubscription.remove();
        ratingSubscription.remove();
        requestSubscription.remove();
        itemSubscription = FirestoreLendingRepository.Subscription.NONE;
        ratingSubscription = FirestoreLendingRepository.Subscription.NONE;
        requestSubscription = FirestoreLendingRepository.Subscription.NONE;
        activeItemId = "";
    }

    @Override protected void onCleared() { stop(); }

    @NonNull
    private static Exception asException(@NonNull Exception error) {
        return error;
    }

    private static boolean isCancellable(
            @Nullable LendingRequest request,
            @Nullable String currentUid) {
        return request != null
                && currentUid != null
                && currentUid.equals(request.getBorrowerUid())
                && ("pending".equals(request.getStatus())
                || "approved".equals(request.getStatus()));
    }

    @Nullable
    private static LendingRequest findBorrowerRequest(
            @NonNull List<LendingRequest> requests,
            @NonNull String itemId,
            @Nullable String currentUid) {
        LendingRequest latest = null;
        for (LendingRequest candidate : requests) {
            if (candidate == null
                    || currentUid == null
                    || !itemId.equals(candidate.getItemId())
                    || !currentUid.equals(candidate.getBorrowerUid())) {
                continue;
            }
            if (latest == null) {
                latest = candidate;
            }
            if (isCancellable(candidate, currentUid)) {
                return candidate;
            }
        }
        return latest;
    }
}
