package com.propcycle.app.ui.lending;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.propcycle.app.R;
import com.propcycle.app.core.maps.MapsEnvironment;
import com.propcycle.app.data.lending.LendingItem;
import com.propcycle.app.data.lending.LendingPolicy;
import com.propcycle.app.data.marketplace.MarketplaceImageLoader;
import com.propcycle.app.databinding.FragmentLendingMapBinding;
import com.propcycle.app.ui.common.ScreenNavigation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Approximate lending map kept in sync with its searchable Firebase list. */
public final class LendingMapFragment extends Fragment {

    private static final String MAP_STATE = "lending_map_state";
    private static final LatLng MALAYSIA_CENTER = new LatLng(4.2105d, 101.9758d);

    private FragmentLendingMapBinding binding;
    private LendingMapViewModel viewModel;
    private LendingItemAdapter adapter;
    private MarketplaceImageLoader imageLoader;
    private FusedLocationProviderClient locationClient;
    private CancellationTokenSource locationCancellation;
    private MapView mapView;
    private GoogleMap googleMap;
    private final Map<String, Marker> markers = new LinkedHashMap<>();
    private List<LendingItem> currentItems = Collections.emptyList();
    private String selectedId;

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
        binding = FragmentLendingMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!ScreenNavigation.navigateAuthenticated(this, R.id.lendingMapFragment, null)) {
            return;
        }
        ScreenNavigation.bindChrome(this, view);
        viewModel = new ViewModelProvider(this).get(LendingMapViewModel.class);
        imageLoader = new MarketplaceImageLoader(requireContext());
        adapter = new LendingItemAdapter(imageLoader, item -> {
            selectedId = item.getId();
            adapter.setSelectedId(selectedId);
            Marker marker = markers.get(selectedId);
            if (marker != null && googleMap != null) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 13f));
                marker.showInfoWindow();
            }
            openDetail(item);
        });
        binding.lendingMapList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.lendingMapList.setAdapter(adapter);
        locationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        binding.lendingMapLocationAction.setOnClickListener(ignored -> requestLocation());
        binding.lendingMapCategoryAction.setOnClickListener(ignored -> showCategoryChoice());
        binding.lendingMapListAction.setOnClickListener(ignored -> {
            Bundle listArguments = new Bundle();
            listArguments.putString("initialQuery", viewModel.getQuery());
            listArguments.putString("initialCategory", viewModel.getCategory());
            ScreenNavigation.navigateAuthenticated(
                    this, R.id.lendingListFragment, listArguments);
        });
        binding.lendingMapSearchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setQuery(s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        Bundle arguments = getArguments();
        String initialQuery = arguments == null
                ? "" : arguments.getString("initialQuery", "");
        String initialCategory = arguments == null
                ? "all" : arguments.getString("initialCategory", "all");
        if (savedInstanceState == null) {
            binding.lendingMapSearchInput.setText(initialQuery);
            binding.lendingMapSearchInput.setSelection(initialQuery.length());
            viewModel.setCategory(initialCategory);
            updateCategoryLabel();
        }
        initialiseMap(savedInstanceState);
        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
    }

    private void showCategoryChoice() {
        String[] labels = {"All", "Equipment", "Tools", "Electronics", "Event gear", "Craft", "Other"};
        String[] values = {"all", "equipment", "tools", "electronics", "event_gear", "craft", "other"};
        int selected = 0;
        for (int index = 0; index < values.length; index++) {
            if (values[index].equals(viewModel.getCategory())) {
                selected = index;
                break;
            }
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Filter lending items")
                .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                    viewModel.setCategory(values[which]);
                    updateCategoryLabel();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateCategoryLabel() {
        String category = viewModel.getCategory();
        String label = "all".equals(category) ? "All" : LendingPolicy.displayLabel(category);
        binding.lendingMapCategoryAction.setText(
                getString(R.string.lending_category_filter_format, label));
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mapView != null) {
            mapView.onStart();
        }
        if (viewModel != null) {
            viewModel.start();
        }
    }

    @Override
    public void onStop() {
        if (viewModel != null) {
            viewModel.stop();
        }
        if (mapView != null) {
            mapView.onStop();
        }
        super.onStop();
    }

    private void initialiseMap(@Nullable Bundle savedInstanceState) {
        if (!MapsEnvironment.hasApiKey() || !MapsEnvironment.hasGooglePlayServices(requireContext())) {
            TextView setup = new TextView(requireContext());
            setup.setGravity(android.view.Gravity.CENTER);
            setup.setPadding(dp(20), dp(20), dp(20), dp(20));
            setup.setText(!MapsEnvironment.hasApiKey()
                    ? "Map setup is required. The lending list still works."
                    : "Google Play services are unavailable. The lending list still works.");
            binding.lendingMapContainer.addView(setup);
            return;
        }
        mapView = new MapView(requireContext());
        binding.lendingMapContainer.addView(mapView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        Bundle mapState = savedInstanceState == null
                ? null : savedInstanceState.getBundle(MAP_STATE);
        mapView.onCreate(mapState);
        mapView.getMapAsync(this::onMapReady);
    }

    private void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        map.getUiSettings().setMapToolbarEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(MALAYSIA_CENTER, 5.5f));
        map.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof String id) {
                selectedId = id;
                adapter.setSelectedId(id);
                int position = findPosition(id);
                if (position >= 0) {
                    binding.lendingMapList.smoothScrollToPosition(position);
                }
            }
            marker.showInfoWindow();
            return true;
        });
        enableMyLocation();
        renderMarkers();
    }

    private void render(@NonNull LendingMapViewModel.State state) {
        if (binding == null || adapter == null) {
            return;
        }
        currentItems = state.getItems();
        adapter.submitList(currentItems, state.getLatitude(), state.getLongitude());
        adapter.setSelectedId(selectedId);
        String message = state.getMessage();
        if (message == null) {
            long mapped = currentItems.stream().filter(LendingItem::hasApproximateLocation).count();
            message = state.isLoading()
                    ? "Loading lending items..."
                    : currentItems.size() + " item(s), " + mapped + " shown on the map.";
        }
        binding.lendingMapStatus.setText(message);
        binding.lendingMapLocationAction.setEnabled(!state.isLoading());
        renderMarkers();
    }

    private void renderMarkers() {
        if (googleMap == null) {
            return;
        }
        googleMap.clear();
        markers.clear();
        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        int count = 0;
        for (LendingItem item : currentItems) {
            if (!item.hasApproximateLocation()) {
                continue;
            }
            LatLng point = new LatLng(item.getLatitude(), item.getLongitude());
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(point)
                    .title(item.getTitle())
                    .snippet("Approximate area: " + item.getAreaLabel())
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_GREEN)));
            if (marker != null) {
                marker.setTag(item.getId());
                markers.put(item.getId(), marker);
            }
            bounds.include(point);
            count++;
        }
        if (count > 0 && mapView != null) {
            int markerCount = count;
            mapView.post(() -> {
                if (googleMap == null || markers.isEmpty()) {
                    return;
                }
                if (markerCount == 1) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            markers.values().iterator().next().getPosition(), 12f));
                } else {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(
                            bounds.build(), dp(48)));
                }
            });
        }
    }

    private void requestLocation() {
        if (hasLocationPermission()) {
            loadCurrentLocation();
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
            enableMyLocation();
            loadCurrentLocation();
        } else {
            viewModel.showLocationMessage(
                    "Location permission was denied. Search by title or typed area instead.");
        }
    }

    @SuppressLint("MissingPermission")
    private void loadCurrentLocation() {
        if (!hasLocationPermission()) {
            return;
        }
        if (locationCancellation != null) {
            locationCancellation.cancel();
        }
        locationCancellation = new CancellationTokenSource();
        viewModel.showLocationMessage("Finding your approximate location...");
        locationClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        locationCancellation.getToken())
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        viewModel.showLocationMessage(
                                "Location is unavailable. Search by title or area instead.");
                    } else {
                        viewModel.setLocation(location.getLatitude(), location.getLongitude());
                    }
                })
                .addOnFailureListener(error -> viewModel.showLocationMessage(
                        "Location is unavailable. Search by title or area instead."));
    }

    @SuppressLint("MissingPermission")
    private void enableMyLocation() {
        if (googleMap != null && hasLocationPermission()) {
            googleMap.setMyLocationEnabled(true);
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void openDetail(@NonNull LendingItem item) {
        Bundle arguments = new Bundle();
        arguments.putString("itemId", item.getId());
        ScreenNavigation.navigateAuthenticated(this, R.id.lendingDetailFragment, arguments);
    }

    private int findPosition(@NonNull String id) {
        for (int index = 0; index < currentItems.size(); index++) {
            if (id.equals(currentItems.get(index).getId())) {
                return index;
            }
        }
        return -1;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }
    @Override public void onPause() {
        if (mapView != null) mapView.onPause();
        super.onPause();
    }
    @Override public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }
    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            Bundle mapState = new Bundle();
            mapView.onSaveInstanceState(mapState);
            outState.putBundle(MAP_STATE, mapState);
        }
    }

    @Override
    public void onDestroyView() {
        if (locationCancellation != null) {
            locationCancellation.cancel();
        }
        if (mapView != null) {
            mapView.onDestroy();
        }
        if (binding != null) {
            binding.lendingMapList.setAdapter(null);
        }
        if (imageLoader != null) {
            imageLoader.close();
        }
        mapView = null;
        googleMap = null;
        markers.clear();
        adapter = null;
        imageLoader = null;
        binding = null;
        super.onDestroyView();
    }
}
