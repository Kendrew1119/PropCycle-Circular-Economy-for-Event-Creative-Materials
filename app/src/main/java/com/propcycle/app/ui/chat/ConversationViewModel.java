package com.propcycle.app.ui.chat;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.propcycle.app.core.firebase.FirebaseEnvironment;
import com.propcycle.app.data.chat.ChatMessage;
import com.propcycle.app.data.chat.ChatRepository;
import com.propcycle.app.data.chat.ChatThread;
import com.propcycle.app.data.chat.ChatValidator;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Owns one thread, its bounded message listener, and duplicate-safe sending state. */
public final class ConversationViewModel extends AndroidViewModel {

    private final ChatRepository repository;
    private final MutableLiveData<ConversationUiState> state =
            new MutableLiveData<>(ConversationUiState.loading());
    private final MutableLiveData<UiEvent<Boolean>> sendSucceeded = new MutableLiveData<>();

    private ChatRepository.Subscription threadSubscription = ChatRepository.Subscription.NONE;
    private ChatRepository.Subscription messageSubscription = ChatRepository.Subscription.NONE;
    private String activeThreadId = "";
    private int listenerGeneration;
    private boolean threadLoaded;
    private boolean messagesLoaded;
    private boolean threadFromCache;
    private boolean messagesFromCache;
    private String threadError;
    private String messagesError;
    private String actionError;
    private ChatThread thread;
    private List<ChatMessage> messages = Collections.emptyList();
    private String pendingOperationId;
    private String pendingText;

    public ConversationViewModel(@NonNull Application application) {
        super(application);
        repository = new ChatRepository(application);
    }

    @NonNull
    public LiveData<ConversationUiState> getState() {
        return state;
    }

    @NonNull
    public LiveData<UiEvent<Boolean>> getSendSucceeded() {
        return sendSucceeded;
    }

    @NonNull
    public String currentUserId() {
        String uid = repository.currentUserId();
        return uid == null ? "" : uid;
    }

    public void start(@NonNull String threadId) {
        stop();
        String validationError = ChatValidator.threadIdError(threadId);
        if (validationError != null) {
            state.setValue(new ConversationUiState(
                    false, false, false, false, validationError, null, Collections.emptyList()));
            return;
        }
        if (!repository.isConfigured()) {
            state.setValue(new ConversationUiState(
                    false,
                    true,
                    false,
                    false,
                    FirebaseEnvironment.SETUP_MESSAGE,
                    null,
                    Collections.emptyList()));
            return;
        }
        if (repository.currentUserId() == null) {
            state.setValue(new ConversationUiState(
                    false,
                    false,
                    false,
                    false,
                    "Sign in to access this conversation.",
                    null,
                    Collections.emptyList()));
            return;
        }

        activeThreadId = threadId.trim();
        threadLoaded = false;
        messagesLoaded = false;
        threadFromCache = false;
        messagesFromCache = false;
        threadError = null;
        messagesError = null;
        actionError = null;
        thread = null;
        messages = Collections.emptyList();
        state.setValue(ConversationUiState.loading());
        int generation = ++listenerGeneration;

        threadSubscription = repository.listenToThread(
                activeThreadId,
                new ChatRepository.SnapshotCallback<>() {
                    @Override
                    public void onData(@NonNull ChatThread value, boolean fromCache) {
                        if (generation != listenerGeneration) {
                            return;
                        }
                        thread = value;
                        threadLoaded = true;
                        threadFromCache = fromCache;
                        threadError = null;
                        publish(isSending());
                    }

                    @Override
                    public void onError(@NonNull Exception error) {
                        if (generation == listenerGeneration) {
                            threadError = ChatUiError.message(error);
                            publish(isSending());
                        }
                    }
                });

        messageSubscription = repository.listenToMessages(
                activeThreadId,
                new ChatRepository.SnapshotCallback<>() {
                    @Override
                    public void onData(@NonNull List<ChatMessage> value, boolean fromCache) {
                        if (generation != listenerGeneration) {
                            return;
                        }
                        messages = value;
                        messagesLoaded = true;
                        messagesFromCache = fromCache;
                        messagesError = null;
                        publish(isSending());
                    }

                    @Override
                    public void onError(@NonNull Exception error) {
                        if (generation == listenerGeneration) {
                            messagesError = ChatUiError.message(error);
                            publish(isSending());
                        }
                    }
                });
    }

    public void sendMessage(@NonNull String value) {
        ConversationUiState current = state.getValue();
        if (current != null && current.isSending()) {
            return;
        }
        String validationError = ChatValidator.messageError(value);
        if (validationError != null) {
            actionError = validationError;
            publish(false);
            return;
        }
        if (activeThreadId.isEmpty() || thread == null) {
            actionError = "Wait for the conversation to finish loading.";
            publish(false);
            return;
        }

        String cleanText = value.trim();
        if (!cleanText.equals(pendingText) || pendingOperationId == null) {
            pendingText = cleanText;
            pendingOperationId = UUID.randomUUID().toString();
        }
        String operationId = pendingOperationId;
        actionError = null;
        publish(true);
        repository.sendMessage(activeThreadId, cleanText, operationId)
                .addOnSuccessListener(ignored -> {
                    pendingOperationId = null;
                    pendingText = null;
                    actionError = null;
                    publish(false);
                    sendSucceeded.setValue(new UiEvent<>(Boolean.TRUE));
                })
                .addOnFailureListener(error -> {
                    actionError = ChatUiError.message(error);
                    publish(false);
                });
    }

    public void stop() {
        listenerGeneration++;
        threadSubscription.remove();
        messageSubscription.remove();
        threadSubscription = ChatRepository.Subscription.NONE;
        messageSubscription = ChatRepository.Subscription.NONE;
    }

    private boolean isSending() {
        ConversationUiState current = state.getValue();
        return current != null && current.isSending();
    }

    private void publish(boolean sending) {
        String errorMessage = actionError != null
                ? actionError
                : (threadError != null ? threadError : messagesError);
        state.setValue(new ConversationUiState(
                errorMessage == null && !(threadLoaded && messagesLoaded),
                false,
                threadFromCache || messagesFromCache,
                sending,
                errorMessage,
                thread,
                messages));
    }

    @Override
    protected void onCleared() {
        stop();
    }
}
