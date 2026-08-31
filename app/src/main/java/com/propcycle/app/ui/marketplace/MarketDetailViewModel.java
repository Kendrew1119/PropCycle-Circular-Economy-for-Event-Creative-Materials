package com.propcycle.app.ui.marketplace;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.propcycle.app.data.chat.ChatRepository;
import com.propcycle.app.data.activity.ActivityLogRepository;
import com.propcycle.app.data.marketplace.FirestoreMarketplaceRepository;
import com.propcycle.app.data.marketplace.FirestoreMarketplaceRatingRepository;
import com.propcycle.app.data.marketplace.MarketplaceListing;
import com.propcycle.app.data.marketplace.MarketplaceListingStatusPolicy;
import com.propcycle.app.data.marketplace.MarketplaceRatingPolicy;
import com.propcycle.app.data.marketplace.MarketplaceRepository;
import com.propcycle.app.data.marketplace.MarketplaceSellerRating;
import com.propcycle.app.core.firebase.FirebaseEnvironment;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Collections;
import java.util.List;

/** Owns the live detail document plus owner edit/withdraw/relist actions. */
public final class MarketDetailViewModel extends AndroidViewModel {

    private final MarketplaceRepository repository;
    private final FirestoreMarketplaceRatingRepository ratingRepository;
    private final ActivityLogRepository activityLog;
    private final MutableLiveData<State> state = new MutableLiveData<>(State.loading());
    private final MutableLiveData<String> chatNotice = new MutableLiveData<>();
    private final MutableLiveData<OwnerActionState> ownerActionState =
            new MutableLiveData<>(OwnerActionState.idle());
    private final MutableLiveData<Event<String>> openedThread = new MutableLiveData<>();
    private final MutableLiveData<String> sellerName =
            new MutableLiveData<>("Community member");
    private final MutableLiveData<RatingState> ratingState =
            new MutableLiveData<>(RatingState.empty());
    private MarketplaceRepository.Subscription subscription;
    private FirestoreMarketplaceRatingRepository.Subscription ratingSubscription =
            FirestoreMarketplaceRatingRepository.Subscription.NONE;
    private String loadedListingId;
    private boolean openingChat;
    private String loadedSellerId = "";
    private String loadedRatingsSellerId = "";
    private List<MarketplaceSellerRating> sellerRatings = Collections.emptyList();
    private boolean ratingFromCache;
    private boolean ratingSaving;
    private String ratingMessage = "";

    public MarketDetailViewModel(@NonNull Application application) {
        super(application);
        repository = new FirestoreMarketplaceRepository(application);
        ratingRepository = new FirestoreMarketplaceRatingRepository(application);
        activityLog = new ActivityLogRepository(application);
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

    @NonNull
    public LiveData<String> getSellerName() {
        return sellerName;
    }

    @NonNull
    public LiveData<RatingState> getRatingState() {
        return ratingState;
    }

    public void load(@Nullable String listingId) {
        String resolvedId = listingId == null ? "" : listingId.trim();
        if (resolvedId.equals(loadedListingId)) {
            return;
        }
        loadedListingId = resolvedId;
        closeSubscription();
        closeRatingSubscription();
        loadedSellerId = "";
        loadedRatingsSellerId = "";
        sellerRatings = Collections.emptyList();
        ratingMessage = "";
        publishRatingState();

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
                        loadSellerName(listing.getOwnerId());
                        observeSellerRatings(listing.getOwnerId());
                        state.setValue(State.content(listing, owner, fromCache));
                    }

                    @Override
                    public void onError(@NonNull MarketplaceRepository.RepositoryError error) {
                        state.setValue(State.error(error.getType(), error.getMessage()));
                    }
                });
    }

    private void loadSellerName(@Nullable String ownerId) {
        String cleanId = ownerId == null ? "" : ownerId.trim();
        if (cleanId.isEmpty() || cleanId.equals(loadedSellerId)) {
            return;
        }
        loadedSellerId = cleanId;
        sellerName.setValue("Community member");
        FirebaseFirestore firestore = FirebaseEnvironment.firestore(getApplication());
        if (firestore == null) {
            return;
        }
        firestore.collection("users").document(cleanId).get()
                .addOnSuccessListener(snapshot -> {
                    if (!cleanId.equals(loadedSellerId) || !snapshot.exists()) {
                        return;
                    }
                    String displayName = snapshot.getString("displayName");
                    if (displayName != null && !displayName.trim().isEmpty()) {
                        sellerName.setValue(displayName.trim());
                    }
                });
    }

    private void observeSellerRatings(@Nullable String ownerId) {
        String cleanId = ownerId == null ? "" : ownerId.trim();
        if (cleanId.isEmpty()) {
            return;
        }
        if (cleanId.equals(loadedRatingsSellerId)) {
            return;
        }
        closeRatingSubscription();
        loadedRatingsSellerId = cleanId;
        sellerRatings = Collections.emptyList();
        ratingFromCache = false;
        publishRatingState();
        ratingSubscription = ratingRepository.observeRatings(
                cleanId,
                new FirestoreMarketplaceRatingRepository.RatingsCallback() {
                    @Override
                    public void onData(
                            @NonNull List<MarketplaceSellerRating> ratings,
                            boolean fromCache) {
                        if (!cleanId.equals(loadedSellerId)) {
                            return;
                        }
                        sellerRatings = ratings;
                        ratingFromCache = fromCache;
                        publishRatingState();
                    }

                    @Override
                    public void onError(@NonNull Exception error) {
                        if (cleanId.equals(loadedSellerId)) {
                            ratingMessage = error.getMessage() == null
                                    ? "Marketplace ratings are unavailable."
                                    : error.getMessage();
                            publishRatingState();
                        }
                    }
                });
    }

    public void saveSellerRating(int score) {
        State current = state.getValue();
        MarketplaceListing listing = current == null ? null : current.getListing();
        if (ratingSaving || listing == null || current.isOwner()
                || listing.getId() == null || listing.getOwnerId() == null) {
            return;
        }
        if (!MarketplaceListingStatusPolicy.AVAILABLE.equals(listing.getStatus())) {
            ratingMessage = "Only an available listing can be used to rate its seller.";
            publishRatingState();
            return;
        }
        try {
            MarketplaceRatingPolicy.requireScore(score);
        } catch (IllegalArgumentException error) {
            ratingMessage = error.getMessage();
            publishRatingState();
            return;
        }
        ratingSaving = true;
        ratingMessage = "Saving your marketplace rating...";
        publishRatingState();
        ratingRepository.saveRating(listing.getOwnerId(), listing.getId(), score)
                .addOnSuccessListener(ignored -> {
                    ratingSaving = false;
                    ratingMessage = "Your marketplace rating was saved.";
                    publishRatingState();
                })
                .addOnFailureListener(error -> {
                    ratingSaving = false;
                    ratingMessage = error.getMessage() == null
                            ? "The marketplace rating could not be saved."
                            : error.getMessage();
                    publishRatingState();
                });
    }

    private void publishRatingState() {
        MarketplaceRatingPolicy.Summary summary =
                MarketplaceRatingPolicy.summarize(sellerRatings);
        int myScore = 0;
        String currentUid = ratingRepository.currentUserId();
        if (currentUid != null) {
            for (MarketplaceSellerRating rating : sellerRatings) {
                if (currentUid.equals(rating.getRaterUid()) && rating.getScore() != null) {
                    myScore = rating.getScore().intValue();
                    break;
                }
            }
        }
        ratingState.setValue(new RatingState(
                summary.getAverage(),
                summary.getCount(),
                myScore,
                ratingSaving,
                ratingFromCache,
                ratingMessage));
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
        boolean allowed = switch (targetStatus) {
            case MarketplaceListingStatusPolicy.AVAILABLE ->
                    MarketplaceListingStatusPolicy.canRelist(true, listing.getStatus());
            case MarketplaceListingStatusPolicy.WITHDRAWN ->
                    MarketplaceListingStatusPolicy.canWithdraw(true, listing.getStatus());
            case MarketplaceListingStatusPolicy.SOLD ->
                    MarketplaceListingStatusPolicy.canMarkSold(true, listing.getStatus());
            default -> false;
        };
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

        String workingMessage = switch (targetStatus) {
            case MarketplaceListingStatusPolicy.WITHDRAWN -> "Withdrawing listing...";
            case MarketplaceListingStatusPolicy.SOLD -> "Marking listing as sold...";
            default -> "Relisting item...";
        };
        ownerActionState.setValue(OwnerActionState.loading(workingMessage));
        repository.setListingStatus(
                listing.getId(),
                targetStatus,
                listing.getUpdatedAt(),
                new MarketplaceRepository.MutationCallback() {
                    @Override
                    public void onUpdated() {
                        boolean sold = MarketplaceListingStatusPolicy.SOLD.equals(targetStatus);
                        boolean withdrawn = MarketplaceListingStatusPolicy.WITHDRAWN.equals(
                                targetStatus);
                        activityLog.record(
                                ActivityLogRepository.TYPE_MARKETPLACE_STATUS,
                                sold
                                        ? "Marketplace listing sold"
                                        : withdrawn
                                                ? "Marketplace listing withdrawn"
                                                : "Marketplace listing relisted",
                                listing.getTitle() == null
                                        ? "Marketplace item" : listing.getTitle(),
                                ActivityLogRepository.DESTINATION_MARKETPLACE,
                                listing.getId());
                        ownerActionState.setValue(OwnerActionState.success(sold
                                ? "Listing marked as sold. It is removed from public browse; existing chats are kept."
                                : withdrawn
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
        closeRatingSubscription();
        super.onCleared();
    }

    private void closeSubscription() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }

    private void closeRatingSubscription() {
        ratingSubscription.remove();
        ratingSubscription = FirestoreMarketplaceRatingRepository.Subscription.NONE;
    }

    public static final class RatingState {
        private final double average;
        private final int count;
        private final int myScore;
        private final boolean saving;
        private final boolean fromCache;
        private final String message;

        private RatingState(
                double average,
                int count,
                int myScore,
                boolean saving,
                boolean fromCache,
                @NonNull String message) {
            this.average = average;
            this.count = count;
            this.myScore = myScore;
            this.saving = saving;
            this.fromCache = fromCache;
            this.message = message;
        }

        private static RatingState empty() {
            return new RatingState(0d, 0, 0, false, false, "");
        }

        public double getAverage() { return average; }
        public int getCount() { return count; }
        public int getMyScore() { return myScore; }
        public boolean isSaving() { return saving; }
        public boolean isFromCache() { return fromCache; }
        @NonNull public String getMessage() { return message; }

        @NonNull
        public String summaryText() {
            if (count == 0) {
                return "No marketplace ratings yet";
            }
            return String.format(
                    java.util.Locale.ROOT,
                    "%.1f / 5 from %d %s",
                    average,
                    count,
                    count == 1 ? "rating" : "ratings");
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
