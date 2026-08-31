package com.propcycle.app.data.marketplace;

import androidx.annotation.NonNull;

/** One important Marketplace lifecycle update shown in the in-app Notifications screen. */
public final class MarketplaceStatusNotice {

    private final String listingId;
    private final String title;
    private final boolean ownerView;
    private final long updatedAtMillis;

    public MarketplaceStatusNotice(
            @NonNull String listingId,
            @NonNull String title,
            boolean ownerView,
            long updatedAtMillis) {
        this.listingId = listingId;
        this.title = title;
        this.ownerView = ownerView;
        this.updatedAtMillis = updatedAtMillis;
    }

    @NonNull public String getListingId() { return listingId; }
    @NonNull public String getTitle() { return title; }
    public boolean isOwnerView() { return ownerView; }
    public long getUpdatedAtMillis() { return updatedAtMillis; }
}
