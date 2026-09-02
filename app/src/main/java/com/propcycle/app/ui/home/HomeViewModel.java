package com.propcycle.app.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.propcycle.app.data.activity.ActivityLogRepository;
import com.propcycle.app.data.activity.ActivityRecord;
import com.propcycle.app.data.lending.FirestoreLendingRepository;
import com.propcycle.app.data.lending.LendingItem;
import com.propcycle.app.data.lending.LendingPolicy;
import com.propcycle.app.data.marketplace.FirestoreMarketplaceRepository;
import com.propcycle.app.data.marketplace.MarketplaceListing;
import com.propcycle.app.data.marketplace.MarketplaceListingValidator;
import com.propcycle.app.data.marketplace.MarketplaceRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HomeViewModel extends AndroidViewModel {

    private final LiveData<List<ActivityRecord>> activities;
    private final MutableLiveData<SearchState> searchState =
            new MutableLiveData<>(SearchState.hidden());
    private final MarketplaceRepository.Subscription marketplaceSubscription;
    private final FirestoreLendingRepository.Subscription lendingSubscription;
    private List<MarketplaceListing> marketplaceSource = Collections.emptyList();
    private List<LendingItem> lendingSource = Collections.emptyList();
    private String searchQuery = "";
    private boolean marketplaceReady;
    private boolean lendingReady;
    private boolean marketplaceFailed;
    private boolean lendingFailed;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        activities = new ActivityLogRepository(application).observeCurrentUser();

        MarketplaceRepository marketplaceRepository =
                new FirestoreMarketplaceRepository(application);
        marketplaceSubscription = marketplaceRepository.observeAvailableListings(
                new MarketplaceRepository.ListingsObserver() {
                    @Override
                    public void onListings(
                            @NonNull List<MarketplaceListing> listings,
                            boolean fromCache) {
                        marketplaceSource = new ArrayList<>(listings);
                        marketplaceReady = true;
                        marketplaceFailed = false;
                        publishSearchState();
                    }

                    @Override
                    public void onError(@NonNull MarketplaceRepository.RepositoryError error) {
                        marketplaceSource = Collections.emptyList();
                        marketplaceReady = true;
                        marketplaceFailed = true;
                        publishSearchState();
                    }
                });

        FirestoreLendingRepository lendingRepository =
                new FirestoreLendingRepository(application);
        lendingSubscription = lendingRepository.observeAvailableItems(
                new FirestoreLendingRepository.SnapshotCallback<>() {
                    @Override
                    public void onData(@NonNull List<LendingItem> value, boolean fromCache) {
                        lendingSource = new ArrayList<>(value);
                        lendingReady = true;
                        lendingFailed = false;
                        publishSearchState();
                    }

                    @Override
                    public void onError(@NonNull Exception error) {
                        lendingSource = Collections.emptyList();
                        lendingReady = true;
                        lendingFailed = true;
                        publishSearchState();
                    }
                });
    }

    @NonNull
    public LiveData<List<ActivityRecord>> getActivities() {
        return activities;
    }

    @NonNull
    public LiveData<SearchState> getSearchState() {
        return searchState;
    }

    public void setSearchQuery(@NonNull String query) {
        String cleanQuery = query.trim();
        if (cleanQuery.equals(searchQuery)) {
            return;
        }
        searchQuery = cleanQuery;
        publishSearchState();
    }

    private void publishSearchState() {
        if (searchQuery.length() < 2) {
            searchState.setValue(SearchState.hidden());
            return;
        }

        List<MarketplaceListing> marketplaceMatches = filterMarketplace(searchQuery);
        List<LendingItem> lendingMatches = LendingPolicy.filterAndSort(
                lendingSource, searchQuery, "all", null, null);
        searchState.setValue(new SearchState(
                true,
                searchQuery,
                marketplaceMatches,
                lendingMatches,
                !marketplaceReady || !lendingReady,
                marketplaceFailed || lendingFailed,
                marketplaceFailed && lendingFailed));
    }

    @NonNull
    private List<MarketplaceListing> filterMarketplace(@NonNull String query) {
        String normalizedQuery = MarketplaceListingValidator.normalizeSearchText(query);
        List<MarketplaceListing> matches = new ArrayList<>();
        for (MarketplaceListing listing : marketplaceSource) {
            if (listing == null) {
                continue;
            }
            String searchable = MarketplaceListingValidator.normalizeSearchText(
                    safe(listing.getTitle()) + " "
                            + safe(listing.getDescription()) + " "
                            + safe(listing.getCategory()));
            if (searchable.contains(normalizedQuery)) {
                matches.add(listing);
            }
        }
        return matches;
    }

    @NonNull
    private static String safe(String value) {
        return value == null ? "" : value;
    }

    @Override
    protected void onCleared() {
        marketplaceSubscription.close();
        lendingSubscription.remove();
        super.onCleared();
    }

    public static final class SearchState {
        private final boolean active;
        private final String query;
        private final List<MarketplaceListing> marketplaceResults;
        private final List<LendingItem> lendingResults;
        private final boolean loading;
        private final boolean partialFailure;
        private final boolean resourcesUnavailable;

        private SearchState(
                boolean active,
                @NonNull String query,
                @NonNull List<MarketplaceListing> marketplaceResults,
                @NonNull List<LendingItem> lendingResults,
                boolean loading,
                boolean partialFailure,
                boolean resourcesUnavailable) {
            this.active = active;
            this.query = query;
            this.marketplaceResults = Collections.unmodifiableList(
                    new ArrayList<>(marketplaceResults));
            this.lendingResults = Collections.unmodifiableList(
                    new ArrayList<>(lendingResults));
            this.loading = loading;
            this.partialFailure = partialFailure;
            this.resourcesUnavailable = resourcesUnavailable;
        }

        @NonNull
        private static SearchState hidden() {
            return new SearchState(
                    false,
                    "",
                    Collections.emptyList(),
                    Collections.emptyList(),
                    false,
                    false,
                    false);
        }

        public boolean isActive() {
            return active;
        }

        @NonNull
        public String getQuery() {
            return query;
        }

        @NonNull
        public List<MarketplaceListing> getMarketplaceResults() {
            return marketplaceResults;
        }

        @NonNull
        public List<LendingItem> getLendingResults() {
            return lendingResults;
        }

        public boolean isLoading() {
            return loading;
        }

        public boolean hasPartialFailure() {
            return partialFailure;
        }

        public boolean areResourcesUnavailable() {
            return resourcesUnavailable;
        }
    }
}
