package com.propcycle.app.ui.chat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.propcycle.app.data.chat.ChatMessage;
import com.propcycle.app.data.chat.ChatThread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Complete render state for one real-time conversation. */
public final class ConversationUiState {

    private final boolean loading;
    private final boolean configurationRequired;
    private final boolean fromCache;
    private final boolean sending;
    private final String errorMessage;
    private final ChatThread thread;
    private final List<ChatMessage> messages;

    public ConversationUiState(
            boolean loading,
            boolean configurationRequired,
            boolean fromCache,
            boolean sending,
            @Nullable String errorMessage,
            @Nullable ChatThread thread,
            @NonNull List<ChatMessage> messages) {
        this.loading = loading;
        this.configurationRequired = configurationRequired;
        this.fromCache = fromCache;
        this.sending = sending;
        this.errorMessage = errorMessage;
        this.thread = thread;
        this.messages = Collections.unmodifiableList(new ArrayList<>(messages));
    }

    @NonNull
    public static ConversationUiState loading() {
        return new ConversationUiState(
                true, false, false, false, null, null, Collections.emptyList());
    }

    public boolean isLoading() {
        return loading;
    }

    public boolean isConfigurationRequired() {
        return configurationRequired;
    }

    public boolean isFromCache() {
        return fromCache;
    }

    public boolean isSending() {
        return sending;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    @Nullable
    public ChatThread getThread() {
        return thread;
    }

    @NonNull
    public List<ChatMessage> getMessages() {
        return messages;
    }
}
