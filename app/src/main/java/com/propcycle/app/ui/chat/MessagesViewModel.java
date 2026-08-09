package com.propcycle.app.ui.chat;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.propcycle.app.core.firebase.FirebaseEnvironment;
import com.propcycle.app.data.chat.ChatRepository;
import com.propcycle.app.data.chat.ChatThread;

import java.util.Collections;
import java.util.List;

/** Owns the bounded real-time conversation-list listener. */
public final class MessagesViewModel extends AndroidViewModel {

    private final ChatRepository repository;
    private final MutableLiveData<MessagesUiState> state =
            new MutableLiveData<>(MessagesUiState.loading());
    private ChatRepository.Subscription subscription = ChatRepository.Subscription.NONE;
    private int listenerGeneration;

    public MessagesViewModel(@NonNull Application application) {
        super(application);
        repository = new ChatRepository(application);
    }

    @NonNull
    public LiveData<MessagesUiState> getState() {
        return state;
    }

    public void start() {
        stop();
        if (!repository.isConfigured()) {
            state.setValue(new MessagesUiState(
                    false,
                    true,
                    false,
                    FirebaseEnvironment.SETUP_MESSAGE,
                    Collections.emptyList()));
            return;
        }
        if (repository.currentUserId() == null) {
            state.setValue(new MessagesUiState(
                    false,
                    false,
                    false,
                    "Sign in to view your conversations.",
                    Collections.emptyList()));
            return;
        }

        MessagesUiState current = state.getValue();
        List<ChatThread> previous = current == null
                ? Collections.emptyList()
                : current.getThreads();
        state.setValue(new MessagesUiState(
                previous.isEmpty(), false, false, null, previous));
        int generation = ++listenerGeneration;
        subscription = repository.listenToThreads(new ChatRepository.SnapshotCallback<>() {
            @Override
            public void onData(@NonNull List<ChatThread> value, boolean fromCache) {
                if (generation != listenerGeneration) {
                    return;
                }
                state.setValue(new MessagesUiState(false, false, fromCache, null, value));
            }

            @Override
            public void onError(@NonNull Exception error) {
                if (generation != listenerGeneration) {
                    return;
                }
                MessagesUiState currentState = state.getValue();
                List<ChatThread> retained = currentState == null
                        ? Collections.emptyList()
                        : currentState.getThreads();
                state.setValue(new MessagesUiState(
                        false, false, false, ChatUiError.message(error), retained));
            }
        });
    }

    public void stop() {
        listenerGeneration++;
        subscription.remove();
        subscription = ChatRepository.Subscription.NONE;
    }

    @Override
    protected void onCleared() {
        stop();
    }
}
