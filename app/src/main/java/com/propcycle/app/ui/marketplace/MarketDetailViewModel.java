package com.propcycle.app.ui.marketplace;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.propcycle.app.data.chat.ChatRepository;
import com.propcycle.app.data.marketplace.FirestoreMarketplaceRepository;
import com.propcycle.app.data.marketplace.MarketplaceListing;
import com.propcycle.app.data.marketplace.MarketplaceListingStatusPolicy;
import com.propcycle.app.data.marketplace.MarketplaceRepository;

/** Owns the live detail document plus owner edit/withdraw/relist actions. */
public final class MarketDetailViewModel extends AndroidViewModel {

    private final MarketplaceRepository repository;
    private final MutableLiveData<State> state = new MutableLiveData<>(State.loading());
    private final MutableLiveData<String> chatNotice = new MutableLiveData<>();
    private final MutableLiveData<OwnerActionState> ownerActionState =
            new MutableLiveData<>(OwnerActionState.idle());
    private final MutableLiveData<Event<String>> openedThread = new MutableLiveData<>();
    private MarketplaceRepository.Subscription subscription;
    private String loadedListingId;
    private boolean openingChat;

    public MarketDetailViewModel(@NonNull Application application) {
        super(application);
        repository = new FirestoreMarketplaceRepository(application);
    }

    @NonNull
    public LiveData<State> getState() {
        return state;
    }

    @NonNull
    public LiveData<String> getChatNotice() {
        return chatNotice;
    }

    @NonNull
    public LiveData<OwnerActionState> getOwnerActionState() {
        return ownerActionState;
    }

    @NonNull
    public LiveData<Event<String>> getOpenedThread() {
        return openedThread;
    }

    public void load(@Nullable String listingId) {
        String resolvedId = listingId == null ? "" : listingId.trim();
        if (resolvedId.equals(loadedListingId)) {
            return;
        }
        loadedListingId = resolvedId;
        closeSubscription();

        if (resolvedId.isEmpty()) {
            state.setValue(State.error(
                    MarketplaceRepository.ErrorType.NOT_FOUND,
                    "Choose a marketplace listing to view its details."));
            return;
        }

        state.setValue(State.loading());
        subscription = repository.observeListing(
                resolvedId,
                new MarketplaceRepository.ListingObserver() {
                    @Override
                    public void onListing(
                            @Nullable MarketplaceListing listing,
                            boolean fromCache) {
                        if (listing == null) {
                            state.setValue(State.error(
                                    MarketplaceRepository.ErrorType.NOT_FOUND,
                                    fromCache
                                            ? "This listing is not in the offline cache."
                                            : "This marketplace listing no longer exists."));
                            return;
                        }
                        String currentUserId = repository.currentUserId();
                        boolean owner = currentUserId != null
                                && currentUserId.equals(listing.getOwnerId());
                        state.setValue(State.content(listing, owner, fromCache));
                    }

                    @Override
                    public void onError(@NonNull MarketplaceRepository.RepositoryError error) {
                        state.setValue(State.error(error.getType(), error.getMessage()));
                    }
                });
    }

    public void requestStatusChange(@NonNull String targetStatus) {
        OwnerActionState action = ownerActionState.getValue();
        State current = state.getValue();
        if (action != null && action.isBusy()) {
            return;
        }
        if (current == null || current.getListing() == null || !current.isOwner()) {
            ownerActionState.setValue(OwnerActionState.error(
                    "Only the listing owner can change availability."));
            return;
        }

        MarketplaceListing listing = current.getListing();
        boolean allowed = MarketplaceListingStatusPolicy.AVAILABLE.equals(targetStatus)
                ? MarketplaceListingStatusPolicy.canRelist(true, listing.getStatus())
                : MarketplaceListingStatusPolicy.WITHDRAWN.equals(targetStatus)
                        && MarketplaceListingStatusPolicy.canWithdraw(true, listing.getStatus());
        if (!allowed) {
            ownerActionState.setValue(OwnerActionState.error(
                    "The listing changed. Review its latest status and try again."));
            return;
        }
        if (listing.getId() == null || listing.getUpdatedAt() == null) {
            ownerActionState.setValue(OwnerActionState.error(
                    "Reopen this listing before changing its availability."));
            return;
        }

        ownerActionState.setValue(OwnerActionState.loading(
                MarketplaceListingStatusPolicy.WITHDRAWN.equals(targetStatus)
                        ? "Withdrawing listing..."
                        : "Relisting item..."));
        repository.setListingStatus(
                listing.getId(),
                targetStatus,
                listing.getUpdatedAt(),
                new MarketplaceRepository.MutationCallback() {
                    @Override
                    public void onUpdated() {
                        ownerActionState.setValue(OwnerActionState.success(
                                MarketplaceListingStatusPolicy.WITHDRAWN.equals(targetStatus)
                                        ? "Listing withdrawn. It is hidden from public browse."
                                        : "Listing relisted. People can find it again."));
                    }

                    @Override
                    public void onError(@NonNull MarketplaceRepository.RepositoryError error) {
                        ownerActionState.setValue(OwnerActionState.error(error.getMessage()));
                    }
                });
    }

    public void requestChat() {
        State current = state.getValue();
        if (current == null || current.getListing() == null || openingChat) {
            return;
        }
        MarketplaceListing listing = current.getListing();
        if (!MarketplaceListingStatusPolicy.canContactSeller(
                current.isOwner(), listing.getStatus())) {
            chatNotice.setValue(current.isOwner()
                    ? "Use the owner controls to manage this listing."
                    : "This listing is not available for a new conversation.");
            return;
        }
        String listingId = listing.getId();
        String ownerId = listing.getOwnerId();
        String title = listing.getTitle();
        if (listingId == null || ownerId == null || title == null) {
            chatNotice.setValue("This listing does not have enough information to open chat.");
            return;
        }

        openingChat = true;
        chatNotice.setValue("Opening conversation...");
        ChatRepository.createOrGetMarketplaceThread(
                        getApplication(),
                        listingId,
                        ownerId,
                        title)
                .addOnSuccessListener(threadId -> {
                    openingChat = false;
                    chatNotice.setValue("");
                    openedThread.setValue(new Event<>(threadId));
                })
                .addOnFailureListener(error -> {
                    openingChat = false;
                    String detail = error.getMessage();
                    chatNotice.setValue(detail == null || detail.trim().isEmpty()
                            ? "Could not open marketplace chat. Please try again."
                            : detail);
                });
    }

    @Override
    protected void onCleared() {
        closeSubscription();
        super.onCleared();
    }

    private void closeSubscription() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }

    public static final class State {

        public enum Kind {
            LOADING,
            CONTENT,
            ERROR,
            CONFIGURATION_REQUIRED,
            AUTHENTICATION_REQUIRED,
            NOT_FOUND
        }

        private final Kind kind;
        private final MarketplaceListing listing;
        private final String message;
        private final boolean owner;
        private final boolean fromCache;

        private State(
                @NonNull Kind kind,
                @Nullable MarketplaceListing listing,
                @NonNull String message,
                boolean owner,
                boolean fromCache) {
            this.kind = kind;
            this.listing = listing;
            this.message = message;
            this.owner = owner;
            this.fromCache = fromCache;
        }

        private static State loading() {
            return new State(Kind.LOADING, null, "", false, false);
        }

        private static State content(
                @NonNull MarketplaceListing listing,
                boolean owner,
                boolean fromCache) {
            return new State(Kind.CONTENT, listing, "", owner, fromCache);
        }

        private static State error(
                @NonNull MarketplaceRepository.ErrorType type,
                @NonNull String message) {
            Kind kind = switch (type) {
                case CONFIGURATION_REQUIRED -> Kind.CONFIGURATION_REQUIRED;
                case AUTHENTICATION_REQUIRED -> Kind.AUTHENTICATION_REQUIRED;
                case NOT_FOUND -> Kind.NOT_FOUND;
                default -> Kind.ERROR;
            };
            return new State(kind, null, message, false, false);
        }

        @NonNull
        public Kind getKind() {
            return kind;
        }

        @Nullable
        public MarketplaceListing getListing() {
            return listing;
        }

        @NonNull
        public String getMessage() {
            return message;
        }

        public boolean isOwner() {
            return owner;
        }

        public boolean isFromCache() {
            return fromCache;
        }
    }

    public static final class OwnerActionState {

        public enum Kind {
            IDLE,
            LOADING,
            SUCCESS,
            ERROR
        }

        private final Kind kind;
        private final String message;

        private OwnerActionState(@NonNull Kind kind, @NonNull String message) {
            this.kind = kind;
            this.message = message;
        }

        private static OwnerActionState idle() {
            return new OwnerActionState(Kind.IDLE, "");
        }

        private static OwnerActionState loading(@NonNull String message) {
            return new OwnerActionState(Kind.LOADING, message);
        }

        private static OwnerActionState success(@NonNull String message) {
            return new OwnerActionState(Kind.SUCCESS, message);
        }

        private static OwnerActionState error(@NonNull String message) {
            return new OwnerActionState(Kind.ERROR, message);
        }

        @NonNull
        public Kind getKind() {
            return kind;
        }

        @NonNull
        public String getMessage() {
            return message;
        }

        public boolean isBusy() {
            return kind == Kind.LOADING;
        }
    }

    /** One-shot navigation result for a successfully created or recovered chat thread. */
    public static final class Event<T> {
        private final T value;
        private boolean handled;

        private Event(@NonNull T value) {
            this.value = value;
        }

        @Nullable
        public T getIfNotHandled() {
            if (handled) {
                return null;
            }
            handled = true;
            return value;
        }
    }
}
