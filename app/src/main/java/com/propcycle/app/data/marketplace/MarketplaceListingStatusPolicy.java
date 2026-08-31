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

    public static boolean canMarkSold(boolean owner, @Nullable String status) {
        return owner && AVAILABLE.equals(status);
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
}
