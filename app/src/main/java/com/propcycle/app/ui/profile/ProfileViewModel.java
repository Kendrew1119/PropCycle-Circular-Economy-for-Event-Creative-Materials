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
import com.google.android.gms.tasks.Tasks;
import com.propcycle.app.core.firebase.FirebaseEnvironment;
import com.propcycle.app.data.auth.AuthInputValidator;
import com.propcycle.app.data.activity.ActivityLogRepository;
import com.propcycle.app.data.activity.ActivityRecord;
import com.propcycle.app.data.marketplace.FirestoreMarketplaceRepository;
import com.propcycle.app.data.marketplace.MarketplaceListing;
import com.propcycle.app.data.marketplace.MarketplaceRepository;
import com.propcycle.app.ui.common.OneTimeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ProfileViewModel extends AndroidViewModel {

    private final MarketplaceRepository marketplaceRepository;
    private final LiveData<List<ActivityRecord>> activities;
    private final MutableLiveData<List<MarketplaceListing>> ownedListings =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<OneTimeEvent<ProfileUpdate>> profileUpdate =
            new MutableLiveData<>();
    private MarketplaceRepository.Subscription subscription;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        marketplaceRepository = new FirestoreMarketplaceRepository(application);
        activities = new ActivityLogRepository(application).observeCurrentUser();
    }

    @NonNull public LiveData<List<ActivityRecord>> getActivities() { return activities; }
    @NonNull public LiveData<List<MarketplaceListing>> getOwnedListings() { return ownedListings; }
    @NonNull public LiveData<OneTimeEvent<ProfileUpdate>> getProfileUpdate() {
        return profileUpdate;
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
        if (user == null || firestore == null) {
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

    public void start() {
        stop();
        subscription = marketplaceRepository.observeAvailableListings(
                new MarketplaceRepository.ListingsObserver() {
                    @Override
                    public void onListings(
                            @NonNull List<MarketplaceListing> listings,
                            boolean fromCache) {
                        String uid = marketplaceRepository.currentUserId();
                        List<MarketplaceListing> owned = new ArrayList<>();
                        if (uid != null) {
                            for (MarketplaceListing listing : listings) {
                                if (uid.equals(listing.getOwnerId())) {
                                    owned.add(listing);
                                }
                            }
                        }
                        ownedListings.setValue(owned);
                    }

                    @Override
                    public void onError(@NonNull MarketplaceRepository.RepositoryError error) {
                        ownedListings.setValue(Collections.emptyList());
                    }
                });
    }

    public void stop() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
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
}
