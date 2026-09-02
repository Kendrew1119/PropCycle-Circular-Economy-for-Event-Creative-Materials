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
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.propcycle.app.R;
import com.propcycle.app.data.auth.AuthRepository;
import com.propcycle.app.databinding.FragmentLoginBinding;
import com.propcycle.app.ui.common.ScreenNavigation;

/** Email/password sign-in screen backed by Firebase Authentication. */
public final class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private LoginViewModel viewModel;
    private boolean successHandled;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ScreenNavigation.bindChrome(this, view);

        AuthRepository repository = new AuthRepository(requireContext());
        viewModel = new ViewModelProvider(this, new LoginViewModel.Factory(repository))
                .get(LoginViewModel.class);

        binding.closeButton.setOnClickListener(ignored -> {
            NavController controller = NavHostFragment.findNavController(this);
            if (!controller.popBackStack()) {
                NavOptions options = new NavOptions.Builder()
                        .setEnterAnim(R.anim.login_transition_hold)
                        .setExitAnim(R.anim.login_slide_out_down)
                        .setPopUpTo(R.id.nav_graph, true)
                        .build();
                controller.navigate(R.id.welcomeFragment, null, options);
            }
        });
        binding.primaryAction.setOnClickListener(ignored -> submit());
        binding.secondaryAction.setOnClickListener(ignored ->
                ScreenNavigation.navigate(this, R.id.registerFragment, null));
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
        binding.emailInput.addTextChangedListener(clearErrorWatcher);
        binding.passwordInput.addTextChangedListener(clearErrorWatcher);

        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
    }

    private void submit() {
        viewModel.submit(
                binding.emailInput.getText().toString(),
                binding.passwordInput.getText().toString());
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
        binding.primaryAction.setText(loading ? "" : "Sign In");
        binding.primaryAction.setEnabled(!loading && !configurationRequired);
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
