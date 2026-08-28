package com.propcycle.app.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.propcycle.app.R;
import com.propcycle.app.core.firebase.FirebaseEnvironment;
import com.propcycle.app.data.activity.ActivityLogRepository;
import com.propcycle.app.data.activity.ActivityRecord;
import com.propcycle.app.databinding.FragmentHomeBinding;
import com.propcycle.app.ui.common.ResourceCreationFlow;
import com.propcycle.app.ui.common.ScreenNavigation;

import java.util.Collections;
import java.util.List;

/** Functional home routing and truthful device-local impact summary. */
public final class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!ScreenNavigation.navigateAuthenticated(this, R.id.homeFragment, null)) {
            return;
        }
        ScreenNavigation.bindChrome(this, view);
        bindGreeting();
        binding.scannerCard.setOnClickListener(ignored ->
                ScreenNavigation.navigateAuthenticated(this, R.id.scannerFragment, null));
        binding.homeSearchAction.setOnClickListener(ignored -> showResourceSearch());
        binding.homeRecycleAction.setOnClickListener(ignored ->
                ScreenNavigation.navigateAuthenticated(this, R.id.recycleCenterFragment, null));
        binding.homeCreateListingAction.setOnClickListener(ignored ->
                ResourceCreationFlow.show(this, ResourceCreationFlow.TARGET_MARKETPLACE));
        binding.homeLendResourceAction.setOnClickListener(ignored ->
                ResourceCreationFlow.show(this, ResourceCreationFlow.TARGET_LENDING));
        binding.recentAction.setOnClickListener(ignored ->
                ScreenNavigation.navigateAuthenticated(
                        this, R.id.recentActivitiesFragment, null));

        HomeViewModel viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        viewModel.getActivities().observe(getViewLifecycleOwner(), this::renderImpact);
    }

    private void bindGreeting() {
        FirebaseAuth auth = FirebaseEnvironment.auth(requireContext());
        FirebaseUser user = auth == null ? null : auth.getCurrentUser();
        String name = user == null ? null : user.getDisplayName();
        if (name == null || name.trim().isEmpty()) {
            name = "PropCycle Member";
        }
        binding.homeGreeting.setText(getString(R.string.home_greeting, name.trim()));
    }

    private void showResourceSearch() {
        EditText input = new EditText(requireContext());
        input.setHint("Item or material name");
        input.setSingleLine(true);
        int horizontalPadding = Math.round(24 * getResources().getDisplayMetrics().density);
        input.setPadding(horizontalPadding, 0, horizontalPadding, 0);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Search resources")
                .setMessage("Choose where to look. Lending searches available community items, not places.")
                .setView(input)
                .setPositiveButton("Marketplace", (dialog, which) ->
                        openSearch(R.id.marketplaceFragment, input.getText().toString()))
                .setNeutralButton("Lending items", (dialog, which) ->
                        openSearch(R.id.lendingListFragment, input.getText().toString()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openSearch(int destination, @NonNull String query) {
        Bundle arguments = new Bundle();
        arguments.putString("initialQuery", query.trim());
        ScreenNavigation.navigateAuthenticated(this, destination, arguments);
    }

    private void renderImpact(@Nullable List<ActivityRecord> value) {
        List<ActivityRecord> records = value == null ? Collections.emptyList() : value;
        int shared = 0;
        int lending = 0;
        int scans = 0;
        for (ActivityRecord record : records) {
            if (ActivityLogRepository.TYPE_MARKETPLACE_LISTED.equals(record.getType())) {
                shared++;
            } else if (ActivityLogRepository.TYPE_LENDING_LISTED.equals(record.getType())) {
                lending++;
            } else if (ActivityLogRepository.TYPE_SCAN.equals(record.getType())) {
                scans++;
            }
        }
        binding.homeReusedValue.setText(String.valueOf(shared));
        binding.homeLentValue.setText(String.valueOf(lending));
        binding.homeScannedValue.setText(String.valueOf(scans));
        int total = shared + lending + scans;
        binding.homeImpactMessage.setText(total == 0
                ? "Complete a scan or publish a resource to begin your device activity summary."
                : total + " completed action(s) recorded for this account on this device.");
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
