package com.propcycle.app.data.chat;

import androidx.annotation.NonNull;

/** Immutable presentation-safe view of a marketplace or lending chat thread. */
public final class ChatThread {

    private final String threadId;
    private final String contextType;
    private final String contextId;
    private final String contextTitle;
    private final String ownerUid;
    private final String contactUid;
    private final String lastMessageId;
    private final String lastMessageText;
    private final long lastMessageAtMillis;
    private final long updatedAtMillis;

    public ChatThread(
            @NonNull String threadId,
            @NonNull String contextType,
            @NonNull String contextId,
            @NonNull String contextTitle,
            @NonNull String ownerUid,
            @NonNull String contactUid,
            @NonNull String lastMessageId,
            @NonNull String lastMessageText,
            long lastMessageAtMillis,
            long updatedAtMillis) {
        this.threadId = threadId;
        this.contextType = contextType;
        this.contextId = contextId;
        this.contextTitle = contextTitle;
        this.ownerUid = ownerUid;
        this.contactUid = contactUid;
        this.lastMessageId = lastMessageId;
        this.lastMessageText = lastMessageText;
        this.lastMessageAtMillis = lastMessageAtMillis;
        this.updatedAtMillis = updatedAtMillis;
    }

    @NonNull
    public String getThreadId() {
        return threadId;
    }

    @NonNull
    public String getContextType() {
        return contextType;
    }

    @NonNull
    public String getContextId() {
        return contextId;
    }

    @NonNull
    public String getContextTitle() {
        return contextTitle;
    }

    @NonNull
    public String getOwnerUid() {
        return ownerUid;
    }

    @NonNull
    public String getContactUid() {
        return contactUid;
    }

    @NonNull
    public String getLastMessageId() {
        return lastMessageId;
    }

    @NonNull
    public String getLastMessageText() {
        return lastMessageText;
    }

    public long getLastMessageAtMillis() {
        return lastMessageAtMillis;
    }

    public long getUpdatedAtMillis() {
        return updatedAtMillis;
    }

    public boolean hasMessages() {
        return !lastMessageId.isEmpty();
    }
}
