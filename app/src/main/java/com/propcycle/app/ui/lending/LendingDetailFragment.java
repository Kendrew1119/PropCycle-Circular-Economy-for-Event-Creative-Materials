package com.propcycle.app.ui.lending;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.propcycle.app.R;
import com.propcycle.app.data.chat.ChatRepository;
import com.propcycle.app.data.lending.LendingItem;
import com.propcycle.app.data.lending.LendingPolicy;
import com.propcycle.app.data.lending.LendingRating;
import com.propcycle.app.data.marketplace.MarketplaceImageLoader;
import com.propcycle.app.databinding.FragmentLendingDetailBinding;
import com.propcycle.app.ui.common.ScreenNavigation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Proposal detail surface with real chat and bounded date requests. */
public final class LendingDetailFragment extends Fragment {

    private FragmentLendingDetailBinding binding;
    private LendingDetailViewModel viewModel;
    private MarketplaceImageLoader imageLoader;
    private MarketplaceImageLoader.LoadHandle imageHandle;
    private String itemId = "";
    private String displayedImageUrl;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentLendingDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ScreenNavigation.bindChrome(this, view);
        Bundle arguments = getArguments();
        itemId = arguments == null ? "" : arguments.getString("itemId", "");
        imageLoader = new MarketplaceImageLoader(requireContext());
        viewModel = new ViewModelProvider(this).get(LendingDetailViewModel.class);
        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getRequestCreated().observe(getViewLifecycleOwner(), id -> {
            if (id != null && binding != null) {
                Toast.makeText(requireContext(),
                        "Request sent. Open Notifications to manage it.",
                        Toast.LENGTH_LONG).show();
            }
        });
        binding.requestLendingAction.setOnClickListener(ignored -> showDatePicker());
        binding.chatAction.setOnClickListener(ignored -> openChat());
        binding.editLendingAction.setOnClickListener(ignored -> {
            Bundle edit = new Bundle();
            edit.putString("itemId", itemId);
            ScreenNavigation.navigateAuthenticated(this, R.id.lendResourceFragment, edit);
        });
        binding.toggleLendingStatusAction.setOnClickListener(ignored -> confirmStatusChange());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (ScreenNavigation.navigateAuthenticated(this, R.id.lendingDetailFragment, null)) {
            viewModel.start(itemId);
        }
    }

    @Override
    public void onStop() {
        viewModel.stop();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        if (imageHandle != null) {
            imageHandle.cancel();
        }
        if (imageLoader != null) {
            imageLoader.close();
        }
        binding = null;
        super.onDestroyView();
    }

    private void showDatePicker() {
        LendingDetailViewModel.State state = viewModel.getState().getValue();
        LendingItem item = state == null ? null : state.getItem();
        if (item == null || state.isBusy()) {
            return;
        }
        MaterialDatePicker<Pair<Long, Long>> picker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Choose borrowing dates")
                        .build();
        picker.addOnPositiveButtonClickListener(range -> {
            if (range == null || range.first == null || range.second == null) {
                return;
            }
            viewModel.request(formatPickerDate(range.first), formatPickerDate(range.second));
        });
        picker.show(getParentFragmentManager(), "lending-date-range");
    }

    private void openChat() {
        LendingDetailViewModel.State state = viewModel.getState().getValue();
        LendingItem item = state == null ? null : state.getItem();
        if (item == null || state.isBusy()) {
            return;
        }
        ChatRepository.createOrGetLendingThread(
                        requireContext(), item.getId(), item.getOwnerId(), item.getTitle())
                .addOnSuccessListener(threadId -> {
                    if (!isAdded()) {
                        return;
                    }
                    Bundle arguments = new Bundle();
                    arguments.putString("threadId", threadId);
                    ScreenNavigation.navigateAuthenticated(
                            this, R.id.conversationFragment, arguments);
                })
                .addOnFailureListener(error -> Toast.makeText(
                        requireContext(),
                        error.getMessage() == null
                                ? "The lending conversation could not be opened."
                                : error.getMessage(),
                        Toast.LENGTH_LONG).show());
    }

    private void confirmStatusChange() {
        LendingDetailViewModel.State state = viewModel.getState().getValue();
        LendingItem item = state == null ? null : state.getItem();
        if (item == null || state.isBusy()) {
            return;
        }
        boolean withdraw = "available".equals(item.getStatus());
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(withdraw ? "Withdraw lending item?" : "Relist lending item?")
                .setMessage(withdraw
                        ? "It will disappear from public lending browse. Existing requests remain visible."
                        : "It will be available for new borrowing requests again.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton(withdraw ? "Withdraw" : "Relist",
                        (dialog, which) -> viewModel.toggleStatus())
                .show();
    }

    private void render(@NonNull LendingDetailViewModel.State state) {
        if (binding == null) {
            return;
        }
        LendingItem item = state.getItem();
        binding.lendingDetailProgress.setVisibility(
                state.isLoading() || state.isBusy() ? View.VISIBLE : View.GONE);
        binding.lendingDetailStatus.setText(state.getMessage() == null ? "" : state.getMessage());
        if (item == null) {
            binding.requestLendingAction.setEnabled(false);
            binding.chatAction.setEnabled(false);
            return;
        }
        binding.lendingDetailTitle.setText(item.getTitle());
        binding.lendingDetailMeta.setText(
                LendingPolicy.displayLabel(item.getCategory()) + "  •  "
                        + LendingPolicy.displayLabel(item.getCondition()) + "  •  "
                        + item.getAreaLabel());
        binding.lendingDetailDescription.setText(item.getDescription());
        long deposit = item.getDepositMinor() == null ? 0L : item.getDepositMinor();
        binding.lendingDetailTerms.setText(String.format(
                Locale.ROOT,
                "%s  •  Up to %d days\n%s",
                LendingPolicy.displayLabel(item.getPickupMethod()),
                item.getMaxBorrowDays() == null ? 31 : item.getMaxBorrowDays(),
                deposit > 0
                        ? String.format(Locale.ROOT,
                                "Optional refundable deposit: RM %.2f (arranged outside PropCycle)",
                                deposit / 100d)
                        : "No deposit stated"));
        binding.lendingDetailTrust.setText(ratingText(state.getRatings()));
        boolean owner = item.getOwnerId().equals(viewModel.currentUserId());
        boolean available = "available".equals(item.getStatus());
        binding.lendingOwnerActions.setVisibility(owner ? View.VISIBLE : View.GONE);
        binding.requestLendingAction.setVisibility(owner ? View.GONE : View.VISIBLE);
        binding.chatAction.setVisibility(owner ? View.GONE : View.VISIBLE);
        binding.requestLendingAction.setEnabled(available && !state.isBusy());
        binding.chatAction.setEnabled(available && !state.isBusy());
        binding.editLendingAction.setEnabled(!state.isBusy());
        binding.toggleLendingStatusAction.setEnabled(!state.isBusy());
        binding.toggleLendingStatusAction.setText(available ? "Withdraw" : "Relist");
        updateImage(item.getImageUrl());
    }

    private void updateImage(@Nullable String url) {
        if ((url == null || url.trim().isEmpty())) {
            displayedImageUrl = null;
            binding.lendingDetailImage.setImageResource(R.drawable.ic_bottom_nav_lend_out);
            return;
        }
        if (url.equals(displayedImageUrl)) {
            return;
        }
        if (imageHandle != null) {
            imageHandle.cancel();
        }
        displayedImageUrl = url;
        binding.lendingDetailImage.setImageResource(R.drawable.ic_bottom_nav_lend_out);
        imageHandle = imageLoader.load(url, new MarketplaceImageLoader.Callback() {
            @Override public void onLoaded(@NonNull Bitmap bitmap) {
                if (binding != null && url.equals(displayedImageUrl)) {
                    binding.lendingDetailImage.setImageBitmap(bitmap);
                }
            }
            @Override public void onError() { }
        });
    }

    @NonNull
    private static String ratingText(@NonNull List<LendingRating> ratings) {
        if (ratings.isEmpty()) {
            return "Owner rating: No ratings yet";
        }
        long total = 0L;
        int count = 0;
        for (LendingRating rating : ratings) {
            if (rating.getScore() != null) {
                total += rating.getScore();
                count++;
            }
        }
        return count == 0 ? "Owner rating: No ratings yet" : String.format(
                Locale.ROOT, "Owner rating: %.1f/5 from %d completed loan(s)",
                total / (double) count, count);
    }

    @NonNull
    private static String formatPickerDate(long millis) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date(millis));
    }
}
