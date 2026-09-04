package com.propcycle.app.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.propcycle.app.R;
import com.propcycle.app.core.firebase.FirebaseEnvironment;
import com.propcycle.app.data.activity.ActivityRecord;
import com.propcycle.app.data.marketplace.MarketplaceListing;
import com.propcycle.app.data.profile.ProfileAvatarPolicy;
import com.propcycle.app.databinding.FragmentProfileBinding;
import com.propcycle.app.ui.common.ProfileAvatarRenderer;
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
    private String currentAvatarKey = ProfileAvatarPolicy.DEFAULT;
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
            binding.profileAvatarAction.setEnabled(!result.isWorking());
            binding.profileAvatarContainer.setEnabled(!result.isWorking() && ownProfile);
            if (result.isSuccess()) {
                viewModel.start(requestedUserId);
            }
            Toast.makeText(requireContext(), result.getMessage(), Toast.LENGTH_LONG).show();
        });
        binding.profileEditAction.setOnClickListener(ignored -> showEditName());
        binding.profileAvatarAction.setOnClickListener(ignored -> showAvatarChooser());
        binding.profileAvatarContainer.setOnClickListener(ignored -> showAvatarChooser());
        binding.itemCard.setOnClickListener(ignored -> openListingOrCreate());
        binding.changePasswordAction.setOnClickListener(ignored -> showChangePasswordDialog());
        binding.deleteAccountAction.setOnClickListener(ignored -> showDeleteAccountDialog());
        binding.logoutAction.setOnClickListener(ignored -> signOut());
    }

    private void showAvatarChooser() {
        if (!ownProfile) {
            return;
        }
        List<String> keys = ProfileAvatarPolicy.keys();
        List<String> labels = ProfileAvatarPolicy.labels();
        int selected = Math.max(0, keys.indexOf(currentAvatarKey));
        ProfileAvatarOptionAdapter adapter = new ProfileAvatarOptionAdapter(
                requireContext(), keys, labels, selected);
        new MaterialAlertDialogBuilder(
                requireContext(), R.style.ThemeOverlay_PropCycle_MaterialAlertDialog)
                .setTitle("Choose your avatar")
                .setIcon(R.drawable.ic_person)
                .setAdapter(
                        adapter,
                        (dialog, which) -> {
                            viewModel.updateAvatar(keys.get(which));
                            dialog.dismiss();
                        })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditName() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_profile_name, null);
        TextInputEditText input = dialogView.findViewById(R.id.profile_name_input);
        input.setSingleLine(true);
        input.setText(binding.profileName.getText());
        input.setSelection(input.length());
        new MaterialAlertDialogBuilder(
                requireContext(), R.style.ThemeOverlay_PropCycle_MaterialAlertDialog)
                .setTitle("Edit display name")
                .setIcon(R.drawable.ic_register_person)
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) ->
                        viewModel.updateDisplayName(input.getText().toString()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChangePasswordDialog() {
        if (!ownProfile) {
            return;
        }
        if (!viewModel.isEmailPasswordProvider()) {
            Toast.makeText(requireContext(), R.string.profile_change_password_not_email, Toast.LENGTH_LONG).show();
            return;
        }
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_change_password, null);
        TextInputEditText currentPasswordInput = dialogView.findViewById(R.id.current_password_input);
        TextInputEditText newPasswordInput = dialogView.findViewById(R.id.new_password_input);
        TextInputEditText confirmPasswordInput = dialogView.findViewById(R.id.confirm_password_input);

        AlertDialog dialog = new MaterialAlertDialogBuilder(
                requireContext(), R.style.ThemeOverlay_PropCycle_MaterialAlertDialog)
                .setTitle(R.string.profile_change_password_title)
                .setIcon(R.drawable.ic_login_lock)
                .setView(dialogView)
                .setPositiveButton("Update", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String current = currentPasswordInput.getText() == null
                        ? "" : currentPasswordInput.getText().toString().trim();
                String newPass = newPasswordInput.getText() == null
                        ? "" : newPasswordInput.getText().toString().trim();
                String confirm = confirmPasswordInput.getText() == null
                        ? "" : confirmPasswordInput.getText().toString().trim();

                if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.profile_change_password_fill_all, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (newPass.length() < 6) {
                    Toast.makeText(requireContext(), R.string.profile_change_password_short, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (newPass.equals(current)) {
                    Toast.makeText(requireContext(), R.string.profile_change_password_same, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!newPass.equals(confirm)) {
                    Toast.makeText(requireContext(), R.string.profile_change_password_mismatch, Toast.LENGTH_SHORT).show();
                    return;
                }

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                Toast.makeText(requireContext(), R.string.profile_change_password_saving, Toast.LENGTH_SHORT).show();

                viewModel.changePassword(current, newPass, new ProfileViewModel.PasswordChangeCallback() {
                    @Override
                    public void onSuccess() {
                        if (!isAdded()) return;
                        dialog.dismiss();
                        Toast.makeText(requireContext(), R.string.profile_change_password_success, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        if (!isAdded()) return;
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                    }
                });
            });
        });

        dialog.show();
    }

    private void showDeleteAccountDialog() {
        if (!ownProfile) {
            return;
        }
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_delete_account, null);
        View passwordLayout = dialogView.findViewById(R.id.delete_account_password_layout);
        TextInputEditText passwordInput = dialogView.findViewById(R.id.delete_password_input);

        boolean isEmailPassword = viewModel.isEmailPasswordProvider();
        passwordLayout.setVisibility(isEmailPassword ? View.VISIBLE : View.GONE);

        AlertDialog dialog = new MaterialAlertDialogBuilder(
                requireContext(), R.style.ThemeOverlay_PropCycle_MaterialAlertDialog)
                .setTitle(R.string.profile_delete_account_title)
                .setIcon(R.drawable.ic_lend_delete)
                .setView(dialogView)
                .setPositiveButton(R.string.profile_delete_account_confirm, null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(ContextCompat.getColor(requireContext(), R.color.pc_brand_accent_red));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String password = passwordInput.getText() == null
                        ? "" : passwordInput.getText().toString().trim();
                if (isEmailPassword && password.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.profile_delete_account_password, Toast.LENGTH_SHORT).show();
                    return;
                }

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
                Toast.makeText(requireContext(), R.string.profile_delete_account_deleting, Toast.LENGTH_SHORT).show();

                viewModel.deleteAccount(password, new ProfileViewModel.AccountDeleteCallback() {
                    @Override
                    public void onSuccess() {
                        if (!isAdded()) return;
                        dialog.dismiss();
                        Toast.makeText(requireContext(), R.string.profile_delete_account_success, Toast.LENGTH_LONG).show();
                        ScreenNavigation.navigateClearingBackStack(ProfileFragment.this, R.id.loginFragment);
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        if (!isAdded()) return;
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                    }
                });
            });
        });

        dialog.show();
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
        binding.profileActivityLabel.setText(ownProfile ? "AT A GLANCE" : "PROFILE OVERVIEW");
        binding.profileListingsLabel.setText(ownProfile ? "MY LISTINGS" : "ACTIVE LISTING");
        binding.profileEditAction.setVisibility(ownProfile ? View.VISIBLE : View.GONE);
        binding.profileAvatarAction.setVisibility(ownProfile ? View.VISIBLE : View.GONE);
        binding.profileIdentityActionRow.setVisibility(ownProfile ? View.VISIBLE : View.GONE);
        binding.profileAccountSectionLabel.setVisibility(ownProfile ? View.VISIBLE : View.GONE);
        binding.profileAccountCard.setVisibility(ownProfile ? View.VISIBLE : View.GONE);
        binding.changePasswordAction.setVisibility(ownProfile ? View.VISIBLE : View.GONE);
        binding.deleteAccountAction.setVisibility(ownProfile ? View.VISIBLE : View.GONE);
        binding.logoutAction.setVisibility(ownProfile ? View.VISIBLE : View.GONE);
        binding.profileAvatarContainer.setClickable(ownProfile && !state.isLoading());
        binding.profileAvatarContainer.setFocusable(ownProfile && !state.isLoading());
        binding.profileAvatarContainer.setContentDescription(ownProfile
                ? "Choose your profile avatar"
                : "User profile avatar");

        if (state.isLoading()) {
            binding.profileName.setText("Loading profile...");
            currentAvatarKey = ProfileAvatarPolicy.DEFAULT;
            ProfileAvatarRenderer.render(binding.profileAvatarInitial,
                    binding.profileAvatarIcon, currentAvatarKey, "PropCycle Member");
            binding.profileEmail.setText("Please wait");
            binding.profileMemberSummary.setText("Loading member details");
            return;
        }
        if (!state.getErrorMessage().isEmpty()) {
            binding.profileName.setText("Profile unavailable");
            currentAvatarKey = ProfileAvatarPolicy.DEFAULT;
            ProfileAvatarRenderer.render(binding.profileAvatarInitial,
                    binding.profileAvatarIcon, currentAvatarKey, "PropCycle Member");
            binding.profileEmail.setText(state.getErrorMessage());
            binding.profileMemberSummary.setText("Member details unavailable");
            binding.itemCard.setEnabled(false);
            return;
        }

        String name = state.getDisplayName();
        binding.profileName.setText(name);
        currentAvatarKey = state.getAvatarKey();
        ProfileAvatarRenderer.render(binding.profileAvatarInitial,
                binding.profileAvatarIcon, currentAvatarKey, name);
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
