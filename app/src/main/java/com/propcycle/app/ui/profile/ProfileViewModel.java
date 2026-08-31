package com.propcycle.app.ui.profile;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;
import com.google.android.gms.tasks.Tasks;
import com.propcycle.app.core.firebase.FirebaseEnvironment;
import com.propcycle.app.data.auth.AuthInputValidator;
import com.propcycle.app.data.activity.ActivityLogRepository;
import com.propcycle.app.data.activity.ActivityRecord;
import com.propcycle.app.data.marketplace.FirestoreMarketplaceRepository;
import com.propcycle.app.data.marketplace.FirestoreMarketplaceRatingRepository;
import com.propcycle.app.data.marketplace.MarketplaceListing;
import com.propcycle.app.data.marketplace.MarketplaceRatingPolicy;
import com.propcycle.app.data.marketplace.MarketplaceRepository;
import com.propcycle.app.data.marketplace.MarketplaceSellerRating;
import com.propcycle.app.ui.common.OneTimeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ProfileViewModel extends AndroidViewModel {

    private final MarketplaceRepository marketplaceRepository;
    private final FirestoreMarketplaceRatingRepository ratingRepository;
    private final LiveData<List<ActivityRecord>> activities;
    private final MutableLiveData<List<MarketplaceListing>> ownedListings =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<OneTimeEvent<ProfileUpdate>> profileUpdate =
            new MutableLiveData<>();
    private final MutableLiveData<ProfileState> profileState =
            new MutableLiveData<>(ProfileState.loading());
    private final MutableLiveData<MarketplaceRatingPolicy.Summary> ratingSummary =
            new MutableLiveData<>(MarketplaceRatingPolicy.summarize(Collections.emptyList()));
    private MarketplaceRepository.Subscription subscription;
    private FirestoreMarketplaceRatingRepository.Subscription ratingSubscription =
            FirestoreMarketplaceRatingRepository.Subscription.NONE;
    private String targetUserId = "";
    private int loadGeneration;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        marketplaceRepository = new FirestoreMarketplaceRepository(application);
        ratingRepository = new FirestoreMarketplaceRatingRepository(application);
        activities = new ActivityLogRepository(application).observeCurrentUser();
    }

    @NonNull public LiveData<List<ActivityRecord>> getActivities() { return activities; }
    @NonNull public LiveData<List<MarketplaceListing>> getOwnedListings() { return ownedListings; }
    @NonNull public LiveData<OneTimeEvent<ProfileUpdate>> getProfileUpdate() {
        return profileUpdate;
    }
    @NonNull public LiveData<ProfileState> getProfileState() { return profileState; }
    @NonNull public LiveData<MarketplaceRatingPolicy.Summary> getRatingSummary() {
        return ratingSummary;
    }

    public void updateDisplayName(@Nullable String value) {
        AuthInputValidator.ValidationResult validation =
                AuthInputValidator.validateDisplayName(value);
        if (!validation.isValid()) {
            profileUpdate.setValue(new OneTimeEvent<>(
                    ProfileUpdate.error(validation.getMessage())));
            return;
        }
        FirebaseAuth auth = FirebaseEnvironment.auth(getApplication());
        FirebaseFirestore firestore = FirebaseEnvironment.firestore(getApplication());
        FirebaseUser user = auth == null ? null : auth.getCurrentUser();
        if (user == null || firestore == null || !user.getUid().equals(targetUserId)) {
            profileUpdate.setValue(new OneTimeEvent<>(
                    ProfileUpdate.error("Firebase setup or the signed-in account is unavailable.")));
            return;
        }
        String name = value == null ? "" : value.trim();
        profileUpdate.setValue(new OneTimeEvent<>(ProfileUpdate.working()));
        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();
        Tasks.whenAll(
                        user.updateProfile(request),
                        firestore.collection("users").document(user.getUid()).update(
                                "displayName", name,
                                "updatedAt", FieldValue.serverTimestamp()))
                .addOnSuccessListener(ignored -> profileUpdate.setValue(
                        new OneTimeEvent<>(ProfileUpdate.success(name))))
                .addOnFailureListener(error -> profileUpdate.setValue(
                        new OneTimeEvent<>(ProfileUpdate.error(
                                error.getMessage() == null
                                        ? "The display name could not be updated."
                                        : error.getMessage()))));
    }

    public void start(@Nullable String requestedUserId) {
        stop();
        FirebaseAuth auth = FirebaseEnvironment.auth(getApplication());
        FirebaseUser currentUser = auth == null ? null : auth.getCurrentUser();
        if (currentUser == null) {
            profileState.setValue(ProfileState.error("Sign in to view this profile."));
            ownedListings.setValue(Collections.emptyList());
            return;
        }
        String requested = requestedUserId == null ? "" : requestedUserId.trim();
        targetUserId = requested.isEmpty() ? currentUser.getUid() : requested;
        ownedListings.setValue(Collections.emptyList());
        ratingSummary.setValue(MarketplaceRatingPolicy.summarize(Collections.emptyList()));
        if (!MarketplaceRatingPolicy.isSafeSegment(targetUserId)) {
            profileState.setValue(ProfileState.error("This user profile is invalid."));
            ownedListings.setValue(Collections.emptyList());
            return;
        }
        int generation = ++loadGeneration;
        boolean ownProfile = currentUser.getUid().equals(targetUserId);
        profileState.setValue(ProfileState.loading());
        if (ownProfile) {
            String name = currentUser.getDisplayName();
            long createdAt = currentUser.getMetadata() == null
                    ? 0L : currentUser.getMetadata().getCreationTimestamp();
            profileState.setValue(ProfileState.content(
                    targetUserId,
                    cleanName(name),
                    currentUser.getEmail(),
                    createdAt,
                    true));
        }
        loadPublicProfile(generation, ownProfile);
        observeRatings(generation);
        subscription = marketplaceRepository.observeAvailableListings(
                new MarketplaceRepository.ListingsObserver() {
                    @Override
                    public void onListings(
                            @NonNull List<MarketplaceListing> listings,
                            boolean fromCache) {
                        List<MarketplaceListing> owned = new ArrayList<>();
                        if (generation == loadGeneration) {
                            for (MarketplaceListing listing : listings) {
                                if (targetUserId.equals(listing.getOwnerId())) {
                                    owned.add(listing);
                                }
                            }
                            ownedListings.setValue(owned);
                        }
                    }

                    @Override
                    public void onError(@NonNull MarketplaceRepository.RepositoryError error) {
                        if (generation == loadGeneration) {
                            ownedListings.setValue(Collections.emptyList());
                        }
                    }
                });
    }

    private void loadPublicProfile(int generation, boolean ownProfile) {
        FirebaseFirestore firestore = FirebaseEnvironment.firestore(getApplication());
        if (firestore == null) {
            if (!ownProfile) {
                profileState.setValue(ProfileState.error(
                        "Firebase setup is required to view this public profile."));
            }
            return;
        }
        firestore.collection("users").document(targetUserId).get()
                .addOnSuccessListener(snapshot -> {
                    if (generation != loadGeneration) {
                        return;
                    }
                    if (!snapshot.exists()) {
                        if (!ownProfile) {
                            profileState.setValue(ProfileState.error(
                                    "This public profile is unavailable."));
                        }
                        return;
                    }
                    Timestamp createdAt = snapshot.getTimestamp("createdAt");
                    String displayName = cleanName(snapshot.getString("displayName"));
                    ProfileState current = profileState.getValue();
                    String email = ownProfile && current != null ? current.getEmail() : null;
                    long createdMillis = createdAt == null
                            ? (current == null ? 0L : current.getCreatedAtMillis())
                            : createdAt.toDate().getTime();
                    profileState.setValue(ProfileState.content(
                            targetUserId,
                            displayName,
                            email,
                            createdMillis,
                            ownProfile));
                })
                .addOnFailureListener(error -> {
                    if (generation == loadGeneration && !ownProfile) {
                        profileState.setValue(ProfileState.error(
                                error.getMessage() == null
                                        ? "This public profile could not be loaded."
                                        : error.getMessage()));
                    }
                });
    }

    private void observeRatings(int generation) {
        ratingSubscription = ratingRepository.observeRatings(
                targetUserId,
                new FirestoreMarketplaceRatingRepository.RatingsCallback() {
                    @Override
                    public void onData(
                            @NonNull List<MarketplaceSellerRating> ratings,
                            boolean fromCache) {
                        if (generation == loadGeneration) {
                            ratingSummary.setValue(MarketplaceRatingPolicy.summarize(ratings));
                        }
                    }

                    @Override
                    public void onError(@NonNull Exception error) {
                        if (generation == loadGeneration) {
                            ratingSummary.setValue(
                                    MarketplaceRatingPolicy.summarize(Collections.emptyList()));
                        }
                    }
                });
    }

    @NonNull
    private static String cleanName(@Nullable String value) {
        return value == null || value.trim().isEmpty()
                ? "PropCycle Member" : value.trim();
    }

    public void stop() {
        loadGeneration++;
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
        ratingSubscription.remove();
        ratingSubscription = FirestoreMarketplaceRatingRepository.Subscription.NONE;
    }

    @Override
    protected void onCleared() {
        stop();
        super.onCleared();
    }

    public static final class ProfileUpdate {
        private final boolean working;
        private final boolean success;
        private final String message;

        private ProfileUpdate(boolean working, boolean success, @NonNull String message) {
            this.working = working;
            this.success = success;
            this.message = message;
        }

        private static ProfileUpdate working() {
            return new ProfileUpdate(true, false, "Updating display name...");
        }

        private static ProfileUpdate success(@NonNull String name) {
            return new ProfileUpdate(false, true, "Display name updated to " + name + ".");
        }

        private static ProfileUpdate error(@NonNull String message) {
            return new ProfileUpdate(false, false, message);
        }

        public boolean isWorking() { return working; }
        public boolean isSuccess() { return success; }
        @NonNull public String getMessage() { return message; }
    }

    public static final class ProfileState {
        private final boolean loading;
        private final String userId;
        private final String displayName;
        private final String email;
        private final long createdAtMillis;
        private final boolean ownProfile;
        private final String errorMessage;

        private ProfileState(
                boolean loading,
                @NonNull String userId,
                @NonNull String displayName,
                @Nullable String email,
                long createdAtMillis,
                boolean ownProfile,
                @NonNull String errorMessage) {
            this.loading = loading;
            this.userId = userId;
            this.displayName = displayName;
            this.email = email;
            this.createdAtMillis = createdAtMillis;
            this.ownProfile = ownProfile;
            this.errorMessage = errorMessage;
        }

        private static ProfileState loading() {
            return new ProfileState(true, "", "PropCycle Member", null, 0L, false, "");
        }

        private static ProfileState content(
                @NonNull String userId,
                @NonNull String displayName,
                @Nullable String email,
                long createdAtMillis,
                boolean ownProfile) {
            return new ProfileState(
                    false, userId, displayName, email, createdAtMillis, ownProfile, "");
        }

        private static ProfileState error(@NonNull String message) {
            return new ProfileState(false, "", "Profile unavailable", null, 0L, false, message);
        }

        public boolean isLoading() { return loading; }
        @NonNull public String getUserId() { return userId; }
        @NonNull public String getDisplayName() { return displayName; }
        @Nullable public String getEmail() { return email; }
        public long getCreatedAtMillis() { return createdAtMillis; }
        public boolean isOwnProfile() { return ownProfile; }
        @NonNull public String getErrorMessage() { return errorMessage; }
    }
}
