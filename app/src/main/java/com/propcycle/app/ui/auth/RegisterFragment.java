package com.propcycle.app.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.propcycle.app.R;
import com.propcycle.app.data.auth.AuthRepository;
import com.propcycle.app.databinding.FragmentRegisterBinding;
import com.propcycle.app.ui.common.ScreenNavigation;

/** Email/password registration screen backed by Firebase Auth and Firestore. */
public final class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private RegisterViewModel viewModel;
    private boolean successHandled;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ScreenNavigation.bindChrome(this, view);

        AuthRepository repository = new AuthRepository(requireContext());
        viewModel = new ViewModelProvider(this, new RegisterViewModel.Factory(repository))
                .get(RegisterViewModel.class);

        binding.primaryAction.setOnClickListener(ignored -> submit());
        binding.secondaryAction.setOnClickListener(ignored -> navigateToLogin());
        binding.passwordInput.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit();
                return true;
            }
            return false;
        });

        TextWatcher clearErrorWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                viewModel.clearError();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        };
        binding.displayNameInput.addTextChangedListener(clearErrorWatcher);
        binding.emailInput.addTextChangedListener(clearErrorWatcher);
        binding.passwordInput.addTextChangedListener(clearErrorWatcher);

        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
    }

    private void submit() {
        viewModel.submit(
                binding.displayNameInput.getText().toString(),
                binding.emailInput.getText().toString(),
                binding.passwordInput.getText().toString());
    }

    private void navigateToLogin() {
        NavController controller = NavHostFragment.findNavController(this);
        if (!controller.popBackStack(R.id.loginFragment, false)) {
            ScreenNavigation.navigateClearingBackStack(this, R.id.loginFragment);
        }
    }

    private void render(@NonNull AuthUiState state) {
        if (state.getStatus() == AuthUiState.Status.SUCCESS) {
            if (!successHandled) {
                successHandled = true;
                ScreenNavigation.navigateClearingBackStack(this, R.id.homeFragment);
            }
            return;
        }

        boolean loading = state.getStatus() == AuthUiState.Status.LOADING;
        boolean configurationRequired =
                state.getStatus() == AuthUiState.Status.CONFIGURATION_REQUIRED;
        binding.authProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.primaryAction.setText(loading ? "" : "Register");
        binding.primaryAction.setEnabled(!loading && !configurationRequired);
        binding.displayNameInput.setEnabled(!loading && !configurationRequired);
        binding.emailInput.setEnabled(!loading && !configurationRequired);
        binding.passwordInput.setEnabled(!loading && !configurationRequired);
        binding.secondaryAction.setEnabled(!loading);
        binding.closeButton.setEnabled(!loading);

        String message = state.getMessage();
        if (message == null || message.isEmpty()) {
            binding.authStatus.setText("");
            binding.authStatus.setVisibility(View.GONE);
        } else {
            binding.authStatus.setText(message);
            binding.authStatus.setTextColor(ContextCompat.getColor(
                    requireContext(),
                    state.getStatus() == AuthUiState.Status.LOADING
                            ? R.color.pc_text_secondary
                            : R.color.pc_error));
            binding.authStatus.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
