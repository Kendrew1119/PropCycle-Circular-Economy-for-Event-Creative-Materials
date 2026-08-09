package com.propcycle.app.data.chat;

import androidx.annotation.NonNull;

/** Immutable text-message row returned by the chat repository. */
public final class ChatMessage {

    private final String messageId;
    private final String senderId;
    private final String text;
    private final long sentAtMillis;
    private final boolean pendingWrite;

    public ChatMessage(
            @NonNull String messageId,
            @NonNull String senderId,
            @NonNull String text,
            long sentAtMillis,
            boolean pendingWrite) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.text = text;
        this.sentAtMillis = sentAtMillis;
        this.pendingWrite = pendingWrite;
    }

    @NonNull
    public String getMessageId() {
        return messageId;
    }

    @NonNull
    public String getSenderId() {
        return senderId;
    }

    @NonNull
    public String getText() {
        return text;
    }

    public long getSentAtMillis() {
        return sentAtMillis;
    }

    public boolean isPendingWrite() {
        return pendingWrite;
    }
}
