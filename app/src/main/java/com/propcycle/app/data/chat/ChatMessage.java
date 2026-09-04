package com.propcycle.app.data.chat;

import androidx.annotation.NonNull;

/** Immutable text or typed-card row returned by the chat repository. */
public final class ChatMessage {

    public static final String TYPE_MARKETPLACE_ITEM = "marketplace_item";
    public static final String TYPE_LENDING_REQUEST = "lending_request";

    private final String messageId;
    private final String senderId;
    private final String text;
    private final String type;
    private final String itemId;
    private final String requestId;
    private final long sentAtMillis;
    private final boolean pendingWrite;

    public ChatMessage(
            @NonNull String messageId,
            @NonNull String senderId,
            @NonNull String text,
            long sentAtMillis,
            boolean pendingWrite) {
        this(messageId, senderId, text, "", "", "", sentAtMillis, pendingWrite);
    }

    public ChatMessage(
            @NonNull String messageId,
            @NonNull String senderId,
            @NonNull String text,
            @NonNull String type,
            @NonNull String itemId,
            long sentAtMillis,
            boolean pendingWrite) {
        this(messageId, senderId, text, type, itemId, "", sentAtMillis, pendingWrite);
    }

    public ChatMessage(
            @NonNull String messageId,
            @NonNull String senderId,
            @NonNull String text,
            @NonNull String type,
            @NonNull String itemId,
            @NonNull String requestId,
            long sentAtMillis,
            boolean pendingWrite) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.text = text;
        this.type = type;
        this.itemId = itemId;
        this.requestId = requestId;
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

    @NonNull
    public String getType() {
        return type;
    }

    @NonNull
    public String getItemId() {
        return itemId;
    }

    @NonNull
    public String getRequestId() {
        return requestId;
    }

    public boolean isMarketplaceItem() {
        return TYPE_MARKETPLACE_ITEM.equals(type) && !itemId.isEmpty();
    }

    public boolean isLendingRequest() {
        return TYPE_LENDING_REQUEST.equals(type)
                && !itemId.isEmpty()
                && !requestId.isEmpty();
    }

    public long getSentAtMillis() {
        return sentAtMillis;
    }

    public boolean isPendingWrite() {
        return pendingWrite;
    }
}
