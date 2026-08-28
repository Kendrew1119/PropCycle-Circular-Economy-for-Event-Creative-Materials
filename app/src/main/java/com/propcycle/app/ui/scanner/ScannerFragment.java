package com.propcycle.app.ui.scanner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.util.concurrent.ListenableFuture;
import com.propcycle.app.R;
import com.propcycle.app.data.scanner.ScanAnalysis;
import com.propcycle.app.databinding.FragmentScannerBinding;
import com.propcycle.app.ui.common.ScreenNavigation;

import java.io.File;
import java.util.concurrent.ExecutionException;

/** CameraX and Photo Picker input for the proposal's AI Smart Scanner screen. */
public final class ScannerFragment extends Fragment {

    private static final String STATE_PERMISSION_REQUESTED = "cameraPermissionRequested";

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    this::onCameraPermissionResult);
    private final ActivityResultLauncher<PickVisualMediaRequest> photoPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.PickVisualMedia(),
                    this::onPhotoPicked);

    @Nullable
    private FragmentScannerBinding binding;
    @Nullable
    private ScannerViewModel viewModel;
    @Nullable
    private PreviewView previewView;
    @Nullable
    private ProcessCameraProvider cameraProvider;
    @Nullable
    private ImageCapture imageCapture;
    @Nullable
    private String displayedImagePath;
    private boolean cameraBound;
    private boolean cameraStartInFlight;
    private boolean cameraDesired;
    private boolean cameraPermissionRequested;
    private long cameraGeneration;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentScannerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cameraPermissionRequested = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_PERMISSION_REQUESTED, false);
        ScreenNavigation.bindChrome(this, view);
        viewModel = new ViewModelProvider(this).get(ScannerViewModel.class);

        createPreviewView();
        binding.primaryAction.setOnClickListener(ignored -> onCameraAction());
        binding.secondaryAction.setOnClickListener(ignored -> openPhotoPicker());
        binding.analyzeAction.setOnClickListener(ignored -> analyzeSelectedImage());
        binding.retryAction.setOnClickListener(ignored -> retryCurrentStep());
        binding.scannerDisclosureCheckbox.setOnCheckedChangeListener(
                (button, checked) -> updateAnalyzeEnabled());

        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getCompletedAnalysis().observe(getViewLifecycleOwner(), event -> {
            ScannerViewModel.CompletedScan completed =
                    event == null ? null : event.getIfNotHandled();
            if (completed == null || binding == null) {
                return;
            }
            stopCamera();
            Bundle arguments = new Bundle();
            arguments.putString("analysisJson", completed.getAnalysis().toJson());
            arguments.putString("scanImagePath", completed.getImagePath());
            Bundle sourceArguments = getArguments();
            arguments.putString(
                    "handoffTarget",
                    sourceArguments == null
                            ? ""
                            : sourceArguments.getString("handoffTarget", ""));
            ScreenNavigation.navigate(this, R.id.aiResultFragment, arguments);
        });

    }

    private void createPreviewView() {
        if (binding == null) {
            return;
        }
        PreviewView created = new PreviewView(requireContext());
        created.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        created.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        created.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        created.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        created.setVisibility(View.GONE);
        binding.scannerPreviewContainer.addView(created, 0);
        binding.scannerPreviewContainer.setClipToOutline(true);
        previewView = created;
    }

    private void onCameraAction() {
        ScannerViewModel model = viewModel;
        if (model == null) {
            return;
        }
        ScannerUiState current = model.getState().getValue();
        if (current != null && current.isBusy()) {
            return;
        }
        if (!deviceHasCamera()) {
            model.showCameraMessage(getString(R.string.scanner_camera_unavailable));
            return;
        }

        if (!hasCameraPermission()) {
            requestCameraPermissionWithExplanation();
            return;
        }
        if (model.hasImage()) {
            binding.scannerDisclosureCheckbox.setChecked(false);
            displayedImagePath = null;
            model.clearImageForCamera();
            startCamera();
            return;
        }
        if (!cameraBound || imageCapture == null) {
            startCamera();
            return;
        }
        capturePhoto();
    }

    private void requestCameraPermissionWithExplanation() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Camera permission")
                    .setMessage("PropCycle needs camera access only while you take an item photo. "
                            + "You can choose a photo instead without this permission.")
                    .setNegativeButton("Choose photo", (dialog, which) -> openPhotoPicker())
                    .setPositiveButton("Continue", (dialog, which) -> launchCameraPermission())
                    .show();
        } else if (cameraPermissionRequested) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Camera permission is off")
                    .setMessage("Turn on Camera permission in Android Settings, or choose a photo instead.")
                    .setNegativeButton("Choose photo", (dialog, which) -> openPhotoPicker())
                    .setPositiveButton("Open Settings", (dialog, which) -> openAppSettings())
                    .show();
        } else {
            launchCameraPermission();
        }
    }

    private void launchCameraPermission() {
        cameraPermissionRequested = true;
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void onCameraPermissionResult(boolean granted) {
        ScannerViewModel model = viewModel;
        if (model == null) {
            return;
        }
        if (granted) {
            startCamera();
        } else {
            model.showCameraMessage(
                    "Camera permission was not granted. You can still choose a photo from your device.");
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", requireContext().getPackageName(), null));
        startActivity(intent);
    }

    private void openPhotoPicker() {
        ScannerViewModel model = viewModel;
        if (model == null) {
            return;
        }
        ScannerUiState current = model.getState().getValue();
        if (current != null && current.isBusy()) {
            return;
        }
        photoPickerLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void onPhotoPicked(@Nullable Uri uri) {
        if (uri == null || viewModel == null) {
            return;
        }
        stopCamera();
        binding.scannerDisclosureCheckbox.setChecked(false);
        displayedImagePath = null;
        viewModel.processGalleryImage(uri);
    }

    private void startCamera() {
        ScannerViewModel model = viewModel;
        if (binding == null || model == null || !hasCameraPermission() || !deviceHasCamera()) {
            return;
        }
        ScannerUiState current = model.getState().getValue();
        if (model.hasImage() || (current != null && current.isBusy())) {
            return;
        }
        cameraDesired = true;
        if (cameraBound || cameraStartInFlight) {
            return;
        }
        cameraStartInFlight = true;
        long generation = ++cameraGeneration;
        model.showCameraStarting();
        ListenableFuture<ProcessCameraProvider> providerFuture =
                ProcessCameraProvider.getInstance(requireContext());
        providerFuture.addListener(() -> {
            if (generation != cameraGeneration
                    || !cameraDesired
                    || binding == null
                    || previewView == null
                    || !isAdded()
                    || model.hasImage()) {
                return;
            }
            ScannerUiState latest = model.getState().getValue();
            if (latest != null && latest.isBusy()) {
                return;
            }
            cameraStartInFlight = false;
            try {
                cameraProvider = providerFuture.get();
                bindCameraUseCases(generation);
            } catch (ExecutionException | InterruptedException cameraError) {
                if (cameraError instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                model.showCameraMessage(
                        "The camera could not start. Try again or choose a photo.");
            } catch (RuntimeException cameraError) {
                model.showCameraMessage(
                        "The camera could not start. Try again or choose a photo.");
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindCameraUseCases(long generation) {
        if (generation != cameraGeneration
                || !cameraDesired
                || binding == null
                || previewView == null
                || cameraProvider == null) {
            return;
        }
        int rotation = previewView.getDisplay() == null
                ? Surface.ROTATION_0
                : previewView.getDisplay().getRotation();
        Preview preview = new Preview.Builder()
                .setTargetRotation(rotation)
                .build();
        ImageCapture capture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(rotation)
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        CameraSelector selector = availableCameraSelector();
        if (selector == null) {
            cameraDesired = false;
            viewModel.showCameraMessage(
                    "No usable camera was found. Choose a photo instead.");
            return;
        }
        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(
                    getViewLifecycleOwner(),
                    selector,
                    preview,
                    capture);
            imageCapture = capture;
            cameraBound = true;
            previewView.setVisibility(View.VISIBLE);
            binding.scannerPreviewPlaceholder.setVisibility(View.GONE);
            viewModel.showCameraReady();
        } catch (RuntimeException cameraError) {
            cameraBound = false;
            imageCapture = null;
            cameraDesired = false;
            viewModel.showCameraMessage(
                    "The camera could not be opened. Choose a photo instead.");
        }
    }

    @Nullable
    private CameraSelector availableCameraSelector() {
        ProcessCameraProvider provider = cameraProvider;
        if (provider == null) {
            return null;
        }
        try {
            if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                return CameraSelector.DEFAULT_BACK_CAMERA;
            }
            if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                return CameraSelector.DEFAULT_FRONT_CAMERA;
            }
        } catch (CameraInfoUnavailableException ignored) {
            return null;
        }
        return null;
    }

    private void capturePhoto() {
        ImageCapture capture = imageCapture;
        ScannerViewModel model = viewModel;
        if (capture == null || model == null) {
            return;
        }
        final File outputFile;
        try {
            outputFile = model.createCaptureFile();
        } catch (RuntimeException fileError) {
            model.showCameraMessage("A temporary photo file could not be created. Try again.");
            return;
        }
        model.markCaptureStarted();
        ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(
                outputFile).build();
        capture.takePicture(
                options,
                ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(
                            @NonNull ImageCapture.OutputFileResults outputFileResults) {
                        stopCamera();
                        model.processCapturedImage(outputFile);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        model.captureFailed(
                                outputFile,
                                "The photo was not captured. Try again or choose a photo.");
                    }
                });
    }

    private void analyzeSelectedImage() {
        if (binding == null || viewModel == null) {
            return;
        }
        viewModel.analyze(binding.scannerDisclosureCheckbox.isChecked());
    }

    private void retryCurrentStep() {
        if (binding == null || viewModel == null) {
            return;
        }
        ScannerUiState current = viewModel.getState().getValue();
        if (current == null) {
            return;
        }
        if (current.getKind() == ScannerUiState.Kind.AUTHENTICATION_REQUIRED) {
            ScreenNavigation.navigateClearingBackStack(this, R.id.loginFragment);
        } else if (current.getKind() == ScannerUiState.Kind.CONFIGURATION_REQUIRED) {
            showSetupHelp();
        } else if (current.hasImage()) {
            analyzeSelectedImage();
        } else if (cameraBound && imageCapture != null) {
            viewModel.showCameraReady();
        } else if (hasCameraPermission()) {
            startCamera();
        } else {
            openPhotoPicker();
        }
    }

    private void showSetupHelp() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("AI scanner setup is required")
                .setMessage("Ask the project owner to finish Firebase AI Logic and App Check setup. "
                        + "Follow docs/AI_SCANNER_SETUP.md, then return here and try again. "
                        + "Do not paste a Gemini API key into the app.")
                .setNegativeButton("Close", null)
                .setPositiveButton("Try again", (dialog, which) -> analyzeSelectedImage())
                .show();
    }

    private void render(@NonNull ScannerUiState state) {
        if (binding == null || viewModel == null) {
            return;
        }
        binding.scannerStatus.setText(state.getMessage());
        boolean busy = state.isBusy();
        binding.scannerProgress.setVisibility(busy ? View.VISIBLE : View.GONE);
        binding.primaryAction.setEnabled(!busy);
        binding.secondaryAction.setEnabled(!busy);

        File imageFile = viewModel.getCurrentImageFile();
        boolean hasImage = state.hasImage() && imageFile != null && imageFile.isFile();
        if (hasImage) {
            String path = imageFile.getAbsolutePath();
            if (!path.equals(displayedImagePath)) {
                displayedImagePath = path;
                binding.scannerDisclosureCheckbox.setChecked(false);
                binding.selectedImage.setImageURI(Uri.fromFile(imageFile));
            }
            binding.selectedImage.setVisibility(View.VISIBLE);
            binding.scannerDisclosureCheckbox.setVisibility(View.VISIBLE);
            if (previewView != null) {
                previewView.setVisibility(View.GONE);
            }
            binding.scannerPreviewPlaceholder.setVisibility(View.GONE);
        } else {
            displayedImagePath = null;
            binding.selectedImage.setImageDrawable(null);
            binding.selectedImage.setVisibility(View.GONE);
            binding.scannerDisclosureCheckbox.setVisibility(View.GONE);
            binding.scannerDisclosureCheckbox.setChecked(false);
            if (previewView != null) {
                previewView.setVisibility(cameraBound ? View.VISIBLE : View.GONE);
            }
            binding.scannerPreviewPlaceholder.setVisibility(
                    cameraBound ? View.GONE : View.VISIBLE);
        }

        boolean showRetry = state.getKind() == ScannerUiState.Kind.ERROR
                || state.getKind() == ScannerUiState.Kind.CONFIGURATION_REQUIRED
                || state.getKind() == ScannerUiState.Kind.AUTHENTICATION_REQUIRED;
        binding.retryAction.setVisibility(showRetry ? View.VISIBLE : View.GONE);
        if (state.getKind() == ScannerUiState.Kind.AUTHENTICATION_REQUIRED) {
            binding.retryAction.setText(R.string.scanner_sign_in);
        } else if (state.getKind() == ScannerUiState.Kind.CONFIGURATION_REQUIRED) {
            binding.retryAction.setText(R.string.scanner_setup_help);
        } else {
            binding.retryAction.setText(R.string.scanner_retry);
        }
        updateAnalyzeEnabled();
    }

    private void updateAnalyzeEnabled() {
        if (binding == null || viewModel == null) {
            return;
        }
        ScannerUiState current = viewModel.getState().getValue();
        boolean busy = current != null && current.isBusy();
        binding.analyzeAction.setEnabled(
                viewModel.hasImage()
                        && binding.scannerDisclosureCheckbox.isChecked()
                        && !busy);
    }

    private boolean deviceHasCamera() {
        return requireContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void stopCamera() {
        cameraDesired = false;
        cameraStartInFlight = false;
        cameraGeneration++;
        cameraBound = false;
        imageCapture = null;
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        if (previewView != null) {
            previewView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null
                && !viewModel.hasImage()
                && hasCameraPermission()
                && viewModel.getState().getValue() != null
                && !viewModel.getState().getValue().isBusy()) {
            startCamera();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(STATE_PERMISSION_REQUESTED, cameraPermissionRequested);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        stopCamera();
        previewView = null;
        binding = null;
        super.onDestroyView();
    }
}
