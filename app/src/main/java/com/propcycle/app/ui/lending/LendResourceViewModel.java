package com.propcycle.app.ui.lending;

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
import com.propcycle.app.data.lending.FirebaseLendingImageRepository;
import com.propcycle.app.data.lending.FirestoreLendingRepository;
import com.propcycle.app.data.lending.LendingImagePolicy;
import com.propcycle.app.data.lending.LendingItem;
import com.propcycle.app.data.lending.LendingPolicy;
import com.propcycle.app.data.lending.NewLendingItem;
import com.propcycle.app.data.scanner.ScannerImageProcessor;
import com.propcycle.app.ui.common.OneTimeEvent;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/** Create or owner-edit one lending item with an optional protected image. */
public final class LendResourceViewModel extends AndroidViewModel {

    public static final class State {
        private final boolean loading;
        private final boolean busy;
        private final boolean formAvailable;
        private final int progress;
        private final String message;

        private State(
                boolean loading,
                boolean busy,
                boolean formAvailable,
                int progress,
                @Nullable String message) {
            this.loading = loading;
            this.busy = busy;
            this.formAvailable = formAvailable;
            this.progress = progress;
            this.message = message;
        }

        public boolean isLoading() { return loading; }
        public boolean isBusy() { return busy; }
        public boolean isFormAvailable() { return formAvailable; }
        public int getProgress() { return progress; }
        @Nullable public String getMessage() { return message; }
    }

    private final FirestoreLendingRepository repository;
    private final FirebaseLendingImageRepository imageRepository;
    private final ScannerImageProcessor imageProcessor;
    private final ActivityLogRepository activityLog;
    private final MutableLiveData<State> state = new MutableLiveData<>(
            new State(false, false, true, 0, null));
    private final MutableLiveData<LendingItem> initialItem = new MutableLiveData<>();
    private final MutableLiveData<OneTimeEvent<String>> completedItemId =
            new MutableLiveData<>();
    private FirestoreLendingRepository.Subscription subscription =
            FirestoreLendingRepository.Subscription.NONE;
    private FirebaseLendingImageRepository.UploadHandle uploadHandle;
    private CompletableFuture<ScannerImageProcessor.ProcessedImage> imageFuture;
    private final AtomicLong imageGeneration = new AtomicLong();
    private ScannerImageProcessor.ProcessedImage selectedImage;
    private String itemId = "";
    private String existingImageUrl;
    private String existingDemoImageKey = "";
    private String selectedDemoImageKey = "";
    private Timestamp expectedUpdatedAt;
    private boolean editMode;
    private boolean ownerConfirmed;
    private boolean imageChoiceChanged;
    private boolean initialItemSent;
    private boolean cleared;

    public LendResourceViewModel(@NonNull Application application) {
        super(application);
        repository = new FirestoreLendingRepository(application);
        imageRepository = new FirebaseLendingImageRepository(application);
        imageProcessor = new ScannerImageProcessor(application);
        activityLog = new ActivityLogRepository(application);
    }

    @NonNull public LiveData<State> getState() { return state; }
    @NonNull public LiveData<LendingItem> getInitialItem() { return initialItem; }
    @NonNull public LiveData<OneTimeEvent<String>> getCompletedItemId() {
        return completedItemId;
    }
    public boolean isEditMode() { return editMode; }
    @Nullable public File getSelectedImageFile() {
        return selectedImage == null ? null : selectedImage.getFile();
    }
    @Nullable public String getExistingImageUrl() {
        return imageChoiceChanged ? null : existingImageUrl;
    }
    @NonNull public String getSelectedDemoImageKey() { return selectedDemoImageKey; }

    public void start(@Nullable String requestedItemId) {
        String cleanId = requestedItemId == null ? "" : requestedItemId.trim();
        stopSubscription();
        editMode = !cleanId.isEmpty();
        itemId = editMode ? cleanId : LendingImagePolicy.newItemId();
        existingImageUrl = null;
        existingDemoImageKey = "";
        selectedDemoImageKey = "";
        expectedUpdatedAt = null;
        ownerConfirmed = !editMode;
        imageChoiceChanged = false;
        initialItemSent = false;
        if (!editMode) {
            state.setValue(new State(false, false, true, 0,
                    "A photo and approximate map point are optional."));
            return;
        }
        state.setValue(new State(true, false, false, 0, "Loading lending item..."));
        subscription = repository.observeItem(cleanId,
                new FirestoreLendingRepository.SnapshotCallback<>() {
                    @Override
                    public void onData(@NonNull LendingItem item, boolean fromCache) {
                        String uid = repository.currentUserId();
                        if (uid == null || !uid.equals(item.getOwnerId())) {
                            ownerConfirmed = false;
                            state.setValue(new State(false, false, false, 0,
                                    "Only the item owner can edit these details."));
                            return;
                        }
                        ownerConfirmed = true;
                        existingImageUrl = item.getImageUrl();
                        existingDemoImageKey = DemoImagePolicy.normalize(item.getDemoImageKey());
                        selectedDemoImageKey = existingDemoImageKey;
                        if (!initialItemSent) {
                            initialItemSent = true;
                            expectedUpdatedAt = item.getUpdatedAt();
                            initialItem.setValue(item);
                        } else if (expectedUpdatedAt != null
                                && item.getUpdatedAt() != null
                                && !expectedUpdatedAt.equals(item.getUpdatedAt())) {
                            state.setValue(new State(false, false, false, 0,
                                    "This item changed on another device. Reopen it before saving."));
                            return;
                        }
                        state.setValue(new State(false, false, true, 0,
                                fromCache
                                        ? "Offline copy shown. Reconnect before saving."
                                        : "Edit the details, then save your changes."));
                    }

                    @Override
                    public void onError(@NonNull Exception error) {
                        state.setValue(new State(false, false, false, 0,
                                LendingListViewModel.safeMessage(
                                        error, "This lending item could not be loaded.")));
                    }
                });
    }

    public void processImage(@NonNull Uri uri) {
        if (isBusy()) {
            return;
        }
        long generation = imageGeneration.incrementAndGet();
        deleteSelectedImage();
        if (imageFuture != null) {
            imageFuture.cancel(true);
        }
        state.setValue(new State(false, true, true, 0, "Preparing photo safely..."));
        imageFuture = imageProcessor.process(uri);
        imageFuture.whenComplete((image, error) ->
                ContextCompat.getMainExecutor(getApplication()).execute(() -> {
                    imageFuture = null;
                    if (cleared || generation != imageGeneration.get()) {
                        ScannerImageProcessor.deleteQuietly(image);
                        return;
                    }
                    if (error != null || image == null) {
                        state.setValue(new State(false, false, true, 0,
                                "The photo could not be prepared. Choose another JPEG image."));
                        return;
                    }
                    selectedImage = image;
                    selectedDemoImageKey = "";
                    imageChoiceChanged = true;
                    state.setValue(new State(false, false, true, 0,
                            "Photo ready. It uploads only when you save."));
                }));
    }

    /** Consumes only an app-private processed image transferred by the AI scanner. */
    public void processTransferredImage(@Nullable String absolutePath) {
        if (absolutePath == null || absolutePath.trim().isEmpty()
                || selectedImage != null || isBusy()) {
            return;
        }
        File transferred = ScannerImageProcessor.resolveTransferredImage(
                getApplication(), absolutePath);
        if (transferred == null) {
            state.setValue(new State(false, false, true, 0,
                    "The scan photo is no longer available. You can add another photo."));
            return;
        }
        processImageFuture(imageProcessor.process(transferred));
    }

    private void processImageFuture(
            @NonNull CompletableFuture<ScannerImageProcessor.ProcessedImage> future) {
        long generation = imageGeneration.incrementAndGet();
        deleteSelectedImage();
        if (imageFuture != null) {
            imageFuture.cancel(true);
        }
        state.setValue(new State(false, true, true, 0, "Preparing photo safely..."));
        imageFuture = future;
        imageFuture.whenComplete((image, error) ->
                ContextCompat.getMainExecutor(getApplication()).execute(() -> {
                    imageFuture = null;
                    if (cleared || generation != imageGeneration.get()) {
                        ScannerImageProcessor.deleteQuietly(image);
                        return;
                    }
                    if (error != null || image == null) {
                        state.setValue(new State(false, false, true, 0,
                                "The photo could not be prepared. Choose another JPEG image."));
                        return;
                    }
                    selectedImage = image;
                    selectedDemoImageKey = "";
                    imageChoiceChanged = true;
                    state.setValue(new State(false, false, true, 0,
                            "Photo ready. It uploads only when you save."));
                }));
    }

    public void removePhoto() {
        if (isBusy()) {
            return;
        }
        if (imageFuture != null) {
            imageGeneration.incrementAndGet();
            imageFuture.cancel(true);
            imageFuture = null;
        }
        deleteSelectedImage();
        selectedDemoImageKey = "";
        imageChoiceChanged = true;
        state.setValue(new State(false, false, true, 0,
                editMode && (existingImageUrl != null
                        || DemoImagePolicy.isSelected(existingDemoImageKey))
                        ? "The current image will be removed after you save."
                        : "Image selection cleared."));
    }

    public void selectDemoImage(@Nullable String demoImageKey) {
        if (isBusy()) {
            return;
        }
        String normalized = DemoImagePolicy.normalize(demoImageKey);
        if (!DemoImagePolicy.isSelected(normalized)) {
            state.setValue(new State(false, false, true, 0,
                    "Choose a valid built-in demo image."));
            return;
        }
        if (imageFuture != null) {
            imageGeneration.incrementAndGet();
            imageFuture.cancel(true);
            imageFuture = null;
        }
        deleteSelectedImage();
        selectedDemoImageKey = normalized;
        imageChoiceChanged = true;
        state.setValue(new State(false, false, true, 0,
                "Built-in demo image selected. No cloud photo upload is needed."));
    }

    public void submit(
            @Nullable String title,
            @Nullable String description,
            @Nullable String category,
            @Nullable String condition,
            @Nullable String pickupMethod,
            @Nullable String area,
            @Nullable String maxDays,
            @Nullable String deposit,
            @Nullable Double latitude,
            @Nullable Double longitude) {
        if (isBusy()) {
            return;
        }
        final NewLendingItem input;
        try {
            input = LendingPolicy.validateItem(
                    title, description, category, condition, pickupMethod,
                    area, maxDays, deposit, latitude, longitude, selectedDemoImageKey);
        } catch (IllegalArgumentException error) {
            state.setValue(new State(false, false, true, 0, error.getMessage()));
            return;
        }
        if (editMode && (!ownerConfirmed || expectedUpdatedAt == null)) {
            state.setValue(new State(false, false, false, 0,
                    "Reopen this item before saving the latest version."));
            return;
        }
        File image = getSelectedImageFile();
        if (image == null) {
            String finalImageUrl = imageChoiceChanged ? null : existingImageUrl;
            save(input, finalImageUrl, null);
            return;
        }
        state.setValue(new State(false, true, true, 0, "Uploading photo... 0%"));
        uploadHandle = imageRepository.upload(itemId, image,
                new FirebaseLendingImageRepository.UploadCallback() {
                    @Override public void onProgress(int percent) {
                        if (!cleared) {
                            state.setValue(new State(false, true, true, percent,
                                    "Uploading photo... " + percent + "%"));
                        }
                    }

                    @Override public void onUploaded(@NonNull String gsUrl) {
                        uploadHandle = null;
                        if (cleared) {
                            imageRepository.deleteOwned(itemId, gsUrl, quietCompletion());
                            return;
                        }
                        save(input, gsUrl, gsUrl);
                    }

                    @Override public void onError(@NonNull String message) {
                        uploadHandle = null;
                        if (!cleared) {
                            state.setValue(new State(false, false, true, 0, message));
                        }
                    }
                });
    }

    private void save(
            @NonNull NewLendingItem input,
            @Nullable String imageUrl,
            @Nullable String newlyUploadedUrl) {
        state.setValue(new State(false, true, true, 100,
                editMode ? "Saving changes..." : "Publishing lending item..."));
        com.google.android.gms.tasks.Task<Void> task = editMode
                ? repository.updateItem(itemId, input, imageUrl, expectedUpdatedAt)
                : repository.createItem(itemId, input, imageUrl);
        task.addOnSuccessListener(ignored -> {
            String oldUrl = existingImageUrl;
            if (oldUrl != null && !oldUrl.equals(imageUrl)) {
                imageRepository.deleteOwned(itemId, oldUrl, quietCompletion());
            }
            existingImageUrl = imageUrl;
            existingDemoImageKey = input.getDemoImageKey();
            selectedDemoImageKey = existingDemoImageKey;
            imageChoiceChanged = false;
            deleteSelectedImage();
            activityLog.record(
                    editMode
                            ? ActivityLogRepository.TYPE_LENDING_UPDATED
                            : ActivityLogRepository.TYPE_LENDING_LISTED,
                    editMode ? "Lending item updated" : "Lending item published",
                    input.getTitle(),
                    ActivityLogRepository.DESTINATION_LENDING_ITEM,
                    itemId);
            completedItemId.setValue(new OneTimeEvent<>(itemId));
            state.setValue(new State(false, false, true, 100, "Lending item saved."));
        }).addOnFailureListener(error -> {
            if (newlyUploadedUrl != null) {
                imageRepository.deleteOwned(itemId, newlyUploadedUrl, quietCompletion());
            }
            state.setValue(new State(false, false, true, 0,
                    LendingListViewModel.safeMessage(
                            error, "The lending item could not be saved.")));
        });
    }

    @NonNull
    private static FirebaseLendingImageRepository.CompletionCallback quietCompletion() {
        return new FirebaseLendingImageRepository.CompletionCallback() {
            @Override public void onComplete() { }
            @Override public void onError(@NonNull String message) { }
        };
    }

    private boolean isBusy() {
        State current = state.getValue();
        return current != null && current.isBusy();
    }

    private void deleteSelectedImage() {
        if (selectedImage != null) {
            selectedImage.delete();
            selectedImage = null;
        }
    }

    private void stopSubscription() {
        subscription.remove();
        subscription = FirestoreLendingRepository.Subscription.NONE;
    }

    @Override
    protected void onCleared() {
        cleared = true;
        stopSubscription();
        if (uploadHandle != null) {
            uploadHandle.cancel();
        }
        if (imageFuture != null) {
            imageGeneration.incrementAndGet();
            imageFuture.cancel(true);
        }
        deleteSelectedImage();
        imageProcessor.close();
    }
}
