package com.propcycle.app.data.marketplace;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/** Boundary between marketplace screens and Firebase. */
public interface MarketplaceRepository {

    enum ErrorType {
        CONFIGURATION_REQUIRED,
        AUTHENTICATION_REQUIRED,
        PERMISSION_DENIED,
        NETWORK,
        NOT_FOUND,
        UNKNOWN
    }

    final class RepositoryError {
        private final ErrorType type;
        private final String message;

        public RepositoryError(@NonNull ErrorType type, @NonNull String message) {
            this.type = type;
            this.message = message;
        }

        @NonNull
        public ErrorType getType() {
            return type;
        }

        @NonNull
        public String getMessage() {
            return message;
        }
    }

    interface Subscription {
        void close();
    }

    interface ListingsObserver {
        void onListings(@NonNull List<MarketplaceListing> listings, boolean fromCache);

        void onError(@NonNull RepositoryError error);
    }

    interface ListingObserver {
        void onListing(@Nullable MarketplaceListing listing, boolean fromCache);

        void onError(@NonNull RepositoryError error);
    }

    interface CreateCallback {
        void onCreated(@NonNull String listingId);

        void onError(@NonNull RepositoryError error);
    }

    @NonNull
    Subscription observeAvailableListings(@NonNull ListingsObserver observer);

    @NonNull
    Subscription observeListing(
            @NonNull String listingId,
            @NonNull ListingObserver observer);

    void createListing(
            @NonNull NewMarketplaceListing listing,
            @NonNull CreateCallback callback);

    @Nullable
    String currentUserId();
}
