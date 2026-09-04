package com.propcycle.app.ui.common;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.propcycle.app.core.firebase.FirebaseEnvironment;
import com.propcycle.app.data.chat.ChatRepository;
import com.propcycle.app.data.chat.ChatThread;
import com.propcycle.app.data.lending.FirestoreLendingRepository;
import com.propcycle.app.data.lending.LendingRequest;
import com.propcycle.app.data.marketplace.MarketplaceStatusNotice;
import com.propcycle.app.data.marketplace.MarketplaceStatusNoticeRepository;

import java.util.Collections;
import java.util.List;

/** Derives local unread badges from the existing participant-only live data. */
public final class UnreadBadgeManager {

    public interface Listener {
        void onUnreadCounts(int notificationCount, int messageCount);
    }

    private static final String PREFERENCES = "propcycle_unread_badges";
    private static final String NOTIFICATIONS_SEEN = "notifications_seen_";
    private static final String MESSAGES_SEEN = "messages_seen_";

    private final Context context;
    private final SharedPreferences preferences;
    private final Listener listener;
    private final FirestoreLendingRepository lendingRepository;
    private final ChatRepository chatRepository;
    private final MarketplaceStatusNoticeRepository marketplaceNoticeRepository;

    private FirestoreLendingRepository.Subscription lendingSubscription =
            FirestoreLendingRepository.Subscription.NONE;
    private ChatRepository.Subscription chatSubscription = ChatRepository.Subscription.NONE;
    private MarketplaceStatusNoticeRepository.Subscription marketplaceSubscription =
            MarketplaceStatusNoticeRepository.Subscription.NONE;
    private List<LendingRequest> lendingRequests = Collections.emptyList();
    private List<MarketplaceStatusNotice> marketplaceNotices = Collections.emptyList();
    private List<ChatThread> chatThreads = Collections.emptyList();
    private String currentUserId = "";
    private boolean notificationsOpen;
    private boolean messagesOpen;

    public UnreadBadgeManager(@NonNull Context context, @NonNull Listener listener) {
        this.context = context.getApplicationContext();
        preferences = this.context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        this.listener = listener;
        lendingRepository = new FirestoreLendingRepository(this.context);
        chatRepository = new ChatRepository(this.context);
        marketplaceNoticeRepository = new MarketplaceStatusNoticeRepository(this.context);
    }

    /** Starts or replaces listeners when the authenticated account changes. */
    public void refreshUser() {
        FirebaseAuth auth = FirebaseEnvironment.auth(context);
        String userId = auth == null || auth.getCurrentUser() == null
                ? "" : auth.getCurrentUser().getUid();
        if (userId.equals(currentUserId)) {
            return;
        }
        stopSubscriptions();
        currentUserId = userId;
        lendingRequests = Collections.emptyList();
        marketplaceNotices = Collections.emptyList();
        chatThreads = Collections.emptyList();
        publish();
        if (userId.isEmpty()) {
            return;
        }

        lendingSubscription = lendingRepository.observeMyRequests(
                new FirestoreLendingRepository.SnapshotCallback<>() {
                    @Override
                    public void onData(
                            @NonNull List<LendingRequest> value,
                            boolean fromCache) {
                        lendingRequests = value;
                        publish();
                    }

                    @Override
                    public void onError(@NonNull Exception error) {
                        lendingRequests = Collections.emptyList();
                        publish();
                    }
                });
        marketplaceSubscription = marketplaceNoticeRepository.observe(
                new MarketplaceStatusNoticeRepository.Callback() {
                    @Override
                    public void onData(
                            @NonNull List<MarketplaceStatusNotice> notices,
                            boolean fromCache) {
                        marketplaceNotices = notices;
                        publish();
                    }

                    @Override
                    public void onError(@NonNull Exception error) {
                        marketplaceNotices = Collections.emptyList();
                        publish();
                    }
                });
        chatSubscription = chatRepository.listenToThreads(
                new ChatRepository.SnapshotCallback<>() {
                    @Override
                    public void onData(@NonNull List<ChatThread> value, boolean fromCache) {
                        chatThreads = value;
                        publish();
                    }

                    @Override
                    public void onError(@NonNull Exception error) {
                        chatThreads = Collections.emptyList();
                        publish();
                    }
                });
    }

    public void setOpenScreens(boolean notificationsOpen, boolean messagesOpen) {
        this.notificationsOpen = notificationsOpen;
        this.messagesOpen = messagesOpen;
        if (notificationsOpen) {
            saveSeen(NOTIFICATIONS_SEEN, newestNotificationMillis());
        }
        if (messagesOpen) {
            saveSeen(MESSAGES_SEEN, newestMessageMillis());
        }
        publish();
    }

    public void stop() {
        stopSubscriptions();
        currentUserId = "";
        lendingRequests = Collections.emptyList();
        marketplaceNotices = Collections.emptyList();
        chatThreads = Collections.emptyList();
    }

    private void publish() {
        if (currentUserId.isEmpty()) {
            listener.onUnreadCounts(0, 0);
            return;
        }
        long notificationsSeen = preferences.getLong(
                NOTIFICATIONS_SEEN + currentUserId, 0L);
        long messagesSeen = preferences.getLong(MESSAGES_SEEN + currentUserId, 0L);
        int notificationCount = 0;
        if (notificationsOpen) {
            saveSeen(NOTIFICATIONS_SEEN, newestNotificationMillis());
        } else {
            for (LendingRequest request : lendingRequests) {
                if (isNotificationForCurrentUser(request)
                        && timestampMillis(request.getUpdatedAt()) > notificationsSeen) {
                    notificationCount++;
                }
            }
            for (MarketplaceStatusNotice notice : marketplaceNotices) {
                if (!notice.isOwnerView() && notice.getUpdatedAtMillis() > notificationsSeen) {
                    notificationCount++;
                }
            }
        }
        int messageCount = 0;
        if (messagesOpen) {
            saveSeen(MESSAGES_SEEN, newestMessageMillis());
        } else {
            for (ChatThread thread : chatThreads) {
                if (thread.hasMessages()
                        && !currentUserId.equals(thread.getLastMessageSenderId())
                        && thread.getLastMessageAtMillis() > messagesSeen) {
                    messageCount++;
                }
            }
        }
        listener.onUnreadCounts(notificationCount, messageCount);
    }

    private boolean isNotificationForCurrentUser(@NonNull LendingRequest request) {
        String status = request.getStatus() == null ? "" : request.getStatus();
        boolean owner = currentUserId.equals(request.getOwnerUid());
        boolean borrower = currentUserId.equals(request.getBorrowerUid());
        return switch (status) {
            case "pending", "cancelled", "rated" -> owner;
            case "approved", "rejected", "returned" -> borrower;
            case "active" -> request.isReturnReported() ? owner : borrower;
            default -> false;
        };
    }

    private long newestNotificationMillis() {
        long newest = 0L;
        for (LendingRequest request : lendingRequests) {
            if (isNotificationForCurrentUser(request)) {
                newest = Math.max(newest, timestampMillis(request.getUpdatedAt()));
            }
        }
        for (MarketplaceStatusNotice notice : marketplaceNotices) {
            if (!notice.isOwnerView()) {
                newest = Math.max(newest, notice.getUpdatedAtMillis());
            }
        }
        return newest;
    }

    private long newestMessageMillis() {
        long newest = 0L;
        for (ChatThread thread : chatThreads) {
            newest = Math.max(newest, thread.getLastMessageAtMillis());
        }
        return newest;
    }

    private void stopSubscriptions() {
        lendingSubscription.remove();
        lendingSubscription = FirestoreLendingRepository.Subscription.NONE;
        marketplaceSubscription.remove();
        marketplaceSubscription = MarketplaceStatusNoticeRepository.Subscription.NONE;
        chatSubscription.remove();
        chatSubscription = ChatRepository.Subscription.NONE;
    }

    private void saveSeen(@NonNull String keyPrefix, long newestMillis) {
        if (currentUserId.isEmpty()) {
            return;
        }
        long seen = Math.max(System.currentTimeMillis(), newestMillis);
        preferences.edit().putLong(keyPrefix + currentUserId, seen).apply();
    }

    private static long timestampMillis(Timestamp timestamp) {
        return timestamp == null ? 0L : timestamp.toDate().getTime();
    }
}
