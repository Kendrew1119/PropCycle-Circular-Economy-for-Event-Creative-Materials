package com.propcycle.app.ui.marketplace;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.propcycle.app.data.marketplace.FirestoreMarketplaceRepository;
import com.propcycle.app.data.marketplace.MarketplaceListing;
import com.propcycle.app.data.marketplace.MarketplaceListingValidator;
import com.propcycle.app.data.marketplace.MarketplaceRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Holds the live Firebase result window and applies proposal filters client-side. */
public final class MarketplaceViewModel extends AndroidViewModel {

    private final MutableLiveData<State> state = new MutableLiveData<>(State.loading());
    private final MarketplaceRepository.Subscription subscription;
    private List<MarketplaceListing> remoteListings = Collections.emptyList();
    private boolean fromCache;
    private String searchQuery = "";
    private String category = "all";

    public MarketplaceViewModel(@NonNull Application application) {
        super(application);
        MarketplaceRepository repository = new FirestoreMarketplaceRepository(application);
        subscription = repository.observeAvailableListings(new MarketplaceRepository.ListingsObserver() {
            @Override
            public void onListings(
                    @NonNull List<MarketplaceListing> listings,
                    boolean resultFromCache) {
                remoteListings = new ArrayList<>(listings);
                fromCache = resultFromCache;
                publishFilteredState();
            }

            @Override
            public void onError(@NonNull MarketplaceRepository.RepositoryError error) {
                state.setValue(State.error(error.getType(), error.getMessage()));
            }
        });
    }

    @NonNull
    public LiveData<State> getState() {
        return state;
    }

    public void setSearchQuery(@NonNull String query) {
        String normalized = MarketplaceListingValidator.normalizeSearchText(query);
        if (normalized.equals(searchQuery)) {
            return;
        }
        searchQuery = normalized;
        publishFilteredState();
    }

    public void setCategory(@NonNull String stableCategoryId) {
        if (stableCategoryId.equals(category)) {
            return;
        }
        category = stableCategoryId;
        publishFilteredState();
    }

    @NonNull
    public String getSelectedCategory() {
        return category;
    }

    private void publishFilteredState() {
        List<MarketplaceListing> filtered = new ArrayList<>();
        for (MarketplaceListing listing : remoteListings) {
            if (!"all".equals(category) && !category.equals(listing.getCategory())) {
                continue;
            }
            String searchable = MarketplaceListingValidator.normalizeSearchText(
                    safe(listing.getTitle()) + " "
                            + safe(listing.getDescription()) + " "
                            + safe(listing.getCategory()));
            if (!searchQuery.isEmpty() && !searchable.contains(searchQuery)) {
                continue;
            }
            filtered.add(listing);
        }

        if (filtered.isEmpty()) {
            String message = remoteListings.isEmpty()
                    ? "No available marketplace listings yet."
                    : "No listings match this search and category.";
            state.setValue(State.empty(message, fromCache));
        } else {
            state.setValue(State.content(filtered, fromCache));
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    @Override
    protected void onCleared() {
        subscription.close();
        super.onCleared();
    }

    public static final class State {

        public enum Kind {
            LOADING,
            CONTENT,
            EMPTY,
            ERROR,
            CONFIGURATION_REQUIRED,
            AUTHENTICATION_REQUIRED
        }

        private final Kind kind;
        private final List<MarketplaceListing> listings;
        private final String message;
        private final boolean fromCache;

        private State(
                @NonNull Kind kind,
                @NonNull List<MarketplaceListing> listings,
                @NonNull String message,
                boolean fromCache) {
            this.kind = kind;
            this.listings = listings;
            this.message = message;
            this.fromCache = fromCache;
        }

        private static State loading() {
            return new State(Kind.LOADING, Collections.emptyList(), "", false);
        }

        private static State content(
                @NonNull List<MarketplaceListing> listings,
                boolean fromCache) {
            return new State(
                    Kind.CONTENT,
                    Collections.unmodifiableList(new ArrayList<>(listings)),
                    "",
                    fromCache);
        }

        private static State empty(@NonNull String message, boolean fromCache) {
            return new State(Kind.EMPTY, Collections.emptyList(), message, fromCache);
        }

        private static State error(
                @NonNull MarketplaceRepository.ErrorType type,
                @NonNull String message) {
            Kind kind = switch (type) {
                case CONFIGURATION_REQUIRED -> Kind.CONFIGURATION_REQUIRED;
                case AUTHENTICATION_REQUIRED -> Kind.AUTHENTICATION_REQUIRED;
                default -> Kind.ERROR;
            };
            return new State(kind, Collections.emptyList(), message, false);
        }

        @NonNull
        public Kind getKind() {
            return kind;
        }

        @NonNull
        public List<MarketplaceListing> getListings() {
            return listings;
        }

        @NonNull
        public String getMessage() {
            return message;
        }

        public boolean isFromCache() {
            return fromCache;
        }
    }
}
