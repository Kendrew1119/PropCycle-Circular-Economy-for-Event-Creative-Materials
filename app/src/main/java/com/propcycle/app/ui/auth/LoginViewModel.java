package com.propcycle.app.ui.auth;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.propcycle.app.data.auth.AuthInputValidator;
import com.propcycle.app.data.auth.AuthRepository;

/** Coordinates validation, duplicate-submit protection, and login state. */
public final class LoginViewModel extends ViewModel {

    private final AuthRepository repository;
    private final MutableLiveData<AuthUiState> state = new MutableLiveData<>();
    private boolean requestInFlight;

    public LoginViewModel(@NonNull AuthRepository repository) {
        this.repository = repository;
        state.setValue(repository.isConfigured()
                ? AuthUiState.idle()
                : AuthUiState.configurationRequired(
                        com.propcycle.app.core.firebase.FirebaseEnvironment.SETUP_MESSAGE));
    }

    @NonNull
    public LiveData<AuthUiState> getState() {
        return state;
    }

    public void submit(@NonNull String email, @NonNull String password) {
        AuthInputValidator.ValidationResult validation =
                AuthInputValidator.validateLogin(email, password);
        if (!validation.isValid()) {
            state.setValue(AuthUiState.error(validation.getMessage()));
            return;
        }
        if (!beginRequest()) {
            return;
        }

        state.setValue(AuthUiState.loading("Signing in…"));
        repository.signIn(email.trim(), password, new AuthRepository.Completion() {
            @Override
            public void onSuccess() {
                finishRequest();
                state.postValue(AuthUiState.success());
            }

            @Override
            public void onFailure(@NonNull AuthRepository.Failure failure) {
                finishRequest();
                if (failure.getKind() == AuthRepository.FailureKind.CONFIGURATION_REQUIRED) {
                    state.postValue(AuthUiState.configurationRequired(failure.getMessage()));
                } else {
                    state.postValue(AuthUiState.error(failure.getMessage()));
                }
            }
        });
    }

    public void clearError() {
        AuthUiState current = state.getValue();
        if (!isRequestInFlight()
                && current != null
                && current.getStatus() == AuthUiState.Status.ERROR) {
            state.setValue(AuthUiState.idle());
        }
    }

    private synchronized boolean beginRequest() {
        if (requestInFlight) {
            return false;
        }
        requestInFlight = true;
        return true;
    }

    private synchronized void finishRequest() {
        requestInFlight = false;
    }

    private synchronized boolean isRequestInFlight() {
        return requestInFlight;
    }

    public static final class Factory implements ViewModelProvider.Factory {

        private final AuthRepository repository;

        public Factory(@NonNull AuthRepository repository) {
            this.repository = repository;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(LoginViewModel.class)) {
                return (T) new LoginViewModel(repository);
            }
            throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
        }
    }
}
