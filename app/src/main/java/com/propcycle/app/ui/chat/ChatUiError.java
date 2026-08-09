package com.propcycle.app.ui.chat;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestoreException;

/** Maps vendor exceptions to stable, non-sensitive messages. */
final class ChatUiError {

    private ChatUiError() {
    }

    @NonNull
    static String message(@NonNull Exception error) {
        if (error instanceof IllegalArgumentException || error instanceof IllegalStateException) {
            String message = error.getMessage();
            return message == null || message.trim().isEmpty()
                    ? "Chat is unavailable right now."
                    : message;
        }
        if (error instanceof FirebaseFirestoreException firestoreError) {
            return switch (firestoreError.getCode()) {
                case PERMISSION_DENIED, UNAUTHENTICATED ->
                        "You do not have permission to access this conversation.";
                case UNAVAILABLE, DEADLINE_EXCEEDED ->
                        "Chat is offline. Check your connection and try again.";
                case FAILED_PRECONDITION ->
                        "Chat setup is still being prepared. Try again shortly.";
                default -> "Chat is unavailable right now. Please try again.";
            };
        }
        return "Chat is unavailable right now. Please try again.";
    }
}
