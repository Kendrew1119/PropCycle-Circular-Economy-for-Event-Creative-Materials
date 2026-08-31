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
    private List<ActivityRecord> activityRecords = Collections.emptyList();
    private String requestedUserId = "";
    private boolean ownProfile;

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
        Bundle arguments = getArguments();
        requestedUserId = arguments == null ? "" : arguments.getString("userId", "");
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        viewModel.getProfileState().observe(getViewLifecycleOwner(), this::renderProfile);
        viewModel.getRatingSummary().observe(getViewLifecycleOwner(), summary -> {
            if (binding == null || summary == null) {
                return;
            }
            binding.profileRatingBar.setRating((float) summary.getAverage());
            binding.profileRatingSummary.setText(summary.displayText());
            binding.profileRatingRow.setContentDescription(
                    "Marketplace seller rating: " + summary.displayText());
        });
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
                viewModel.start(requestedUserId);
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
            viewModel.start(requestedUserId);
        }
    }

    @Override
    public void onStop() {
        if (viewModel != null) {
            viewModel.stop();
        }
        super.onStop();
    }

    private void renderProfile(@NonNull ProfileViewModel.ProfileState state) {
        if (binding == null) {
            return;
        }
        ownProfile = state.isOwnProfile();
        binding.profileHeaderLabel.setText(ownProfile ? "MY PROFILE" : "PUBLIC PROFILE");
        binding.profileActivityLabel.setText(ownProfile ? "YOUR ACTIVITY" : "PROFILE");
        binding.profileListingsLabel.setText(ownProfile ? "MY LISTINGS" : "ACTIVE LISTING");
        binding.profileEditAction.setVisibility(ownProfile ? View.VISIBLE : View.GONE);
        binding.logoutAction.setVisibility(ownProfile ? View.VISIBLE : View.GONE);

        if (state.isLoading()) {
            binding.profileName.setText("Loading profile...");
            binding.profileAvatarInitial.setText("P");
            binding.profileEmail.setText("Please wait");
            binding.profileMemberSummary.setText("Loading member details");
            return;
        }
        if (!state.getErrorMessage().isEmpty()) {
            binding.profileName.setText("Profile unavailable");
            binding.profileAvatarInitial.setText("P");
            binding.profileEmail.setText(state.getErrorMessage());
            binding.profileMemberSummary.setText("Member details unavailable");
            binding.itemCard.setEnabled(false);
            return;
        }

        String name = state.getDisplayName();
        binding.profileName.setText(name);
        binding.profileAvatarInitial.setText(
                name.substring(0, 1).toUpperCase(Locale.ROOT));
        binding.profileEmail.setText(ownProfile
                ? state.getEmail() == null ? "Email unavailable" : state.getEmail()
                : "Public PropCycle profile");
        long created = state.getCreatedAtMillis();
        binding.profileMemberSummary.setText(created <= 0L
                ? "PropCycle member"
                : "Member since " + new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
                        .format(new Date(created)));
        renderActivities(activityRecords);
        renderListings(viewModel.getOwnedListings().getValue());
    }

    private void renderActivities(@Nullable List<ActivityRecord> value) {
        activityRecords = value == null ? Collections.emptyList() : value;
        if (binding == null) {
            return;
        }
        if (!ownProfile) {
            binding.profileActivitySummary.setText("Marketplace community member");
            return;
        }
        binding.profileActivitySummary.setText(activityRecords.isEmpty()
                ? "No recorded activity yet"
                : activityRecords.size() + " action(s) recorded on this device");
    }

    private void renderListings(@Nullable List<MarketplaceListing> value) {
        firstListing = value == null || value.isEmpty() ? null : value.get(0);
        if (firstListing == null) {
            binding.profileListingTitle.setText("No active marketplace listings");
            binding.profileListingMeta.setText(ownProfile
                    ? "Tap to create one" : "This member has no available listing");
            binding.itemCard.setEnabled(ownProfile);
            binding.itemCard.setClickable(ownProfile);
            binding.itemCard.setContentDescription(ownProfile
                    ? "Create a marketplace listing"
                    : "No active marketplace listings");
            return;
        }
        binding.itemCard.setEnabled(true);
        binding.itemCard.setClickable(true);
        binding.profileListingTitle.setText(firstListing.getTitle());
        binding.profileListingMeta.setText(ownProfile
                ? "Active listing • Tap to manage"
                : "Available now • Tap to view");
        binding.itemCard.setContentDescription(
                "Open active marketplace listing " + firstListing.getTitle());
    }

    private void openListingOrCreate() {
        if (firstListing == null || firstListing.getId() == null) {
            if (ownProfile) {
                ResourceCreationFlow.show(this, ResourceCreationFlow.TARGET_MARKETPLACE);
            }
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
