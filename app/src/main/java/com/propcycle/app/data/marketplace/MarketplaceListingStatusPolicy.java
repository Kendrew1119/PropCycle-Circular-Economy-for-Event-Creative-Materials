package com.propcycle.app.data.marketplace;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Pure owner/non-owner action policy for the marketplace listing lifecycle. */
public final class MarketplaceListingStatusPolicy {

    public static final String AVAILABLE = "available";
    public static final String WITHDRAWN = "withdrawn";
    public static final String SOLD = "sold";

    private MarketplaceListingStatusPolicy() {
    }

    public static boolean isSupportedStatus(@Nullable String status) {
        return AVAILABLE.equals(status) || WITHDRAWN.equals(status) || SOLD.equals(status);
    }

    public static boolean canEdit(boolean owner, @Nullable String status) {
        return owner && (AVAILABLE.equals(status) || WITHDRAWN.equals(status));
    }

    public static boolean canWithdraw(boolean owner, @Nullable String status) {
        return owner && AVAILABLE.equals(status);
    }

    public static boolean canRelist(boolean owner, @Nullable String status) {
        return owner && WITHDRAWN.equals(status);
    }

    public static boolean canComplete(boolean owner, @Nullable String status) {
        return owner && AVAILABLE.equals(status);
    }

    public static boolean canMarkSold(boolean owner, @Nullable String status) {
        return canComplete(owner, status);
    }

    public static boolean canContactSeller(boolean owner, @Nullable String status) {
        return !owner && AVAILABLE.equals(status);
    }

    @NonNull
    public static String ownerStatusActionLabel(@Nullable String status) {
        if (WITHDRAWN.equals(status)) {
            return "Relist";
        }
        return AVAILABLE.equals(status) ? "Withdraw" : "Sold";
    }

    @NonNull
    public static String completionActionLabel(@Nullable String transactionIntent) {
        if ("donation".equalsIgnoreCase(transactionIntent)) {
            return "Mark as Given Away";
        }
        if ("exchange".equalsIgnoreCase(transactionIntent)) {
            return "Mark as Exchanged";
        }
        return "Mark as Sold";
    }

    @NonNull
    public static String completedDisplayLabel(@Nullable String transactionIntent) {
        if ("donation".equalsIgnoreCase(transactionIntent)) {
            return "Given Away";
        }
        if ("exchange".equalsIgnoreCase(transactionIntent)) {
            return "Exchanged";
        }
        return "Sold";
    }

    @NonNull
    public static String completionConfirmationTitle(@Nullable String transactionIntent) {
        if ("donation".equalsIgnoreCase(transactionIntent)) {
            return "Mark this item as given away?";
        }
        if ("exchange".equalsIgnoreCase(transactionIntent)) {
            return "Mark this item as exchanged?";
        }
        return "Mark this item as sold?";
    }

    @NonNull
    public static String completionConfirmationMessage(@Nullable String transactionIntent) {
        if ("donation".equalsIgnoreCase(transactionIntent)) {
            return "Mark this item as given away? It will be permanently removed from public browse; existing chats are kept.";
        }
        if ("exchange".equalsIgnoreCase(transactionIntent)) {
            return "Mark this item as exchanged? It will be permanently removed from public browse; existing chats are kept.";
        }
        return "Mark this item as sold? It will be permanently removed from public browse; existing chats are kept.";
    }
}
