package com.propcycle.app.ui.chat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.propcycle.app.data.chat.ChatThread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Complete render state for the conversation list. */
public final class MessagesUiState {

    private final boolean loading;
    private final boolean configurationRequired;
    private final boolean fromCache;
    private final String errorMessage;
    private final List<ChatThread> threads;

    public MessagesUiState(
            boolean loading,
            boolean configurationRequired,
            boolean fromCache,
            @Nullable String errorMessage,
            @NonNull List<ChatThread> threads) {
        this.loading = loading;
        this.configurationRequired = configurationRequired;
        this.fromCache = fromCache;
        this.errorMessage = errorMessage;
        this.threads = Collections.unmodifiableList(new ArrayList<>(threads));
    }

    @NonNull
    public static MessagesUiState loading() {
        return new MessagesUiState(true, false, false, null, Collections.emptyList());
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

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    @NonNull
    public List<ChatThread> getThreads() {
        return threads;
    }
}
