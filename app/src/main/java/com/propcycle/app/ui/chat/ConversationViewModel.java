package com.propcycle.app.ui.chat;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Task;
import com.propcycle.app.core.firebase.FirebaseEnvironment;
import com.propcycle.app.data.activity.ActivityLogRepository;
import com.propcycle.app.data.chat.ChatMessage;
import com.propcycle.app.data.chat.ChatRepository;
import com.propcycle.app.data.chat.ChatThread;
import com.propcycle.app.data.chat.ChatValidator;
import com.propcycle.app.data.chat.ChatParticipantPolicy;
import com.propcycle.app.data.profile.ProfileAvatarPolicy;
import com.propcycle.app.data.profile.PublicProfile;
import com.propcycle.app.data.profile.PublicProfileRepository;
import com.propcycle.app.data.marketplace.FirestoreMarketplaceRepository;
import com.propcycle.app.data.marketplace.MarketplaceListing;
import com.propcycle.app.data.marketplace.MarketplaceRepository;
import com.propcycle.app.data.lending.FirestoreLendingRepository;
import com.propcycle.app.data.lending.LendingItem;
import com.propcycle.app.data.lending.LendingRequest;
import com.propcycle.app.data.lending.LendingRequestActionPolicy;
import com.propcycle.app.data.lending.LendingRequestActionExecutor;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Owns one thread, its bounded message listener, and duplicate-safe sending state. */
public final class ConversationViewModel extends AndroidViewModel {

    private final ChatRepository repository;
    private final PublicProfileRepository profileRepository;
    private final MarketplaceRepository marketplaceRepository;
    private final FirestoreLendingRepository lendingRepository;
    private final ActivityLogRepository activityLog;
    private final MutableLiveData<ConversationUiState> state =
            new MutableLiveData<>(ConversationUiState.loading());
    private final MutableLiveData<UiEvent<Boolean>> sendSucceeded = new MutableLiveData<>();
    private final MutableLiveData<PublicProfile> otherProfile = new MutableLiveData<>();

    private ChatRepository.Subscription threadSubscription = ChatRepository.Subscription.NONE;
    private ChatRepository.Subscription messageSubscription = ChatRepository.Subscription.NONE;
    private MarketplaceRepository.Subscription marketplaceSubscription;
    private FirestoreLendingRepository.Subscription lendingItemSubscription =
            FirestoreLendingRepository.Subscription.NONE;
    private final Map<String, FirestoreLendingRepository.Subscription>
            lendingRequestSubscriptions = new HashMap<>();
    private String activeThreadId = "";
    private int listenerGeneration;
    private boolean threadLoaded;
    private boolean messagesLoaded;
    private boolean threadFromCache;
    private boolean messagesFromCache;
    private String threadError;
    private String messagesError;
    private String actionError;
    private ChatThread thread;
    private List<ChatMessage> messages = Collections.emptyList();
    private String pendingOperationId;
    private String pendingText;
    private MarketplaceListing marketplaceListing;
    private boolean marketplaceListingLoading;
    private boolean marketplaceListingFromCache;
    private String observedMarketplaceListingId = "";
    private final Map<String, LendingRequest> lendingRequests = new HashMap<>();
    private final Set<String> loadingLendingRequestIds = new HashSet<>();
    private final Map<String, Boolean> lendingRequestCacheState = new HashMap<>();
    private LendingItem lendingItem;
    private boolean lendingItemFromCache;
    private String observedLendingItemId = "";
    private String busyLendingRequestId = "";

    public ConversationViewModel(@NonNull Application application) {
        super(application);
        repository = new ChatRepository(application);
        profileRepository = new PublicProfileRepository(application);
        marketplaceRepository = new FirestoreMarketplaceRepository(application);
        lendingRepository = new FirestoreLendingRepository(application);
        activityLog = new ActivityLogRepository(application);
    }

    @NonNull
    public LiveData<ConversationUiState> getState() {
        return state;
    }

    @NonNull
    public LiveData<UiEvent<Boolean>> getSendSucceeded() {
        return sendSucceeded;
    }

    @NonNull
    public LiveData<PublicProfile> getOtherProfile() {
        return otherProfile;
    }

    @NonNull
    public String currentUserId() {
        String uid = repository.currentUserId();
        return uid == null ? "" : uid;
    }

    public void start(@NonNull String threadId) {
        stop();
        String validationError = ChatValidator.threadIdError(threadId);
        if (validationError != null) {
            state.setValue(new ConversationUiState(
                    false, false, false, false, validationError, null, Collections.emptyList()));
            return;
        }
        if (!repository.isConfigured()) {
            state.setValue(new ConversationUiState(
                    false,
                    true,
                    false,
                    false,
                    FirebaseEnvironment.SETUP_MESSAGE,
                    null,
                    Collections.emptyList()));
            return;
        }
        if (repository.currentUserId() == null) {
            state.setValue(new ConversationUiState(
                    false,
                    false,
                    false,
                    false,
                    "Sign in to access this conversation.",
                    null,
                    Collections.emptyList()));
            return;
        }

        activeThreadId = threadId.trim();
        threadLoaded = false;
        messagesLoaded = false;
        threadFromCache = false;
        messagesFromCache = false;
        threadError = null;
        messagesError = null;
        actionError = null;
        thread = null;
        otherProfile.setValue(null);
        messages = Collections.emptyList();
        marketplaceListing = null;
        marketplaceListingLoading = false;
        marketplaceListingFromCache = false;
        observedMarketplaceListingId = "";
        lendingRequests.clear();
        loadingLendingRequestIds.clear();
        lendingRequestCacheState.clear();
        lendingItem = null;
        lendingItemFromCache = false;
        observedLendingItemId = "";
        busyLendingRequestId = "";
        state.setValue(ConversationUiState.loading());
        int generation = ++listenerGeneration;

        threadSubscription = repository.listenToThread(
                activeThreadId,
                new ChatRepository.SnapshotCallback<>() {
                    @Override
                    public void onData(@NonNull ChatThread value, boolean fromCache) {
                        if (generation != listenerGeneration) {
                            return;
                        }
                        thread = value;
                        threadLoaded = true;
                        threadFromCache = fromCache;
                        threadError = null;
                        publish(isSending());
                        loadOtherProfile(generation, value);
                        observeMarketplaceListing(generation, value);
                        observeLendingItem(generation, value);
                    }

                    @Override
                    public void onError(@NonNull Exception error) {
                        if (generation == listenerGeneration) {
                            threadError = ChatUiError.message(error);
                            publish(isSending());
                        }
                    }
                });

        messageSubscription = repository.listenToMessages(
                activeThreadId,
                new ChatRepository.SnapshotCallback<>() {
                    @Override
                    public void onData(@NonNull List<ChatMessage> value, boolean fromCache) {
                        if (generation != listenerGeneration) {
                            return;
                        }
                        messages = value;
                        messagesLoaded = true;
                        messagesFromCache = fromCache;
                        messagesError = null;
                        syncLendingRequestObservers(generation, value);
                        publish(isSending());
                    }

                    @Override
                    public void onError(@NonNull Exception error) {
                        if (generation == listenerGeneration) {
                            messagesError = ChatUiError.message(error);
                            publish(isSending());
                        }
                    }
                });
    }

    private void observeLendingItem(int generation, @NonNull ChatThread value) {
        if (!"lending".equals(value.getContextType())) {
            lendingItemSubscription.remove();
            lendingItemSubscription = FirestoreLendingRepository.Subscription.NONE;
            observedLendingItemId = "";
            lendingItem = null;
            lendingItemFromCache = false;
            return;
        }
        String itemId = value.getContextId().trim();
        if (itemId.isEmpty() || itemId.equals(observedLendingItemId)) {
            return;
        }
        lendingItemSubscription.remove();
        observedLendingItemId = itemId;
        lendingItem = null;
        lendingItemFromCache = false;
        lendingItemSubscription = lendingRepository.observeItem(
                itemId,
                new FirestoreLendingRepository.SnapshotCallback<>() {
                    @Override
                    public void onData(@NonNull LendingItem value, boolean fromCache) {
                        if (generation != listenerGeneration
                                || !itemId.equals(observedLendingItemId)) {
                            return;
                        }
                        lendingItem = value;
                        lendingItemFromCache = fromCache;
                        publish(isSending());
                    }

                    @Override
                    public void onError(@NonNull Exception error) {
                        if (generation == listenerGeneration
                                && itemId.equals(observedLendingItemId)) {
                            lendingItem = null;
                            lendingItemFromCache = false;
                            publish(isSending());
                        }
                    }
                });
    }

    private void syncLendingRequestObservers(
            int generation,
            @NonNull List<ChatMessage> currentMessages) {
        Set<String> currentRequestIds = new HashSet<>();
        for (ChatMessage message : currentMessages) {
            if (message.isLendingRequest()) {
                currentRequestIds.add(message.getRequestId());
            }
        }

        Set<String> removed = new HashSet<>(lendingRequestSubscriptions.keySet());
        removed.removeAll(currentRequestIds);
        for (String requestId : removed) {
            FirestoreLendingRepository.Subscription subscription =
                    lendingRequestSubscriptions.remove(requestId);
            if (subscription != null) {
                subscription.remove();
            }
            lendingRequests.remove(requestId);
            loadingLendingRequestIds.remove(requestId);
            lendingRequestCacheState.remove(requestId);
        }

        for (String requestId : currentRequestIds) {
            if (lendingRequestSubscriptions.containsKey(requestId)) {
                continue;
            }
            loadingLendingRequestIds.add(requestId);
            FirestoreLendingRepository.Subscription subscription =
                    lendingRepository.observeRequest(
                            requestId,
                            new FirestoreLendingRepository.SnapshotCallback<>() {
                                @Override
                                public void onData(
                                        @NonNull LendingRequest value,
                                        boolean fromCache) {
                                    if (generation != listenerGeneration
                                            || !lendingRequestSubscriptions.containsKey(
                                                    requestId)) {
                                        return;
                                    }
                                    lendingRequests.put(requestId, value);
                                    loadingLendingRequestIds.remove(requestId);
                                    lendingRequestCacheState.put(requestId, fromCache);
                                    publish(isSending());
                                }

                                @Override
                                public void onError(@NonNull Exception error) {
                                    if (generation != listenerGeneration
                                            || !lendingRequestSubscriptions.containsKey(
                                                    requestId)) {
                                        return;
                                    }
                                    lendingRequests.remove(requestId);
                                    loadingLendingRequestIds.remove(requestId);
                                    lendingRequestCacheState.remove(requestId);
                                    publish(isSending());
                                }
                            });
            lendingRequestSubscriptions.put(requestId, subscription);
        }
    }

    private void observeMarketplaceListing(int generation, @NonNull ChatThread value) {
        if (!"marketplace".equals(value.getContextType())) {
            closeMarketplaceSubscription();
            return;
        }
        String listingId = value.getContextId().trim();
        if (listingId.isEmpty() || listingId.equals(observedMarketplaceListingId)) {
            return;
        }
        closeMarketplaceSubscription();
        observedMarketplaceListingId = listingId;
        marketplaceListing = null;
        marketplaceListingLoading = true;
        marketplaceListingFromCache = false;
        publish(isSending());
        marketplaceSubscription = marketplaceRepository.observeListing(
                listingId,
                new MarketplaceRepository.ListingObserver() {
                    @Override
                    public void onListing(
                            @androidx.annotation.Nullable MarketplaceListing listing,
                            boolean fromCache) {
                        if (generation != listenerGeneration
                                || !listingId.equals(observedMarketplaceListingId)) {
                            return;
                        }
                        marketplaceListing = listing;
                        marketplaceListingLoading = false;
                        marketplaceListingFromCache = fromCache;
                        publish(isSending());
                    }

                    @Override
                    public void onError(@NonNull MarketplaceRepository.RepositoryError error) {
                        if (generation != listenerGeneration
                                || !listingId.equals(observedMarketplaceListingId)) {
                            return;
                        }
                        marketplaceListing = null;
                        marketplaceListingLoading = false;
                        marketplaceListingFromCache = false;
                        publish(isSending());
                    }
                });
    }

    private void loadOtherProfile(int generation, @NonNull ChatThread value) {
        String userId = ChatParticipantPolicy.otherUserId(value, currentUserId());
        if (userId.isEmpty()) {
            otherProfile.setValue(null);
            return;
        }
        profileRepository.get(userId)
                .addOnSuccessListener(profile -> {
                    if (generation == listenerGeneration) {
                        otherProfile.setValue(profile);
                    }
                })
                .addOnFailureListener(error -> {
                    if (generation == listenerGeneration) {
                        otherProfile.setValue(new PublicProfile(
                                userId,
                                "PropCycle Member",
                                ProfileAvatarPolicy.DEFAULT));
                    }
                });
    }

    public void sendMessage(@NonNull String value) {
        ConversationUiState current = state.getValue();
        if (current != null && current.isSending()) {
            return;
        }
        String validationError = ChatValidator.messageError(value);
        if (validationError != null) {
            actionError = validationError;
            publish(false);
            return;
        }
        if (activeThreadId.isEmpty() || thread == null) {
            actionError = "Wait for the conversation to finish loading.";
            publish(false);
            return;
        }

        String cleanText = value.trim();
        if (!cleanText.equals(pendingText) || pendingOperationId == null) {
            pendingText = cleanText;
            pendingOperationId = UUID.randomUUID().toString();
        }
        String operationId = pendingOperationId;
        actionError = null;
        publish(true);
        repository.sendMessage(activeThreadId, cleanText, operationId)
                .addOnSuccessListener(ignored -> {
                    pendingOperationId = null;
                    pendingText = null;
                    actionError = null;
                    publish(false);
                    sendSucceeded.setValue(new UiEvent<>(Boolean.TRUE));
                })
                .addOnFailureListener(error -> {
                    actionError = ChatUiError.message(error);
                    publish(false);
                });
    }

    public void performLendingAction(
            @NonNull LendingRequest request,
            @NonNull LendingRequestActionPolicy.Action action) {
        LendingRequest current = lendingRequests.get(request.getId());
        if (!busyLendingRequestId.isEmpty()
                || !LendingRequestActionPolicy.isAllowed(current, currentUserId(), action)
                || action == LendingRequestActionPolicy.Action.RATE) {
            return;
        }
        Task<Void> task = LendingRequestActionExecutor.execute(lendingRepository, current, action);
        busyLendingRequestId = current.getId();
        actionError = null;
        publish(isSending());
        task.addOnSuccessListener(ignored -> {
                    recordLendingAction(current, action);
                    busyLendingRequestId = "";
                    actionError = null;
                    publish(isSending());
                })
                .addOnFailureListener(error -> {
                    busyLendingRequestId = "";
                    actionError = ChatUiError.message(error);
                    publish(isSending());
                });
    }

    public void rateLendingRequest(
            @NonNull LendingRequest request,
            int score,
            @androidx.annotation.Nullable String comment) {
        LendingRequest current = lendingRequests.get(request.getId());
        LendingRequestActionPolicy.Action action = LendingRequestActionPolicy.Action.RATE;
        if (!busyLendingRequestId.isEmpty()
                || !LendingRequestActionPolicy.isAllowed(current, currentUserId(), action)) {
            return;
        }
        busyLendingRequestId = current.getId();
        actionError = null;
        publish(isSending());
        lendingRepository.rate(current.getId(), score, comment)
                .addOnSuccessListener(ignored -> {
                    recordLendingAction(current, action);
                    busyLendingRequestId = "";
                    actionError = null;
                    publish(isSending());
                })
                .addOnFailureListener(error -> {
                    busyLendingRequestId = "";
                    actionError = ChatUiError.message(error);
                    publish(isSending());
                });
    }

    private void recordLendingAction(
            @NonNull LendingRequest request,
            @NonNull LendingRequestActionPolicy.Action action) {
        activityLog.record(
                ActivityLogRepository.TYPE_LENDING_STATUS,
                LendingRequestActionPolicy.activityTitle(action),
                request.getItemTitle() == null ? "Lending item" : request.getItemTitle(),
                ActivityLogRepository.DESTINATION_LENDING_REQUESTS,
                request.getId());
    }

    public void stop() {
        listenerGeneration++;
        threadSubscription.remove();
        messageSubscription.remove();
        closeMarketplaceSubscription();
        closeLendingSubscriptions();
        threadSubscription = ChatRepository.Subscription.NONE;
        messageSubscription = ChatRepository.Subscription.NONE;
    }

    private void closeMarketplaceSubscription() {
        if (marketplaceSubscription != null) {
            marketplaceSubscription.close();
            marketplaceSubscription = null;
        }
        observedMarketplaceListingId = "";
    }

    private void closeLendingSubscriptions() {
        lendingItemSubscription.remove();
        lendingItemSubscription = FirestoreLendingRepository.Subscription.NONE;
        for (FirestoreLendingRepository.Subscription subscription
                : lendingRequestSubscriptions.values()) {
            subscription.remove();
        }
        lendingRequestSubscriptions.clear();
        observedLendingItemId = "";
    }

    private boolean isSending() {
        ConversationUiState current = state.getValue();
        return current != null && current.isSending();
    }

    private void publish(boolean sending) {
        String errorMessage = actionError != null
                ? actionError
                : (threadError != null ? threadError : messagesError);
        boolean lendingFromCache = lendingItemFromCache;
        for (Boolean cached : lendingRequestCacheState.values()) {
            lendingFromCache = lendingFromCache || Boolean.TRUE.equals(cached);
        }
        state.setValue(new ConversationUiState(
                errorMessage == null && !(threadLoaded && messagesLoaded),
                false,
                threadFromCache || messagesFromCache || marketplaceListingFromCache
                        || lendingFromCache,
                sending,
                errorMessage,
                thread,
                messages,
                marketplaceListing,
                marketplaceListingLoading,
                lendingRequests,
                loadingLendingRequestIds,
                lendingItem,
                busyLendingRequestId));
    }

    @Override
    protected void onCleared() {
        stop();
    }
}
