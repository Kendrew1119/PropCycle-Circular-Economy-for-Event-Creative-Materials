package com.propcycle.app.data.chat;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.propcycle.app.core.firebase.FirebaseEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Firebase-backed marketplace conversation and text-message data source. */
public final class ChatRepository {

    public static final String THREADS_COLLECTION = "chatThreads";
    public static final String MESSAGES_COLLECTION = "messages";
    public static final String LISTINGS_COLLECTION = "marketplaceListings";
    public static final String LENDING_COLLECTION = "lendingItems";
    private static final int THREAD_LIMIT = 50;
    private static final int MESSAGE_LIMIT = 100;
    public static final String MARKETPLACE_ITEM_CARD_ID = "marketplace_item_card";
    public static final String MARKETPLACE_ITEM_FALLBACK_TEXT = "Marketplace item shared";
    public static final String LENDING_REQUEST_CARD_PREFIX = "lending_request_";
    public static final String LENDING_REQUEST_FALLBACK_TEXT = "Lending request sent";

    @Nullable private final FirebaseAuth auth;
    @Nullable private final FirebaseFirestore firestore;

    public ChatRepository(@NonNull Context context) {
        Context applicationContext = context.getApplicationContext();
        auth = FirebaseEnvironment.auth(applicationContext);
        firestore = FirebaseEnvironment.firestore(applicationContext);
    }

    public boolean isConfigured() {
        return auth != null && firestore != null;
    }

    @Nullable
    public String currentUserId() {
        FirebaseUser user = auth == null ? null : auth.getCurrentUser();
        return user == null ? null : user.getUid();
    }

    /**
     * Marketplace entry point. The market detail screen calls this method and navigates with the
     * returned thread ID. A failed create is read back so concurrent calls converge safely.
     */
    @NonNull
    public static Task<String> createOrGetMarketplaceThread(
            @NonNull Context context,
            @NonNull String listingId,
            @NonNull String ownerUid,
            @NonNull String contextTitle) {
        FirebaseAuth auth = FirebaseEnvironment.auth(context.getApplicationContext());
        FirebaseFirestore firestore = FirebaseEnvironment.firestore(context.getApplicationContext());
        if (auth == null || firestore == null) {
            return Tasks.forException(new IllegalStateException(FirebaseEnvironment.SETUP_MESSAGE));
        }
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return Tasks.forException(new IllegalStateException("Sign in to start a conversation."));
        }

        String cleanListingId = listingId.trim();
        String cleanOwnerUid = ownerUid.trim();
        String cleanTitle = contextTitle.trim();
        String contactUid = user.getUid();
        String validationError = ChatValidator.marketplaceThreadError(
                cleanListingId, cleanOwnerUid, cleanTitle, contactUid);
        if (validationError != null) {
            return Tasks.forException(new IllegalArgumentException(validationError));
        }

        String threadId = ChatValidator.marketplaceThreadId(
                cleanListingId, cleanOwnerUid, contactUid);
        DocumentReference thread = firestore.collection(THREADS_COLLECTION).document(threadId);
        Map<String, Object> values = new HashMap<>();
        values.put("contextType", "marketplace");
        values.put("contextId", cleanListingId);
        values.put("contextTitle", cleanTitle);
        values.put("ownerUid", cleanOwnerUid);
        values.put("contactUid", contactUid);
        values.put("participantIds", Arrays.asList(cleanOwnerUid, contactUid));
        values.put("lastMessageId", "");
        values.put("lastMessageText", "");
        values.put("lastMessageSenderId", "");
        values.put("lastMessageAt", FieldValue.serverTimestamp());
        values.put("createdAt", FieldValue.serverTimestamp());
        values.put("updatedAt", FieldValue.serverTimestamp());

        return thread.set(values).continueWithTask(create -> {
            if (create.isSuccessful()) {
                return ensureMarketplaceItemCard(
                        firestore, thread, cleanListingId, contactUid, threadId);
            }
            Exception original = create.getException() == null
                    ? new IllegalStateException("The conversation could not be opened.")
                    : create.getException();
            return thread.get().continueWithTask(read -> {
                if (read.isSuccessful() && read.getResult() != null && read.getResult().exists()) {
                    verifyExistingThread(
                            read.getResult(), cleanListingId, cleanOwnerUid, contactUid);
                    return ensureMarketplaceItemCard(
                            firestore, thread, cleanListingId, contactUid, threadId);
                }
                return Tasks.forException(original);
            });
        });
    }

    @NonNull
    private static Task<String> ensureMarketplaceItemCard(
            @NonNull FirebaseFirestore firestore,
            @NonNull DocumentReference thread,
            @NonNull String listingId,
            @NonNull String contactUid,
            @NonNull String threadId) {
        DocumentReference message = thread.collection(MESSAGES_COLLECTION)
                .document(MARKETPLACE_ITEM_CARD_ID);

        Map<String, Object> messageValues = new HashMap<>();
        messageValues.put("type", ChatMessage.TYPE_MARKETPLACE_ITEM);
        messageValues.put("itemId", listingId);
        messageValues.put("senderId", contactUid);
        messageValues.put("text", MARKETPLACE_ITEM_FALLBACK_TEXT);
        messageValues.put("clientOperationId", MARKETPLACE_ITEM_CARD_ID);
        messageValues.put("sentAt", FieldValue.serverTimestamp());

        Map<String, Object> previewValues = new HashMap<>();
        previewValues.put("lastMessageId", MARKETPLACE_ITEM_CARD_ID);
        previewValues.put("lastMessageText", MARKETPLACE_ITEM_FALLBACK_TEXT);
        previewValues.put("lastMessageSenderId", contactUid);
        previewValues.put("lastMessageAt", FieldValue.serverTimestamp());
        previewValues.put("updatedAt", FieldValue.serverTimestamp());

        WriteBatch batch = firestore.batch();
        batch.set(message, messageValues);
        batch.update(thread, previewValues);
        return batch.commit().continueWithTask(result -> {
            if (result.isSuccessful()) {
                return Tasks.forResult(threadId);
            }
            Exception original = result.getException() == null
                    ? new IllegalStateException("The Marketplace item card could not be added.")
                    : result.getException();
            return message.get().continueWithTask(read -> {
                if (read.isSuccessful() && read.getResult() != null
                        && read.getResult().exists()) {
                    DocumentSnapshot saved = read.getResult();
                    if (ChatMessage.TYPE_MARKETPLACE_ITEM.equals(saved.getString("type"))
                            && listingId.equals(saved.getString("itemId"))
                            && contactUid.equals(saved.getString("senderId"))
                            && MARKETPLACE_ITEM_FALLBACK_TEXT.equals(saved.getString("text"))
                            && MARKETPLACE_ITEM_CARD_ID.equals(
                                    saved.getString("clientOperationId"))) {
                        return Tasks.forResult(threadId);
                    }
                }
                return Tasks.forException(original);
            });
        });
    }

    /** Lending entry point using the same participant-only conversation contract. */
    @NonNull
    public static Task<String> createOrGetLendingThread(
            @NonNull Context context,
            @NonNull String itemId,
            @NonNull String ownerUid,
            @NonNull String contextTitle) {
        FirebaseAuth auth = FirebaseEnvironment.auth(context.getApplicationContext());
        FirebaseFirestore firestore = FirebaseEnvironment.firestore(context.getApplicationContext());
        if (auth == null || firestore == null) {
            return Tasks.forException(new IllegalStateException(FirebaseEnvironment.SETUP_MESSAGE));
        }
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return Tasks.forException(new IllegalStateException("Sign in to start a conversation."));
        }
        String cleanItemId = itemId.trim();
        String cleanOwnerUid = ownerUid.trim();
        String cleanTitle = contextTitle.trim();
        String contactUid = user.getUid();
        String validationError = ChatValidator.lendingThreadError(
                cleanItemId, cleanOwnerUid, cleanTitle, contactUid);
        if (validationError != null) {
            return Tasks.forException(new IllegalArgumentException(validationError));
        }

        String threadId = ChatValidator.lendingThreadId(
                cleanItemId, cleanOwnerUid, contactUid);
        DocumentReference thread = firestore.collection(THREADS_COLLECTION).document(threadId);
        Map<String, Object> values = new HashMap<>();
        values.put("contextType", "lending");
        values.put("contextId", cleanItemId);
        values.put("contextTitle", cleanTitle);
        values.put("ownerUid", cleanOwnerUid);
        values.put("contactUid", contactUid);
        values.put("participantIds", Arrays.asList(cleanOwnerUid, contactUid));
        values.put("lastMessageId", "");
        values.put("lastMessageText", "");
        values.put("lastMessageSenderId", "");
        values.put("lastMessageAt", FieldValue.serverTimestamp());
        values.put("createdAt", FieldValue.serverTimestamp());
        values.put("updatedAt", FieldValue.serverTimestamp());
        return thread.set(values).continueWithTask(create -> {
            if (create.isSuccessful()) {
                return Tasks.forResult(threadId);
            }
            Exception original = create.getException() == null
                    ? new IllegalStateException("The conversation could not be opened.")
                    : create.getException();
            return thread.get().continueWithTask(read -> {
                if (read.isSuccessful() && read.getResult() != null && read.getResult().exists()) {
                    verifyExistingLendingThread(
                            read.getResult(), cleanItemId, cleanOwnerUid, contactUid);
                    return Tasks.forResult(threadId);
                }
                return Tasks.forException(original);
            });
        });
    }

    /** Creates/finds the lending thread and idempotently attaches its request card. */
    @NonNull
    public static Task<String> createOrGetLendingRequestThread(
            @NonNull Context context,
            @NonNull String itemId,
            @NonNull String ownerUid,
            @NonNull String contextTitle,
            @NonNull String requestId) {
        String cleanRequestId = requestId.trim();
        String operationId = LENDING_REQUEST_CARD_PREFIX + cleanRequestId;
        if (!ChatValidator.isValidDocumentPart(cleanRequestId)
                || !ChatValidator.isValidOperationId(operationId)) {
            return Tasks.forException(new IllegalArgumentException(
                    "The lending request card is invalid."));
        }
        Context app = context.getApplicationContext();
        return createOrGetLendingThread(app, itemId, ownerUid, contextTitle)
                .continueWithTask(threadTask -> {
                    if (!threadTask.isSuccessful() || threadTask.getResult() == null) {
                        Exception error = threadTask.getException();
                        return Tasks.forException(error == null
                                ? new IllegalStateException(
                                        "The lending conversation could not be opened.")
                                : error);
                    }
                    FirebaseAuth auth = FirebaseEnvironment.auth(app);
                    FirebaseFirestore firestore = FirebaseEnvironment.firestore(app);
                    FirebaseUser user = auth == null ? null : auth.getCurrentUser();
                    if (firestore == null || user == null) {
                        return Tasks.forException(new IllegalStateException(
                                "Sign in to add the lending request card."));
                    }
                    String threadId = threadTask.getResult();
                    DocumentReference thread = firestore.collection(THREADS_COLLECTION)
                            .document(threadId);
                    return ensureLendingRequestCard(
                            firestore,
                            thread,
                            itemId.trim(),
                            cleanRequestId,
                            user.getUid(),
                            threadId);
                });
    }

    @NonNull
    private static Task<String> ensureLendingRequestCard(
            @NonNull FirebaseFirestore firestore,
            @NonNull DocumentReference thread,
            @NonNull String itemId,
            @NonNull String requestId,
            @NonNull String borrowerUid,
            @NonNull String threadId) {
        String operationId = LENDING_REQUEST_CARD_PREFIX + requestId;
        DocumentReference message = thread.collection(MESSAGES_COLLECTION).document(operationId);

        Map<String, Object> messageValues = new HashMap<>();
        messageValues.put("type", ChatMessage.TYPE_LENDING_REQUEST);
        messageValues.put("requestId", requestId);
        messageValues.put("itemId", itemId);
        messageValues.put("senderId", borrowerUid);
        messageValues.put("text", LENDING_REQUEST_FALLBACK_TEXT);
        messageValues.put("clientOperationId", operationId);
        messageValues.put("sentAt", FieldValue.serverTimestamp());

        Map<String, Object> previewValues = new HashMap<>();
        previewValues.put("lastMessageId", operationId);
        previewValues.put("lastMessageText", LENDING_REQUEST_FALLBACK_TEXT);
        previewValues.put("lastMessageSenderId", borrowerUid);
        previewValues.put("lastMessageAt", FieldValue.serverTimestamp());
        previewValues.put("updatedAt", FieldValue.serverTimestamp());

        WriteBatch batch = firestore.batch();
        batch.set(message, messageValues);
        batch.update(thread, previewValues);
        return batch.commit().continueWithTask(result -> {
            if (result.isSuccessful()) {
                return Tasks.forResult(threadId);
            }
            Exception original = result.getException() == null
                    ? new IllegalStateException("The lending request card could not be added.")
                    : result.getException();
            return message.get().continueWithTask(read -> {
                if (read.isSuccessful() && read.getResult() != null
                        && read.getResult().exists()) {
                    DocumentSnapshot saved = read.getResult();
                    if (ChatMessage.TYPE_LENDING_REQUEST.equals(saved.getString("type"))
                            && requestId.equals(saved.getString("requestId"))
                            && itemId.equals(saved.getString("itemId"))
                            && borrowerUid.equals(saved.getString("senderId"))
                            && LENDING_REQUEST_FALLBACK_TEXT.equals(saved.getString("text"))
                            && operationId.equals(saved.getString("clientOperationId"))) {
                        return Tasks.forResult(threadId);
                    }
                }
                return Tasks.forException(original);
            });
        });
    }

    @NonNull
    public Subscription listenToThreads(@NonNull SnapshotCallback<List<ChatThread>> callback) {
        if (firestore == null) {
            callback.onError(new IllegalStateException(FirebaseEnvironment.SETUP_MESSAGE));
            return Subscription.NONE;
        }
        String uid = currentUserId();
        if (uid == null) {
            callback.onError(new IllegalStateException("Sign in to view your conversations."));
            return Subscription.NONE;
        }

        ListenerRegistration registration = firestore.collection(THREADS_COLLECTION)
                .whereArrayContains("participantIds", uid)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(THREAD_LIMIT)
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
                    if (error != null) {
                        callback.onError(error);
                        return;
                    }
                    if (snapshot == null) {
                        callback.onError(new IllegalStateException("Conversation data is unavailable."));
                        return;
                    }
                    List<ChatThread> threads = new ArrayList<>(snapshot.size());
                    for (QueryDocumentSnapshot document : snapshot) {
                        ChatThread thread = mapThread(document);
                        if (thread != null) {
                            threads.add(thread);
                        }
                    }
                    callback.onData(threads, snapshot.getMetadata().isFromCache());
                });
        return registration::remove;
    }

    @NonNull
    public Subscription listenToThread(
            @NonNull String threadId,
            @NonNull SnapshotCallback<ChatThread> callback) {
        if (firestore == null) {
            callback.onError(new IllegalStateException(FirebaseEnvironment.SETUP_MESSAGE));
            return Subscription.NONE;
        }
        ListenerRegistration registration = firestore.collection(THREADS_COLLECTION)
                .document(threadId)
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
                    if (error != null) {
                        callback.onError(error);
                        return;
                    }
                    ChatThread thread = snapshot == null ? null : mapThread(snapshot);
                    if (snapshot == null || !snapshot.exists() || thread == null) {
                        callback.onError(new IllegalStateException("This conversation is unavailable."));
                        return;
                    }
                    callback.onData(thread, snapshot.getMetadata().isFromCache());
                });
        return registration::remove;
    }

    @NonNull
    public Subscription listenToMessages(
            @NonNull String threadId,
            @NonNull SnapshotCallback<List<ChatMessage>> callback) {
        if (firestore == null) {
            callback.onError(new IllegalStateException(FirebaseEnvironment.SETUP_MESSAGE));
            return Subscription.NONE;
        }
        ListenerRegistration registration = firestore.collection(THREADS_COLLECTION)
                .document(threadId)
                .collection(MESSAGES_COLLECTION)
                .orderBy("sentAt", Query.Direction.ASCENDING)
                .limitToLast(MESSAGE_LIMIT)
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
                    if (error != null) {
                        callback.onError(error);
                        return;
                    }
                    if (snapshot == null) {
                        callback.onError(new IllegalStateException("Messages are unavailable."));
                        return;
                    }
                    List<ChatMessage> messages = new ArrayList<>(snapshot.size());
                    for (QueryDocumentSnapshot document : snapshot) {
                        ChatMessage message = mapMessage(document);
                        if (message != null) {
                            messages.add(message);
                        }
                    }
                    callback.onData(messages, snapshot.getMetadata().isFromCache());
                });
        return registration::remove;
    }

    /** Creates an immutable message and updates all parent preview fields atomically. */
    @NonNull
    public Task<Void> sendMessage(
            @NonNull String threadId,
            @NonNull String text,
            @NonNull String operationId) {
        if (firestore == null || auth == null) {
            return Tasks.forException(new IllegalStateException(FirebaseEnvironment.SETUP_MESSAGE));
        }
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return Tasks.forException(new IllegalStateException("Sign in to send a message."));
        }
        String validationError = ChatValidator.messageError(text);
        if (validationError != null) {
            return Tasks.forException(new IllegalArgumentException(validationError));
        }
        if (ChatValidator.threadIdError(threadId) != null
                || !ChatValidator.isValidOperationId(operationId)) {
            return Tasks.forException(new IllegalArgumentException("The message request is invalid."));
        }

        String cleanText = text.trim();
        DocumentReference thread = firestore.collection(THREADS_COLLECTION).document(threadId);
        DocumentReference message = thread.collection(MESSAGES_COLLECTION).document(operationId);

        Map<String, Object> messageValues = new HashMap<>();
        messageValues.put("senderId", user.getUid());
        messageValues.put("text", cleanText);
        messageValues.put("clientOperationId", operationId);
        messageValues.put("sentAt", FieldValue.serverTimestamp());

        Map<String, Object> previewValues = new HashMap<>();
        previewValues.put("lastMessageId", operationId);
        previewValues.put("lastMessageText", cleanText);
        previewValues.put("lastMessageSenderId", user.getUid());
        previewValues.put("lastMessageAt", FieldValue.serverTimestamp());
        previewValues.put("updatedAt", FieldValue.serverTimestamp());

        WriteBatch batch = firestore.batch();
        batch.set(message, messageValues);
        batch.update(thread, previewValues);
        Task<Void> commit = batch.commit();

        // If a response was lost after commit, a same-operation retry reads back as success.
        return commit.continueWithTask(result -> {
            if (result.isSuccessful()) {
                return Tasks.<Void>forResult(null);
            }
            Exception original = result.getException() == null
                    ? new IllegalStateException("The message could not be sent.")
                    : result.getException();
            return message.get().continueWithTask(read -> {
                if (read.isSuccessful() && read.getResult() != null && read.getResult().exists()) {
                    DocumentSnapshot saved = read.getResult();
                    if (user.getUid().equals(saved.getString("senderId"))
                            && cleanText.equals(saved.getString("text"))
                            && operationId.equals(saved.getString("clientOperationId"))) {
                        return Tasks.<Void>forResult(null);
                    }
                }
                return Tasks.forException(original);
            });
        });
    }

    private static void verifyExistingThread(
            @NonNull DocumentSnapshot existing,
            @NonNull String listingId,
            @NonNull String ownerUid,
            @NonNull String contactUid) {
        List<String> expectedParticipants = Arrays.asList(ownerUid, contactUid);
        Object participantValue = existing.get("participantIds");
        if (!"marketplace".equals(existing.getString("contextType"))
                || !listingId.equals(existing.getString("contextId"))
                || !ownerUid.equals(existing.getString("ownerUid"))
                || !contactUid.equals(existing.getString("contactUid"))
                || !expectedParticipants.equals(participantValue)) {
            throw new IllegalStateException("The existing conversation does not match this listing.");
        }
    }

    private static void verifyExistingLendingThread(
            @NonNull DocumentSnapshot existing,
            @NonNull String itemId,
            @NonNull String ownerUid,
            @NonNull String contactUid) {
        List<String> expectedParticipants = Arrays.asList(ownerUid, contactUid);
        if (!"lending".equals(existing.getString("contextType"))
                || !itemId.equals(existing.getString("contextId"))
                || !ownerUid.equals(existing.getString("ownerUid"))
                || !contactUid.equals(existing.getString("contactUid"))
                || !expectedParticipants.equals(existing.get("participantIds"))) {
            throw new IllegalStateException("The existing conversation does not match this item.");
        }
    }

    @Nullable
    private static ChatThread mapThread(@NonNull DocumentSnapshot document) {
        String contextId = document.getString("contextId");
        String contextType = document.getString("contextType");
        String contextTitle = document.getString("contextTitle");
        String ownerUid = document.getString("ownerUid");
        String contactUid = document.getString("contactUid");
        if (contextType == null || contextId == null || contextTitle == null
                || ownerUid == null || contactUid == null) {
            return null;
        }
        return new ChatThread(
                document.getId(),
                contextType,
                contextId,
                contextTitle,
                ownerUid,
                contactUid,
                valueOrEmpty(document.getString("lastMessageId")),
                valueOrEmpty(document.getString("lastMessageText")),
                valueOrEmpty(document.getString("lastMessageSenderId")),
                timestampMillis(document.getTimestamp("lastMessageAt")),
                timestampMillis(document.getTimestamp("updatedAt")));
    }

    @Nullable
    private static ChatMessage mapMessage(@NonNull DocumentSnapshot document) {
        String senderId = document.getString("senderId");
        String text = document.getString("text");
        if (senderId == null || text == null) {
            return null;
        }
        return new ChatMessage(
                document.getId(),
                senderId,
                text,
                valueOrEmpty(document.getString("type")),
                valueOrEmpty(document.getString("itemId")),
                valueOrEmpty(document.getString("requestId")),
                timestampMillis(document.getTimestamp("sentAt")),
                document.getMetadata().hasPendingWrites());
    }

    private static long timestampMillis(@Nullable Timestamp timestamp) {
        // A Firestore Timestamp is a UTC instant. Epoch milliseconds stay timezone-neutral;
        // the UI applies the phone's local timezone exactly once when formatting it.
        return timestamp == null
                ? 0L
                : timestamp.getSeconds() * 1000L + timestamp.getNanoseconds() / 1_000_000L;
    }

    @NonNull
    private static String valueOrEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }

    public interface SnapshotCallback<T> {
        void onData(@NonNull T value, boolean fromCache);

        void onError(@NonNull Exception error);
    }

    public interface Subscription {
        Subscription NONE = () -> { };

        void remove();
    }
}
