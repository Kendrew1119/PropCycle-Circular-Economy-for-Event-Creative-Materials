package com.propcycle.app.data.lending;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

/** Single UI-facing dispatcher for lifecycle mutations shared by Chat and Notifications. */
public final class LendingRequestActionExecutor {

    private LendingRequestActionExecutor() {
    }

    @NonNull
    public static Task<Void> execute(
            @NonNull FirestoreLendingRepository repository,
            @NonNull LendingRequest request,
            @NonNull LendingRequestActionPolicy.Action action) {
        return switch (action) {
            case APPROVE -> repository.approve(request.getId());
            case REJECT -> repository.reject(request.getId());
            case CANCEL -> repository.cancel(request.getId());
            case ACTIVATE -> repository.activate(request.getId());
            case REPORT_RETURN -> repository.reportReturn(request.getId());
            case CONFIRM_RETURN -> repository.confirmReturn(request.getId());
            case RATE -> Tasks.forException(new IllegalArgumentException(
                    "A score is required to rate this lending request."));
        };
    }
}