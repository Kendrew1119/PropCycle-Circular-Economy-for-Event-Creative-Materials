package com.propcycle.app.ui.lending;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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
import com.propcycle.app.data.lending.LendingRequest;
import com.propcycle.app.data.lending.LendingRating;
import com.propcycle.app.data.marketplace.MarketplaceImageLoader;
import com.propcycle.app.databinding.FragmentLendingDetailBinding;
import com.propcycle.app.ui.common.DemoImageCatalog;
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
    private String ownerUserId = "";
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
        viewModel.getRequestCreated().observe(getViewLifecycleOwner(), event -> {
            String id = event == null ? null : event.getIfNotHandled();
            if (id != null && binding != null) {
                Toast.makeText(requireContext(),
                        "Request sent. Open Chat or Notifications to manage it.",
                        Toast.LENGTH_LONG).show();
            }
        });
        binding.requestLendingAction.setOnClickListener(ignored -> showDatePicker());
        binding.cancelRequestAction.setOnClickListener(ignored -> confirmCancelRequest());
        binding.chatAction.setOnClickListener(ignored -> openChat());
        binding.editLendingAction.setOnClickListener(ignored -> {
            Bundle edit = new Bundle();
            edit.putString("itemId", itemId);
            ScreenNavigation.navigateAuthenticated(this, R.id.lendResourceFragment, edit);
        });
        binding.toggleLendingStatusAction.setOnClickListener(ignored -> confirmStatusChange());
        binding.lendingOwnerCard.setOnClickListener(ignored -> openOwnerProfile());
        binding.lendingOwnerAvatar.setOnClickListener(ignored -> openOwnerProfile());
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
        int maximumDays = item.getMaxBorrowDays() == null
                ? LendingPolicy.MAX_REQUEST_DAYS
                : item.getMaxBorrowDays().intValue();
        MaterialDatePicker<Pair<Long, Long>> picker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTheme(R.style.ThemeOverlay_PropCycle_MaterialDatePicker)
                        .setTitleText(getResources().getQuantityString(
                                R.plurals.lending_date_picker_title_with_limit,
                                maximumDays,
                                maximumDays))
                        .setNegativeButtonText(R.string.lending_date_picker_cancel)
                        .setPositiveButtonText(R.string.lending_date_picker_confirm)
                        .build();
        picker.addOnPositiveButtonClickListener(range -> {
            if (range == null || range.first == null || range.second == null) {
                return;
            }
            showBorrowingPeriodConfirmation(
                    maximumDays,
                    range.first,
                    range.second);
        });
        picker.show(getParentFragmentManager(), "lending-date-range");
    }

    private void showBorrowingPeriodConfirmation(
            int maximumDays,
            long startMillis,
            long endMillis) {
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_lending_date_summary, null, false);
        TextView range = content.findViewById(R.id.lending_date_summary_range);
        TextView limit = content.findViewById(R.id.lending_date_summary_limit);
        range.setText(getString(
                R.string.lending_date_summary_range,
                formatDisplayDate(startMillis),
                formatDisplayDate(endMillis)));
        limit.setText(getResources().getQuantityString(
                R.plurals.lending_date_summary_limit,
                maximumDays,
                maximumDays));

        new MaterialAlertDialogBuilder(
                requireContext(),
                R.style.ThemeOverlay_PropCycle_MaterialAlertDialog)
                .setView(content)
                .setNegativeButton(R.string.lending_date_summary_back, null)
                .setPositiveButton(
                        R.string.lending_date_summary_send,
                        (dialog, which) -> viewModel.request(
                                formatPickerDate(startMillis),
                                formatPickerDate(endMillis)))
                .show();
    }

    private void openChat() {
        LendingDetailViewModel.State state = viewModel.getState().getValue();
        LendingItem item = state == null ? null : state.getItem();
        if (item == null || state.isBusy()) {
            return;
        }
        LendingRequest request = state.getRequest();
        com.google.android.gms.tasks.Task<String> chatTask = request == null
                ? ChatRepository.createOrGetLendingThread(
                        requireContext(), item.getId(), item.getOwnerId(), item.getTitle())
                : ChatRepository.createOrGetLendingRequestThread(
                        requireContext(),
                        item.getId(),
                        item.getOwnerId(),
                        item.getTitle(),
                        request.getId());
        chatTask
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

    private void confirmCancelRequest() {
        LendingDetailViewModel.State state = viewModel.getState().getValue();
        LendingRequest request = state == null ? null : state.getRequest();
        if (request == null || state.isBusy()) {
            return;
        }
        new MaterialAlertDialogBuilder(
                requireContext(),
                R.style.ThemeOverlay_PropCycle_MaterialAlertDialog)
                .setTitle("Cancel this request?")
                .setMessage(request.getItemTitle() + "\n" + request.getStartDate()
                        + " to " + request.getEndDate())
                .setNegativeButton("Back", null)
                .setPositiveButton("Cancel request",
                        (dialog, which) -> viewModel.cancelRequest())
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
            ownerUserId = "";
            binding.requestLendingAction.setEnabled(false);
            binding.cancelRequestAction.setVisibility(View.GONE);
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
        ownerUserId = item.getOwnerId();
        binding.lendingOwnerAvatar.setContentDescription("Open lending owner's profile");
        boolean owner = item.getOwnerId().equals(viewModel.currentUserId());
        boolean available = "available".equals(item.getStatus());
        LendingRequest request = state.getRequest();
        boolean cancellable = request != null
                && viewModel.currentUserId() != null
                && viewModel.currentUserId().equals(request.getBorrowerUid())
                && ("pending".equals(request.getStatus())
                || "approved".equals(request.getStatus()));
        binding.lendingOwnerActions.setVisibility(owner ? View.VISIBLE : View.GONE);
        binding.requestLendingAction.setVisibility(owner ? View.GONE : View.VISIBLE);
        binding.cancelRequestAction.setVisibility(
                !owner && cancellable ? View.VISIBLE : View.GONE);
        binding.chatAction.setVisibility(owner ? View.GONE : View.VISIBLE);
        binding.requestLendingAction.setEnabled(available && !state.isBusy());
        binding.cancelRequestAction.setEnabled(cancellable && !state.isBusy());
        binding.chatAction.setEnabled(available && !state.isBusy());
        binding.editLendingAction.setEnabled(!state.isBusy());
        binding.toggleLendingStatusAction.setEnabled(!state.isBusy());
        binding.toggleLendingStatusAction.setText(available ? "Withdraw" : "Relist");
        if (state.getMessage() == null && request != null && !owner) {
            binding.lendingDetailStatus.setText(
                    "Your request: " + LendingPolicy.displayLabel(request.getStatus()));
        }
        updateImage(item.getImageUrl(), item.getDemoImageKey());
    }

    private void openOwnerProfile() {
        if (ownerUserId.isEmpty()) {
            return;
        }
        Bundle arguments = new Bundle();
        arguments.putString("userId", ownerUserId);
        ScreenNavigation.navigateAuthenticated(this, R.id.profileFragment, arguments);
    }

    private void updateImage(@Nullable String url, @Nullable String demoImageKey) {
        if ((url == null || url.trim().isEmpty())) {
            if (imageHandle != null) {
                imageHandle.cancel();
                imageHandle = null;
            }
            int demoDrawable = DemoImageCatalog.drawableFor(demoImageKey);
            displayedImageUrl = demoDrawable == 0 ? null : "demo:" + demoImageKey;
            binding.lendingDetailImage.setScaleType(
                    demoDrawable == 0
                            ? android.widget.ImageView.ScaleType.CENTER_INSIDE
                            : android.widget.ImageView.ScaleType.FIT_CENTER);
            binding.lendingDetailImage.setImageResource(
                    demoDrawable == 0 ? R.drawable.ic_bottom_nav_lend_out : demoDrawable);
            return;
        }
        if (url.equals(displayedImageUrl)) {
            return;
        }
        if (imageHandle != null) {
            imageHandle.cancel();
        }
        displayedImageUrl = url;
        binding.lendingDetailImage.setScaleType(
                android.widget.ImageView.ScaleType.CENTER_CROP);
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

    @NonNull
    private static String formatDisplayDate(long millis) {
        SimpleDateFormat format = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date(millis));
    }
}
