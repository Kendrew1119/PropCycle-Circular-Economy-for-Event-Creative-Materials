package com.propcycle.app.ui.recycle;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.propcycle.app.R;
import com.propcycle.app.core.maps.MapsEnvironment;
import com.propcycle.app.data.recycle.RecyclingCenter;
import com.propcycle.app.data.recycle.RecyclingCenterPolicy;
import com.propcycle.app.databinding.FragmentRecycleCenterBinding;
import com.propcycle.app.ui.common.ScreenNavigation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Functional proposal-faithful recycling-centre map and nearby list. */
public final class RecycleCenterFragment extends Fragment {

    private static final String MAP_STATE = "recycle_map_state";
    private static final LatLng MALAYSIA_CENTER = new LatLng(4.2105d, 101.9758d);

    private enum LastAction {
        NONE,
        AREA,
        LOCATION
    }

    private final ActivityResultLauncher<String[]> locationPermissions =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    this::handleLocationPermissionResult);

    private FragmentRecycleCenterBinding binding;
    private RecycleCenterViewModel viewModel;
    private RecyclingCenterAdapter adapter;
    private MapView mapView;
    private GoogleMap googleMap;
    private final Map<String, Marker> markers = new LinkedHashMap<>();
    private List<RecyclingCenter> currentCenters = Collections.emptyList();
    private RecyclingCenter selectedCenter;
    private LastAction lastAction = LastAction.NONE;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentRecycleCenterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ScreenNavigation.bindChrome(this, view);
        viewModel = new ViewModelProvider(this).get(RecycleCenterViewModel.class);
        adapter = new RecyclingCenterAdapter(this::selectCenterFromList);
        binding.centerList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.centerList.setAdapter(adapter);

        binding.searchAreaAction.setOnClickListener(ignored -> searchArea());
        binding.useLocationAction.setOnClickListener(ignored -> requestLocationSearch());
        binding.externalMapSearchAction.setOnClickListener(
                ignored -> openExternalManualSearch());
        binding.retryAction.setOnClickListener(ignored -> retryLastAction());
        binding.openMapsAction.setOnClickListener(ignored -> openSelectedCenter());
        binding.areaInput.setOnEditorActionListener((input, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchArea();
                return true;
            }
            return false;
        });

        initialiseMap(savedInstanceState);
        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
    }

    private void initialiseMap(@Nullable Bundle savedInstanceState) {
        if (!MapsEnvironment.hasApiKey()) {
            binding.mapPlaceholder.setText(R.string.recycle_map_setup_required);
            return;
        }
        if (!MapsEnvironment.hasGooglePlayServices(requireContext())) {
            binding.mapPlaceholder.setText(R.string.recycle_play_services_unavailable);
            return;
        }
        Bundle mapState = savedInstanceState == null
                ? null
                : savedInstanceState.getBundle(MAP_STATE);
        mapView = new MapView(requireContext());
        mapView.setId(View.generateViewId());
        binding.mapContainer.addView(
                mapView,
                0,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        mapView.onCreate(mapState);
        mapView.getMapAsync(this::onMapReady);
    }

    private void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        binding.mapPlaceholder.setVisibility(View.GONE);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setCompassEnabled(true);
        map.setPadding(0, 0, 0, dp(12));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(MALAYSIA_CENTER, 5.5f));
        map.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof String id) {
                RecyclingCenter center = findCenter(id);
                if (center != null) {
                    selectCenter(center, false);
                    int position = adapter.findPosition(id);
                    if (position >= 0) {
                        binding.centerList.smoothScrollToPosition(position);
                    }
                }
            }
            marker.showInfoWindow();
            return true;
        });
        enableMyLocationIfAllowed();
        renderMarkers(currentCenters);
    }

    private void searchArea() {
        String area = RecyclingCenterPolicy.normalizeArea(
                binding.areaInput.getText() == null
                        ? ""
                        : binding.areaInput.getText().toString());
        if (!RecyclingCenterPolicy.isValidArea(area)) {
            binding.areaInputLayout.setError(getString(R.string.recycle_area_error));
            return;
        }
        binding.areaInputLayout.setError(null);
        hideKeyboard();
        lastAction = LastAction.AREA;
        if (!viewModel.isInAppSearchAvailable()) {
            Toast.makeText(
                    requireContext(),
                    R.string.recycle_external_search,
                    Toast.LENGTH_LONG).show();
            openExternalSearch(RecyclingCenterPolicy.buildManualQuery(area));
            return;
        }
        viewModel.searchArea(area);
    }

    private void requestLocationSearch() {
        lastAction = LastAction.LOCATION;
        if (!viewModel.isInAppSearchAvailable()) {
            Toast.makeText(
                    requireContext(),
                    R.string.recycle_external_search,
                    Toast.LENGTH_LONG).show();
            openExternalSearch("recycling centre");
            return;
        }
        if (hasLocationPermission()) {
            enableMyLocationIfAllowed();
            viewModel.searchUsingCurrentLocation();
            return;
        }
        locationPermissions.launch(new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        });
    }

    private void handleLocationPermissionResult(@NonNull Map<String, Boolean> result) {
        boolean allowed = Boolean.TRUE.equals(
                result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                || Boolean.TRUE.equals(
                result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
        if (allowed) {
            enableMyLocationIfAllowed();
            viewModel.searchUsingCurrentLocation();
        } else {
            viewModel.showPermissionDenied();
        }
    }

    private void retryLastAction() {
        if (lastAction == LastAction.LOCATION) {
            requestLocationSearch();
        } else {
            searchArea();
        }
    }

    private void render(@NonNull RecycleCenterUiState state) {
        boolean working = state.getKind() == RecycleCenterUiState.Kind.LOCATING
                || state.getKind() == RecycleCenterUiState.Kind.SEARCHING;
        boolean content = state.getKind() == RecycleCenterUiState.Kind.CONTENT;
        boolean retry = state.getKind() == RecycleCenterUiState.Kind.ERROR;

        binding.searchProgress.setVisibility(working ? View.VISIBLE : View.GONE);
        binding.statusCard.setVisibility(content ? View.GONE : View.VISIBLE);
        binding.statusText.setText(state.getMessage());
        binding.retryAction.setVisibility(retry ? View.VISIBLE : View.GONE);
        binding.searchAreaAction.setEnabled(!working);
        binding.useLocationAction.setEnabled(!working);

        currentCenters = content ? state.getCenters() : Collections.emptyList();
        adapter.submitList(currentCenters);
        binding.nearbyHeading.setText(content
                ? getString(R.string.recycle_nearby_count, currentCenters.size())
                : getString(R.string.recycle_nearby_heading));
        binding.centerList.setVisibility(content ? View.VISIBLE : View.GONE);

        if (content) {
            RecyclingCenter preserved = selectedCenter == null
                    ? null
                    : findCenter(selectedCenter.getId());
            selectCenter(preserved == null ? currentCenters.get(0) : preserved, false);
        } else {
            selectedCenter = null;
            binding.selectedCenterCard.setVisibility(View.GONE);
        }
        renderMarkers(currentCenters);
    }

    private void selectCenter(@NonNull RecyclingCenter center, boolean moveCamera) {
        selectedCenter = center;
        adapter.setSelectedId(center.getId());
        binding.selectedCenterCard.setVisibility(View.VISIBLE);
        binding.selectedCenterName.setText(center.getName());
        binding.selectedCenterAddress.setText(center.getAddress());
        binding.selectedCenterMeta.setText(getString(
                R.string.recycle_selected_meta,
                RecyclingCenterPolicy.formatDistance(center.getDistanceKm()),
                RecyclingCenterPolicy.formatRating(center.getRating())));
        updateMarkerSelection();
        if (moveCamera && googleMap != null) {
            GeoPointValues point = GeoPointValues.from(center);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point.latLng, 15f));
            Marker marker = markers.get(center.getId());
            if (marker != null) {
                marker.showInfoWindow();
            }
        }
    }

    private void selectCenterFromList(@NonNull RecyclingCenter center) {
        selectCenter(center, true);
        binding.mapContainer.post(this::scrollMapIntoView);
    }

    private void scrollMapIntoView() {
        if (binding == null) {
            return;
        }
        int[] scrollPosition = new int[2];
        int[] mapPosition = new int[2];
        binding.getRoot().getLocationInWindow(scrollPosition);
        binding.mapContainer.getLocationInWindow(mapPosition);
        int targetY = binding.getRoot().getScrollY()
                + mapPosition[1]
                - scrollPosition[1]
                - dp(12);
        binding.getRoot().smoothScrollTo(0, Math.max(0, targetY));
    }

    private void renderMarkers(@NonNull List<RecyclingCenter> centers) {
        if (googleMap == null) {
            return;
        }
        googleMap.clear();
        markers.clear();
        if (centers.isEmpty()) {
            return;
        }
        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        for (RecyclingCenter center : centers) {
            LatLng point = GeoPointValues.from(center).latLng;
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(point)
                    .title(center.getName())
                    .snippet(center.getAddress())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            if (marker != null) {
                marker.setTag(center.getId());
                markers.put(center.getId(), marker);
            }
            bounds.include(point);
        }
        updateMarkerSelection();
        mapView.post(() -> {
            if (googleMap == null || currentCenters.isEmpty()) {
                return;
            }
            if (currentCenters.size() == 1) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        GeoPointValues.from(currentCenters.get(0)).latLng,
                        15f));
            } else {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(
                        bounds.build(),
                        dp(52)));
            }
        });
    }

    private void updateMarkerSelection() {
        for (Map.Entry<String, Marker> entry : markers.entrySet()) {
            boolean selected = selectedCenter != null
                    && selectedCenter.getId().equals(entry.getKey());
            entry.getValue().setIcon(BitmapDescriptorFactory.defaultMarker(
                    selected ? BitmapDescriptorFactory.HUE_ORANGE
                            : BitmapDescriptorFactory.HUE_GREEN));
        }
    }

    @Nullable
    private RecyclingCenter findCenter(@NonNull String id) {
        for (RecyclingCenter center : currentCenters) {
            if (center.getId().equals(id)) {
                return center;
            }
        }
        return null;
    }

    private void openSelectedCenter() {
        if (selectedCenter == null) {
            return;
        }
        double latitude = selectedCenter.getLocation().getLatitude();
        double longitude = selectedCenter.getLocation().getLongitude();
        String label = latitude + "," + longitude + " (" + selectedCenter.getName() + ")";
        Uri uri = Uri.parse("geo:" + latitude + "," + longitude + "?q=" + Uri.encode(label));
        openMapIntent(uri);
    }

    private void openExternalSearch(@NonNull String query) {
        openMapIntent(Uri.parse("geo:0,0?q=" + Uri.encode(query)));
    }

    private void openExternalManualSearch() {
        String area = RecyclingCenterPolicy.normalizeArea(
                binding.areaInput.getText() == null
                        ? ""
                        : binding.areaInput.getText().toString());
        String query = RecyclingCenterPolicy.isValidArea(area)
                ? RecyclingCenterPolicy.buildManualQuery(area)
                : "recycling centre";
        openExternalSearch(query);
    }

    private void openMapIntent(@NonNull Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException error) {
            Toast.makeText(requireContext(), R.string.recycle_no_map_app, Toast.LENGTH_LONG).show();
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private void enableMyLocationIfAllowed() {
        if (googleMap != null && hasLocationPermission()) {
            googleMap.setMyLocationEnabled(true);
        }
    }

    private void hideKeyboard() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        InputMethodManager manager = (InputMethodManager) activity
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.hideSoftInputFromWindow(binding.areaInput.getWindowToken(), 0);
        }
        binding.areaInput.clearFocus();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mapView != null) {
            mapView.onStart();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        if (mapView != null) {
            mapView.onStop();
        }
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            Bundle mapState = new Bundle();
            mapView.onSaveInstanceState(mapState);
            outState.putBundle(MAP_STATE, mapState);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }

    @Override
    public void onDestroyView() {
        binding.centerList.setAdapter(null);
        if (mapView != null) {
            mapView.onDestroy();
        }
        mapView = null;
        googleMap = null;
        markers.clear();
        adapter = null;
        binding = null;
        super.onDestroyView();
    }

    private static final class GeoPointValues {
        private final LatLng latLng;

        private GeoPointValues(@NonNull LatLng latLng) {
            this.latLng = latLng;
        }

        @NonNull
        private static GeoPointValues from(@NonNull RecyclingCenter center) {
            return new GeoPointValues(new LatLng(
                    center.getLocation().getLatitude(),
                    center.getLocation().getLongitude()));
        }
    }
}
