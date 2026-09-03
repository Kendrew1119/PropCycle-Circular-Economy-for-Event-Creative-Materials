package com.propcycle.app.ui.lending;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RatingBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.propcycle.app.R;
import com.propcycle.app.data.lending.LendingRequest;
import com.propcycle.app.data.marketplace.MarketplaceStatusNotice;
import com.propcycle.app.databinding.FragmentNotificationsBinding;
import com.propcycle.app.ui.common.ScreenNavigation;
import com.propcycle.app.ui.marketplace.MarketplaceStatusNoticeAdapter;

/** Existing Notifications surface used as a real in-app lending request inbox. */
public final class LendingRequestsFragment extends Fragment {

    private static final String DIAGNOSTIC_TAG = "PropCycleLendingDebug";

    private FragmentNotificationsBinding binding;
    private LendingRequestsViewModel viewModel;
    private LendingRequestAdapter adapter;
    private MarketplaceStatusNoticeAdapter marketplaceAdapter;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ScreenNavigation.bindChrome(this, view);
        viewModel = new ViewModelProvider(this).get(LendingRequestsViewModel.class);
        String uid = viewModel.currentUserId();
        if (uid == null) {
            ScreenNavigation.navigateAuthenticated(this, R.id.notificationsFragment, null);
            return;
        }
        adapter = new LendingRequestAdapter(uid, this::confirmAction);
        marketplaceAdapter = new MarketplaceStatusNoticeAdapter(this::openMarketplaceNotice);
        binding.marketplaceUpdateList.setLayoutManager(new LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.marketplaceUpdateList.setAdapter(marketplaceAdapter);
        binding.lendingRequestList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.lendingRequestList.setAdapter(adapter);
        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getMarketplaceNoticeState().observe(
                getViewLifecycleOwner(), this::renderMarketplaceNotices);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(DIAGNOSTIC_TAG, "LendingRequestsFragment onStart");
        if (viewModel != null) {
            Log.d(DIAGNOSTIC_TAG, "LendingRequestsFragment ViewModel start requested");
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

    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.lendingRequestList.setAdapter(null);
            binding.marketplaceUpdateList.setAdapter(null);
        }
        binding = null;
        super.onDestroyView();
    }

    private void confirmAction(
            @NonNull LendingRequest request,
            @NonNull LendingRequestAdapter.Action action) {
        if (action == LendingRequestAdapter.Action.RATE) {
            showRatingDialog(request);
            return;
        }
        String title = switch (action) {
            case APPROVE -> "Approve this request?";
            case REJECT -> "Reject this request?";
            case CANCEL -> "Cancel this request?";
            case ACTIVATE -> "Confirm the item was picked up?";
            case REPORT_RETURN -> "Report the item as returned?";
            case CONFIRM_RETURN -> "Confirm you received the returned item?";
            case RATE -> "Rate this lending experience";
        };
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage(request.getItemTitle() + "\n" + request.getStartDate()
                        + " to " + request.getEndDate())
                .setNegativeButton("Back", null)
                .setPositiveButton("Confirm",
                        (dialog, which) -> viewModel.perform(request, action))
                .show();
    }

    private void showRatingDialog(@NonNull LendingRequest request) {
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        content.setPadding(padding, padding / 2, padding, 0);
        RatingBar rating = new RatingBar(requireContext(), null,
                android.R.attr.ratingBarStyleSmall);
        rating.setNumStars(5);
        rating.setStepSize(1f);
        rating.setRating(5f);
        EditText comment = new EditText(requireContext());
        comment.setHint("Optional public comment");
        comment.setMaxLines(4);
        content.addView(rating);
        content.addView(comment);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Rate the item owner")
                .setView(content)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> viewModel.rate(
                        request,
                        Math.max(1, Math.round(rating.getRating())),
                        comment.getText().toString()))
                .show();
    }

    private void render(@NonNull LendingRequestsViewModel.State state) {
        if (binding == null || adapter == null) {
            return;
        }
        Log.d(DIAGNOSTIC_TAG,
                "Fragment observer request list size=" + state.getRequests().size());
        adapter.submitList(state.getRequests(), state.getBusyRequestId());
        Log.d(DIAGNOSTIC_TAG, "adapter item count=" + adapter.getItemCount());
        binding.lendingRequestProgress.setVisibility(
                state.isLoading() ? View.VISIBLE : View.GONE);
        String message = state.getMessage();
        boolean showEmptyState = message == null
                && !state.isLoading()
                && state.getRequests().isEmpty();
        Log.d(DIAGNOSTIC_TAG,
                "empty-state visibility decision=" + showEmptyState);
        if (message == null) {
            message = !state.isLoading() && state.getRequests().isEmpty()
                    ? "No lending updates yet."
                    : state.getRequests().size() + " lending update(s)";
        }
        binding.lendingRequestStatus.setText(message);
    }

    private void renderMarketplaceNotices(
            @NonNull LendingRequestsViewModel.MarketplaceNoticeState state) {
        if (binding == null || marketplaceAdapter == null) {
            return;
        }
        marketplaceAdapter.submitList(state.getNotices());
        boolean hasNotices = !state.getNotices().isEmpty();
        binding.marketplaceUpdateList.setVisibility(hasNotices ? View.VISIBLE : View.GONE);
        String message = state.getMessage();
        if (state.isLoading()) {
            message = "Loading Marketplace status updates...";
        } else if (message == null) {
            message = hasNotices
                    ? state.getNotices().size() + " recent sold listing update(s)"
                    : "No sold listing updates from your conversations.";
        }
        binding.marketplaceUpdateStatus.setText(message);
    }

    private void openMarketplaceNotice(@NonNull MarketplaceStatusNotice notice) {
        Bundle arguments = new Bundle();
        arguments.putString("listingId", notice.getListingId());
        ScreenNavigation.navigateAuthenticated(this, R.id.marketDetailFragment, arguments);
    }
}
