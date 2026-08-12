package com.propcycle.app.ui.scanner;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.propcycle.app.data.scanner.ScanAnalysis;
import com.propcycle.app.data.scanner.ScannerAiRepository;
import com.propcycle.app.data.scanner.ScannerImageProcessor;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/** Owns temporary scanner images and one bounded AI request across configuration changes. */
public final class ScannerViewModel extends AndroidViewModel {

    private final ScannerImageProcessor imageProcessor;
    private final ScannerAiRepository aiRepository;
    private final MutableLiveData<ScannerUiState> state =
            new MutableLiveData<>(ScannerUiState.idle());
    private final MutableLiveData<Event<ScanAnalysis>> completedAnalysis =
            new MutableLiveData<>();
    private final AtomicLong imageGeneration = new AtomicLong();

    @Nullable
    private ScannerImageProcessor.ProcessedImage currentImage;
    @Nullable
    private CompletableFuture<ScannerImageProcessor.ProcessedImage> imageFuture;
    @Nullable
    private File pendingCaptureFile;
    private boolean cleared;

    public ScannerViewModel(@NonNull Application application) {
        super(application);
        imageProcessor = new ScannerImageProcessor(application);
        aiRepository = new ScannerAiRepository(
                application,
                ContextCompat.getMainExecutor(application));
    }

    @NonNull
    public LiveData<ScannerUiState> getState() {
        return state;
    }

    @NonNull
    public LiveData<Event<ScanAnalysis>> getCompletedAnalysis() {
        return completedAnalysis;
    }

    public boolean hasImage() {
        return currentImage != null;
    }

    @Nullable
    public File getCurrentImageFile() {
        return currentImage == null ? null : currentImage.getFile();
    }

    @NonNull
    public File createCaptureFile() {
        deletePendingCapture();
        pendingCaptureFile = imageProcessor.createCaptureFile();
        return pendingCaptureFile;
    }

    public void processGalleryImage(@NonNull Uri uri) {
        process(imageProcessor.process(uri));
    }

    public void processCapturedImage(@NonNull File captureFile) {
        if (cleared || pendingCaptureFile != captureFile) {
            ScannerImageProcessor.deleteQuietly(captureFile);
            return;
        }
        pendingCaptureFile = null;
        process(imageProcessor.process(captureFile));
    }

    private void process(
            @NonNull CompletableFuture<ScannerImageProcessor.ProcessedImage> future) {
        long generation = imageGeneration.incrementAndGet();
        aiRepository.cancelActive();
        deleteCurrentImage();
        if (imageFuture != null) {
            imageFuture.cancel(true);
        }
        imageFuture = future;
        state.setValue(ScannerUiState.of(
                ScannerUiState.Kind.PROCESSING_IMAGE,
                "Preparing the photo safely...",
                false));

        future.whenComplete((image, error) -> ContextCompat.getMainExecutor(getApplication())
                .execute(() -> {
                    if (cleared || generation != imageGeneration.get()) {
                        if (image != null) {
                            image.delete();
                        }
                        return;
                    }
                    imageFuture = null;
                    if (error != null || image == null) {
                        state.setValue(ScannerUiState.of(
                                ScannerUiState.Kind.ERROR,
                                "This photo could not be opened. Choose a clear JPEG, PNG, or WebP image.",
                                false));
                        return;
                    }
                    currentImage = image;
                    state.setValue(ScannerUiState.of(
                            ScannerUiState.Kind.IMAGE_READY,
                            "Photo ready. Read the notice, tick the box, then analyse it.",
                            true));
                }));
    }

    public void markCaptureStarted() {
        if (currentImage == null) {
            state.setValue(ScannerUiState.of(
                    ScannerUiState.Kind.CAPTURING,
                    "Capturing photo...",
                    false));
        }
    }

    public void captureFailed(@NonNull File captureFile, @NonNull String message) {
        if (pendingCaptureFile != captureFile) {
            ScannerImageProcessor.deleteQuietly(captureFile);
            return;
        }
        pendingCaptureFile = null;
        ScannerImageProcessor.deleteQuietly(captureFile);
        ScannerUiState current = state.getValue();
        if (!cleared && current != null && current.getKind() == ScannerUiState.Kind.CAPTURING) {
            state.setValue(ScannerUiState.of(
                    ScannerUiState.Kind.ERROR,
                    message,
                    false));
        }
    }

    public void showCameraStarting() {
        if (currentImage == null && !isBusy()) {
            state.setValue(ScannerUiState.of(
                    ScannerUiState.Kind.CAMERA_STARTING,
                    "Starting camera...",
                    false));
        }
    }

    public void showCameraReady() {
        if (currentImage == null && !isBusy()) {
            state.setValue(ScannerUiState.of(
                    ScannerUiState.Kind.CAMERA_READY,
                    "Camera ready. Put one item inside the frame and tap the camera button.",
                    false));
        }
    }

    public void showCameraMessage(@NonNull String message) {
        if (currentImage == null && !isBusy()) {
            state.setValue(ScannerUiState.of(
                    ScannerUiState.Kind.ERROR,
                    message,
                    false));
        }
    }

    public void clearImageForCamera() {
        imageGeneration.incrementAndGet();
        aiRepository.cancelActive();
        if (imageFuture != null) {
            imageFuture.cancel(true);
            imageFuture = null;
        }
        deletePendingCapture();
        deleteCurrentImage();
        state.setValue(ScannerUiState.idle());
    }

    public void analyze(boolean disclosureAccepted) {
        ScannerUiState currentState = state.getValue();
        if (currentImage == null) {
            state.setValue(ScannerUiState.of(
                    ScannerUiState.Kind.ERROR,
                    "Take or choose a photo first.",
                    false));
            return;
        }
        if (!disclosureAccepted) {
            state.setValue(ScannerUiState.of(
                    ScannerUiState.Kind.ERROR,
                    "Tick the consent box before sending the image for AI analysis.",
                    true));
            return;
        }
        if ((currentState != null && currentState.isBusy()) || aiRepository.hasActiveRequest()) {
            return;
        }

        state.setValue(ScannerUiState.of(
                ScannerUiState.Kind.ANALYZING,
                "Gemini is analysing this item. Keep this screen open...",
                true));
        ScannerImageProcessor.ProcessedImage requestedImage = currentImage;
        aiRepository.analyze(requestedImage, new ScannerAiRepository.Callback() {
            @Override
            public void onSuccess(@NonNull ScanAnalysis analysis) {
                if (cleared || requestedImage != currentImage) {
                    return;
                }
                deleteCurrentImage();
                state.setValue(ScannerUiState.idle());
                completedAnalysis.setValue(new Event<>(analysis));
            }

            @Override
            public void onFailure(@NonNull ScannerAiRepository.Failure failure) {
                if (cleared || requestedImage != currentImage) {
                    return;
                }
                ScannerUiState.Kind stateKind = switch (failure.getKind()) {
                    case CONFIGURATION_REQUIRED, SETUP_REQUIRED,
                            APP_CHECK_OR_PERMISSION ->
                            ScannerUiState.Kind.CONFIGURATION_REQUIRED;
                    case AUTHENTICATION_REQUIRED ->
                            ScannerUiState.Kind.AUTHENTICATION_REQUIRED;
                    default -> ScannerUiState.Kind.ERROR;
                };
                state.setValue(ScannerUiState.of(
                        stateKind,
                        failure.getMessage(),
                        true));
            }
        });
    }

    public void resetAfterResult() {
        state.setValue(ScannerUiState.idle());
    }

    private boolean isBusy() {
        ScannerUiState currentState = state.getValue();
        return currentState != null && currentState.isBusy();
    }

    private void deleteCurrentImage() {
        if (currentImage != null) {
            currentImage.delete();
            currentImage = null;
        }
    }

    private void deletePendingCapture() {
        if (pendingCaptureFile != null) {
            ScannerImageProcessor.deleteQuietly(pendingCaptureFile);
            pendingCaptureFile = null;
        }
    }

    @Override
    protected void onCleared() {
        cleared = true;
        imageGeneration.incrementAndGet();
        if (imageFuture != null) {
            imageFuture.cancel(true);
        }
        deletePendingCapture();
        deleteCurrentImage();
        aiRepository.close();
        imageProcessor.close();
    }

    /** One-shot result event that will not navigate twice after a rotation. */
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
