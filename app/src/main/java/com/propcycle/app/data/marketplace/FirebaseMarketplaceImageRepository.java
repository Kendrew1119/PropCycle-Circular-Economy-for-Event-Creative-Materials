package com.propcycle.app.data.marketplace;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.propcycle.app.core.firebase.FirebaseEnvironment;

import java.io.File;

/** Authenticated, owner-scoped Firebase Storage operations for marketplace images. */
public final class FirebaseMarketplaceImageRepository {

    public enum ErrorType {
        CONFIGURATION_REQUIRED,
        AUTHENTICATION_REQUIRED,
        PERMISSION_DENIED,
        NETWORK,
        NOT_FOUND,
        CANCELED,
        INVALID,
        UNKNOWN
    }

    public static final class RepositoryError {
        private final ErrorType type;
        private final String message;

        private RepositoryError(@NonNull ErrorType type, @NonNull String message) {
            this.type = type;
            this.message = message;
        }

        @NonNull
        public ErrorType getType() {
            return type;
        }

        @NonNull
        public String getMessage() {
            return message;
        }
    }

    public interface UploadCallback {
        void onProgress(int percent);

        void onUploaded(@NonNull String gsUrl);

        void onError(@NonNull RepositoryError error);
    }

    public interface CompletionCallback {
        void onComplete();

        void onError(@NonNull RepositoryError error);
    }

    public interface UploadHandle {
        void cancel();
    }

    private static final UploadHandle NO_OP_UPLOAD = () -> {
    };

    private final FirebaseAuth auth;
    private final FirebaseStorage storage;
    private final ConnectivityManager connectivityManager;

    public FirebaseMarketplaceImageRepository(@NonNull Context context) {
        Context applicationContext = context.getApplicationContext();
        auth = FirebaseEnvironment.auth(applicationContext);
        storage = FirebaseEnvironment.storage(applicationContext);
        connectivityManager = applicationContext.getSystemService(ConnectivityManager.class);
    }

    @Nullable
    public String currentUserId() {
        return auth == null || auth.getCurrentUser() == null
                ? null
                : auth.getCurrentUser().getUid();
    }

    @NonNull
    public UploadHandle upload(
            @NonNull String listingId,
            @NonNull File imageFile,
            @NonNull UploadCallback callback) {
        RepositoryError readinessError = readinessError();
        if (readinessError != null) {
            callback.onError(readinessError);
            return NO_OP_UPLOAD;
        }
        String ownerId = currentUserId();
        if (ownerId == null) {
            callback.onError(authenticationError());
            return NO_OP_UPLOAD;
        }
        if (!imageFile.isFile()
                || imageFile.length() <= 0
                || imageFile.length() > MarketplaceImagePolicy.MAX_ENCODED_BYTES) {
            callback.onError(new RepositoryError(
                    ErrorType.INVALID,
                    "Choose a valid marketplace photo smaller than 4 MB."));
            return NO_OP_UPLOAD;
        }
        if (!hasUsableNetwork()) {
            callback.onError(networkError());
            return NO_OP_UPLOAD;
        }

        final String objectPath;
        final String gsUrl;
        final StorageReference reference;
        try {
            objectPath = MarketplaceImagePolicy.objectPath(
                    ownerId,
                    listingId,
                    MarketplaceImagePolicy.newVersionId());
            reference = storage.getReference().child(objectPath);
            gsUrl = MarketplaceImagePolicy.gsUrl(reference.getBucket(), objectPath);
        } catch (IllegalArgumentException error) {
            callback.onError(new RepositoryError(
                    ErrorType.INVALID,
                    "The marketplace photo path could not be created."));
            return NO_OP_UPLOAD;
        }

        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType(MarketplaceImagePolicy.CONTENT_TYPE)
                .setCustomMetadata("ownerId", ownerId)
                .setCustomMetadata("listingId", listingId)
                .setCustomMetadata("kind", MarketplaceImagePolicy.METADATA_KIND)
                .build();
        UploadTask task = reference.putFile(Uri.fromFile(imageFile), metadata);
        task.addOnProgressListener(snapshot -> {
            long total = snapshot.getTotalByteCount();
            int percent = total <= 0
                    ? 0
                    : (int) Math.min(100L, snapshot.getBytesTransferred() * 100L / total);
            callback.onProgress(percent);
        });
        task.addOnSuccessListener(snapshot -> callback.onUploaded(gsUrl));
        task.addOnFailureListener(error -> callback.onError(mapError(error)));
        return task::cancel;
    }

    public void deleteOwned(
            @NonNull String listingId,
            @Nullable String gsUrl,
            @NonNull CompletionCallback callback) {
        if (gsUrl == null || gsUrl.trim().isEmpty()) {
            callback.onComplete();
            return;
        }
        RepositoryError readinessError = readinessError();
        if (readinessError != null) {
            callback.onError(readinessError);
            return;
        }
        String ownerId = currentUserId();
        if (ownerId == null) {
            callback.onError(authenticationError());
            return;
        }
        if (!MarketplaceImagePolicy.isOwnedListingGsUrl(gsUrl, ownerId, listingId)
                || !MarketplaceImagePolicy.isGsUrlForBucket(
                        gsUrl, storage.getReference().getBucket())) {
            callback.onError(new RepositoryError(
                    ErrorType.PERMISSION_DENIED,
                    "PropCycle will not delete a photo outside this listing."));
            return;
        }
        if (!hasUsableNetwork()) {
            callback.onError(networkError());
            return;
        }
        try {
            storage.getReferenceFromUrl(gsUrl)
                    .delete()
                    .addOnSuccessListener(ignored -> callback.onComplete())
                    .addOnFailureListener(error -> {
                        RepositoryError mapped = mapError(error);
                        if (mapped.getType() == ErrorType.NOT_FOUND) {
                            callback.onComplete();
                        } else {
                            callback.onError(mapped);
                        }
                    });
        } catch (IllegalArgumentException error) {
            callback.onError(new RepositoryError(
                    ErrorType.INVALID,
                    "The marketplace photo reference is invalid."));
        }
    }

    @Nullable
    private RepositoryError readinessError() {
        if (auth == null) {
            return new RepositoryError(
                    ErrorType.CONFIGURATION_REQUIRED,
                    FirebaseEnvironment.SETUP_MESSAGE);
        }
        if (auth.getCurrentUser() == null) {
            return authenticationError();
        }
        if (storage == null) {
            return new RepositoryError(
                    ErrorType.CONFIGURATION_REQUIRED,
                    FirebaseEnvironment.STORAGE_SETUP_MESSAGE);
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
    private static RepositoryError mapError(@NonNull Exception error) {
        if (error instanceof StorageException storageError) {
            return switch (storageError.getErrorCode()) {
                case StorageException.ERROR_NOT_AUTHENTICATED -> authenticationError();
                case StorageException.ERROR_NOT_AUTHORIZED -> new RepositoryError(
                        ErrorType.PERMISSION_DENIED,
                        "Firebase denied this photo request. Check the deployed Storage Rules.");
                case StorageException.ERROR_OBJECT_NOT_FOUND -> new RepositoryError(
                        ErrorType.NOT_FOUND,
                        "This marketplace photo no longer exists.");
                case StorageException.ERROR_CANCELED -> new RepositoryError(
                        ErrorType.CANCELED,
                        "Photo upload was canceled.");
                case StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> networkError();
                case StorageException.ERROR_BUCKET_NOT_FOUND,
                        StorageException.ERROR_PROJECT_NOT_FOUND -> new RepositoryError(
                        ErrorType.CONFIGURATION_REQUIRED,
                        FirebaseEnvironment.STORAGE_SETUP_MESSAGE);
                case StorageException.ERROR_QUOTA_EXCEEDED -> new RepositoryError(
                        ErrorType.CONFIGURATION_REQUIRED,
                        "Firebase Storage quota is unavailable. Check the Blaze plan and quota.");
                default -> new RepositoryError(
                        ErrorType.UNKNOWN,
                        "The marketplace photo request failed. Please try again.");
            };
        }
        return new RepositoryError(
                ErrorType.UNKNOWN,
                "The marketplace photo request failed. Please try again.");
    }

    @NonNull
    private static RepositoryError authenticationError() {
        return new RepositoryError(
                ErrorType.AUTHENTICATION_REQUIRED,
                "Sign in before changing a marketplace photo.");
    }

    @NonNull
    private static RepositoryError networkError() {
        return new RepositoryError(
                ErrorType.NETWORK,
                "Connect to the internet before uploading a marketplace photo.");
    }
}
