package com.propcycle.app.data.marketplace;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.propcycle.app.core.firebase.FirebaseEnvironment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Firebase implementation for the first authenticated marketplace vertical slice. */
public final class FirestoreMarketplaceRepository implements MarketplaceRepository {

    public static final String COLLECTION = "marketplaceListings";
    public static final String STATUS_AVAILABLE = "available";
    private static final int BROWSE_LIMIT = 50;

    private static final Subscription NO_OP_SUBSCRIPTION = () -> {
    };

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public FirestoreMarketplaceRepository(@NonNull Context context) {
        Context applicationContext = context.getApplicationContext();
        auth = FirebaseEnvironment.auth(applicationContext);
        firestore = FirebaseEnvironment.firestore(applicationContext);
    }

    @NonNull
    @Override
    public Subscription observeAvailableListings(@NonNull ListingsObserver observer) {
        RepositoryError readinessError = readinessError();
        if (readinessError != null) {
            observer.onError(readinessError);
            return NO_OP_SUBSCRIPTION;
        }

        ListenerRegistration registration = firestore.collection(COLLECTION)
                .whereEqualTo("status", STATUS_AVAILABLE)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(BROWSE_LIMIT)
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
                    if (error != null) {
                        observer.onError(mapError(error));
                        return;
                    }
                    if (snapshot == null) {
                        observer.onError(new RepositoryError(
                                ErrorType.UNKNOWN,
                                "Marketplace data was unavailable."));
                        return;
                    }

                    List<MarketplaceListing> listings = new ArrayList<>();
                    for (QueryDocumentSnapshot document : snapshot) {
                        // Do not present an optimistic local write as a published listing.
                        if (document.getMetadata().hasPendingWrites()) {
                            continue;
                        }
                        MarketplaceListing listing = document.toObject(MarketplaceListing.class);
                        listing.setId(document.getId());
                        listings.add(listing);
                    }
                    observer.onListings(listings, snapshot.getMetadata().isFromCache());
                });
        return registration::remove;
    }

    @NonNull
    @Override
    public Subscription observeListing(
            @NonNull String listingId,
            @NonNull ListingObserver observer) {
        RepositoryError readinessError = readinessError();
        if (readinessError != null) {
            observer.onError(readinessError);
            return NO_OP_SUBSCRIPTION;
        }

        ListenerRegistration registration = firestore.collection(COLLECTION)
                .document(listingId)
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
                    if (error != null) {
                        observer.onError(mapError(error));
                        return;
                    }
                    if (snapshot == null || !snapshot.exists()) {
                        observer.onListing(null,
                                snapshot != null && snapshot.getMetadata().isFromCache());
                        return;
                    }
                    observer.onListing(
                            fromDocument(snapshot),
                            snapshot.getMetadata().isFromCache());
                });
        return registration::remove;
    }

    @Override
    public void createListing(
            @NonNull NewMarketplaceListing listing,
            @NonNull CreateCallback callback) {
        RepositoryError readinessError = readinessError();
        if (readinessError != null) {
            callback.onError(readinessError);
            return;
        }

        String ownerId = currentUserId();
        if (ownerId == null) {
            callback.onError(new RepositoryError(
                    ErrorType.AUTHENTICATION_REQUIRED,
                    "Sign in before publishing a listing."));
            return;
        }

        Map<String, Object> values = new HashMap<>();
        values.put("ownerId", ownerId);
        values.put("title", listing.getTitle());
        values.put("titleNormalized", listing.getTitleNormalized());
        values.put("description", listing.getDescription());
        values.put("category", listing.getCategory());
        values.put("condition", listing.getCondition());
        values.put("transactionIntent", listing.getTransactionIntent());
        values.put("fulfilmentMethod", listing.getFulfilmentMethod());
        values.put("priceMinor", listing.getPriceMinor());
        values.put("exchangeTerms", listing.getExchangeTerms());
        values.put("status", STATUS_AVAILABLE);
        values.put("imageUrl", null);
        values.put("createdAt", FieldValue.serverTimestamp());
        values.put("updatedAt", FieldValue.serverTimestamp());

        firestore.collection(COLLECTION)
                .add(values)
                .addOnSuccessListener(reference -> callback.onCreated(reference.getId()))
                .addOnFailureListener(error -> callback.onError(mapError(error)));
    }

    @Nullable
    @Override
    public String currentUserId() {
        return auth == null || auth.getCurrentUser() == null
                ? null
                : auth.getCurrentUser().getUid();
    }

    @Nullable
    private RepositoryError readinessError() {
        if (auth == null || firestore == null) {
            return new RepositoryError(
                    ErrorType.CONFIGURATION_REQUIRED,
                    FirebaseEnvironment.SETUP_MESSAGE);
        }
        if (auth.getCurrentUser() == null) {
            return new RepositoryError(
                    ErrorType.AUTHENTICATION_REQUIRED,
                    "Sign in to use the marketplace.");
        }
        return null;
    }

    @NonNull
    private static MarketplaceListing fromDocument(@NonNull DocumentSnapshot document) {
        MarketplaceListing listing = document.toObject(MarketplaceListing.class);
        if (listing == null) {
            listing = new MarketplaceListing();
        }
        listing.setId(document.getId());
        return listing;
    }

    @NonNull
    private static RepositoryError mapError(@NonNull Exception error) {
        if (error instanceof FirebaseFirestoreException firestoreError) {
            return switch (firestoreError.getCode()) {
                case PERMISSION_DENIED -> new RepositoryError(
                        ErrorType.PERMISSION_DENIED,
                        "Firebase denied this marketplace request. Check the deployed rules.");
                case UNAVAILABLE, DEADLINE_EXCEEDED -> new RepositoryError(
                        ErrorType.NETWORK,
                        "Marketplace is offline. Check your connection and try again.");
                case NOT_FOUND -> new RepositoryError(
                        ErrorType.NOT_FOUND,
                        "This marketplace listing no longer exists.");
                case UNAUTHENTICATED -> new RepositoryError(
                        ErrorType.AUTHENTICATION_REQUIRED,
                        "Your session expired. Sign in again.");
                default -> new RepositoryError(
                        ErrorType.UNKNOWN,
                        "Marketplace request failed. Please try again.");
            };
        }
        return new RepositoryError(
                ErrorType.UNKNOWN,
                "Marketplace request failed. Please try again.");
    }
}
