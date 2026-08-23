package com.propcycle.app.data.marketplace;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
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
    public static final String STATUS_AVAILABLE = MarketplaceListingStatusPolicy.AVAILABLE;
    public static final String STATUS_WITHDRAWN = MarketplaceListingStatusPolicy.WITHDRAWN;
    private static final int BROWSE_LIMIT = 50;

    private static final Subscription NO_OP_SUBSCRIPTION = () -> {
    };

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final ConnectivityManager connectivityManager;

    public FirestoreMarketplaceRepository(@NonNull Context context) {
        Context applicationContext = context.getApplicationContext();
        auth = FirebaseEnvironment.auth(applicationContext);
        firestore = FirebaseEnvironment.firestore(applicationContext);
        connectivityManager = applicationContext.getSystemService(ConnectivityManager.class);
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
            @NonNull String listingId,
            @NonNull NewMarketplaceListing listing,
            @Nullable String imageUrl,
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
        if (!hasUsableNetwork()) {
            callback.onError(networkMutationError());
            return;
        }
        if (listingId.trim().isEmpty()
                || imageUrl != null
                && !MarketplaceImagePolicy.isOwnedListingGsUrl(
                        imageUrl, ownerId, listingId)) {
            callback.onError(new RepositoryError(
                    ErrorType.UNKNOWN,
                    "The marketplace photo reference is invalid."));
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
        values.put("imageUrl", imageUrl);
        values.put("createdAt", FieldValue.serverTimestamp());
        values.put("updatedAt", FieldValue.serverTimestamp());

        firestore.collection(COLLECTION)
                .document(listingId)
                .set(values)
                .addOnSuccessListener(ignored -> callback.onCreated(listingId))
                .addOnFailureListener(error -> callback.onError(mapError(error)));
    }

    @Override
    public void updateListing(
            @NonNull String listingId,
            @NonNull NewMarketplaceListing listing,
            @Nullable Timestamp expectedUpdatedAt,
            @Nullable String expectedImageUrl,
            @Nullable String replacementImageUrl,
            @NonNull MutationCallback callback) {
        RepositoryError readinessError = readinessError();
        if (readinessError != null) {
            callback.onError(readinessError);
            return;
        }
        String ownerId = currentUserId();
        if (ownerId == null) {
            callback.onError(authenticationMutationError());
            return;
        }
        if (listingId.trim().isEmpty()) {
            callback.onError(notFoundMutationError());
            return;
        }
        if (expectedUpdatedAt == null) {
            callback.onError(conflictMutationError());
            return;
        }
        if (!hasUsableNetwork()) {
            callback.onError(networkMutationError());
            return;
        }
        if (replacementImageUrl != null
                && !MarketplaceImagePolicy.isOwnedListingGsUrl(
                        replacementImageUrl, ownerId, listingId)) {
            callback.onError(new RepositoryError(
                    ErrorType.UNKNOWN,
                    "The replacement photo reference is invalid."));
            return;
        }

        DocumentReference reference = firestore.collection(COLLECTION).document(listingId);
        firestore.runTransaction(transaction -> {
                    DocumentSnapshot current = transaction.get(reference);
                    verifyMutableListing(current, ownerId, expectedUpdatedAt);
                    if (replacementImageUrl != null
                            && !java.util.Objects.equals(
                                    expectedImageUrl, current.getString("imageUrl"))) {
                        throw new ListingConflictException();
                    }
                    Map<String, Object> values = editableValues(listing);
                    if (replacementImageUrl != null) {
                        values.put("imageUrl", replacementImageUrl);
                    }
                    transaction.update(reference, values);
                    return null;
                })
                .addOnSuccessListener(ignored -> callback.onUpdated())
                .addOnFailureListener(error -> callback.onError(mapError(error)));
    }

    @Override
    public void setListingStatus(
            @NonNull String listingId,
            @NonNull String targetStatus,
            @Nullable Timestamp expectedUpdatedAt,
            @NonNull MutationCallback callback) {
        RepositoryError readinessError = readinessError();
        if (readinessError != null) {
            callback.onError(readinessError);
            return;
        }
        String ownerId = currentUserId();
        if (ownerId == null) {
            callback.onError(authenticationMutationError());
            return;
        }
        if (listingId.trim().isEmpty()) {
            callback.onError(notFoundMutationError());
            return;
        }
        if (!MarketplaceListingStatusPolicy.isSupportedStatus(targetStatus)) {
            callback.onError(new RepositoryError(
                    ErrorType.UNKNOWN,
                    "Choose a supported marketplace status."));
            return;
        }
        if (expectedUpdatedAt == null) {
            callback.onError(conflictMutationError());
            return;
        }
        if (!hasUsableNetwork()) {
            callback.onError(networkMutationError());
            return;
        }

        DocumentReference reference = firestore.collection(COLLECTION).document(listingId);
        firestore.runTransaction(transaction -> {
                    DocumentSnapshot current = transaction.get(reference);
                    verifyMutableListing(current, ownerId, expectedUpdatedAt);
                    String currentStatus = current.getString("status");
                    boolean validTransition =
                            (STATUS_AVAILABLE.equals(currentStatus)
                                    && STATUS_WITHDRAWN.equals(targetStatus))
                            || (STATUS_WITHDRAWN.equals(currentStatus)
                                    && STATUS_AVAILABLE.equals(targetStatus));
                    if (!validTransition) {
                        throw new ListingConflictException();
                    }
                    Map<String, Object> values = new HashMap<>();
                    values.put("status", targetStatus);
                    values.put("updatedAt", FieldValue.serverTimestamp());
                    transaction.update(reference, values);
                    return null;
                })
                .addOnSuccessListener(ignored -> callback.onUpdated())
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

    private boolean hasUsableNetwork() {
        if (connectivityManager == null) {
            return false;
        }
        Network network = connectivityManager.getActiveNetwork();
        NetworkCapabilities capabilities = network == null
                ? null
                : connectivityManager.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    @NonNull
    private static Map<String, Object> editableValues(
            @NonNull NewMarketplaceListing listing) {
        Map<String, Object> values = new HashMap<>();
        values.put("title", listing.getTitle());
        values.put("titleNormalized", listing.getTitleNormalized());
        values.put("description", listing.getDescription());
        values.put("category", listing.getCategory());
        values.put("condition", listing.getCondition());
        values.put("transactionIntent", listing.getTransactionIntent());
        values.put("fulfilmentMethod", listing.getFulfilmentMethod());
        values.put("priceMinor", listing.getPriceMinor());
        values.put("exchangeTerms", listing.getExchangeTerms());
        values.put("updatedAt", FieldValue.serverTimestamp());
        return values;
    }

    private static void verifyMutableListing(
            @NonNull DocumentSnapshot current,
            @NonNull String ownerId,
            @NonNull Timestamp expectedUpdatedAt) {
        if (!current.exists()) {
            throw new ListingNotFoundException();
        }
        if (!ownerId.equals(current.getString("ownerId"))) {
            throw new ListingPermissionException();
        }
        Timestamp serverUpdatedAt = current.getTimestamp("updatedAt");
        if (!expectedUpdatedAt.equals(serverUpdatedAt)) {
            throw new ListingConflictException();
        }
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
        if (hasCause(error, ListingNotFoundException.class)) {
            return notFoundMutationError();
        }
        if (hasCause(error, ListingPermissionException.class)) {
            return new RepositoryError(
                    ErrorType.PERMISSION_DENIED,
                    "Only the listing owner can make this change.");
        }
        if (hasCause(error, ListingConflictException.class)) {
            return conflictMutationError();
        }
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

    private static boolean hasCause(
            @NonNull Throwable error,
            @NonNull Class<? extends Throwable> type) {
        Throwable current = error;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    @NonNull
    private static RepositoryError authenticationMutationError() {
        return new RepositoryError(
                ErrorType.AUTHENTICATION_REQUIRED,
                "Sign in before changing a listing.");
    }

    @NonNull
    private static RepositoryError notFoundMutationError() {
        return new RepositoryError(
                ErrorType.NOT_FOUND,
                "This marketplace listing no longer exists.");
    }

    @NonNull
    private static RepositoryError networkMutationError() {
        return new RepositoryError(
                ErrorType.NETWORK,
                "Connect to the internet before changing this listing.");
    }

    @NonNull
    private static RepositoryError conflictMutationError() {
        return new RepositoryError(
                ErrorType.CONFLICT,
                "This listing changed on another device. Reopen it before saving.");
    }

    private static final class ListingNotFoundException extends RuntimeException {
    }

    private static final class ListingPermissionException extends RuntimeException {
    }

    private static final class ListingConflictException extends RuntimeException {
    }
}
