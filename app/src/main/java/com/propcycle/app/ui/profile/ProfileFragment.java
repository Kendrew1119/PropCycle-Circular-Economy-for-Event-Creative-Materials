package com.propcycle.app.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseUserMetadata;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.propcycle.app.R;
import com.propcycle.app.core.firebase.FirebaseEnvironment;
import com.propcycle.app.data.activity.ActivityRecord;
import com.propcycle.app.data.marketplace.MarketplaceListing;
import com.propcycle.app.databinding.FragmentProfileBinding;
import com.propcycle.app.ui.common.ResourceCreationFlow;
import com.propcycle.app.ui.common.ScreenNavigation;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Real account identity, local activity summary, and the user's first active listing. */
public final class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;
    private MarketplaceListing firstListing;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!ScreenNavigation.navigateAuthenticated(this, R.id.profileFragment, null)) {
            return;
        }
        ScreenNavigation.bindChrome(this, view);
        bindAccount();
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        viewModel.getActivities().observe(getViewLifecycleOwner(), this::renderActivities);
        viewModel.getOwnedListings().observe(getViewLifecycleOwner(), this::renderListings);
        viewModel.getProfileUpdate().observe(getViewLifecycleOwner(), event -> {
            ProfileViewModel.ProfileUpdate result =
                    event == null ? null : event.getIfNotHandled();
            if (result == null || binding == null) {
                return;
            }
            binding.profileEditAction.setEnabled(!result.isWorking());
            if (result.isSuccess()) {
                bindAccount();
            }
            Toast.makeText(requireContext(), result.getMessage(), Toast.LENGTH_LONG).show();
        });
        binding.profileEditAction.setOnClickListener(ignored -> showEditName());
        binding.itemCard.setOnClickListener(ignored -> openListingOrCreate());
        binding.logoutAction.setOnClickListener(ignored -> signOut());
    }

    private void showEditName() {
        EditText input = new EditText(requireContext());
        input.setSingleLine(true);
        input.setText(binding.profileName.getText());
        input.setSelection(input.length());
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Edit display name")
                .setMessage("This public name is shown on your marketplace and lending activity.")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) ->
                        viewModel.updateDisplayName(input.getText().toString()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (viewModel != null) {
            viewModel.start();
        }
    }

    @Override
    public void onStop() {
        if (viewModel != null) {
            viewModel.stop();
        }
        super.onStop();
    }

    private void bindAccount() {
        FirebaseAuth auth = FirebaseEnvironment.auth(requireContext());
        FirebaseUser user = auth == null ? null : auth.getCurrentUser();
        String name = user == null ? null : user.getDisplayName();
        if (name == null || name.trim().isEmpty()) {
            name = "PropCycle Member";
        }
        binding.profileName.setText(name.trim());
        binding.profileAvatarInitial.setText(
                name.substring(0, 1).toUpperCase(Locale.ROOT));
        binding.profileEmail.setText(user == null || user.getEmail() == null
                ? "Email unavailable" : user.getEmail());
        FirebaseUserMetadata metadata = user == null ? null : user.getMetadata();
        long created = metadata == null ? 0L : metadata.getCreationTimestamp();
        binding.profileMemberSummary.setText(created <= 0L
                ? "PropCycle member"
                : "Member since " + new SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                        .format(new Date(created)));
    }

    private void renderActivities(@Nullable List<ActivityRecord> value) {
        List<ActivityRecord> records = value == null ? Collections.emptyList() : value;
        binding.profileActivitySummary.setText(records.isEmpty()
                ? "No recorded activity yet"
                : records.size() + " action(s) recorded on this device");
    }

    private void renderListings(@Nullable List<MarketplaceListing> value) {
        firstListing = value == null || value.isEmpty() ? null : value.get(0);
        if (firstListing == null) {
            binding.profileListingTitle.setText("No active marketplace listings");
            binding.profileListingMeta.setText("Tap to create one");
            binding.itemCard.setContentDescription("Create a marketplace listing");
            return;
        }
        binding.profileListingTitle.setText(firstListing.getTitle());
        binding.profileListingMeta.setText("Active listing • Tap to manage");
        binding.itemCard.setContentDescription(
                "Open active marketplace listing " + firstListing.getTitle());
    }

    private void openListingOrCreate() {
        if (firstListing == null || firstListing.getId() == null) {
            ResourceCreationFlow.show(this, ResourceCreationFlow.TARGET_MARKETPLACE);
            return;
        }
        Bundle arguments = new Bundle();
        arguments.putString("listingId", firstListing.getId());
        ScreenNavigation.navigateAuthenticated(this, R.id.marketDetailFragment, arguments);
    }

    private void signOut() {
        FirebaseAuth auth = FirebaseEnvironment.auth(requireContext());
        if (auth != null) {
            auth.signOut();
        }
        ScreenNavigation.navigateClearingBackStack(this, R.id.loginFragment);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
