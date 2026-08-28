package com.propcycle.app.ui.lending;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.propcycle.app.R;
import com.propcycle.app.data.lending.LendingItem;
import com.propcycle.app.data.marketplace.MarketplaceImageLoader;
import com.propcycle.app.databinding.FragmentLendingListBinding;
import com.propcycle.app.ui.common.ScreenNavigation;
import com.propcycle.app.ui.common.ResourceCreationFlow;

/** Functional real-time lending browse list. */
public final class LendingListFragment extends Fragment {

    private FragmentLendingListBinding binding;
    private LendingListViewModel viewModel;
    private MarketplaceImageLoader imageLoader;
    private LendingItemAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentLendingListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ScreenNavigation.bindChrome(this, view);
        imageLoader = new MarketplaceImageLoader(requireContext());
        adapter = new LendingItemAdapter(imageLoader, this::openItem);
        binding.lendingItemList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.lendingItemList.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(LendingListViewModel.class);
        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
        Bundle arguments = getArguments();
        String initialQuery = arguments == null
                ? "" : arguments.getString("initialQuery", "");
        String initialCategory = arguments == null
                ? "all" : arguments.getString("initialCategory", "all");
        if (savedInstanceState == null) {
            binding.lendingSearchInput.setText(initialQuery);
            viewModel.setQuery(initialQuery);
            applyInitialCategory(initialCategory);
        }
        binding.lendingSearchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void afterTextChanged(Editable s) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setQuery(s == null ? "" : s.toString());
            }
        });
        binding.lendingFilterGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int selected = checkedIds.isEmpty() ? R.id.lending_filter_all : checkedIds.get(0);
            String category = selected == R.id.lending_filter_equipment
                    ? "equipment"
                    : selected == R.id.lending_filter_tools
                            ? "tools"
                            : selected == R.id.lending_filter_electronics
                                    ? "electronics"
                                    : selected == R.id.lending_filter_event_gear
                                            ? "event_gear"
                                            : selected == R.id.lending_filter_craft
                                                    ? "craft"
                                                    : selected == R.id.lending_filter_other
                                                            ? "other" : "all";
            viewModel.setCategory(category);
        });
        binding.createLendingAction.setOnClickListener(ignored ->
                ResourceCreationFlow.show(this, ResourceCreationFlow.TARGET_LENDING));
        binding.openLendingMapAction.setOnClickListener(ignored -> {
            Bundle mapArguments = new Bundle();
            mapArguments.putString("initialQuery", viewModel.getQuery());
            mapArguments.putString("initialCategory", viewModel.getCategory());
            ScreenNavigation.navigateAuthenticated(this, R.id.lendingMapFragment, mapArguments);
        });
    }

    private void applyInitialCategory(@Nullable String category) {
        int checked = "equipment".equals(category)
                ? R.id.lending_filter_equipment
                : "tools".equals(category)
                        ? R.id.lending_filter_tools
                        : "electronics".equals(category)
                                ? R.id.lending_filter_electronics
                                : "event_gear".equals(category)
                                        ? R.id.lending_filter_event_gear
                                        : "craft".equals(category)
                                                ? R.id.lending_filter_craft
                                                : "other".equals(category)
                                                        ? R.id.lending_filter_other
                                                        : R.id.lending_filter_all;
        binding.lendingFilterGroup.check(checked);
        viewModel.setCategory(category == null ? "all" : category);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (ScreenNavigation.navigateAuthenticated(this, R.id.lendingListFragment, null)) {
            viewModel.start();
        }
    }

    @Override
    public void onStop() {
        viewModel.stop();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        binding.lendingItemList.setAdapter(null);
        if (imageLoader != null) {
            imageLoader.close();
        }
        binding = null;
        super.onDestroyView();
    }

    private void openItem(@NonNull LendingItem item) {
        Bundle arguments = new Bundle();
        arguments.putString("itemId", item.getId());
        ScreenNavigation.navigateAuthenticated(this, R.id.lendingDetailFragment, arguments);
    }

    private void render(@NonNull LendingListViewModel.State state) {
        if (binding == null) {
            return;
        }
        adapter.submitList(state.getItems(), null, null);
        binding.lendingProgress.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
        binding.lendingEmptyState.setVisibility(
                !state.isLoading() && state.getItems().isEmpty()
                        ? View.VISIBLE : View.GONE);
        binding.lendingStatus.setText(state.getMessage() == null
                ? state.getItems().size() + " available item(s)"
                : state.getMessage());
    }
}
