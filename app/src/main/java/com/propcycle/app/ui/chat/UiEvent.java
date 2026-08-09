package com.propcycle.app.ui.chat;

import androidx.annotation.Nullable;

/** One-shot LiveData value that survives configuration changes without replaying an action. */
public final class UiEvent<T> {

    private final T value;
    private boolean consumed;

    public UiEvent(T value) {
        this.value = value;
    }

    @Nullable
    public T consume() {
        if (consumed) {
            return null;
        }
        consumed = true;
        return value;
    }
}
