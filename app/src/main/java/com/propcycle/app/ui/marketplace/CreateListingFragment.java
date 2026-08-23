package com.propcycle.app.ui.marketplace;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.util.concurrent.ListenableFuture;
import com.propcycle.app.R;
import com.propcycle.app.data.marketplace.MarketplaceImageLoader;
import com.propcycle.app.data.marketplace.MarketplaceListing;
import com.propcycle.app.data.marketplace.MarketplaceListingValidator;
import com.propcycle.app.databinding.FragmentCreateListingBinding;
import com.propcycle.app.ui.common.ScreenNavigation;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/** Create/edit form with one optional, safely processed marketplace image. */
public final class CreateListingFragment extends Fragment {

    private static final String ARG_LISTING_ID = "listingId";
    private static final String STATE_PERMISSION_REQUESTED = "cameraPermissionRequested";

    private FragmentCreateListingBinding binding;
    private CreateListingViewModel viewModel;
    private MarketplaceImageLoader imageLoader;
    private MarketplaceImageLoader.LoadHandle imageLoadHandle;
    private PreviewView previewView;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private boolean editMode;
    private boolean cameraPermissionRequested;
    private boolean cameraDesired;
    private boolean cameraBound;
    private boolean cameraStartInFlight;
    private long cameraGeneration;
    private String displayedLocalPath;
    private String displayedRemoteUrl;
    private OnBackPressedCallback backPressedCallback;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    this::onCameraPermissionResult);
    private final ActivityResultLauncher<PickVisualMediaRequest> photoPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.PickVisualMedia(),
                    this::onPhotoPicked);

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateListingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cameraPermissionRequested = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_PERMISSION_REQUESTED, false);
        Bundle arguments = getArguments();
        String listingId = arguments == null ? "" : arguments.getString(ARG_LISTING_ID, "");
        editMode = listingId != null && !listingId.trim().isEmpty();

        ScreenNavigation.bindChrome(this, view);
        bindBackProtection();
        configureChrome();
        configureDropdowns();
        ensurePreviewView();
        imageLoader = new MarketplaceImageLoader(requireContext());

        viewModel = new ViewModelProvider(this).get(CreateListingViewModel.class);
        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getInitialForm().observe(getViewLifecycleOwner(), listing -> {
            if (listing != null) {
                populate(listing);
                updateImagePreview();
            }
        });
        viewModel.getCompletedListing().observe(getViewLifecycleOwner(), event -> {
            String completedId = event == null ? null : event.getIfNotHandled();
            if (completedId == null) {
                return;
            }
            if (editMode && NavHostFragment.findNavController(this).popBackStack()) {
                return;
            }
            Bundle destinationArguments = new Bundle();
            destinationArguments.putString(ARG_LISTING_ID, completedId);
            ScreenNavigation.navigateAuthenticated(
                    this,
                    R.id.marketDetailFragment,
                    destinationArguments);
        });

        binding.photoCameraAction.setOnClickListener(ignored -> onCameraAction());
        binding.photoGalleryAction.setOnClickListener(ignored -> openPhotoPicker());
        binding.photoClearAction.setOnClickListener(ignored -> {
            stopCamera();
            displayedLocalPath = null;
            viewModel.discardSelectedImage();
            updateImagePreview();
        });
        binding.primaryAction.setOnClickListener(ignored -> viewModel.submit(
                text(binding.listingTitleInput),
                text(binding.listingCategoryInput),
                text(binding.listingConditionInput),
                text(binding.listingTransactionInput),
                text(binding.listingFulfilmentInput),
                text(binding.listingPriceInput),
                text(binding.listingExchangeTermsInput),
                text(binding.listingDescriptionInput)));

        viewModel.start(listingId);
    }

    private void configureChrome() {
        if (!editMode) {
            return;
        }
        binding.menuButton.setImageResource(R.drawable.ic_back);
        binding.menuButton.setContentDescription("Go back without saving");
        binding.menuButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).popBackStack());
    }

    private void bindBackProtection() {
        backPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                CreateListingViewModel.State current = viewModel == null
                        ? null
                        : viewModel.getState().getValue();
                if (current != null && current.isBusy()) {
                    Toast.makeText(
                            requireContext(),
                            "Please wait for the photo or listing to finish.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                NavHostFragment.findNavController(CreateListingFragment.this).popBackStack();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(), backPressedCallback);
    }

    private void configureDropdowns() {
        setDropdown(binding.listingCategoryInput,
                new String[]{
                        "Banner", "Decoration", "Fabric", "Stationery", "Craft",
                        "Cosplay", "Toys", "Wood", "Electronic", "Packaging", "Other"
                });
        setDropdown(binding.listingConditionInput,
                new String[]{"New", "Like new", "Good", "Fair", "Poor"});
        setDropdown(binding.listingTransactionInput,
                new String[]{"Sale", "Donation", "Exchange"});
        setDropdown(binding.listingFulfilmentInput,
                new String[]{"Pickup", "Meet-up"});

        binding.listingTransactionInput.setOnItemClickListener(
                (parent, selected, position, id) -> updateTransactionFields());
        binding.listingTransactionInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence value, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count) {
                updateTransactionFields();
            }

            @Override
            public void afterTextChanged(Editable value) {
            }
        });
        binding.listingTransactionInput.setText("Donation", false);
        binding.listingFulfilmentInput.setText("Pickup", false);
        updateTransactionFields();
    }

    private void setDropdown(
            @NonNull android.widget.AutoCompleteTextView input,
            @NonNull String[] options) {
        input.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                options));
        input.setOnClickListener(ignored -> input.showDropDown());
    }

    private void updateTransactionFields() {
        String transaction = MarketplaceListingValidator.stableTransactionIntentId(
                text(binding.listingTransactionInput));
        binding.listingPriceInput.setVisibility(
                "sale".equals(transaction) ? View.VISIBLE : View.GONE);
        binding.listingExchangeTermsInput.setVisibility(
                "exchange".equals(transaction) ? View.VISIBLE : View.GONE);
    }

    private void populate(@NonNull MarketplaceListing listing) {
        binding.listingTitleInput.setText(value(listing.getTitle()));
        binding.listingCategoryInput.setText(
                MarketplaceListingValidator.displayLabel(listing.getCategory()), false);
        binding.listingConditionInput.setText(
                MarketplaceListingValidator.displayLabel(listing.getCondition()), false);
        binding.listingTransactionInput.setText(
                MarketplaceListingValidator.displayLabel(listing.getTransactionIntent()), false);
        binding.listingFulfilmentInput.setText(
                MarketplaceListingValidator.displayLabel(listing.getFulfilmentMethod()), false);
        updateTransactionFields();
        long priceMinor = listing.getPriceMinor() == null ? 0L : listing.getPriceMinor();
        binding.listingPriceInput.setText(
                "sale".equals(listing.getTransactionIntent())
                        ? String.format(Locale.ROOT, "%.2f", priceMinor / 100.0)
                        : "");
        binding.listingExchangeTermsInput.setText(
                "exchange".equals(listing.getTransactionIntent())
                        ? value(listing.getExchangeTerms())
                        : "");
        binding.listingDescriptionInput.setText(value(listing.getDescription()));
    }

    private void ensurePreviewView() {
        if (previewView != null || binding == null) {
            return;
        }
        PreviewView created = new PreviewView(requireContext());
        created.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        created.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        created.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        created.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        created.setVisibility(View.GONE);
        binding.listingPhotoContainer.addView(created, 0);
        previewView = created;
    }

    private void onCameraAction() {
        CreateListingViewModel.State current = viewModel.getState().getValue();
        if (current != null && current.isBusy()) {
            return;
        }
        if (!deviceHasCamera()) {
            viewModel.showImageMessage(
                    "No camera is available. You can still choose a photo.");
            return;
        }
        if (!hasCameraPermission()) {
            requestCameraPermissionWithExplanation();
            return;
        }
        if (cameraBound && imageCapture != null) {
            capturePhoto();
        } else {
            startCamera();
        }
    }

    private void requestCameraPermissionWithExplanation() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Camera permission")
                    .setMessage("PropCycle uses the camera only while you take the listing photo. "
                            + "You can choose a photo without this permission.")
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
        if (granted) {
            startCamera();
        } else if (viewModel != null) {
            viewModel.showImageMessage(
                    "Camera permission was not granted. You can still choose a photo.");
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", requireContext().getPackageName(), null));
        startActivity(intent);
    }

    private void openPhotoPicker() {
        CreateListingViewModel.State current = viewModel == null
                ? null
                : viewModel.getState().getValue();
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
        displayedLocalPath = null;
        cancelImageLoad();
        viewModel.processGalleryImage(uri);
    }

    private void startCamera() {
        if (binding == null || viewModel == null || !hasCameraPermission()
                || !deviceHasCamera()) {
            return;
        }
        cameraDesired = true;
        if (cameraBound || cameraStartInFlight) {
            return;
        }
        cameraStartInFlight = true;
        long generation = ++cameraGeneration;
        cancelImageLoad();
        viewModel.showImageMessage("Starting camera...");
        ListenableFuture<ProcessCameraProvider> providerFuture =
                ProcessCameraProvider.getInstance(requireContext());
        providerFuture.addListener(() -> {
            if (generation != cameraGeneration || !cameraDesired
                    || binding == null || previewView == null || !isAdded()) {
                return;
            }
            cameraStartInFlight = false;
            try {
                cameraProvider = providerFuture.get();
                bindCameraUseCases(generation);
            } catch (ExecutionException | InterruptedException error) {
                if (error instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                cameraDesired = false;
                viewModel.showImageMessage(
                        "The camera could not start. Try again or choose a photo.");
            } catch (RuntimeException error) {
                cameraDesired = false;
                viewModel.showImageMessage(
                        "The camera could not start. Try again or choose a photo.");
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindCameraUseCases(long generation) {
        if (generation != cameraGeneration || !cameraDesired || binding == null
                || previewView == null || cameraProvider == null) {
            return;
        }
        int rotation = previewView.getDisplay() == null
                ? Surface.ROTATION_0
                : previewView.getDisplay().getRotation();
        Preview preview = new Preview.Builder().setTargetRotation(rotation).build();
        ImageCapture capture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(rotation)
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        CameraSelector selector = availableCameraSelector();
        if (selector == null) {
            cameraDesired = false;
            viewModel.showImageMessage("No usable camera was found. Choose a photo instead.");
            return;
        }
        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(
                    getViewLifecycleOwner(), selector, preview, capture);
            imageCapture = capture;
            cameraBound = true;
            updateImagePreview();
            viewModel.showImageMessage("Camera ready. Press Capture photo.");
        } catch (RuntimeException error) {
            cameraDesired = false;
            cameraBound = false;
            imageCapture = null;
            viewModel.showImageMessage(
                    "The camera could not be opened. Choose a photo instead.");
        }
    }

    @Nullable
    private CameraSelector availableCameraSelector() {
        if (cameraProvider == null) {
            return null;
        }
        try {
            if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                return CameraSelector.DEFAULT_BACK_CAMERA;
            }
            if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                return CameraSelector.DEFAULT_FRONT_CAMERA;
            }
        } catch (CameraInfoUnavailableException ignored) {
            return null;
        }
        return null;
    }

    private void capturePhoto() {
        ImageCapture capture = imageCapture;
        if (capture == null || viewModel == null) {
            return;
        }
        final File outputFile;
        try {
            outputFile = viewModel.createCaptureFile();
        } catch (RuntimeException error) {
            viewModel.showImageMessage("A temporary camera file could not be created.");
            return;
        }
        viewModel.markCaptureStarted();
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
                        displayedLocalPath = null;
                        viewModel.processCapturedImage(outputFile);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        viewModel.captureFailed(
                                outputFile,
                                "The photo was not captured. Try again or choose a photo.");
                    }
                });
    }

    private void render(@NonNull CreateListingViewModel.State state) {
        editMode = state.getMode() == CreateListingViewModel.Mode.EDIT;
        boolean loadingForm = state.getKind() == CreateListingViewModel.State.Kind.LOADING_FORM;
        boolean processingImage =
                state.getKind() == CreateListingViewModel.State.Kind.PROCESSING_IMAGE;
        boolean uploading =
                state.getKind() == CreateListingViewModel.State.Kind.UPLOADING_IMAGE;
        boolean saving = state.getKind() == CreateListingViewModel.State.Kind.SAVING;
        boolean showMessage = !state.getMessage().isEmpty();
        boolean error = switch (state.getKind()) {
            case ERROR, CONFIGURATION_REQUIRED, AUTHENTICATION_REQUIRED,
                    PERMISSION_DENIED, NOT_FOUND, CONFLICT -> true;
            default -> false;
        };

        binding.createListingHeading.setText(
                editMode ? "Edit your\nlisting" : "Complete your\nlisting");
        binding.primaryAction.setText(editMode ? "Save changes" : "Publish");
        binding.primaryAction.setContentDescription(
                editMode ? "Save marketplace listing changes" : "Publish marketplace listing");
        updateMediaNote();

        binding.createListingForm.setVisibility(
                state.isFormAvailable() ? View.VISIBLE : View.GONE);
        binding.createListingActionRow.setVisibility(
                state.isFormAvailable() ? View.VISIBLE : View.GONE);
        binding.createListingProgress.setVisibility(
                loadingForm || saving ? View.VISIBLE : View.GONE);
        binding.createListingUploadProgress.setVisibility(
                uploading ? View.VISIBLE : View.GONE);
        binding.createListingUploadProgress.setProgressCompat(
                state.getProgressPercent(), true);
        binding.createListingStatus.setVisibility(showMessage ? View.VISIBLE : View.GONE);
        binding.createListingStatus.setText(state.getMessage());
        binding.createListingStatus.setTextColor(ContextCompat.getColor(
                requireContext(),
                error ? R.color.pc_error : R.color.pc_text_secondary));

        boolean enabled = state.isFormAvailable() && !state.isBusy();
        binding.menuButton.setEnabled(!state.isBusy());
        binding.menuButton.setAlpha(state.isBusy() ? 0.55f : 1f);
        setFormEnabled(enabled);
        binding.photoCameraAction.setEnabled(enabled);
        binding.photoGalleryAction.setEnabled(enabled);
        binding.photoClearAction.setEnabled(enabled);
        binding.photoCameraAction.setAlpha(enabled ? 1f : 0.55f);
        binding.photoGalleryAction.setAlpha(enabled ? 1f : 0.55f);
        boolean conflict = state.getKind() == CreateListingViewModel.State.Kind.CONFLICT;
        binding.primaryAction.setEnabled(enabled && !state.isFromCache() && !conflict);
        binding.primaryAction.setAlpha(binding.primaryAction.isEnabled() ? 1f : 0.55f);
        binding.listingPhotoProgress.setVisibility(
                processingImage ? View.VISIBLE : View.GONE);
        updateImagePreview();
    }

    private void updateMediaNote() {
        if (viewModel == null) {
            return;
        }
        if (viewModel.hasSelectedImage()) {
            binding.createListingMediaNote.setText(editMode
                    ? "The new photo replaces the current photo only after Save changes."
                    : "The photo uploads securely only after Publish.");
        } else if (editMode && viewModel.getExistingImageUrl() != null) {
            binding.createListingMediaNote.setText(
                    "The current photo stays unless you choose a replacement.");
        } else {
            binding.createListingMediaNote.setText(
                    "A photo is optional. Camera and gallery files are prepared privately first.");
        }
    }

    private void updateImagePreview() {
        if (binding == null || viewModel == null) {
            return;
        }
        if (cameraBound && previewView != null) {
            previewView.setVisibility(View.VISIBLE);
            binding.listingPhotoPreview.setVisibility(View.GONE);
            binding.listingPhotoPlaceholder.setVisibility(View.GONE);
            binding.photoCameraAction.setText("Capture photo");
            binding.photoClearAction.setVisibility(View.GONE);
            return;
        }
        if (previewView != null) {
            previewView.setVisibility(View.GONE);
        }
        binding.photoCameraAction.setText("Camera");
        File selected = viewModel.getSelectedImageFile();
        if (selected != null && selected.isFile()) {
            cancelImageLoad();
            String path = selected.getAbsolutePath();
            if (!path.equals(displayedLocalPath)) {
                displayedLocalPath = path;
                displayedRemoteUrl = null;
                binding.listingPhotoPreview.setImageURI(Uri.fromFile(selected));
            }
            binding.listingPhotoPreview.setVisibility(View.VISIBLE);
            binding.listingPhotoPlaceholder.setVisibility(View.GONE);
            binding.photoClearAction.setVisibility(View.VISIBLE);
            return;
        }
        displayedLocalPath = null;
        binding.photoClearAction.setVisibility(View.GONE);
        String existingUrl = viewModel.getExistingImageUrl();
        if (existingUrl != null && !existingUrl.trim().isEmpty()) {
            loadExistingImage(existingUrl);
        } else {
            showImagePlaceholder("Add one clear item photo\n(optional)");
        }
    }

    private void loadExistingImage(@NonNull String gsUrl) {
        if (gsUrl.equals(displayedRemoteUrl)
                && binding.listingPhotoPreview.getDrawable() != null) {
            binding.listingPhotoPreview.setVisibility(View.VISIBLE);
            binding.listingPhotoPlaceholder.setVisibility(View.GONE);
            return;
        }
        cancelImageLoad();
        displayedRemoteUrl = gsUrl;
        binding.listingPhotoPreview.setImageDrawable(null);
        binding.listingPhotoPreview.setVisibility(View.GONE);
        binding.listingPhotoPlaceholder.setText("Loading current photo...");
        binding.listingPhotoPlaceholder.setVisibility(View.VISIBLE);
        binding.listingPhotoProgress.setVisibility(View.VISIBLE);
        imageLoadHandle = imageLoader.load(gsUrl, new MarketplaceImageLoader.Callback() {
            @Override
            public void onLoaded(@NonNull android.graphics.Bitmap bitmap) {
                if (binding == null || viewModel == null
                        || viewModel.hasSelectedImage()
                        || !gsUrl.equals(displayedRemoteUrl)) {
                    return;
                }
                imageLoadHandle = null;
                binding.listingPhotoProgress.setVisibility(View.GONE);
                binding.listingPhotoPreview.setImageBitmap(bitmap);
                binding.listingPhotoPreview.setVisibility(View.VISIBLE);
                binding.listingPhotoPlaceholder.setVisibility(View.GONE);
            }

            @Override
            public void onError() {
                if (binding == null || !gsUrl.equals(displayedRemoteUrl)) {
                    return;
                }
                imageLoadHandle = null;
                binding.listingPhotoProgress.setVisibility(View.GONE);
                showImagePlaceholder("Current photo could not load\nChoose a replacement if needed");
            }
        });
    }

    private void showImagePlaceholder(@NonNull String message) {
        binding.listingPhotoPreview.setImageDrawable(null);
        binding.listingPhotoPreview.setVisibility(View.GONE);
        binding.listingPhotoPlaceholder.setText(message);
        binding.listingPhotoPlaceholder.setVisibility(View.VISIBLE);
    }

    private void cancelImageLoad() {
        if (imageLoadHandle != null) {
            imageLoadHandle.cancel();
            imageLoadHandle = null;
        }
    }

    private void setFormEnabled(boolean enabled) {
        binding.listingTitleInput.setEnabled(enabled);
        binding.listingCategoryInput.setEnabled(enabled);
        binding.listingConditionInput.setEnabled(enabled);
        binding.listingTransactionInput.setEnabled(enabled);
        binding.listingFulfilmentInput.setEnabled(enabled);
        binding.listingPriceInput.setEnabled(enabled);
        binding.listingExchangeTermsInput.setEnabled(enabled);
        binding.listingDescriptionInput.setEnabled(enabled);
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

    @NonNull
    private static String value(@Nullable String value) {
        return value == null ? "" : value;
    }

    @NonNull
    private static String text(@NonNull android.widget.TextView input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(STATE_PERMISSION_REQUESTED, cameraPermissionRequested);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        stopCamera();
        cancelImageLoad();
        if (imageLoader != null) {
            imageLoader.close();
            imageLoader = null;
        }
        previewView = null;
        backPressedCallback = null;
        binding = null;
        super.onDestroyView();
    }
}
