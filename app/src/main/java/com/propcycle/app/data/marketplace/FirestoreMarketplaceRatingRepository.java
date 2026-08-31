package com.propcycle.app.data.marketplace;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.propcycle.app.core.firebase.FirebaseEnvironment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Firebase boundary for one marketplace seller rating per reviewer. */
public final class FirestoreMarketplaceRatingRepository {

    private static final String USERS = "users";
    private static final String RATINGS = "marketplaceRatings";

    public interface Subscription {
        void remove();

        Subscription NONE = () -> { };
    }

    public interface RatingsCallback {
        void onData(@NonNull List<MarketplaceSellerRating> ratings, boolean fromCache);
        void onError(@NonNull Exception error);
    }

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public FirestoreMarketplaceRatingRepository(@NonNull Context context) {
        Context applicationContext = context.getApplicationContext();
        auth = FirebaseEnvironment.auth(applicationContext);
        firestore = FirebaseEnvironment.firestore(applicationContext);
    }

    @NonNull
    public Subscription observeRatings(
            @NonNull String recipientUid,
            @NonNull RatingsCallback callback) {
        if (firestore == null || currentUserId() == null
                || !MarketplaceRatingPolicy.isSafeSegment(recipientUid)) {
            callback.onError(new IllegalStateException(
                    "Sign in and choose a valid seller to view ratings."));
            return Subscription.NONE;
        }
        ListenerRegistration registration = firestore.collection(USERS)
                .document(recipientUid)
                .collection(RATINGS)
                .limit(MarketplaceRatingPolicy.MAX_RATINGS)
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
                    if (error != null) {
                        callback.onError(error);
                        return;
                    }
                    List<MarketplaceSellerRating> ratings = new ArrayList<>();
                    if (snapshot != null) {
                        snapshot.getDocuments().forEach(document -> {
                            MarketplaceSellerRating rating = mapRating(document);
                            if (rating != null) {
                                ratings.add(rating);
                            }
                        });
                    }
                    callback.onData(
                            ratings,
                            snapshot != null && snapshot.getMetadata().isFromCache());
                });
        return registration::remove;
    }

    @NonNull
    public Task<Void> saveRating(
            @NonNull String recipientUid,
            @NonNull String listingId,
            int score) {
        FirebaseUser user = auth == null ? null : auth.getCurrentUser();
        if (firestore == null || user == null) {
            return Tasks.forException(new IllegalStateException(
                    "Sign in before rating this marketplace seller."));
        }
        if (!MarketplaceRatingPolicy.isSafeSegment(recipientUid)
                || !MarketplaceRatingPolicy.isSafeSegment(listingId)
                || user.getUid().equals(recipientUid)) {
            return Tasks.forException(new IllegalArgumentException(
                    "This marketplace seller cannot be rated from this listing."));
        }
        try {
            MarketplaceRatingPolicy.requireScore(score);
        } catch (IllegalArgumentException error) {
            return Tasks.forException(error);
        }

        String raterUid = user.getUid();
        DocumentReference reference = firestore.collection(USERS)
                .document(recipientUid)
                .collection(RATINGS)
                .document(raterUid);
        return firestore.runTransaction(transaction -> {
            DocumentSnapshot current = transaction.get(reference);
            if (current.exists()) {
                Map<String, Object> update = new HashMap<>();
                update.put("score", score);
                update.put("updatedAt", FieldValue.serverTimestamp());
                transaction.update(reference, update);
            } else {
                Map<String, Object> values = new HashMap<>();
                values.put("raterUid", raterUid);
                values.put("recipientUid", recipientUid);
                values.put("contextListingId", listingId);
                values.put("score", score);
                values.put("createdAt", FieldValue.serverTimestamp());
                values.put("updatedAt", FieldValue.serverTimestamp());
                transaction.set(reference, values);
            }
            return null;
        });
    }

    @Nullable
    public String currentUserId() {
        FirebaseUser user = auth == null ? null : auth.getCurrentUser();
        return user == null ? null : user.getUid();
    }

    @Nullable
    private static MarketplaceSellerRating mapRating(@NonNull DocumentSnapshot document) {
        try {
            MarketplaceSellerRating rating = document.toObject(MarketplaceSellerRating.class);
            if (rating == null || rating.getRaterUid() == null
                    || rating.getRecipientUid() == null || rating.getScore() == null) {
                return null;
            }
            return rating;
        } catch (RuntimeException error) {
            return null;
        }
    }
}
