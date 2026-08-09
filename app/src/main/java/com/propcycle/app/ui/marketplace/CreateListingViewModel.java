package com.propcycle.app.ui.marketplace;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.propcycle.app.data.marketplace.FirestoreMarketplaceRepository;
import com.propcycle.app.data.marketplace.MarketplaceListingValidator;
import com.propcycle.app.data.marketplace.MarketplaceRepository;
import com.propcycle.app.data.marketplace.NewMarketplaceListing;

/** Validates and publishes a text-only marketplace listing. */
public final class CreateListingViewModel extends AndroidViewModel {

    private final MarketplaceRepository repository;
    private final MutableLiveData<State> state = new MutableLiveData<>(State.idle());
    private final MutableLiveData<Event<String>> createdListing = new MutableLiveData<>();

    public CreateListingViewModel(@NonNull Application application) {
        super(application);
        repository = new FirestoreMarketplaceRepository(application);
    }

    @NonNull
    public LiveData<State> getState() {
        return state;
    }

    @NonNull
    public LiveData<Event<String>> getCreatedListing() {
        return createdListing;
    }

    public void publish(
            @Nullable String title,
            @Nullable String category,
            @Nullable String condition,
            @Nullable String transactionIntent,
            @Nullable String fulfilmentMethod,
            @Nullable String price,
            @Nullable String exchangeTerms,
            @Nullable String description) {
        State current = state.getValue();
        if (current != null && current.getKind() == State.Kind.LOADING) {
            return;
        }

        MarketplaceListingValidator.ValidationResult validation =
                MarketplaceListingValidator.validate(
                        title,
                        category,
                        condition,
                        transactionIntent,
                        fulfilmentMethod,
                        price,
                        exchangeTerms,
                        description);
        if (!validation.isValid()) {
            state.setValue(State.error(validation.getErrorMessage()));
            return;
        }

        NewMarketplaceListing listing = validation.getListing();
        if (listing == null) {
            state.setValue(State.error("Review the listing details and try again."));
            return;
        }

        state.setValue(State.loading());
        repository.createListing(listing, new MarketplaceRepository.CreateCallback() {
            @Override
            public void onCreated(@NonNull String listingId) {
                state.setValue(State.success("Listing published."));
                createdListing.setValue(new Event<>(listingId));
            }

            @Override
            public void onError(@NonNull MarketplaceRepository.RepositoryError error) {
                State.Kind kind = switch (error.getType()) {
                    case CONFIGURATION_REQUIRED -> State.Kind.CONFIGURATION_REQUIRED;
                    case AUTHENTICATION_REQUIRED -> State.Kind.AUTHENTICATION_REQUIRED;
                    default -> State.Kind.ERROR;
                };
                state.setValue(new State(kind, error.getMessage()));
            }
        });
    }

    public static final class State {

        public enum Kind {
            IDLE,
            LOADING,
            SUCCESS,
            ERROR,
            CONFIGURATION_REQUIRED,
            AUTHENTICATION_REQUIRED
        }

        private final Kind kind;
        private final String message;

        private State(@NonNull Kind kind, @NonNull String message) {
            this.kind = kind;
            this.message = message;
        }

        private static State idle() {
            return new State(Kind.IDLE, "");
        }

        private static State loading() {
            return new State(Kind.LOADING, "Publishing listing...");
        }

        private static State success(@NonNull String message) {
            return new State(Kind.SUCCESS, message);
        }

        private static State error(@Nullable String message) {
            return new State(
                    Kind.ERROR,
                    message == null ? "Review the listing details and try again." : message);
        }

        @NonNull
        public Kind getKind() {
            return kind;
        }

        @NonNull
        public String getMessage() {
            return message;
        }
    }

    /** One-shot navigation result that survives configuration changes safely. */
    public static final class Event<T> {
        private final T value;
        private boolean handled;

        private Event(@NonNull T value) {
            this.value = value;
        }

        @Nullable
        public T getIfNotHandled() {
            if (handled) {
                return null;
            }
            handled = true;
            return value;
        }
    }
}
