package com.propcycle.app.ui.marketplace;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.propcycle.app.R;
import com.propcycle.app.data.marketplace.MarketplaceListing;
import com.propcycle.app.data.marketplace.MarketplaceImageLoader;
import com.propcycle.app.data.marketplace.MarketplaceListingStatusPolicy;
import com.propcycle.app.data.marketplace.MarketplaceListingValidator;
import com.propcycle.app.databinding.FragmentMarketDetailBinding;
import com.propcycle.app.ui.common.DemoImageCatalog;
import com.propcycle.app.ui.common.ScreenNavigation;

import java.util.Locale;

/** Live marketplace detail screen with protected owner controls and buyer chat. */
public final class MarketDetailFragment extends Fragment {

    private static final String ARG_LISTING_ID = "listingId";

    private FragmentMarketDetailBinding binding;
    private MarketDetailViewModel viewModel;
    private MarketplaceImageLoader imageLoader;
    private MarketplaceImageLoader.LoadHandle imageLoadHandle;
    private MarketplaceListing currentListing;
    private boolean currentOwner;
    private boolean ownerActionBusy;
    private MarketDetailViewModel.RatingState currentRatingState;
    private String displayedImageUrl;
    private String displayedDemoImageKey;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentMarketDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ScreenNavigation.bindChrome(this, view);
        imageLoader = new MarketplaceImageLoader(requireContext());

        viewModel = new ViewModelProvider(this).get(MarketDetailViewModel.class);
        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getOwnerActionState().observe(
                getViewLifecycleOwner(), this::renderOwnerAction);
        viewModel.getRatingState().observe(getViewLifecycleOwner(), this::renderRating);
        viewModel.getSellerName().observe(getViewLifecycleOwner(), name -> {
            if (binding == null || name == null || name.trim().isEmpty()) {
                return;
            }
            binding.sellerName.setText(name);
            binding.sellerAvatar.setText(
                    name.substring(0, 1).toUpperCase(Locale.ROOT));
        });
        viewModel.getChatNotice().observe(getViewLifecycleOwner(), notice -> {
            boolean show = notice != null && !notice.trim().isEmpty();
            binding.marketDetailActionStatus.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) {
                binding.marketDetailActionStatus.setText(notice);
            }
        });
        viewModel.getOpenedThread().observe(getViewLifecycleOwner(), event -> {
            String threadId = event == null ? null : event.getIfNotHandled();
            if (threadId == null) {
                return;
            }
            Bundle arguments = new Bundle();
            arguments.putString("threadId", threadId);
            ScreenNavigation.navigateAuthenticated(
                    this,
                    R.id.conversationFragment,
                    arguments);
        });

        binding.chatAction.setOnClickListener(ignored -> viewModel.requestChat());
        binding.sellerCard.setOnClickListener(ignored -> openSellerProfile());
        binding.sellerAvatar.setOnClickListener(ignored -> openSellerProfile());
        binding.rateSellerAction.setOnClickListener(ignored -> showRatingDialog());
        binding.editListingAction.setOnClickListener(ignored -> openEditor());
        binding.listingStatusAction.setOnClickListener(ignored -> confirmStatusChange());
        binding.markSoldAction.setOnClickListener(ignored -> confirmMarkSold());

        Bundle arguments = getArguments();
        viewModel.load(arguments == null ? "" : arguments.getString(ARG_LISTING_ID, ""));
    }

    private void openSellerProfile() {
        if (currentListing == null || currentListing.getOwnerId() == null) {
            return;
        }
        Bundle arguments = new Bundle();
        arguments.putString("userId", currentListing.getOwnerId());
        ScreenNavigation.navigateAuthenticated(this, R.id.profileFragment, arguments);
    }

    private void showRatingDialog() {
        if (currentListing == null || currentOwner || currentRatingState == null
                || currentRatingState.isSaving()) {
            return;
        }
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(24f * getResources().getDisplayMetrics().density);
        content.setPadding(padding, padding / 2, padding, 0);

        TextView guidance = new TextView(requireContext());
        guidance.setText("Rate your marketplace experience with this seller.");
        guidance.setTextColor(ContextCompat.getColor(
                requireContext(), R.color.pc_brand_text_secondary));
        guidance.setTextSize(14f);
        content.addView(guidance);

        RatingBar rating = new RatingBar(
                requireContext(), null, android.R.attr.ratingBarStyle);
        rating.setNumStars(5);
        rating.setStepSize(1f);
        rating.setRating(currentRatingState.getMyScore() > 0
                ? currentRatingState.getMyScore() : 5f);
        LinearLayout.LayoutParams ratingParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        ratingParams.topMargin = padding / 2;
        rating.setLayoutParams(ratingParams);
        content.addView(rating);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(currentRatingState.getMyScore() > 0
                        ? "Update seller rating" : "Rate seller")
                .setView(content)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) ->
                        viewModel.saveSellerRating(Math.max(1, Math.round(rating.getRating()))))
                .show();
    }

    private void openEditor() {
        if (!currentOwner || currentListing == null || currentListing.getId() == null) {
            return;
        }
        Bundle arguments = new Bundle();
        arguments.putString(ARG_LISTING_ID, currentListing.getId());
        ScreenNavigation.navigateAuthenticated(this, R.id.createListingFragment, arguments);
    }

    private void confirmStatusChange() {
        if (!currentOwner || currentListing == null || ownerActionBusy) {
            return;
        }
        boolean withdraw = MarketplaceListingStatusPolicy.canWithdraw(
                true, currentListing.getStatus());
        boolean relist = MarketplaceListingStatusPolicy.canRelist(
                true, currentListing.getStatus());
        if (!withdraw && !relist) {
            return;
        }

        String target = withdraw
                ? MarketplaceListingStatusPolicy.WITHDRAWN
                : MarketplaceListingStatusPolicy.AVAILABLE;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(withdraw ? "Withdraw listing?" : "Relist this item?")
                .setMessage(withdraw
                        ? "It will disappear from public browse and no new buyer chat can start. Existing conversations are kept."
                        : "The listing will appear in public browse again and people can contact you.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton(withdraw ? "Withdraw" : "Relist",
                        (dialog, which) -> viewModel.requestStatusChange(target))
                .show();
    }

    private void confirmMarkSold() {
        if (!currentOwner || currentListing == null || ownerActionBusy
                || !MarketplaceListingStatusPolicy.canMarkSold(
                        true, currentListing.getStatus())) {
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Mark this listing as sold?")
                .setMessage("This is a final action. The item will disappear from Marketplace and no new buyers can start a chat. Existing conversations will remain available.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Mark as sold", (dialog, which) ->
                        viewModel.requestStatusChange(MarketplaceListingStatusPolicy.SOLD))
                .show();
    }

    private void render(@NonNull MarketDetailViewModel.State state) {
        boolean loading = state.getKind() == MarketDetailViewModel.State.Kind.LOADING;
        MarketplaceListing listing = state.getListing();
        boolean content = listing != null;

        binding.marketDetailProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.marketDetailContent.setVisibility(content ? View.VISIBLE : View.GONE);
        binding.marketDetailStatus.setVisibility(
                loading || content ? View.GONE : View.VISIBLE);
        if (!loading && !content) {
            binding.marketDetailStatus.setText(state.getMessage());
        }
        if (!content) {
            currentListing = null;
            currentOwner = false;
            return;
        }

        currentListing = listing;
        currentOwner = state.isOwner();
        binding.listingTitle.setText(valueOrFallback(listing.getTitle(), "Untitled item"));
        displayListingImage(listing.getImageUrl(), listing.getDemoImageKey());
        binding.listingDescription.setText(
                valueOrFallback(listing.getDescription(), "No description provided."));
        binding.listingCategory.setText(
                MarketplaceListingValidator.displayLabel(listing.getTransactionIntent())
                        + "  |  "
                        + MarketplaceListingValidator.displayLabel(listing.getCategory()));
        binding.listingStatusBadge.setText(
                MarketplaceListingValidator.displayLabel(listing.getStatus()));
        binding.listingCondition.setText(
                "Condition: "
                        + MarketplaceListingValidator.displayLabel(listing.getCondition())
                        + "  |  "
                        + MarketplaceListingValidator.displayLabel(listing.getFulfilmentMethod()));
        binding.listingTransactionTerms.setText(transactionTerms(listing));
        binding.sellerSummary.setText(state.isOwner()
                ? "You own this listing"
                : "Marketplace seller");
        boolean canRate = !state.isOwner()
                && MarketplaceListingStatusPolicy.AVAILABLE.equals(listing.getStatus());
        binding.rateSellerAction.setVisibility(canRate ? View.VISIBLE : View.GONE);
        binding.marketDetailCacheStatus.setVisibility(
                state.isFromCache() ? View.VISIBLE : View.GONE);

        boolean contactAllowed = MarketplaceListingStatusPolicy.canContactSeller(
                state.isOwner(), listing.getStatus());
        boolean ownerControls = MarketplaceListingStatusPolicy.canEdit(
                state.isOwner(), listing.getStatus());
        binding.chatAction.setVisibility(contactAllowed ? View.VISIBLE : View.GONE);
        binding.chatAction.setEnabled(contactAllowed);
        binding.ownerActionsCard.setVisibility(ownerControls ? View.VISIBLE : View.GONE);
        updateOwnerControls();
    }

    private void renderRating(@NonNull MarketDetailViewModel.RatingState state) {
        currentRatingState = state;
        if (binding == null) {
            return;
        }
        binding.sellerRatingBar.setRating((float) state.getAverage());
        String summary = state.summaryText();
        if (state.isFromCache() && state.getCount() > 0) {
            summary += " • offline copy";
        }
        binding.sellerRatingSummary.setText(summary);
        binding.sellerRatingRow.setContentDescription(
                "Marketplace seller rating: " + state.summaryText());
        binding.rateSellerAction.setText(
                state.getMyScore() > 0 ? "Update rating" : "Rate seller");
        binding.rateSellerAction.setEnabled(!state.isSaving());
        binding.rateSellerAction.setAlpha(state.isSaving() ? 0.55f : 1f);
        boolean showMessage = !state.getMessage().isEmpty();
        binding.sellerRatingStatus.setVisibility(showMessage ? View.VISIBLE : View.GONE);
        binding.sellerRatingStatus.setText(state.getMessage());
    }

    private void renderOwnerAction(
            @NonNull MarketDetailViewModel.OwnerActionState actionState) {
        ownerActionBusy = actionState.isBusy();
        boolean showMessage = !actionState.getMessage().isEmpty();
        binding.ownerActionProgress.setVisibility(
                ownerActionBusy ? View.VISIBLE : View.GONE);
        binding.ownerActionStatus.setVisibility(showMessage ? View.VISIBLE : View.GONE);
        binding.ownerActionStatus.setText(actionState.getMessage());
        binding.ownerActionStatus.setTextColor(ContextCompat.getColor(
                requireContext(),
                actionState.getKind() == MarketDetailViewModel.OwnerActionState.Kind.ERROR
                        ? R.color.pc_error
                        : R.color.pc_text_secondary));
        updateOwnerControls();
    }

    private void updateOwnerControls() {
        if (binding == null || currentListing == null || !currentOwner) {
            return;
        }
        boolean editable = MarketplaceListingStatusPolicy.canEdit(
                true, currentListing.getStatus());
        boolean canMarkSold = MarketplaceListingStatusPolicy.canMarkSold(
                true, currentListing.getStatus());
        binding.editListingAction.setEnabled(editable && !ownerActionBusy);
        binding.listingStatusAction.setEnabled(editable && !ownerActionBusy);
        binding.markSoldAction.setVisibility(canMarkSold ? View.VISIBLE : View.GONE);
        binding.markSoldAction.setEnabled(canMarkSold && !ownerActionBusy);
        binding.editListingAction.setAlpha(
                binding.editListingAction.isEnabled() ? 1f : 0.55f);
        binding.listingStatusAction.setAlpha(
                binding.listingStatusAction.isEnabled() ? 1f : 0.55f);
        binding.markSoldAction.setAlpha(
                binding.markSoldAction.isEnabled() ? 1f : 0.55f);
        binding.listingStatusAction.setText(
                MarketplaceListingStatusPolicy.ownerStatusActionLabel(
                        currentListing.getStatus()));
        binding.listingStatusAction.setContentDescription(
                MarketplaceListingStatusPolicy.WITHDRAWN.equals(currentListing.getStatus())
                        ? "Relist this marketplace item"
                        : "Withdraw this marketplace listing");
    }

    private void displayListingImage(
            @Nullable String gsUrl,
            @Nullable String demoImageKey) {
        if (gsUrl == null || gsUrl.trim().isEmpty()) {
            cancelImageLoad();
            displayedImageUrl = null;
            int demoDrawable = DemoImageCatalog.drawableFor(demoImageKey);
            if (demoDrawable != 0) {
                displayedDemoImageKey = demoImageKey;
                binding.listingImage.setScaleType(
                        android.widget.ImageView.ScaleType.FIT_CENTER);
                binding.listingImage.setImageResource(demoDrawable);
                binding.listingImage.setVisibility(View.VISIBLE);
                binding.listingImageProgress.setVisibility(View.GONE);
                binding.listingImagePlaceholder.setVisibility(View.GONE);
                return;
            }
            displayedDemoImageKey = null;
            binding.listingImage.setImageDrawable(null);
            binding.listingImage.setVisibility(View.GONE);
            binding.listingImageProgress.setVisibility(View.GONE);
            binding.listingImagePlaceholder.setText("NO ITEM PHOTO");
            binding.listingImagePlaceholder.setVisibility(View.VISIBLE);
            return;
        }
        if (gsUrl.equals(displayedImageUrl)
                && binding.listingImage.getDrawable() != null) {
            return;
        }
        cancelImageLoad();
        displayedImageUrl = gsUrl;
        displayedDemoImageKey = null;
        binding.listingImage.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        binding.listingImage.setImageDrawable(null);
        binding.listingImage.setVisibility(View.GONE);
        binding.listingImagePlaceholder.setText("Loading item photo...");
        binding.listingImagePlaceholder.setVisibility(View.VISIBLE);
        binding.listingImageProgress.setVisibility(View.VISIBLE);
        imageLoadHandle = imageLoader.load(gsUrl, new MarketplaceImageLoader.Callback() {
            @Override
            public void onLoaded(@NonNull android.graphics.Bitmap bitmap) {
                if (binding == null || !gsUrl.equals(displayedImageUrl)) {
                    return;
                }
                imageLoadHandle = null;
                binding.listingImageProgress.setVisibility(View.GONE);
                binding.listingImage.setImageBitmap(bitmap);
                binding.listingImage.setVisibility(View.VISIBLE);
                binding.listingImagePlaceholder.setVisibility(View.GONE);
            }

            @Override
            public void onError() {
                if (binding == null || !gsUrl.equals(displayedImageUrl)) {
                    return;
                }
                imageLoadHandle = null;
                binding.listingImageProgress.setVisibility(View.GONE);
                binding.listingImagePlaceholder.setText(
                        "PHOTO UNAVAILABLE\nCheck your connection or Storage setup");
                binding.listingImagePlaceholder.setVisibility(View.VISIBLE);
            }
        });
    }

    private void cancelImageLoad() {
        if (imageLoadHandle != null) {
            imageLoadHandle.cancel();
            imageLoadHandle = null;
        }
    }

    @NonNull
    private static String valueOrFallback(@Nullable String value, @NonNull String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    @NonNull
    private static String transactionTerms(@NonNull MarketplaceListing listing) {
        if ("sale".equals(listing.getTransactionIntent())) {
            long priceMinor = listing.getPriceMinor() == null ? 0L : listing.getPriceMinor();
            return String.format(Locale.getDefault(), "Price: RM %.2f", priceMinor / 100.0);
        }
        if ("exchange".equals(listing.getTransactionIntent())) {
            return "Exchange for: "
                    + valueOrFallback(listing.getExchangeTerms(), "terms not provided");
        }
        return "Free donation";
    }

    @Override
    public void onDestroyView() {
        cancelImageLoad();
        if (imageLoader != null) {
            imageLoader.close();
            imageLoader = null;
        }
        binding = null;
        super.onDestroyView();
    }
}
