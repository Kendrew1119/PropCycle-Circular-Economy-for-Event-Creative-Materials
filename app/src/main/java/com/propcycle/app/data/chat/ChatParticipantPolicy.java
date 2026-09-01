package com.propcycle.app.data.chat;

import androidx.annotation.NonNull;

/** Resolves the other immutable participant without trusting UI position. */
public final class ChatParticipantPolicy {

    private ChatParticipantPolicy() {
    }

    @NonNull
    public static String otherUserId(
            @NonNull ChatThread thread,
            @NonNull String currentUserId) {
        if (currentUserId.equals(thread.getOwnerUid())) {
            return thread.getContactUid();
        }
        if (currentUserId.equals(thread.getContactUid())) {
            return thread.getOwnerUid();
        }
        return "";
    }
}
