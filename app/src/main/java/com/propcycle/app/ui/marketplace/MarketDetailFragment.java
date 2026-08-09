package com.propcycle.app.ui.marketplace;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.propcycle.app.R;
import com.propcycle.app.data.marketplace.MarketplaceListing;
import com.propcycle.app.data.marketplace.MarketplaceListingValidator;
import com.propcycle.app.databinding.FragmentMarketDetailBinding;
import com.propcycle.app.ui.common.ScreenNavigation;

import java.util.Locale;

/** Live marketplace detail screen with owner-aware chat affordance. */
public final class MarketDetailFragment extends Fragment {

    private static final String ARG_LISTING_ID = "listingId";

    private FragmentMarketDetailBinding binding;
    private MarketDetailViewModel viewModel;

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

        viewModel = new ViewModelProvider(this).get(MarketDetailViewModel.class);
        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getChatNotice().observe(getViewLifecycleOwner(), notice -> {
            if (notice == null || notice.trim().isEmpty()) {
                binding.marketDetailActionStatus.setVisibility(View.GONE);
            } else {
                binding.marketDetailActionStatus.setText(notice);
                binding.marketDetailActionStatus.setVisibility(View.VISIBLE);
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

        Bundle arguments = getArguments();
        viewModel.load(arguments == null ? "" : arguments.getString(ARG_LISTING_ID, ""));
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
            return;
        }

        binding.listingTitle.setText(valueOrFallback(listing.getTitle(), "Untitled item"));
        binding.listingDescription.setText(
                valueOrFallback(listing.getDescription(), "No description provided."));
        binding.listingCategory.setText(
                MarketplaceListingValidator.displayLabel(listing.getTransactionIntent())
                        + "  |  "
                        + MarketplaceListingValidator.displayLabel(listing.getCategory()));
        binding.listingCondition.setText(
                "Condition: "
                        + MarketplaceListingValidator.displayLabel(listing.getCondition())
                        + "  |  "
                        + MarketplaceListingValidator.displayLabel(listing.getFulfilmentMethod()));
        binding.listingTransactionTerms.setText(transactionTerms(listing));
        binding.sellerSummary.setText(
                MarketplaceListingValidator.displayLabel(listing.getStatus())
                        + "  |  Marketplace seller");
        binding.marketDetailCacheStatus.setVisibility(
                state.isFromCache() ? View.VISIBLE : View.GONE);

        if (state.isOwner()) {
            binding.chatAction.setText("Your listing");
            binding.chatAction.setEnabled(false);
            binding.chatAction.setAlpha(0.55f);
            binding.chatAction.setContentDescription("This is your marketplace listing");
        } else {
            binding.chatAction.setText("Chat with seller");
            binding.chatAction.setEnabled(true);
            binding.chatAction.setAlpha(1f);
            binding.chatAction.setContentDescription("Start a chat with the seller");
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
        binding = null;
        super.onDestroyView();
    }
}
