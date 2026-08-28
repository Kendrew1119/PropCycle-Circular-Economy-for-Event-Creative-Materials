package com.propcycle.app.data.lending;

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

/** Owner-scoped Storage operations for lending item images. */
public final class FirebaseLendingImageRepository {

    public interface UploadCallback {
        void onProgress(int percent);
        void onUploaded(@NonNull String gsUrl);
        void onError(@NonNull String message);
    }

    public interface CompletionCallback {
        void onComplete();
        void onError(@NonNull String message);
    }

    public interface UploadHandle { void cancel(); }

    private static final UploadHandle NONE = () -> { };
    @Nullable private final FirebaseAuth auth;
    @Nullable private final FirebaseStorage storage;
    @Nullable private final ConnectivityManager connectivity;

    public FirebaseLendingImageRepository(@NonNull Context context) {
        Context app = context.getApplicationContext();
        auth = FirebaseEnvironment.auth(app);
        storage = FirebaseEnvironment.storage(app);
        connectivity = app.getSystemService(ConnectivityManager.class);
    }

    @NonNull
    public UploadHandle upload(
            @NonNull String itemId,
            @NonNull File file,
            @NonNull UploadCallback callback) {
        String uid = currentUserId();
        if (uid == null) {
            callback.onError("Sign in before uploading a lending photo.");
            return NONE;
        }
        if (storage == null) {
            callback.onError(FirebaseEnvironment.STORAGE_SETUP_MESSAGE);
            return NONE;
        }
        if (!file.isFile() || file.length() <= 0
                || file.length() > LendingImagePolicy.MAX_ENCODED_BYTES) {
            callback.onError("Choose a valid JPEG smaller than 4 MB.");
            return NONE;
        }
        if (!hasNetwork()) {
            callback.onError("Connect to the internet before uploading the lending photo.");
            return NONE;
        }
        final StorageReference reference;
        final String gsUrl;
        try {
            String path = LendingImagePolicy.objectPath(
                    uid, itemId, LendingImagePolicy.newVersionId());
            reference = storage.getReference().child(path);
            gsUrl = LendingImagePolicy.gsUrl(reference.getBucket(), path);
        } catch (IllegalArgumentException error) {
            callback.onError("The lending photo path is invalid.");
            return NONE;
        }
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType(LendingImagePolicy.CONTENT_TYPE)
                .setCustomMetadata("ownerId", uid)
                .setCustomMetadata("itemId", itemId)
                .setCustomMetadata("kind", LendingImagePolicy.METADATA_KIND)
                .build();
        UploadTask upload = reference.putFile(Uri.fromFile(file), metadata);
        upload.addOnProgressListener(snapshot -> {
            long total = snapshot.getTotalByteCount();
            callback.onProgress(total <= 0 ? 0 : (int) Math.min(
                    100L, snapshot.getBytesTransferred() * 100L / total));
        });
        upload.addOnSuccessListener(ignored -> callback.onUploaded(gsUrl));
        upload.addOnFailureListener(error -> callback.onError(errorMessage(error)));
        return upload::cancel;
    }

    public void deleteOwned(
            @NonNull String itemId,
            @Nullable String gsUrl,
            @NonNull CompletionCallback callback) {
        if (gsUrl == null || gsUrl.trim().isEmpty()) {
            callback.onComplete();
            return;
        }
        String uid = currentUserId();
        if (uid == null || storage == null) {
            callback.onError("Firebase Storage setup or sign-in is required.");
            return;
        }
        if (!LendingImagePolicy.isOwnedItemUrl(gsUrl, uid, itemId)) {
            callback.onError("PropCycle will not delete a photo outside this lending item.");
            return;
        }
        try {
            storage.getReferenceFromUrl(gsUrl).delete()
                    .addOnSuccessListener(ignored -> callback.onComplete())
                    .addOnFailureListener(error -> {
                        if (error instanceof StorageException storageError
                                && storageError.getErrorCode()
                                == StorageException.ERROR_OBJECT_NOT_FOUND) {
                            callback.onComplete();
                        } else {
                            callback.onError(errorMessage(error));
                        }
                    });
        } catch (IllegalArgumentException error) {
            callback.onError("The lending photo reference is invalid.");
        }
    }

    @Nullable
    private String currentUserId() {
        return auth == null || auth.getCurrentUser() == null
                ? null : auth.getCurrentUser().getUid();
    }

    private boolean hasNetwork() {
        Network network = connectivity == null ? null : connectivity.getActiveNetwork();
        NetworkCapabilities capabilities = network == null || connectivity == null
                ? null : connectivity.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    @NonNull
    private static String errorMessage(@NonNull Exception error) {
        if (error instanceof StorageException storageError) {
            return switch (storageError.getErrorCode()) {
                case StorageException.ERROR_NOT_AUTHENTICATED ->
                        "Sign in before changing a lending photo.";
                case StorageException.ERROR_NOT_AUTHORIZED ->
                        "Firebase denied the lending photo. Deploy the reviewed Storage Rules.";
                case StorageException.ERROR_QUOTA_EXCEEDED ->
                        "Firebase Storage quota is unavailable.";
                case StorageException.ERROR_CANCELED -> "Lending photo upload was cancelled.";
                default -> "The lending photo request failed. Please try again.";
            };
        }
        return "The lending photo request failed. Please try again.";
    }
}
