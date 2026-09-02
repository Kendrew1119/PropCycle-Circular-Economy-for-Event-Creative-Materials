package com.propcycle.app.ui.home;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.propcycle.app.R;
import com.propcycle.app.core.firebase.FirebaseEnvironment;
import com.propcycle.app.data.activity.ActivityLogRepository;
import com.propcycle.app.data.activity.ActivityRecord;
import com.propcycle.app.data.lending.LendingItem;
import com.propcycle.app.data.lending.LendingPolicy;
import com.propcycle.app.data.marketplace.MarketplaceListing;
import com.propcycle.app.data.marketplace.MarketplaceListingValidator;
import com.propcycle.app.databinding.FragmentHomeBinding;
import com.propcycle.app.ui.common.ResourceCreationFlow;
import com.propcycle.app.ui.common.ScreenNavigation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Functional home routing, unified search, and truthful device-local impact summary. */
public final class HomeFragment extends Fragment {

    private static final List<ActionSpec> SEARCH_ACTIONS = Collections.unmodifiableList(
            Arrays.asList(
                    new ActionSpec(
                            "Recycle Center",
                            R.id.recycleCenterFragment,
                            "recycle", "recycling", "recycle centre", "center", "centre",
                            "disposal"),
                    new ActionSpec(
                            "AI Smart Scanner",
                            R.id.scannerFragment,
                            "scan", "scanner", "ai", "ai scan", "identify", "item scan"),
                    new ActionSpec(
                            "Lend Resource",
                            R.id.lendResourceFragment,
                            "lend", "share", "borrow", "lend item", "resource"),
                    new ActionSpec(
                            "Create Listing",
                            R.id.createListingFragment,
                            "sell", "list", "create", "marketplace listing"),
                    new ActionSpec(
                            "Marketplace",
                            R.id.marketplaceFragment,
                            "buy", "shop", "browse", "purchase"),
                    new ActionSpec(
                            "Messages",
                            R.id.messagesFragment,
                            "message", "chat", "chats", "conversation"),
                    new ActionSpec(
                            "Recent Activities",
                            R.id.recentActivitiesFragment,
                            "recent", "activity", "activities", "history"),
                    new ActionSpec(
                            "Lending Map",
                            R.id.lendingMapFragment,
                            "map", "nearby lending", "nearby items")));

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;

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
        binding.homeRecycleAction.setOnClickListener(ignored ->
                ScreenNavigation.navigateAuthenticated(this, R.id.recycleCenterFragment, null));
        binding.homeCreateListingAction.setOnClickListener(ignored ->
                ResourceCreationFlow.show(this, ResourceCreationFlow.TARGET_MARKETPLACE));
        binding.homeLendResourceAction.setOnClickListener(ignored ->
                ResourceCreationFlow.show(this, ResourceCreationFlow.TARGET_LENDING));
        binding.recentAction.setOnClickListener(ignored ->
                ScreenNavigation.navigateAuthenticated(
                        this, R.id.recentActivitiesFragment, null));

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        viewModel.getActivities().observe(getViewLifecycleOwner(), this::renderImpact);
        viewModel.getSearchState().observe(getViewLifecycleOwner(), this::renderSearch);
        binding.homeSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence value, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence value, int start, int before, int count) {
                viewModel.setSearchQuery(value == null ? "" : value.toString());
            }

            @Override
            public void afterTextChanged(Editable value) {
            }
        });
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

    private void renderSearch(@NonNull HomeViewModel.SearchState state) {
        if (binding == null) {
            return;
        }
        if (!state.isActive()) {
            binding.homeSearchResults.setVisibility(View.GONE);
            binding.homeSearchActionsList.removeAllViews();
            binding.homeSearchResourcesList.removeAllViews();
            return;
        }

        binding.homeSearchResults.setVisibility(View.VISIBLE);
        List<ActionSpec> actions = matchingActions(state.getQuery());
        renderActions(actions);
        renderResources(state.getMarketplaceResults(), state.getLendingResults());

        boolean hasActions = !actions.isEmpty();
        boolean hasResources = !state.getMarketplaceResults().isEmpty()
                || !state.getLendingResults().isEmpty();
        String status = "";
        if (state.isLoading()) {
            status = getString(R.string.home_search_loading);
        } else if (state.areResourcesUnavailable()) {
            status = getString(R.string.home_search_resources_unavailable);
        } else if (state.hasPartialFailure()) {
            status = getString(R.string.home_search_partial_resources);
        } else if (!hasActions && !hasResources) {
            status = getString(R.string.home_search_no_matches);
        }
        binding.homeSearchStatus.setText(status);
        binding.homeSearchStatus.setVisibility(status.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void renderActions(@NonNull List<ActionSpec> actions) {
        binding.homeSearchActionsList.removeAllViews();
        binding.homeSearchActionsSection.setVisibility(
                actions.isEmpty() ? View.GONE : View.VISIBLE);
        for (ActionSpec action : actions) {
            View row = inflateSearchRow(
                    action.title,
                    getString(R.string.home_search_action_source),
                    () -> ScreenNavigation.navigateAuthenticated(
                            this, action.destinationId, null));
            binding.homeSearchActionsList.addView(row);
        }
    }

    private void renderResources(
            @NonNull List<MarketplaceListing> marketplaceResults,
            @NonNull List<LendingItem> lendingResults) {
        binding.homeSearchResourcesList.removeAllViews();
        boolean hasResources = !marketplaceResults.isEmpty() || !lendingResults.isEmpty();
        binding.homeSearchResourcesSection.setVisibility(
                hasResources ? View.VISIBLE : View.GONE);

        for (MarketplaceListing listing : marketplaceResults) {
            String category = MarketplaceListingValidator.displayLabel(listing.getCategory());
            String meta = getString(R.string.home_search_marketplace_source)
                    + (category.isEmpty() ? "" : " · " + category);
            View row = inflateSearchRow(
                    safeTitle(listing.getTitle()),
                    meta,
                    () -> openMarketplaceListing(listing));
            binding.homeSearchResourcesList.addView(row);
        }
        for (LendingItem item : lendingResults) {
            String category = LendingPolicy.displayLabel(item.getCategory());
            String area = item.getAreaLabel() == null ? "" : item.getAreaLabel().trim();
            StringBuilder meta = new StringBuilder(
                    getString(R.string.home_search_lending_source));
            if (!category.isEmpty()) {
                meta.append(" · ").append(category);
            }
            if (!area.isEmpty()) {
                meta.append(" · ").append(area);
            }
            View row = inflateSearchRow(
                    safeTitle(item.getTitle()),
                    meta.toString(),
                    () -> openLendingItem(item));
            binding.homeSearchResourcesList.addView(row);
        }
    }

    @NonNull
    private View inflateSearchRow(
            @NonNull String title,
            @NonNull String meta,
            @NonNull Runnable action) {
        View row = getLayoutInflater().inflate(
                R.layout.item_home_search_result,
                binding.homeSearchResults,
                false);
        TextView titleView = row.findViewById(R.id.home_search_result_title);
        TextView metaView = row.findViewById(R.id.home_search_result_meta);
        titleView.setText(title);
        metaView.setText(meta);
        row.setContentDescription(title + ", " + meta);
        row.setOnClickListener(ignored -> action.run());
        return row;
    }

    private void openMarketplaceListing(@NonNull MarketplaceListing listing) {
        String listingId = listing.getId();
        if (listingId == null || listingId.trim().isEmpty()) {
            return;
        }
        Bundle arguments = new Bundle();
        arguments.putString("listingId", listingId);
        ScreenNavigation.navigateAuthenticated(
                this, R.id.marketDetailFragment, arguments);
    }

    private void openLendingItem(@NonNull LendingItem item) {
        String itemId = item.getId();
        if (itemId == null || itemId.trim().isEmpty()) {
            return;
        }
        Bundle arguments = new Bundle();
        arguments.putString("itemId", itemId);
        ScreenNavigation.navigateAuthenticated(
                this, R.id.lendingDetailFragment, arguments);
    }

    @NonNull
    private static List<ActionSpec> matchingActions(@NonNull String query) {
        String normalizedQuery = MarketplaceListingValidator.normalizeSearchText(query);
        List<ActionMatch> matches = new ArrayList<>();
        for (ActionSpec action : SEARCH_ACTIONS) {
            int score = action.matchScore(normalizedQuery);
            if (score >= 0) {
                matches.add(new ActionMatch(action, score));
            }
        }
        matches.sort((left, right) -> {
            int scoreComparison = Integer.compare(left.score, right.score);
            return scoreComparison != 0
                    ? scoreComparison
                    : left.action.title.compareToIgnoreCase(right.action.title);
        });
        List<ActionSpec> actions = new ArrayList<>(matches.size());
        for (ActionMatch match : matches) {
            actions.add(match.action);
        }
        return actions;
    }

    @NonNull
    private static String safeTitle(@Nullable String value) {
        return value == null || value.trim().isEmpty() ? "Untitled resource" : value.trim();
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

    private static final class ActionSpec {
        private final String title;
        private final int destinationId;
        private final List<String> aliases;

        private ActionSpec(String title, int destinationId, String... aliases) {
            this.title = title;
            this.destinationId = destinationId;
            this.aliases = Arrays.asList(aliases);
        }

        private int matchScore(@NonNull String query) {
            String normalizedTitle =
                    MarketplaceListingValidator.normalizeSearchText(title);
            if (normalizedTitle.equals(query)) {
                return 0;
            }
            for (String alias : aliases) {
                if (MarketplaceListingValidator.normalizeSearchText(alias).equals(query)) {
                    return 1;
                }
            }
            if (normalizedTitle.startsWith(query)) {
                return 2;
            }
            for (String alias : aliases) {
                if (MarketplaceListingValidator.normalizeSearchText(alias).startsWith(query)) {
                    return 3;
                }
            }
            if (normalizedTitle.contains(query)) {
                return 4;
            }
            for (String alias : aliases) {
                if (MarketplaceListingValidator.normalizeSearchText(alias).contains(query)) {
                    return 5;
                }
            }
            return -1;
        }
    }

    private static final class ActionMatch {
        private final ActionSpec action;
        private final int score;

        private ActionMatch(ActionSpec action, int score) {
            this.action = action;
            this.score = score;
        }
    }
}
