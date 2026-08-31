package com.propcycle.app.ui.settings;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.propcycle.app.R;
import com.propcycle.app.data.activity.ActivityLogRepository;
import com.propcycle.app.databinding.FragmentSettingsBinding;
import com.propcycle.app.ui.common.ScreenNavigation;

/** Honest settings: implemented permissions/data actions are active; deferred options are disabled. */
public final class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!ScreenNavigation.navigateAuthenticated(this, R.id.settingsFragment, null)) {
            return;
        }
        ScreenNavigation.bindChrome(this, view);
        binding.darkThemeSetting.setOnClickListener(ignored -> new MaterialAlertDialogBuilder(
                        requireContext())
                .setTitle("Light theme")
                .setMessage("The current design is light-theme only. This control stays off so the app does not promise an unfinished dark design.")
                .setPositiveButton("OK", null)
                .show());
        binding.pushNotificationSetting.setOnClickListener(ignored ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("In-app updates only")
                        .setMessage("Important Marketplace sold and lending status updates are available inside Notifications. Chat stays in Messages. Phone push delivery is not configured yet, so this switch stays off.")
                        .setPositiveButton("Open Notifications", (dialog, which) ->
                                ScreenNavigation.navigateAuthenticated(
                                        this, R.id.notificationsFragment, null))
                        .setNegativeButton("Close", null)
                        .show());
        binding.locationSetting.setOnClickListener(ignored -> showLocationExplanation());
        binding.accountDetailsAction.setOnClickListener(ignored ->
                ScreenNavigation.navigateAuthenticated(this, R.id.profileFragment, null));
        binding.clearHistoryAction.setOnClickListener(ignored -> confirmClearHistory());
        updateLocationStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateLocationStatus();
    }

    private void updateLocationStatus() {
        if (binding == null) {
            return;
        }
        boolean granted = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        binding.locationSwitch.setChecked(granted);
        binding.locationSwitch.setContentDescription(
                "Location permission, " + (granted ? "allowed" : "not allowed"));
    }

    private void showLocationExplanation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Location permission")
                .setMessage("Location is optional. It is used only after you ask for nearby lending items, attach an approximate lending area, or find recycling centres. Manual area search still works without it. Android Settings controls the permission.")
                .setPositiveButton("Open Android Settings", (dialog, which) -> {
                    Intent intent = new Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", requireContext().getPackageName(), null));
                    startActivity(intent);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void confirmClearHistory() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear local activity?")
                .setMessage("This removes this account's scan and activity history only from this device. Firebase listings, chats, and lending records are not deleted.")
                .setPositiveButton("Clear", (dialog, which) ->
                        new ActivityLogRepository(requireContext()).clearCurrentUser())
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
