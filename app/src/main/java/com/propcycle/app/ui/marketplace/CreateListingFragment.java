package com.propcycle.app.ui.marketplace;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.propcycle.app.R;
import com.propcycle.app.databinding.FragmentCreateListingBinding;
import com.propcycle.app.ui.common.ScreenNavigation;

/** Proposal-parity create form that publishes text metadata to Firestore. */
public final class CreateListingFragment extends Fragment {

    private FragmentCreateListingBinding binding;
    private CreateListingViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateListingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ScreenNavigation.bindChrome(this, view);
        configureDropdowns();

        viewModel = new ViewModelProvider(this).get(CreateListingViewModel.class);
        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getCreatedListing().observe(getViewLifecycleOwner(), event -> {
            String listingId = event == null ? null : event.getIfNotHandled();
            if (listingId == null) {
                return;
            }
            Bundle arguments = new Bundle();
            arguments.putString("listingId", listingId);
            ScreenNavigation.navigateAuthenticated(
                    this,
                    R.id.marketDetailFragment,
                    arguments);
        });

        binding.primaryAction.setOnClickListener(ignored -> viewModel.publish(
                text(binding.listingTitleInput),
                text(binding.listingCategoryInput),
                text(binding.listingConditionInput),
                text(binding.listingTransactionInput),
                text(binding.listingFulfilmentInput),
                text(binding.listingPriceInput),
                text(binding.listingExchangeTermsInput),
                text(binding.listingDescriptionInput)));
    }

    private void configureDropdowns() {
        setDropdown(binding.listingCategoryInput,
                new String[]{
                        "Banner",
                        "Decoration",
                        "Fabric",
                        "Stationery",
                        "Craft",
                        "Cosplay",
                        "Toys",
                        "Wood",
                        "Electronic",
                        "Packaging",
                        "Other"
                });
        setDropdown(binding.listingConditionInput,
                new String[]{"New", "Like new", "Good", "Fair", "Poor"});
        setDropdown(binding.listingTransactionInput,
                new String[]{"Sale", "Donation", "Exchange"});
        setDropdown(binding.listingFulfilmentInput,
                new String[]{"Pickup", "Meet-up"});

        binding.listingTransactionInput.setOnItemClickListener(
                (parent, selected, position, id) -> updateTransactionFields());
        binding.listingTransactionInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence value, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count) {
                updateTransactionFields();
            }

            @Override
            public void afterTextChanged(Editable value) {
            }
        });
        binding.listingTransactionInput.setText("Donation", false);
        binding.listingFulfilmentInput.setText("Pickup", false);
        updateTransactionFields();
    }

    private void setDropdown(
            @NonNull android.widget.AutoCompleteTextView input,
            @NonNull String[] options) {
        input.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                options));
        input.setOnClickListener(ignored -> input.showDropDown());
    }

    private void updateTransactionFields() {
        String transaction = com.propcycle.app.data.marketplace.MarketplaceListingValidator
                .stableTransactionIntentId(text(binding.listingTransactionInput));
        boolean sale = "sale".equals(transaction);
        boolean exchange = "exchange".equals(transaction);

        binding.listingPriceInput.setVisibility(sale ? View.VISIBLE : View.GONE);
        binding.listingExchangeTermsInput.setVisibility(exchange ? View.VISIBLE : View.GONE);
        if (!sale) {
            binding.listingPriceInput.setText("");
        }
        if (!exchange) {
            binding.listingExchangeTermsInput.setText("");
        }
    }

    private void render(@NonNull CreateListingViewModel.State state) {
        boolean loading = state.getKind() == CreateListingViewModel.State.Kind.LOADING;
        boolean showMessage = state.getKind() != CreateListingViewModel.State.Kind.IDLE;
        boolean error = state.getKind() == CreateListingViewModel.State.Kind.ERROR
                || state.getKind() == CreateListingViewModel.State.Kind.CONFIGURATION_REQUIRED
                || state.getKind() == CreateListingViewModel.State.Kind.AUTHENTICATION_REQUIRED;

        binding.createListingProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.createListingStatus.setVisibility(showMessage ? View.VISIBLE : View.GONE);
        binding.createListingStatus.setText(state.getMessage());
        binding.createListingStatus.setTextColor(ContextCompat.getColor(
                requireContext(),
                error ? R.color.pc_error : R.color.pc_text_secondary));
        binding.primaryAction.setEnabled(!loading);
        binding.primaryAction.setAlpha(loading ? 0.55f : 1f);
    }

    @NonNull
    private static String text(@NonNull android.widget.TextView input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
