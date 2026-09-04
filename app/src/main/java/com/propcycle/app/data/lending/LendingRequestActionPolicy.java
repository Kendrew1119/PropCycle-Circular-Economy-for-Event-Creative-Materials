package com.propcycle.app.data.lending;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Shared role/status policy for request actions shown by Notifications and Chat. */
public final class LendingRequestActionPolicy {

    public enum Action {
        APPROVE,
        REJECT,
        CANCEL,
        ACTIVATE,
        REPORT_RETURN,
        CONFIRM_RETURN,
        RATE
    }

    private LendingRequestActionPolicy() {
    }

    @NonNull
    public static List<Action> availableActions(
            @Nullable LendingRequest request,
            @Nullable String currentUid) {
        if (request == null || currentUid == null || currentUid.trim().isEmpty()) {
            return Collections.emptyList();
        }
        boolean owner = currentUid.equals(request.getOwnerUid());
        boolean borrower = currentUid.equals(request.getBorrowerUid());
        String status = request.getStatus();
        if (owner && "pending".equals(status)) {
            return Arrays.asList(Action.APPROVE, Action.REJECT);
        }
        if (owner && "approved".equals(status)) {
            return Collections.singletonList(Action.ACTIVATE);
        }
        if (owner && "active".equals(status) && request.isReturnReported()) {
            return Collections.singletonList(Action.CONFIRM_RETURN);
        }
        if (borrower && ("pending".equals(status) || "approved".equals(status))) {
            return Collections.singletonList(Action.CANCEL);
        }
        if (borrower && "active".equals(status) && !request.isReturnReported()) {
            return Collections.singletonList(Action.REPORT_RETURN);
        }
        if (borrower && "returned".equals(status)) {
            return Collections.singletonList(Action.RATE);
        }
        return Collections.emptyList();
    }

    public static boolean isAllowed(
            @Nullable LendingRequest request,
            @Nullable String currentUid,
            @NonNull Action action) {
        return availableActions(request, currentUid).contains(action);
    }

    /** A borrower request that is still actionable or awaiting its one permitted rating. */
    public static boolean isRelevantForBorrower(
            @Nullable LendingRequest request,
            @Nullable String currentUid) {
        if (request == null || currentUid == null
                || !currentUid.equals(request.getBorrowerUid())) {
            return false;
        }
        String status = request.getStatus();
        return "pending".equals(status)
                || "approved".equals(status)
                || "active".equals(status)
                || "returned".equals(status);
    }

    @NonNull
    public static String actionLabel(@NonNull Action action) {
        return switch (action) {
            case APPROVE -> "Approve";
            case REJECT -> "Reject";
            case CANCEL -> "Cancel request";
            case ACTIVATE -> "Confirm pickup";
            case REPORT_RETURN -> "Mark returned";
            case CONFIRM_RETURN -> "Confirm return";
            case RATE -> "Rate owner";
        };
    }

    @NonNull
    public static String confirmationTitle(@NonNull Action action) {
        return switch (action) {
            case APPROVE -> "Approve this request?";
            case REJECT -> "Reject this request?";
            case CANCEL -> "Cancel this request?";
            case ACTIVATE -> "Confirm the item was picked up?";
            case REPORT_RETURN -> "Report the item as returned?";
            case CONFIRM_RETURN -> "Confirm you received the returned item?";
            case RATE -> "Rate this lending experience";
        };
    }

    @NonNull
    public static String activityTitle(@NonNull Action action) {
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
}
