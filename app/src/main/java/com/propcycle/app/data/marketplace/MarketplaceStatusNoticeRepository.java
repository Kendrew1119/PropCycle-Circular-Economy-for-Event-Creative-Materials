package com.propcycle.app.data.marketplace;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.propcycle.app.data.chat.ChatRepository;
import com.propcycle.app.data.chat.ChatThread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Derives sold-listing notices from the user's existing Marketplace conversations. */
public final class MarketplaceStatusNoticeRepository {

    private static final int DISPLAY_LIMIT = 3;

    public interface Callback {
        void onData(@NonNull List<MarketplaceStatusNotice> notices, boolean fromCache);
        void onError(@NonNull Exception error);
    }

    public interface Subscription {
        void remove();

        Subscription NONE = () -> { };
    }

    private final ChatRepository chatRepository;
    private final MarketplaceRepository marketplaceRepository;
    private final Map<String, MarketplaceRepository.Subscription> listingSubscriptions =
            new HashMap<>();
    private final Map<String, ChatThread> listingThreads = new HashMap<>();
    private final Map<String, MarketplaceStatusNotice> notices = new HashMap<>();
    private final Map<String, Boolean> listingCacheState = new HashMap<>();
    private ChatRepository.Subscription threadSubscription = ChatRepository.Subscription.NONE;
    private Callback callback;
    private boolean threadsFromCache;
    private int generation;

    public MarketplaceStatusNoticeRepository(@NonNull Context context) {
        Context applicationContext = context.getApplicationContext();
        chatRepository = new ChatRepository(applicationContext);
        marketplaceRepository = new FirestoreMarketplaceRepository(applicationContext);
    }

    @NonNull
    public Subscription observe(@NonNull Callback observer) {
        stop();
        callback = observer;
        int activeGeneration = ++generation;
        threadSubscription = chatRepository.listenToThreads(
                new ChatRepository.SnapshotCallback<>() {
                    @Override
                    public void onData(
                            @NonNull List<ChatThread> threads,
                            boolean fromCache) {
                        if (activeGeneration != generation) {
                            return;
                        }
                        threadsFromCache = fromCache;
                        syncListingObservers(threads, activeGeneration);
                    }

                    @Override
                    public void onError(@NonNull Exception error) {
                        if (activeGeneration == generation && callback != null) {
                            callback.onError(error);
                        }
                    }
                });
        return this::stop;
    }

    private void syncListingObservers(
            @NonNull List<ChatThread> threads,
            int activeGeneration) {
        Map<String, ChatThread> current = new HashMap<>();
        for (ChatThread thread : threads) {
            if ("marketplace".equals(thread.getContextType())
                    && !thread.getContextId().trim().isEmpty()) {
                current.putIfAbsent(thread.getContextId(), thread);
            }
        }

        Set<String> removed = new HashSet<>(listingSubscriptions.keySet());
        removed.removeAll(current.keySet());
        for (String listingId : removed) {
            MarketplaceRepository.Subscription subscription =
                    listingSubscriptions.remove(listingId);
            if (subscription != null) {
                subscription.close();
            }
            listingThreads.remove(listingId);
            listingCacheState.remove(listingId);
            notices.remove(listingId);
        }

        listingThreads.putAll(current);
        for (Map.Entry<String, ChatThread> entry : current.entrySet()) {
            String listingId = entry.getKey();
            if (listingSubscriptions.containsKey(listingId)) {
                continue;
            }
            MarketplaceRepository.Subscription subscription =
                    marketplaceRepository.observeListing(
                            listingId,
                            new MarketplaceRepository.ListingObserver() {
                                @Override
                                public void onListing(
                                        @Nullable MarketplaceListing listing,
                                        boolean fromCache) {
                                    if (activeGeneration != generation) {
                                        return;
                                    }
                                    listingCacheState.put(listingId, fromCache);
                                    updateNotice(listingId, listing);
                                }

                                @Override
                                public void onError(
                                        @NonNull MarketplaceRepository.RepositoryError error) {
                                    if (activeGeneration == generation) {
                                        notices.remove(listingId);
                                        publish();
                                    }
                                }
                            });
            listingSubscriptions.put(listingId, subscription);
        }
        publish();
    }

    private void updateNotice(
            @NonNull String listingId,
            @Nullable MarketplaceListing listing) {
        ChatThread thread = listingThreads.get(listingId);
        String currentUid = chatRepository.currentUserId();
        if (listing == null || thread == null || currentUid == null
                || !MarketplaceListingStatusPolicy.SOLD.equals(listing.getStatus())) {
            notices.remove(listingId);
            publish();
            return;
        }
        Timestamp updatedAt = listing.getUpdatedAt();
        long updatedAtMillis = updatedAt == null ? 0L : updatedAt.toDate().getTime();
        String title = listing.getTitle() == null || listing.getTitle().trim().isEmpty()
                ? thread.getContextTitle() : listing.getTitle().trim();
        notices.put(listingId, new MarketplaceStatusNotice(
                listingId,
                title,
                currentUid.equals(thread.getOwnerUid()),
                updatedAtMillis));
        publish();
    }

    private void publish() {
        if (callback == null) {
            return;
        }
        List<MarketplaceStatusNotice> result = new ArrayList<>(notices.values());
        result.sort((left, right) -> Long.compare(
                right.getUpdatedAtMillis(), left.getUpdatedAtMillis()));
        if (result.size() > DISPLAY_LIMIT) {
            result = new ArrayList<>(result.subList(0, DISPLAY_LIMIT));
        }
        boolean fromCache = threadsFromCache;
        for (Boolean cached : listingCacheState.values()) {
            fromCache = fromCache || Boolean.TRUE.equals(cached);
        }
        callback.onData(Collections.unmodifiableList(result), fromCache);
    }

    private void stop() {
        generation++;
        threadSubscription.remove();
        threadSubscription = ChatRepository.Subscription.NONE;
        for (MarketplaceRepository.Subscription subscription : listingSubscriptions.values()) {
            subscription.close();
        }
        listingSubscriptions.clear();
        listingThreads.clear();
        listingCacheState.clear();
        notices.clear();
        callback = null;
    }
}
