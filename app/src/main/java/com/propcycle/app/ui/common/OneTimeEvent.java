package com.propcycle.app.ui.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Lifecycle-safe value that is consumed once, so rotation cannot repeat navigation. */
public final class OneTimeEvent<T> {
    private final T value;
    private boolean handled;

    public OneTimeEvent(@NonNull T value) {
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
