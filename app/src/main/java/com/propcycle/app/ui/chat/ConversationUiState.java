package com.propcycle.app.ui.chat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.propcycle.app.data.chat.ChatMessage;
import com.propcycle.app.data.chat.ChatThread;
import com.propcycle.app.data.marketplace.MarketplaceListing;
import com.propcycle.app.data.lending.LendingItem;
import com.propcycle.app.data.lending.LendingRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/** Complete render state for one real-time conversation. */
public final class ConversationUiState {

    private final boolean loading;
    private final boolean configurationRequired;
    private final boolean fromCache;
    private final boolean sending;
    private final String errorMessage;
    private final ChatThread thread;
    private final List<ChatMessage> messages;
    private final MarketplaceListing marketplaceListing;
    private final boolean marketplaceListingLoading;
    private final Map<String, LendingRequest> lendingRequests;
    private final Set<String> loadingLendingRequestIds;
    private final LendingItem lendingItem;
    private final String busyLendingRequestId;

    public ConversationUiState(
            boolean loading,
            boolean configurationRequired,
            boolean fromCache,
            boolean sending,
            @Nullable String errorMessage,
            @Nullable ChatThread thread,
            @NonNull List<ChatMessage> messages) {
        this(loading, configurationRequired, fromCache, sending, errorMessage,
                thread, messages, null, false, Collections.emptyMap(),
                Collections.emptySet(), null, "");
    }

    public ConversationUiState(
            boolean loading,
            boolean configurationRequired,
            boolean fromCache,
            boolean sending,
            @Nullable String errorMessage,
            @Nullable ChatThread thread,
            @NonNull List<ChatMessage> messages,
            @Nullable MarketplaceListing marketplaceListing,
            boolean marketplaceListingLoading) {
        this(loading, configurationRequired, fromCache, sending, errorMessage,
                thread, messages, marketplaceListing, marketplaceListingLoading,
                Collections.emptyMap(), Collections.emptySet(), null, "");
    }

    public ConversationUiState(
            boolean loading,
            boolean configurationRequired,
            boolean fromCache,
            boolean sending,
            @Nullable String errorMessage,
            @Nullable ChatThread thread,
            @NonNull List<ChatMessage> messages,
            @Nullable MarketplaceListing marketplaceListing,
            boolean marketplaceListingLoading,
            @NonNull Map<String, LendingRequest> lendingRequests,
            @NonNull Set<String> loadingLendingRequestIds,
            @Nullable LendingItem lendingItem,
            @NonNull String busyLendingRequestId) {
        this.loading = loading;
        this.configurationRequired = configurationRequired;
        this.fromCache = fromCache;
        this.sending = sending;
        this.errorMessage = errorMessage;
        this.thread = thread;
        this.messages = Collections.unmodifiableList(new ArrayList<>(messages));
        this.marketplaceListing = marketplaceListing;
        this.marketplaceListingLoading = marketplaceListingLoading;
        this.lendingRequests = Collections.unmodifiableMap(new HashMap<>(lendingRequests));
        this.loadingLendingRequestIds = Collections.unmodifiableSet(
                new HashSet<>(loadingLendingRequestIds));
        this.lendingItem = lendingItem;
        this.busyLendingRequestId = busyLendingRequestId;
    }

    @NonNull
    public static ConversationUiState loading() {
        return new ConversationUiState(
                true, false, false, false, null, null, Collections.emptyList());
    }

    public boolean isLoading() {
        return loading;
    }

    public boolean isConfigurationRequired() {
        return configurationRequired;
    }

    public boolean isFromCache() {
        return fromCache;
    }

    public boolean isSending() {
        return sending;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    @Nullable
    public ChatThread getThread() {
        return thread;
    }

    @NonNull
    public List<ChatMessage> getMessages() {
        return messages;
    }

    @Nullable
    public MarketplaceListing getMarketplaceListing() {
        return marketplaceListing;
    }

    public boolean isMarketplaceListingLoading() {
        return marketplaceListingLoading;
    }

    @NonNull
    public Map<String, LendingRequest> getLendingRequests() {
        return lendingRequests;
    }

    @NonNull
    public Set<String> getLoadingLendingRequestIds() {
        return loadingLendingRequestIds;
    }

    @Nullable
    public LendingItem getLendingItem() {
        return lendingItem;
    }

    @NonNull
    public String getBusyLendingRequestId() {
        return busyLendingRequestId;
    }
}
