package com.propcycle.app.ui.marketplace;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.Timestamp;
import com.propcycle.app.data.activity.ActivityLogRepository;
import com.propcycle.app.data.media.DemoImagePolicy;
import com.propcycle.app.data.marketplace.FirebaseMarketplaceImageRepository;
import com.propcycle.app.data.marketplace.FirestoreMarketplaceRepository;
import com.propcycle.app.data.marketplace.MarketplaceImagePolicy;
import com.propcycle.app.data.marketplace.MarketplaceListing;
import com.propcycle.app.data.marketplace.MarketplaceListingStatusPolicy;
import com.propcycle.app.data.marketplace.MarketplaceListingValidator;
import com.propcycle.app.data.marketplace.MarketplaceRepository;
import com.propcycle.app.data.marketplace.NewMarketplaceListing;
import com.propcycle.app.data.scanner.ScannerImageProcessor;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/** Creates or owner-edits one text listing with an optional protected marketplace image. */
public final class CreateListingViewModel extends AndroidViewModel {

    public enum Mode {
        CREATE,
        EDIT
    }

    private final MarketplaceRepository repository;
    private final FirebaseMarketplaceImageRepository imageRepository;
    private final ScannerImageProcessor imageProcessor;
    private final ActivityLogRepository activityLog;
    private final MutableLiveData<State> state =
            new MutableLiveData<>(State.ready(Mode.CREATE, "", false));
    private final MutableLiveData<MarketplaceListing> initialForm =
            new MutableLiveData<>();
    private final MutableLiveData<Event<String>> completedListing =
            new MutableLiveData<>();
    private final AtomicLong imageGeneration = new AtomicLong();
    private final AtomicLong operationGeneration = new AtomicLong();

    private MarketplaceRepository.Subscription subscription;
    private FirebaseMarketplaceImageRepository.UploadHandle activeUpload;
    private CompletableFuture<ScannerImageProcessor.ProcessedImage> imageFuture;
    private ScannerImageProcessor.ProcessedImage selectedImage;
    private File pendingCaptureFile;
    private Mode mode = Mode.CREATE;
    private String listingId = "";
    private String existingImageUrl;
    private String existingDemoImageKey = "";
    private String selectedDemoImageKey = "";
    private Timestamp expectedUpdatedAt;
    private boolean initialFormSent;
    private boolean ownerConfirmed;
    private boolean started;
    private boolean cleared;
    private boolean imageChoiceChanged;

    public CreateListingViewModel(@NonNull Application application) {
        super(application);
        repository = new FirestoreMarketplaceRepository(application);
        imageRepository = new FirebaseMarketplaceImageRepository(application);
        imageProcessor = new ScannerImageProcessor(application);
        activityLog = new ActivityLogRepository(application);
    }

    @NonNull
    public LiveData<State> getState() {
        return state;
    }

    @NonNull
    public LiveData<MarketplaceListing> getInitialForm() {
        return initialForm;
    }

    @NonNull
    public LiveData<Event<String>> getCompletedListing() {
        return completedListing;
    }

    @Nullable
    public File getSelectedImageFile() {
        return selectedImage == null ? null : selectedImage.getFile();
    }

    public boolean hasSelectedImage() {
        return selectedImage != null && !selectedImage.isDeleted();
    }

    @Nullable
    public String getExistingImageUrl() {
        return existingImageUrl;
    }

    @Nullable
    public String getPreviewImageUrl() {
        return imageChoiceChanged ? null : existingImageUrl;
    }

    @NonNull
    public String getSelectedDemoImageKey() {
        return selectedDemoImageKey;
    }

    public void start(@Nullable String requestedListingId) {
        String resolvedId = requestedListingId == null ? "" : requestedListingId.trim();
        if (resolvedId.isEmpty()) {
            if (started && mode == Mode.CREATE) {
                return;
            }
            started = true;
            closeSubscription();
            mode = Mode.CREATE;
            listingId = "";
            existingImageUrl = null;
            existingDemoImageKey = "";
            selectedDemoImageKey = "";
            imageChoiceChanged = false;
            expectedUpdatedAt = null;
            ownerConfirmed = true;
            initialFormSent = false;
            state.setValue(State.ready(Mode.CREATE, "", false));
            return;
        }
        if (mode == Mode.EDIT && resolvedId.equals(listingId) && subscription != null) {
            return;
        }

        started = true;
        closeSubscription();
        mode = Mode.EDIT;
        listingId = resolvedId;
        existingImageUrl = null;
        existingDemoImageKey = "";
        selectedDemoImageKey = "";
        imageChoiceChanged = false;
        expectedUpdatedAt = null;
        ownerConfirmed = false;
        initialFormSent = false;
        state.setValue(State.loadingForm());
        subscription = repository.observeListing(
                resolvedId,
                new MarketplaceRepository.ListingObserver() {
                    @Override
                    public void onListing(
                            @Nullable MarketplaceListing listing,
                            boolean fromCache) {
                        handleListingSnapshot(listing, fromCache);
                    }

                    @Override
                    public void onError(@NonNull MarketplaceRepository.RepositoryError error) {
                        state.setValue(errorState(Mode.EDIT, error, false, false));
                    }
                });
    }

    private void handleListingSnapshot(
            @Nullable MarketplaceListing listing,
            boolean fromCache) {
        if (listing == null) {
            state.setValue(State.error(
                    Mode.EDIT,
                    State.Kind.NOT_FOUND,
                    fromCache
                            ? "This listing is not in the offline cache. Connect and reopen it."
                            : "This marketplace listing no longer exists.",
                    false,
                    fromCache));
            return;
        }
        String currentUserId = repository.currentUserId();
        if (currentUserId == null) {
            state.setValue(State.error(
                    Mode.EDIT,
                    State.Kind.AUTHENTICATION_REQUIRED,
                    "Sign in again before editing this listing.",
                    false,
                    fromCache));
            return;
        }
        if (!currentUserId.equals(listing.getOwnerId())) {
            state.setValue(State.error(
                    Mode.EDIT,
                    State.Kind.PERMISSION_DENIED,
                    "Only the listing owner can edit these details.",
                    false,
                    fromCache));
            return;
        }
        if (!MarketplaceListingStatusPolicy.canEdit(true, listing.getStatus())) {
            state.setValue(State.error(
                    Mode.EDIT,
                    State.Kind.ERROR,
                    "This listing has an unsupported status and cannot be edited.",
                    false,
                    fromCache));
            return;
        }

        ownerConfirmed = true;
        if (!initialFormSent) {
            initialFormSent = true;
            expectedUpdatedAt = listing.getUpdatedAt();
            existingImageUrl = listing.getImageUrl();
            existingDemoImageKey = DemoImagePolicy.normalize(listing.getDemoImageKey());
            selectedDemoImageKey = existingDemoImageKey;
            initialForm.setValue(listing);
        } else if (expectedUpdatedAt != null
                && listing.getUpdatedAt() != null
                && !expectedUpdatedAt.equals(listing.getUpdatedAt())) {
            State current = state.getValue();
            if (current == null || !current.isBusy()) {
                state.setValue(State.error(
                        Mode.EDIT,
                        State.Kind.CONFLICT,
                        "This listing changed on another device. Reopen it before saving.",
                        true,
                        fromCache));
            }
            return;
        }
        State current = state.getValue();
        if (current == null || !current.isBusy()) {
            state.setValue(State.ready(
                    Mode.EDIT,
                    readyMessage(fromCache),
                    fromCache));
        }
    }

    @NonNull
    public File createCaptureFile() {
        deletePendingCapture();
        pendingCaptureFile = imageProcessor.createCaptureFile();
        return pendingCaptureFile;
    }

    public void processGalleryImage(@NonNull Uri uri) {
        processImage(imageProcessor.process(uri));
    }

    /** Consumes only an app-private processed image transferred by the AI scanner. */
    public void processTransferredImage(@Nullable String absolutePath) {
        if (absolutePath == null || absolutePath.trim().isEmpty() || hasSelectedImage()) {
            return;
        }
        File transferred = ScannerImageProcessor.resolveTransferredImage(
                getApplication(), absolutePath);
        if (transferred == null) {
            showImageMessage(
                    "The scan photo is no longer available. You can add another photo.");
            return;
        }
        processImage(imageProcessor.process(transferred));
    }

    public void processCapturedImage(@NonNull File captureFile) {
        if (cleared || pendingCaptureFile != captureFile) {
            ScannerImageProcessor.deleteQuietly(captureFile);
            return;
        }
        pendingCaptureFile = null;
        processImage(imageProcessor.process(captureFile));
    }

    public void markCaptureStarted() {
        state.setValue(State.working(
                State.Kind.PROCESSING_IMAGE,
                mode,
                "Capturing photo...",
                0));
    }

    public void captureFailed(@NonNull File captureFile, @NonNull String message) {
        if (pendingCaptureFile == captureFile) {
            pendingCaptureFile = null;
        }
        ScannerImageProcessor.deleteQuietly(captureFile);
        state.setValue(State.error(
                mode, State.Kind.ERROR, message, true, false));
    }

    public void showImageMessage(@NonNull String message) {
        State current = state.getValue();
        if (current != null && current.isBusy()) {
            return;
        }
        state.setValue(State.ready(mode, message, false));
    }

    public void selectDemoImage(@Nullable String demoImageKey) {
        State current = state.getValue();
        if (current != null && current.isBusy()) {
            return;
        }
        String normalized = DemoImagePolicy.normalize(demoImageKey);
        if (!DemoImagePolicy.isSelected(normalized)) {
            state.setValue(State.error(
                    mode, State.Kind.ERROR, "Choose a valid built-in demo image.", true, false));
            return;
        }
        imageGeneration.incrementAndGet();
        if (imageFuture != null) {
            imageFuture.cancel(true);
            imageFuture = null;
        }
        deleteSelectedImage();
        selectedDemoImageKey = normalized;
        imageChoiceChanged = true;
        state.setValue(State.ready(
                mode,
                "Built-in demo image selected. No cloud photo upload is needed.",
                false));
    }

    public void discardSelectedImage() {
        State current = state.getValue();
        if (current != null && current.isBusy()) {
            return;
        }
        imageGeneration.incrementAndGet();
        if (imageFuture != null) {
            imageFuture.cancel(true);
            imageFuture = null;
        }
        deleteSelectedImage();
        selectedDemoImageKey = "";
        imageChoiceChanged = true;
        state.setValue(State.ready(
                mode,
                mode == Mode.EDIT && (existingImageUrl != null
                        || DemoImagePolicy.isSelected(existingDemoImageKey))
                        ? "Image cleared. Save the listing to remove its current image."
                        : "Image selection cleared. An image is optional.",
                false));
    }

    private void processImage(
            @NonNull CompletableFuture<ScannerImageProcessor.ProcessedImage> future) {
        long generation = imageGeneration.incrementAndGet();
        deleteSelectedImage();
        if (imageFuture != null) {
            imageFuture.cancel(true);
        }
        imageFuture = future;
        state.setValue(State.working(
                State.Kind.PROCESSING_IMAGE,
                mode,
                "Preparing the photo safely...",
                0));
        future.whenComplete((image, error) -> ContextCompat.getMainExecutor(getApplication())
                .execute(() -> {
                    if (cleared || generation != imageGeneration.get()) {
                        ScannerImageProcessor.deleteQuietly(image);
                        return;
                    }
                    imageFuture = null;
                    if (error != null || image == null) {
                        state.setValue(State.error(
                                mode,
                                State.Kind.ERROR,
                                imageFailureMessage(error),
                                true,
                                false));
                        return;
                    }
                    selectedImage = image;
                    selectedDemoImageKey = "";
                    imageChoiceChanged = true;
                    state.setValue(State.ready(
                            mode,
                            "Photo ready. It will upload only when you save the listing.",
                            false));
                }));
    }

    public void submit(
            @Nullable String title,
            @Nullable String category,
            @Nullable String condition,
            @Nullable String transactionIntent,
            @Nullable String fulfilmentMethod,
            @Nullable String price,
            @Nullable String exchangeTerms,
            @Nullable String description) {
        State current = state.getValue();
        if (current != null && current.isBusy()) {
            return;
        }

        MarketplaceListingValidator.ValidationResult validation =
                MarketplaceListingValidator.validate(
                        title,
                        category,
                        condition,
                        transactionIntent,
                        fulfilmentMethod,
                        price,
                        exchangeTerms,
                        description,
                        selectedDemoImageKey);
        if (!validation.isValid()) {
            state.setValue(State.error(
                    mode,
                    State.Kind.ERROR,
                    validation.getErrorMessage(),
                    mode == Mode.CREATE || ownerConfirmed,
                    false));
            return;
        }

        NewMarketplaceListing listing = validation.getListing();
        if (listing == null) {
            state.setValue(State.error(
                    mode,
                    State.Kind.ERROR,
                    "Review the listing details and try again.",
                    mode == Mode.CREATE || ownerConfirmed,
                    false));
            return;
        }
        if (mode == Mode.EDIT
                && (!ownerConfirmed || listingId.isEmpty() || expectedUpdatedAt == null)) {
            state.setValue(State.error(
                    Mode.EDIT,
                    State.Kind.CONFLICT,
                    "Reopen this listing before saving so the latest version is used.",
                    ownerConfirmed,
                    false));
            return;
        }

        String targetListingId = mode == Mode.CREATE
                ? MarketplaceImagePolicy.newListingId()
                : listingId;
        long generation = operationGeneration.incrementAndGet();
        if (hasSelectedImage()) {
            uploadThenSave(targetListingId, listing, generation);
        } else {
            saveDocument(targetListingId, listing, null, generation);
        }
    }

    private void uploadThenSave(
            @NonNull String targetListingId,
            @NonNull NewMarketplaceListing listing,
            long generation) {
        File imageFile = getSelectedImageFile();
        if (imageFile == null) {
            state.setValue(State.error(
                    mode,
                    State.Kind.ERROR,
                    "The prepared photo is unavailable. Choose it again.",
                    true,
                    false));
            return;
        }
        state.setValue(State.working(
                State.Kind.UPLOADING_IMAGE,
                mode,
                "Uploading photo... 0%",
                0));
        activeUpload = imageRepository.upload(
                targetListingId,
                imageFile,
                new FirebaseMarketplaceImageRepository.UploadCallback() {
                    @Override
                    public void onProgress(int percent) {
                        if (generation == operationGeneration.get() && !cleared) {
                            state.setValue(State.working(
                                    State.Kind.UPLOADING_IMAGE,
                                    mode,
                                    "Uploading photo... " + percent + "%",
                                    percent));
                        }
                    }

                    @Override
                    public void onUploaded(@NonNull String gsUrl) {
                        if (generation != operationGeneration.get() || cleared) {
                            cleanupUploaded(targetListingId, gsUrl, null);
                            return;
                        }
                        activeUpload = null;
                        saveDocument(targetListingId, listing, gsUrl, generation);
                    }

                    @Override
                    public void onError(
                            @NonNull FirebaseMarketplaceImageRepository.RepositoryError error) {
                        if (generation != operationGeneration.get() || cleared) {
                            return;
                        }
                        activeUpload = null;
                        state.setValue(imageErrorState(error));
                    }
                });
    }

    private void saveDocument(
            @NonNull String targetListingId,
            @NonNull NewMarketplaceListing listing,
            @Nullable String uploadedImageUrl,
            long generation) {
        state.setValue(State.working(
                State.Kind.SAVING,
                mode,
                mode == Mode.EDIT ? "Saving listing changes..." : "Publishing listing...",
                100));
        if (mode == Mode.CREATE) {
            repository.createListing(
                    targetListingId,
                    listing,
                    uploadedImageUrl,
                    new MarketplaceRepository.CreateCallback() {
                        @Override
                        public void onCreated(@NonNull String createdId) {
                            if (generation != operationGeneration.get() || cleared) {
                                return;
                            }
                            deleteSelectedImage();
                            state.setValue(State.success(
                                    Mode.CREATE,
                                    uploadedImageUrl != null
                                            ? "Listing and photo published."
                                            : DemoImagePolicy.isSelected(listing.getDemoImageKey())
                                            ? "Listing published with a built-in demo image."
                                            : "Listing published."));
                            activityLog.record(
                                    ActivityLogRepository.TYPE_MARKETPLACE_LISTED,
                                    "Marketplace listing published",
                                    listing.getTitle(),
                                    ActivityLogRepository.DESTINATION_MARKETPLACE,
                                    createdId);
                            completedListing.setValue(new Event<>(createdId));
                        }

                        @Override
                        public void onError(@NonNull MarketplaceRepository.RepositoryError error) {
                            if (generation != operationGeneration.get() || cleared) {
                                return;
                            }
                            cleanupUploaded(targetListingId, uploadedImageUrl, () ->
                                    state.setValue(errorState(
                                            Mode.CREATE, error, true, false)));
                        }
                    });
            return;
        }

        final String finalImageUrl = !imageChoiceChanged && uploadedImageUrl == null
                ? existingImageUrl
                : uploadedImageUrl;

        repository.updateListing(
                targetListingId,
                listing,
                expectedUpdatedAt,
                existingImageUrl,
                existingDemoImageKey,
                finalImageUrl,
                new MarketplaceRepository.MutationCallback() {
                    @Override
                    public void onUpdated() {
                        if (generation != operationGeneration.get() || cleared) {
                            return;
                        }
                        String oldImageUrl = existingImageUrl;
                        existingImageUrl = finalImageUrl;
                        existingDemoImageKey = listing.getDemoImageKey();
                        selectedDemoImageKey = existingDemoImageKey;
                        imageChoiceChanged = false;
                        deleteSelectedImage();
                        activityLog.record(
                                ActivityLogRepository.TYPE_MARKETPLACE_UPDATED,
                                "Marketplace listing updated",
                                listing.getTitle(),
                                ActivityLogRepository.DESTINATION_MARKETPLACE,
                                targetListingId);
                        cleanupOldImageThenComplete(
                                targetListingId, oldImageUrl, finalImageUrl);
                    }

                    @Override
                    public void onError(@NonNull MarketplaceRepository.RepositoryError error) {
                        if (generation != operationGeneration.get() || cleared) {
                            return;
                        }
                        cleanupUploaded(targetListingId, uploadedImageUrl, () ->
                                state.setValue(errorState(
                                        Mode.EDIT, error, true, false)));
                    }
                });
    }

    private void cleanupOldImageThenComplete(
            @NonNull String targetListingId,
            @Nullable String oldImageUrl,
            @Nullable String newImageUrl) {
        if (oldImageUrl == null || Objects.equals(oldImageUrl, newImageUrl)) {
            finishEditSuccess("Listing changes saved.");
            return;
        }
        state.setValue(State.working(
                State.Kind.SAVING,
                Mode.EDIT,
                "Finishing photo replacement...",
                100));
        imageRepository.deleteOwned(
                targetListingId,
                oldImageUrl,
                new FirebaseMarketplaceImageRepository.CompletionCallback() {
                    @Override
                    public void onComplete() {
                        finishEditSuccess("Listing and photo updated.");
                    }

                    @Override
                    public void onError(
                            @NonNull FirebaseMarketplaceImageRepository.RepositoryError error) {
                        finishEditSuccess(
                                "Listing updated. The old photo cleanup needs an online retry.");
                    }
                });
    }

    private void finishEditSuccess(@NonNull String message) {
        if (cleared) {
            return;
        }
        state.setValue(State.success(Mode.EDIT, message));
        completedListing.setValue(new Event<>(listingId));
    }

    private void cleanupUploaded(
            @NonNull String targetListingId,
            @Nullable String uploadedImageUrl,
            @Nullable Runnable afterCleanup) {
        if (uploadedImageUrl == null) {
            if (afterCleanup != null) {
                afterCleanup.run();
            }
            return;
        }
        imageRepository.deleteOwned(
                targetListingId,
                uploadedImageUrl,
                new FirebaseMarketplaceImageRepository.CompletionCallback() {
                    @Override
                    public void onComplete() {
                        if (afterCleanup != null && !cleared) {
                            afterCleanup.run();
                        }
                    }

                    @Override
                    public void onError(
                            @NonNull FirebaseMarketplaceImageRepository.RepositoryError error) {
                        if (afterCleanup != null && !cleared) {
                            afterCleanup.run();
                        }
                    }
                });
    }

    @NonNull
    private String readyMessage(boolean fromCache) {
        if (fromCache) {
            return "Offline copy shown. Connect before saving changes.";
        }
        if (hasSelectedImage()) {
            return "New photo ready. It will upload when you save.";
        }
        return "";
    }

    @NonNull
    private State imageErrorState(
            @NonNull FirebaseMarketplaceImageRepository.RepositoryError error) {
        State.Kind kind = switch (error.getType()) {
            case CONFIGURATION_REQUIRED -> State.Kind.CONFIGURATION_REQUIRED;
            case AUTHENTICATION_REQUIRED -> State.Kind.AUTHENTICATION_REQUIRED;
            case PERMISSION_DENIED -> State.Kind.PERMISSION_DENIED;
            default -> State.Kind.ERROR;
        };
        return State.error(mode, kind, error.getMessage(), true, false);
    }

    @NonNull
    private static State errorState(
            @NonNull Mode mode,
            @NonNull MarketplaceRepository.RepositoryError error,
            boolean formAvailable,
            boolean fromCache) {
        State.Kind kind = switch (error.getType()) {
            case CONFIGURATION_REQUIRED -> State.Kind.CONFIGURATION_REQUIRED;
            case AUTHENTICATION_REQUIRED -> State.Kind.AUTHENTICATION_REQUIRED;
            case PERMISSION_DENIED -> State.Kind.PERMISSION_DENIED;
            case NOT_FOUND -> State.Kind.NOT_FOUND;
            case CONFLICT -> State.Kind.CONFLICT;
            default -> State.Kind.ERROR;
        };
        return State.error(mode, kind, error.getMessage(), formAvailable, fromCache);
    }

    @NonNull
    private static String imageFailureMessage(@Nullable Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof ScannerImageProcessor.ImageProcessingException processing) {
                String message = processing.getMessage();
                if (message != null && !message.trim().isEmpty()) {
                    return message;
                }
            }
            current = current.getCause();
        }
        return "This photo could not be opened. Choose a clear JPEG, PNG, WebP, HEIF, or AVIF image.";
    }

    private void cancelActiveWork() {
        operationGeneration.incrementAndGet();
        if (activeUpload != null) {
            activeUpload.cancel();
            activeUpload = null;
        }
    }

    private void deleteSelectedImage() {
        ScannerImageProcessor.deleteQuietly(selectedImage);
        selectedImage = null;
    }

    private void deletePendingCapture() {
        ScannerImageProcessor.deleteQuietly(pendingCaptureFile);
        pendingCaptureFile = null;
    }

    private void closeSubscription() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }

    @Override
    protected void onCleared() {
        cleared = true;
        cancelActiveWork();
        imageGeneration.incrementAndGet();
        if (imageFuture != null) {
            imageFuture.cancel(true);
            imageFuture = null;
        }
        deletePendingCapture();
        deleteSelectedImage();
        closeSubscription();
        imageProcessor.close();
        super.onCleared();
    }

    public static final class State {

        public enum Kind {
            READY,
            LOADING_FORM,
            PROCESSING_IMAGE,
            UPLOADING_IMAGE,
            SAVING,
            SUCCESS,
            ERROR,
            CONFIGURATION_REQUIRED,
            AUTHENTICATION_REQUIRED,
            PERMISSION_DENIED,
            NOT_FOUND,
            CONFLICT
        }

        private final Kind kind;
        private final Mode mode;
        private final String message;
        private final boolean formAvailable;
        private final boolean fromCache;
        private final int progressPercent;

        private State(
                @NonNull Kind kind,
                @NonNull Mode mode,
                @NonNull String message,
                boolean formAvailable,
                boolean fromCache,
                int progressPercent) {
            this.kind = kind;
            this.mode = mode;
            this.message = message;
            this.formAvailable = formAvailable;
            this.fromCache = fromCache;
            this.progressPercent = progressPercent;
        }

        private static State ready(
                @NonNull Mode mode,
                @NonNull String message,
                boolean fromCache) {
            return new State(Kind.READY, mode, message, true, fromCache, 0);
        }

        private static State loadingForm() {
            return new State(
                    Kind.LOADING_FORM,
                    Mode.EDIT,
                    "Loading the latest listing details...",
                    false,
                    false,
                    0);
        }

        private static State working(
                @NonNull Kind kind,
                @NonNull Mode mode,
                @NonNull String message,
                int progressPercent) {
            return new State(kind, mode, message, true, false, progressPercent);
        }

        private static State success(@NonNull Mode mode, @NonNull String message) {
            return new State(Kind.SUCCESS, mode, message, true, false, 100);
        }

        private static State error(
                @NonNull Mode mode,
                @NonNull Kind kind,
                @Nullable String message,
                boolean formAvailable,
                boolean fromCache) {
            String fallback = mode == Mode.EDIT
                    ? "Could not save this listing. Please try again."
                    : "Review the listing details and try again.";
            return new State(
                    kind,
                    mode,
                    message == null || message.trim().isEmpty() ? fallback : message,
                    formAvailable,
                    fromCache,
                    0);
        }

        @NonNull
        public Kind getKind() {
            return kind;
        }

        @NonNull
        public Mode getMode() {
            return mode;
        }

        @NonNull
        public String getMessage() {
            return message;
        }

        public boolean isFormAvailable() {
            return formAvailable;
        }

        public boolean isFromCache() {
            return fromCache;
        }

        public int getProgressPercent() {
            return progressPercent;
        }

        public boolean isBusy() {
            return kind == Kind.LOADING_FORM
                    || kind == Kind.PROCESSING_IMAGE
                    || kind == Kind.UPLOADING_IMAGE
                    || kind == Kind.SAVING;
        }
    }

    /** One-shot event that survives configuration changes without repeating navigation. */
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
