package com.propcycle.app.ui.marketplace;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.propcycle.app.R;
import com.propcycle.app.data.marketplace.MarketplaceListing;
import com.propcycle.app.data.marketplace.MarketplaceImageLoader;
import com.propcycle.app.databinding.FragmentMarketplaceBinding;
import com.propcycle.app.ui.common.ScreenNavigation;

/** Live, authenticated marketplace browser using the proposal's masonry visual language. */
public final class MarketplaceFragment extends Fragment {

    private FragmentMarketplaceBinding binding;
    private MarketplaceAdapter adapter;
    private MarketplaceImageLoader imageLoader;
    private MarketplaceViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentMarketplaceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ScreenNavigation.bindChrome(this, view);
        viewModel = new ViewModelProvider(this).get(MarketplaceViewModel.class);

        imageLoader = new MarketplaceImageLoader(requireContext());
        adapter = new MarketplaceAdapter(imageLoader, this::openListing);
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(
                2,
                StaggeredGridLayoutManager.VERTICAL);
        layoutManager.setGapStrategy(
                StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        binding.marketplaceList.setLayoutManager(layoutManager);
        binding.marketplaceList.setAdapter(adapter);

        binding.marketSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence value, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count) {
                viewModel.setSearchQuery(value == null ? "" : value.toString());
            }

            @Override
            public void afterTextChanged(Editable value) {
            }
        });

        bindFilter(binding.filterAllAction, "all");
        bindFilter(binding.filterDecorationAction, "decoration");
        bindFilter(binding.filterFabricAction, "fabric");
        bindFilter(binding.filterWoodAction, "wood");

        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
        updateFilterAppearance(viewModel.getSelectedCategory());
    }

    private void bindFilter(@NonNull TextView view, @NonNull String category) {
        view.setOnClickListener(ignored -> {
            viewModel.setCategory(category);
            updateFilterAppearance(category);
        });
    }

    private void updateFilterAppearance(@NonNull String selectedCategory) {
        updateFilter(binding.filterAllAction, "all".equals(selectedCategory));
        updateFilter(binding.filterDecorationAction, "decoration".equals(selectedCategory));
        updateFilter(binding.filterFabricAction, "fabric".equals(selectedCategory));
        updateFilter(binding.filterWoodAction, "wood".equals(selectedCategory));
    }

    private void updateFilter(@NonNull TextView view, boolean selected) {
        view.setBackgroundResource(selected ? R.drawable.bg_pill_dark : R.drawable.bg_pill);
        view.setTextColor(ContextCompat.getColor(
                requireContext(),
                selected ? R.color.pc_white : R.color.pc_ink));
        view.setSelected(selected);
    }

    private void render(@NonNull MarketplaceViewModel.State state) {
        boolean loading = state.getKind() == MarketplaceViewModel.State.Kind.LOADING;
        boolean content = state.getKind() == MarketplaceViewModel.State.Kind.CONTENT;

        binding.marketplaceProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.marketplaceList.setVisibility(content ? View.VISIBLE : View.GONE);
        binding.marketplaceStatus.setVisibility(
                loading || content ? View.GONE : View.VISIBLE);
        binding.marketplaceCacheStatus.setVisibility(
                state.isFromCache() ? View.VISIBLE : View.GONE);

        if (content) {
            adapter.submitList(state.getListings());
        } else {
            adapter.submitList(java.util.Collections.emptyList());
            if (!loading) {
                binding.marketplaceStatus.setText(state.getMessage());
            }
        }
    }

    private void openListing(@NonNull MarketplaceListing listing) {
        if (listing.getId() == null || listing.getId().trim().isEmpty()) {
            return;
        }
        Bundle arguments = new Bundle();
        arguments.putString("listingId", listing.getId());
        ScreenNavigation.navigateAuthenticated(
                this,
                R.id.marketDetailFragment,
                arguments);
    }

    @Override
    public void onDestroyView() {
        binding.marketplaceList.setAdapter(null);
        adapter = null;
        if (imageLoader != null) {
            imageLoader.close();
            imageLoader = null;
        }
        binding = null;
        super.onDestroyView();
    }
}
