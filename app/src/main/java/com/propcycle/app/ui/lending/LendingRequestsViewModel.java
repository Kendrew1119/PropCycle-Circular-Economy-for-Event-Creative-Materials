package com.propcycle.app.ui.lending;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Task;
import com.propcycle.app.data.lending.FirestoreLendingRepository;
import com.propcycle.app.data.lending.LendingRequest;

import java.util.Collections;
import java.util.List;

/** In-app request inbox and lifecycle action state. */
public final class LendingRequestsViewModel extends AndroidViewModel {

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
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(
            true, false, null, null, Collections.emptyList()));
    private FirestoreLendingRepository.Subscription subscription =
            FirestoreLendingRepository.Subscription.NONE;
    private List<LendingRequest> requests = Collections.emptyList();
    private boolean fromCache;

    public LendingRequestsViewModel(@NonNull Application application) {
        super(application);
        repository = new FirestoreLendingRepository(application);
    }

    @NonNull public LiveData<State> getState() { return state; }
    @Nullable public String currentUserId() { return repository.currentUserId(); }

    public void start() {
        stop();
        state.setValue(new State(true, false, null, null, requests));
        subscription = repository.observeMyRequests(
                new FirestoreLendingRepository.SnapshotCallback<>() {
                    @Override
                    public void onData(@NonNull List<LendingRequest> value, boolean cached) {
                        requests = value;
                        fromCache = cached;
                        state.setValue(new State(false, cached,
                                cached ? "Offline - showing cached lending updates." : null,
                                null, requests));
                    }

                    @Override
                    public void onError(@NonNull Exception error) {
                        state.setValue(new State(false, false,
                                LendingListViewModel.safeMessage(
                                        error, "Lending updates could not be loaded."),
                                null, requests));
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
        task.addOnSuccessListener(ignored -> state.setValue(new State(
                        false, fromCache, "Lending update saved.", null, requests)))
                .addOnFailureListener(error -> state.setValue(new State(
                        false, fromCache,
                        LendingListViewModel.safeMessage(
                                error, "The lending update could not be saved."),
                        null, requests)));
    }

    public void rate(@NonNull LendingRequest request, int score, @Nullable String comment) {
        state.setValue(new State(false, fromCache, "Saving rating...",
                request.getId(), requests));
        repository.rate(request.getId(), score, comment)
                .addOnSuccessListener(ignored -> state.setValue(new State(
                        false, fromCache, "Thank you. Your rating was saved.", null, requests)))
                .addOnFailureListener(error -> state.setValue(new State(
                        false, fromCache,
                        LendingListViewModel.safeMessage(error, "The rating could not be saved."),
                        null, requests)));
    }

    public void stop() {
        subscription.remove();
        subscription = FirestoreLendingRepository.Subscription.NONE;
    }

    @Override protected void onCleared() { stop(); }
}
