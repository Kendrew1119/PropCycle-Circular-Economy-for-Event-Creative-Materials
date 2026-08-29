package com.propcycle.app.ui.lending;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.propcycle.app.R;
import com.propcycle.app.data.lending.LendingItem;
import com.propcycle.app.data.lending.LendingPolicy;
import com.propcycle.app.data.marketplace.MarketplaceImageLoader;
import com.propcycle.app.data.scanner.ScanAnalysis;
import com.propcycle.app.databinding.FragmentLendResourceBinding;
import com.propcycle.app.ui.common.ScreenNavigation;
import com.propcycle.app.ui.scanner.ScanPrefillPolicy;

import java.io.File;
import java.util.Locale;
import java.util.Map;

/** Lending item create/edit form with Photo Picker and optional coarse map point. */
public final class LendResourceFragment extends Fragment {

    private static final String ARG_SCAN_ANALYSIS = "scanAnalysisJson";
    private static final String ARG_SCAN_IMAGE = "scanImagePath";

    private FragmentLendResourceBinding binding;
    private LendResourceViewModel viewModel;
    private MarketplaceImageLoader imageLoader;
    private MarketplaceImageLoader.LoadHandle imageHandle;
    private FusedLocationProviderClient locationClient;
    private CancellationTokenSource locationCancellation;
    private Double latitude;
    private Double longitude;
    private String itemId = "";
    private String displayedImage;

    private final ActivityResultLauncher<PickVisualMediaRequest> photoPicker =
            registerForActivityResult(
                    new ActivityResultContracts.PickVisualMedia(),
                    this::onPhotoPicked);
    private final ActivityResultLauncher<String[]> locationPermissions =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    this::onLocationPermissionResult);

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentLendResourceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!ScreenNavigation.navigateAuthenticated(this, R.id.lendResourceFragment, null)) {
            return;
        }
        ScreenNavigation.bindChrome(this, view);
        Bundle arguments = getArguments();
        itemId = arguments == null ? "" : arguments.getString("itemId", "");
        imageLoader = new MarketplaceImageLoader(requireContext());
        locationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        viewModel = new ViewModelProvider(this).get(LendResourceViewModel.class);
        configureDropdowns();

        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getInitialItem().observe(getViewLifecycleOwner(), item -> {
            if (item != null) {
                populate(item);
            }
        });
        viewModel.getCompletedItemId().observe(getViewLifecycleOwner(), event -> {
            String completedId = event == null ? null : event.getIfNotHandled();
            if (completedId == null || binding == null) {
                return;
            }
            Bundle detail = new Bundle();
            detail.putString("itemId", completedId);
            ScreenNavigation.navigateAuthenticated(this, R.id.lendingDetailFragment, detail);
        });

        binding.photoAddAction.setOnClickListener(ignored -> photoPicker.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));
        binding.photoClearAction.setOnClickListener(ignored -> {
            viewModel.removePhoto();
            displayedImage = null;
            showImagePreview();
        });
        binding.attachLocationAction.setOnClickListener(ignored -> requestCurrentArea());
        binding.clearLocationAction.setOnClickListener(ignored -> clearLocation());
        binding.primaryAction.setOnClickListener(ignored -> viewModel.submit(
                text(binding.lendingTitleInput),
                text(binding.lendingDescriptionInput),
                text(binding.lendingCategoryInput),
                text(binding.lendingConditionInput),
                text(binding.lendingPickupInput),
                text(binding.lendingAreaInput),
                text(binding.lendingMaxDaysInput),
                text(binding.lendingDepositInput),
                latitude,
                longitude));
        viewModel.start(itemId);
        if (itemId.trim().isEmpty() && savedInstanceState == null) {
            applyScannerDraft(arguments);
        }
    }

    private void applyScannerDraft(@Nullable Bundle arguments) {
        if (arguments == null) {
            return;
        }
        String analysisJson = arguments.getString(ARG_SCAN_ANALYSIS, "");
        if (!analysisJson.trim().isEmpty()) {
            try {
                ScanPrefillPolicy.LendingDraft draft =
                        ScanPrefillPolicy.lending(ScanAnalysis.fromJson(analysisJson));
                binding.lendingTitleInput.setText(draft.title());
                binding.lendingCategoryInput.setText(draft.category(), false);
                binding.lendingConditionInput.setText(draft.condition(), false);
                binding.lendingDescriptionInput.setText(draft.description());
            } catch (ScanAnalysis.ValidationException ignored) {
                // Manual entry remains available if a saved AI draft is no longer valid.
            }
        }
        viewModel.processTransferredImage(arguments.getString(ARG_SCAN_IMAGE, ""));
    }

    private void configureDropdowns() {
        setDropdown(binding.lendingCategoryInput,
                new String[]{"Equipment", "Tools", "Electronics", "Event gear", "Craft", "Other"});
        setDropdown(binding.lendingConditionInput,
                new String[]{"New", "Like new", "Good", "Fair"});
        setDropdown(binding.lendingPickupInput, new String[]{"Pickup", "Meet-up"});
        binding.lendingCategoryInput.setText("Equipment", false);
        binding.lendingConditionInput.setText("Good", false);
        binding.lendingPickupInput.setText("Pickup", false);
        binding.lendingMaxDaysInput.setText("7");
    }

    private void setDropdown(
            @NonNull android.widget.AutoCompleteTextView input,
            @NonNull String[] choices) {
        input.setAdapter(new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, choices));
        input.setOnClickListener(ignored -> input.showDropDown());
    }

    private void populate(@NonNull LendingItem item) {
        binding.lendingTitleInput.setText(value(item.getTitle()));
        binding.lendingDescriptionInput.setText(value(item.getDescription()));
        binding.lendingCategoryInput.setText(
                LendingPolicy.displayLabel(item.getCategory()), false);
        binding.lendingConditionInput.setText(
                LendingPolicy.displayLabel(item.getCondition()), false);
        binding.lendingPickupInput.setText(
                LendingPolicy.displayLabel(item.getPickupMethod()), false);
        binding.lendingAreaInput.setText(value(item.getAreaLabel()));
        binding.lendingMaxDaysInput.setText(item.getMaxBorrowDays() == null
                ? "7" : String.valueOf(item.getMaxBorrowDays()));
        long deposit = item.getDepositMinor() == null ? 0L : item.getDepositMinor();
        binding.lendingDepositInput.setText(deposit == 0L
                ? "" : String.format(Locale.ROOT, "%.2f", deposit / 100d));
        latitude = item.getLatitude();
        longitude = item.getLongitude();
        updateLocationStatus();
        displayedImage = null;
        showImagePreview();
    }

    private void onPhotoPicked(@Nullable Uri uri) {
        if (uri != null && viewModel != null) {
            displayedImage = null;
            viewModel.processImage(uri);
        }
    }

    private void requestCurrentArea() {
        if (hasLocationPermission()) {
            loadCurrentArea();
        } else {
            locationPermissions.launch(new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            });
        }
    }

    private void onLocationPermissionResult(@NonNull Map<String, Boolean> permissions) {
        boolean allowed = Boolean.TRUE.equals(
                permissions.get(Manifest.permission.ACCESS_COARSE_LOCATION))
                || Boolean.TRUE.equals(
                permissions.get(Manifest.permission.ACCESS_FINE_LOCATION));
        if (allowed) {
            loadCurrentArea();
        } else if (binding != null) {
            binding.lendingLocationStatus.setText(
                    "Location permission was not granted. Type the public area manually.");
        }
    }

    @SuppressLint("MissingPermission")
    private void loadCurrentArea() {
        if (!hasLocationPermission() || locationClient == null) {
            return;
        }
        if (locationCancellation != null) {
            locationCancellation.cancel();
        }
        locationCancellation = new CancellationTokenSource();
        binding.lendingLocationStatus.setText("Finding your current approximate area...");
        binding.attachLocationAction.setEnabled(false);
        locationClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        locationCancellation.getToken())
                .addOnSuccessListener(location -> {
                    if (binding == null) {
                        return;
                    }
                    binding.attachLocationAction.setEnabled(true);
                    if (location == null) {
                        binding.lendingLocationStatus.setText(
                                "Location is unavailable. Type the public area manually.");
                        return;
                    }
                    latitude = LendingPolicy.roundLatitude(location.getLatitude());
                    longitude = LendingPolicy.roundLongitude(location.getLongitude());
                    updateLocationStatus();
                })
                .addOnFailureListener(error -> {
                    if (binding != null) {
                        binding.attachLocationAction.setEnabled(true);
                        binding.lendingLocationStatus.setText(
                                "Location is unavailable. Type the public area manually.");
                    }
                });
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void clearLocation() {
        latitude = null;
        longitude = null;
        updateLocationStatus();
    }

    private void updateLocationStatus() {
        if (binding == null) {
            return;
        }
        boolean attached = latitude != null && longitude != null;
        binding.lendingLocationStatus.setText(attached
                ? "Approximate map point attached (rounded for privacy)."
                : "No map point attached. The typed area remains searchable.");
        binding.clearLocationAction.setVisibility(attached ? View.VISIBLE : View.GONE);
    }

    private void render(@NonNull LendResourceViewModel.State state) {
        if (binding == null) {
            return;
        }
        binding.lendFormTitle.setText(viewModel.isEditMode()
                ? "Edit lending item" : "Lend Resource");
        binding.primaryAction.setText(viewModel.isEditMode()
                ? "Save changes" : "Publish lending item");
        binding.lendingFormProgress.setVisibility(
                state.isLoading() || state.isBusy() ? View.VISIBLE : View.GONE);
        String statusMessage = state.getMessage() == null ? "" : state.getMessage();
        binding.lendingFormStatus.setText(statusMessage);
        binding.lendingFormStatus.setVisibility(statusMessage.isEmpty() ? View.GONE : View.VISIBLE);
        boolean enabled = state.isFormAvailable() && !state.isBusy();
        setEnabled(enabled);
        showImagePreview();
    }

    private void setEnabled(boolean enabled) {
        binding.lendingTitleInput.setEnabled(enabled);
        binding.lendingDescriptionInput.setEnabled(enabled);
        binding.lendingCategoryInput.setEnabled(enabled);
        binding.lendingConditionInput.setEnabled(enabled);
        binding.lendingPickupInput.setEnabled(enabled);
        binding.lendingAreaInput.setEnabled(enabled);
        binding.lendingMaxDaysInput.setEnabled(enabled);
        binding.lendingDepositInput.setEnabled(enabled);
        binding.photoAddAction.setEnabled(enabled);
        binding.photoClearAction.setEnabled(enabled);
        binding.attachLocationAction.setEnabled(enabled);
        binding.clearLocationAction.setEnabled(enabled);
        binding.primaryAction.setEnabled(enabled);
    }

    private void showImagePreview() {
        if (binding == null || viewModel == null) {
            return;
        }
        File local = viewModel.getSelectedImageFile();
        if (local != null && local.isFile()) {
            cancelImageLoad();
            String path = local.getAbsolutePath();
            if (!path.equals(displayedImage)) {
                displayedImage = path;
                binding.lendingPhotoPreview.setImageURI(Uri.fromFile(local));
            }
            binding.photoClearAction.setVisibility(View.VISIBLE);
            return;
        }
        String remote = viewModel.getExistingImageUrl();
        if (remote == null || remote.trim().isEmpty()) {
            cancelImageLoad();
            displayedImage = null;
            binding.lendingPhotoPreview.setImageResource(R.drawable.ic_bottom_nav_lend_out);
            binding.photoClearAction.setVisibility(View.GONE);
            return;
        }
        binding.photoClearAction.setVisibility(View.VISIBLE);
        if (remote.equals(displayedImage)) {
            return;
        }
        cancelImageLoad();
        displayedImage = remote;
        imageHandle = imageLoader.load(remote, new MarketplaceImageLoader.Callback() {
            @Override public void onLoaded(@NonNull Bitmap bitmap) {
                if (binding != null && remote.equals(displayedImage)) {
                    binding.lendingPhotoPreview.setImageBitmap(bitmap);
                }
            }
            @Override public void onError() { }
        });
    }

    private void cancelImageLoad() {
        if (imageHandle != null) {
            imageHandle.cancel();
            imageHandle = null;
        }
    }

    @NonNull private static String value(@Nullable String value) {
        return value == null ? "" : value;
    }

    @NonNull private static String text(@NonNull TextView view) {
        return view.getText() == null ? "" : view.getText().toString();
    }

    @Override
    public void onDestroyView() {
        if (locationCancellation != null) {
            locationCancellation.cancel();
            locationCancellation = null;
        }
        cancelImageLoad();
        if (imageLoader != null) {
            imageLoader.close();
            imageLoader = null;
        }
        binding = null;
        super.onDestroyView();
    }
}
