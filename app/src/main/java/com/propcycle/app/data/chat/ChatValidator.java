package com.propcycle.app.data.chat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Pure validation shared by the chat UI and repository boundary. */
public final class ChatValidator {

    public static final int MAX_MESSAGE_LENGTH = 2_000;
    public static final int MAX_CONTEXT_TITLE_LENGTH = 120;
    private static final int MAX_DOCUMENT_PART_LENGTH = 128;
    private static final int MAX_THREAD_ID_LENGTH = 512;

    private ChatValidator() {
    }

    @Nullable
    public static String messageError(@Nullable String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return "Type a message first.";
        }
        if (trimmed.length() > MAX_MESSAGE_LENGTH) {
            return "Messages can contain at most 2000 characters.";
        }
        return null;
    }

    @Nullable
    public static String marketplaceThreadError(
            @Nullable String listingId,
            @Nullable String ownerUid,
            @Nullable String contextTitle,
            @Nullable String contactUid) {
        if (!isValidDocumentPart(listingId)) {
            return "This marketplace listing is unavailable.";
        }
        if (!isValidDocumentPart(ownerUid) || !isValidDocumentPart(contactUid)) {
            return "The chat participants are invalid.";
        }
        if (ownerUid.equals(contactUid)) {
            return "You cannot start a chat on your own listing.";
        }
        String title = contextTitle == null ? "" : contextTitle.trim();
        if (title.isEmpty() || title.length() > MAX_CONTEXT_TITLE_LENGTH) {
            return "The marketplace listing title is invalid.";
        }
        return null;
    }

    @Nullable
    public static String lendingThreadError(
            @Nullable String itemId,
            @Nullable String ownerUid,
            @Nullable String contextTitle,
            @Nullable String contactUid) {
        String error = marketplaceThreadError(itemId, ownerUid, contextTitle, contactUid);
        if (error == null) {
            return null;
        }
        return error.replace("marketplace listing", "lending item")
                .replace("own listing", "own item");
    }

    @Nullable
    public static String threadIdError(@Nullable String threadId) {
        return isValidPathSegment(threadId, MAX_THREAD_ID_LENGTH)
                ? null
                : "Open a conversation from Messages.";
    }

    public static boolean isValidDocumentPart(@Nullable String value) {
        if (value == null) {
            return false;
        }
        return isValidPathSegment(value, MAX_DOCUMENT_PART_LENGTH);
    }

    public static boolean isValidOperationId(@Nullable String value) {
        if (!isValidPathSegment(value, 80)) {
            return false;
        }
        int length = value == null ? 0 : value.trim().length();
        return length >= 20;
    }

    @NonNull
    public static String marketplaceThreadId(
            @NonNull String listingId,
            @NonNull String ownerUid,
            @NonNull String contactUid) {
        return "marketplace_" + listingId + "_" + ownerUid + "_" + contactUid;
    }

    @NonNull
    public static String lendingThreadId(
            @NonNull String itemId,
            @NonNull String ownerUid,
            @NonNull String contactUid) {
        return "lending_" + itemId + "_" + ownerUid + "_" + contactUid;
    }

    private static boolean isValidPathSegment(@Nullable String value, int maximumLength) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return !trimmed.isEmpty()
                && trimmed.length() <= maximumLength
                && !trimmed.contains("/");
    }
}
