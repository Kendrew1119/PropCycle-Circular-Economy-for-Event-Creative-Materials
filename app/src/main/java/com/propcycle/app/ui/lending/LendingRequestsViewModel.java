package com.propcycle.app.ui.lending;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Task;
import com.propcycle.app.data.activity.ActivityLogRepository;
import com.propcycle.app.data.lending.FirestoreLendingRepository;
import com.propcycle.app.data.lending.LendingRequest;
import com.propcycle.app.data.marketplace.MarketplaceStatusNotice;
import com.propcycle.app.data.marketplace.MarketplaceStatusNoticeRepository;

import java.util.Collections;
import java.util.List;

/** In-app request inbox and lifecycle action state. */
public final class LendingRequestsViewModel extends AndroidViewModel {

    private static final String DIAGNOSTIC_TAG = "PropCycleLendingDebug";

    public static final class MarketplaceNoticeState {
        private final boolean loading;
        private final boolean fromCache;
        private final String message;
        private final List<MarketplaceStatusNotice> notices;

        private MarketplaceNoticeState(
                boolean loading,
                boolean fromCache,
                @Nullable String message,
                @NonNull List<MarketplaceStatusNotice> notices) {
            this.loading = loading;
            this.fromCache = fromCache;
            this.message = message;
            this.notices = notices;
        }

        public boolean isLoading() { return loading; }
        public boolean isFromCache() { return fromCache; }
        @Nullable public String getMessage() { return message; }
        @NonNull public List<MarketplaceStatusNotice> getNotices() { return notices; }
    }

    public static final class State {
        private final boolean loading;
        private final boolean fromCache;
        private final String message;
        private final String busyRequestId;
        private final List<LendingRequest> requests;

        private State(
                boolean loading,
                boolean fromCache,
                String message,
                String busyRequestId,
                List<LendingRequest> requests) {
            this.loading = loading;
            this.fromCache = fromCache;
            this.message = message;
            this.busyRequestId = busyRequestId;
            this.requests = requests;
        }

        public boolean isLoading() { return loading; }
        public boolean isFromCache() { return fromCache; }
        public String getMessage() { return message; }
        public String getBusyRequestId() { return busyRequestId; }
        public List<LendingRequest> getRequests() { return requests; }
    }

    private final FirestoreLendingRepository repository;
    private final ActivityLogRepository activityLog;
    private final MarketplaceStatusNoticeRepository marketplaceNoticeRepository;
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(
            true, false, null, null, Collections.emptyList()));
    private FirestoreLendingRepository.Subscription subscription =
            FirestoreLendingRepository.Subscription.NONE;
    private MarketplaceStatusNoticeRepository.Subscription marketplaceNoticeSubscription =
            MarketplaceStatusNoticeRepository.Subscription.NONE;
    private final MutableLiveData<MarketplaceNoticeState> marketplaceNoticeState =
            new MutableLiveData<>(new MarketplaceNoticeState(
                    true, false, null, Collections.emptyList()));
    private List<LendingRequest> requests = Collections.emptyList();
    private boolean fromCache;

    public LendingRequestsViewModel(@NonNull Application application) {
        super(application);
        repository = new FirestoreLendingRepository(application);
        activityLog = new ActivityLogRepository(application);
        marketplaceNoticeRepository = new MarketplaceStatusNoticeRepository(application);
    }

    @NonNull public LiveData<State> getState() { return state; }
    @NonNull public LiveData<MarketplaceNoticeState> getMarketplaceNoticeState() {
        return marketplaceNoticeState;
    }
    @Nullable public String currentUserId() { return repository.currentUserId(); }

    public void start() {
        stop();
        Log.d(DIAGNOSTIC_TAG, "ViewModel loading=true error=false");
        state.setValue(new State(true, false, null, null, requests));
        subscription = repository.observeMyRequests(
                new FirestoreLendingRepository.SnapshotCallback<>() {
                    @Override
                    public void onData(@NonNull List<LendingRequest> value, boolean cached) {
                        Log.d(DIAGNOSTIC_TAG,
                                "ViewModel repository emitted request list size=" + value.size());
                        requests = value;
                        fromCache = cached;
                        Log.d(DIAGNOSTIC_TAG,
                                "ViewModel loading=false error=false fromCache=" + cached);
                        state.setValue(new State(false, cached,
                                cached ? "Offline - showing cached lending updates." : null,
                                null, requests));
                    }

                    @Override
                    public void onError(@NonNull Exception error) {
                        Log.e(DIAGNOSTIC_TAG,
                                "ViewModel loading=false error=true type="
                                        + error.getClass().getSimpleName());
                        state.setValue(new State(false, false,
                                LendingListViewModel.safeMessage(
                                        error, "Lending updates could not be loaded."),
                                null, requests));
                    }
                });
        marketplaceNoticeState.setValue(new MarketplaceNoticeState(
                true, false, null, Collections.emptyList()));
        marketplaceNoticeSubscription = marketplaceNoticeRepository.observe(
                new MarketplaceStatusNoticeRepository.Callback() {
                    @Override
                    public void onData(
                            @NonNull List<MarketplaceStatusNotice> notices,
                            boolean cached) {
                        marketplaceNoticeState.setValue(new MarketplaceNoticeState(
                                false,
                                cached,
                                cached ? "Offline - Marketplace status may be outdated." : null,
                                notices));
                    }

                    @Override
                    public void onError(@NonNull Exception error) {
                        marketplaceNoticeState.setValue(new MarketplaceNoticeState(
                                false,
                                false,
                                "Marketplace status updates could not be loaded.",
                                Collections.emptyList()));
                    }
                });
    }

    public void perform(
            @NonNull LendingRequest request,
            @NonNull LendingRequestAdapter.Action action) {
        Task<Void> task = switch (action) {
            case APPROVE -> repository.approve(request.getId());
            case REJECT -> repository.reject(request.getId());
            case CANCEL -> repository.cancel(request.getId());
            case ACTIVATE -> repository.activate(request.getId());
            case REPORT_RETURN -> repository.reportReturn(request.getId());
            case CONFIRM_RETURN -> repository.confirmReturn(request.getId());
            case RATE -> null;
        };
        if (task == null) {
            return;
        }
        state.setValue(new State(false, fromCache, "Saving lending update...",
                request.getId(), requests));
        android.widget.Toast.makeText(getApplication(), "Processing " + action, android.widget.Toast.LENGTH_SHORT).show();
        task.addOnSuccessListener(ignored -> {
                    activityLog.record(
                            ActivityLogRepository.TYPE_LENDING_STATUS,
                            activityTitle(action),
                            request.getItemTitle() == null
                                    ? "Lending item" : request.getItemTitle(),
                            ActivityLogRepository.DESTINATION_LENDING_REQUESTS,
                            request.getId());
                    state.setValue(new State(
                            false, fromCache, "Lending update saved.", null, requests));
                    android.widget.Toast.makeText(getApplication(), "Success", android.widget.Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(error -> {
                    android.widget.Toast.makeText(getApplication(), "Failed: " + error.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                    state.setValue(new State(
                        false, fromCache,
                        LendingListViewModel.safeMessage(
                                error, "The lending update could not be saved."),
                        null, requests));
                });
    }

    public void rate(@NonNull LendingRequest request, int score, @Nullable String comment) {
        state.setValue(new State(false, fromCache, "Saving rating...",
                request.getId(), requests));
        repository.rate(request.getId(), score, comment)
                .addOnSuccessListener(ignored -> {
                    activityLog.record(
                            ActivityLogRepository.TYPE_LENDING_STATUS,
                            "Lending experience rated",
                            request.getItemTitle() == null
                                    ? "Lending item" : request.getItemTitle(),
                            ActivityLogRepository.DESTINATION_LENDING_REQUESTS,
                            request.getId());
                    state.setValue(new State(
                            false, fromCache, "Thank you. Your rating was saved.", null, requests));
                })
                .addOnFailureListener(error -> state.setValue(new State(
                        false, fromCache,
                        LendingListViewModel.safeMessage(error, "The rating could not be saved."),
                        null, requests)));
    }

    @NonNull
    private static String activityTitle(@NonNull LendingRequestAdapter.Action action) {
        return switch (action) {
            case APPROVE -> "Borrowing request approved";
            case REJECT -> "Borrowing request rejected";
            case CANCEL -> "Borrowing request cancelled";
            case ACTIVATE -> "Borrowing started";
            case REPORT_RETURN -> "Item return reported";
            case CONFIRM_RETURN -> "Item return confirmed";
            case RATE -> "Lending experience rated";
        };
    }

    public void stop() {
        subscription.remove();
        subscription = FirestoreLendingRepository.Subscription.NONE;
        marketplaceNoticeSubscription.remove();
        marketplaceNoticeSubscription = MarketplaceStatusNoticeRepository.Subscription.NONE;
    }

    @Override protected void onCleared() { stop(); }
}
