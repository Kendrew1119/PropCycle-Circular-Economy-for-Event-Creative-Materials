package com.propcycle.app.data.lending;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.propcycle.app.core.firebase.FirebaseEnvironment;
import com.propcycle.app.data.media.DemoImagePolicy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Firebase boundary for lending items, requests, booked days, and ratings. */
public final class FirestoreLendingRepository {

    private static final String DIAGNOSTIC_TAG = "PropCycleLendingDebug";
    public static final String ITEMS = "lendingItems";
    public static final String REQUESTS = "lendingRequests";
    public static final String RATINGS = "lendingRatings";
    public static final String BOOKED_DAYS = "bookedDays";

    public interface SnapshotCallback<T> {
        void onData(@NonNull T value, boolean fromCache);
        void onError(@NonNull Exception error);
    }

    public interface Subscription {
        Subscription NONE = () -> { };
        void remove();
    }

    @Nullable private final FirebaseAuth auth;
    @Nullable private final FirebaseFirestore firestore;

    public FirestoreLendingRepository(@NonNull Context context) {
        Context app = context.getApplicationContext();
        auth = FirebaseEnvironment.auth(app);
        firestore = FirebaseEnvironment.firestore(app);
    }

    public boolean isConfigured() {
        return auth != null && firestore != null;
    }

    @Nullable
    public String currentUserId() {
        FirebaseUser user = auth == null ? null : auth.getCurrentUser();
        return user == null ? null : user.getUid();
    }

    @NonNull
    public Subscription observeAvailableItems(
            @NonNull SnapshotCallback<List<LendingItem>> callback) {
        if (firestore == null) {
            callback.onError(new IllegalStateException(FirebaseEnvironment.SETUP_MESSAGE));
            return Subscription.NONE;
        }
        if (currentUserId() == null) {
            callback.onError(new IllegalStateException("Sign in to browse lending items."));
            return Subscription.NONE;
        }
        ListenerRegistration registration = firestore.collection(ITEMS)
                .whereEqualTo("status", "available")
                .limit(LendingPolicy.MAX_RESULTS)
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
                    if (error != null) {
                        callback.onError(error);
                        return;
                    }
                    if (snapshot == null) {
                        callback.onError(new IllegalStateException("Lending data is unavailable."));
                        return;
                    }
                    List<LendingItem> items = new ArrayList<>();
                    for (QueryDocumentSnapshot document : snapshot) {
                        LendingItem item = mapItem(document);
                        if (item != null) {
                            items.add(item);
                        }
                    }
                    items.sort((left, right) -> timestampMillis(right.getUpdatedAt())
                            .compareTo(timestampMillis(left.getUpdatedAt())));
                    callback.onData(items, snapshot.getMetadata().isFromCache());
                });
        return registration::remove;
    }

    @NonNull
    public Subscription observeItem(
            @NonNull String itemId,
            @NonNull SnapshotCallback<LendingItem> callback) {
        if (firestore == null || !LendingPolicy.isSafeSegment(itemId)) {
            callback.onError(new IllegalArgumentException("This lending item is invalid."));
            return Subscription.NONE;
        }
        ListenerRegistration registration = firestore.collection(ITEMS).document(itemId)
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
                    if (error != null) {
                        callback.onError(error);
                        return;
                    }
                    LendingItem item = snapshot == null ? null : mapItem(snapshot);
                    if (snapshot == null || !snapshot.exists() || item == null) {
                        callback.onError(new IllegalStateException("This lending item is unavailable."));
                        return;
                    }
                    callback.onData(item, snapshot.getMetadata().isFromCache());
                });
        return registration::remove;
    }

    @NonNull
    public Task<Void> createItem(
            @NonNull String itemId,
            @NonNull NewLendingItem input,
            @Nullable String imageUrl) {
        FirebaseUser user = currentUser();
        if (firestore == null || user == null) {
            return Tasks.forException(new IllegalStateException(
                    "Firebase setup and sign-in are required to lend an item."));
        }
        if (!LendingPolicy.isSafeSegment(itemId)) {
            return Tasks.forException(new IllegalArgumentException("The lending item ID is invalid."));
        }
        if (!DemoImagePolicy.isValid(input.getDemoImageKey())
                || imageUrl != null && DemoImagePolicy.isSelected(input.getDemoImageKey())) {
            return Tasks.forException(new IllegalArgumentException(
                    "Choose either a personal photo or a built-in demo image."));
        }
        Map<String, Object> values = itemValues(input, imageUrl);
        values.put("ownerId", user.getUid());
        values.put("status", "available");
        values.put("createdAt", FieldValue.serverTimestamp());
        values.put("updatedAt", FieldValue.serverTimestamp());
        return firestore.collection(ITEMS).document(itemId).set(values);
    }

    @NonNull
    public Task<Void> updateItem(
            @NonNull String itemId,
            @NonNull NewLendingItem input,
            @Nullable String imageUrl,
            @Nullable Timestamp expectedUpdatedAt) {
        FirebaseUser user = currentUser();
        if (firestore == null || user == null || expectedUpdatedAt == null) {
            return Tasks.forException(new IllegalStateException(
                    "Reload the lending item before saving changes."));
        }
        if (!DemoImagePolicy.isValid(input.getDemoImageKey())
                || imageUrl != null && DemoImagePolicy.isSelected(input.getDemoImageKey())) {
            return Tasks.forException(new IllegalArgumentException(
                    "Choose either a personal photo or a built-in demo image."));
        }
        DocumentReference itemReference = firestore.collection(ITEMS).document(itemId);
        return firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(itemReference);
            if (!snapshot.exists()) {
                throw new IllegalStateException("This lending item no longer exists.");
            }
            if (!user.getUid().equals(snapshot.getString("ownerId"))) {
                throw new IllegalStateException("Only the owner can edit this lending item.");
            }
            Timestamp actualUpdatedAt = snapshot.getTimestamp("updatedAt");
            if (!expectedUpdatedAt.equals(actualUpdatedAt)) {
                throw new IllegalStateException(
                        "This lending item changed on another device. Reopen it and try again.");
            }
            Map<String, Object> values = itemValues(input, imageUrl);
            values.put("updatedAt", FieldValue.serverTimestamp());
            transaction.update(itemReference, values);
            return null;
        });
    }

    @NonNull
    public Task<Void> setItemStatus(@NonNull String itemId, @NonNull String nextStatus) {
        FirebaseUser user = currentUser();
        if (firestore == null || user == null
                || !("available".equals(nextStatus) || "withdrawn".equals(nextStatus))) {
            return Tasks.forException(new IllegalArgumentException("The lending status is invalid."));
        }
        DocumentReference reference = firestore.collection(ITEMS).document(itemId);
        return firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(reference);
            if (!snapshot.exists() || !user.getUid().equals(snapshot.getString("ownerId"))) {
                throw new IllegalStateException("Only the owner can change this lending item.");
            }
            transaction.update(reference, twoFieldUpdate(
                    "status", nextStatus,
                    "updatedAt", FieldValue.serverTimestamp()));
            return null;
        });
    }

    @NonNull
    public Task<String> createRequest(
            @NonNull LendingItem item,
            @NonNull String startDate,
            @NonNull String endDate) {
        FirebaseUser user = currentUser();
        if (firestore == null || user == null) {
            return Tasks.forException(new IllegalStateException("Sign in to request an item."));
        }
        if (user.getUid().equals(item.getOwnerId())) {
            return Tasks.forException(new IllegalArgumentException("You cannot borrow your own item."));
        }
        int maxDays = item.getMaxBorrowDays() == null
                ? LendingPolicy.MAX_REQUEST_DAYS
                : item.getMaxBorrowDays().intValue();
        final List<String> dayKeys;
        try {
            dayKeys = LendingPolicy.dateKeys(
                    startDate, endDate, LendingPolicy.todayMalaysia(), maxDays);
        } catch (IllegalArgumentException error) {
            return Tasks.forException(error);
        }
        String requestId = UUID.randomUUID().toString();
        DocumentReference itemReference = firestore.collection(ITEMS).document(item.getId());
        DocumentReference requestReference = firestore.collection(REQUESTS).document(requestId);
        return firestore.runTransaction(transaction -> {
            DocumentSnapshot liveItem = transaction.get(itemReference);
            if (!liveItem.exists() || !"available".equals(liveItem.getString("status"))) {
                throw new IllegalStateException("This lending item is no longer available.");
            }
            String ownerUid = liveItem.getString("ownerId");
            String itemTitle = liveItem.getString("title");
            if (ownerUid == null || itemTitle == null || ownerUid.equals(user.getUid())) {
                throw new IllegalStateException("This lending request is not allowed.");
            }
            Map<String, Object> values = new HashMap<>();
            values.put("itemId", item.getId());
            values.put("itemTitle", itemTitle);
            values.put("ownerUid", ownerUid);
            values.put("borrowerUid", user.getUid());
            values.put("participantIds", Arrays.asList(ownerUid, user.getUid()));
            values.put("startDate", startDate);
            values.put("endDate", endDate);
            values.put("dayKeys", dayKeys);
            values.put("status", "pending");
            values.put("lockToken", "");
            values.put("returnReported", false);
            values.put("createdAt", FieldValue.serverTimestamp());
            values.put("updatedAt", FieldValue.serverTimestamp());
            transaction.set(requestReference, values);
            return requestId;
        });
    }

    @NonNull
    public Subscription observeMyRequests(
            @NonNull SnapshotCallback<List<LendingRequest>> callback) {
        Log.d(DIAGNOSTIC_TAG, "repository listener started");
        FirebaseUser diagnosticUser = null;
        String projectId = "unavailable";
        try {
            diagnosticUser = FirebaseAuth.getInstance().getCurrentUser();
            String configuredProjectId = FirebaseApp.getInstance().getOptions().getProjectId();
            if (configuredProjectId != null && !configuredProjectId.trim().isEmpty()) {
                projectId = configuredProjectId;
            }
        } catch (IllegalStateException unavailable) {
            Log.e(DIAGNOSTIC_TAG,
                    "Firebase diagnostic unavailable message=" + safeErrorMessage(unavailable));
        }
        Log.d(DIAGNOSTIC_TAG,
                "current uid=" + maskUid(diagnosticUser == null ? null : diagnosticUser.getUid()));
        Log.d(DIAGNOSTIC_TAG, "Firebase project ID=" + projectId);
        if (firestore == null || currentUserId() == null) {
            Log.e(DIAGNOSTIC_TAG,
                    "listener error code=UNAUTHENTICATED message=Sign in required");
            callback.onError(new IllegalStateException("Sign in to view lending updates."));
            return Subscription.NONE;
        }
        String uid = currentUserId();
        Log.d(DIAGNOSTIC_TAG, "query started participantIds arrayContains current user");
        ListenerRegistration registration = firestore.collection(REQUESTS)
                .whereArrayContains("participantIds", uid)
                .limit(50)
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
                    if (error != null) {
                        String code = error instanceof FirebaseFirestoreException
                                ? ((FirebaseFirestoreException) error).getCode().name()
                                : error.getClass().getSimpleName();
                        Log.e(DIAGNOSTIC_TAG,
                                "listener error code=" + code
                                        + " message=" + safeErrorMessage(error));
                        callback.onError(error);
                        return;
                    }
                    if (snapshot == null) {
                        Log.e(DIAGNOSTIC_TAG,
                                "listener error code=NULL_SNAPSHOT message=Snapshot unavailable");
                        callback.onError(new IllegalStateException("Lending updates are unavailable."));
                        return;
                    }
                    Log.d(DIAGNOSTIC_TAG, "snapshot received");
                    Log.d(DIAGNOSTIC_TAG, "snapshot size=" + snapshot.size());
                    Log.d(DIAGNOSTIC_TAG,
                            "fromCache=" + snapshot.getMetadata().isFromCache());
                    Log.d(DIAGNOSTIC_TAG,
                            "hasPendingWrites=" + snapshot.getMetadata().hasPendingWrites());
                    List<LendingRequest> requests = new ArrayList<>();
                    for (QueryDocumentSnapshot document : snapshot) {
                        LendingRequest request = mapRequest(document);
                        if (request != null) {
                            requests.add(request);
                        }
                    }
                    requests.sort(Comparator.comparingLong(
                            (LendingRequest value) -> timestampMillis(value.getUpdatedAt()))
                            .reversed());
                    callback.onData(requests, snapshot.getMetadata().isFromCache());
                });
        return registration::remove;
    }

    /** Observes one participant-private request without running the participant inbox query. */
    @NonNull
    public Subscription observeRequest(
            @NonNull String requestId,
            @NonNull SnapshotCallback<LendingRequest> callback) {
        if (firestore == null || currentUserId() == null
                || !LendingPolicy.isSafeSegment(requestId)) {
            callback.onError(new IllegalArgumentException("This lending request is invalid."));
            return Subscription.NONE;
        }
        ListenerRegistration registration = firestore.collection(REQUESTS).document(requestId)
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
                    if (error != null) {
                        callback.onError(error);
                        return;
                    }
                    LendingRequest request = snapshot == null ? null : mapRequest(snapshot);
                    if (snapshot == null || !snapshot.exists() || request == null) {
                        callback.onError(new IllegalStateException(
                                "This lending request is unavailable."));
                        return;
                    }
                    callback.onData(request, snapshot.getMetadata().isFromCache());
                });
        return registration::remove;
    }

    @NonNull
    private static String maskUid(@Nullable String uid) {
        if (uid == null || uid.isEmpty()) {
            return "none";
        }
        if (uid.length() <= 8) {
            return uid.substring(0, Math.min(4, uid.length())) + "...";
        }
        return uid.substring(0, 4) + "..." + uid.substring(uid.length() - 4);
    }

    @NonNull
    private static String safeErrorMessage(@NonNull Exception error) {
        String message = error.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return error.getClass().getSimpleName();
        }
        String safe = message.replaceAll("[\\r\\n]+", " ")
                .replaceAll("[A-Za-z0-9_-]{16,}", "[redacted]")
                .trim();
        return safe.length() <= 160 ? safe : safe.substring(0, 160);
    }

    @NonNull
    public Task<Void> approve(@NonNull String requestId) {
        FirebaseUser user = currentUser();
        if (firestore == null || user == null) {
            return unavailableTask();
        }
        DocumentReference requestReference = firestore.collection(REQUESTS).document(requestId);
        return firestore.runTransaction(transaction -> {
            DocumentSnapshot request = transaction.get(requestReference);
            requireRequestParticipant(request, user.getUid(), true, "pending");
            String itemId = required(request.getString("itemId"), "Lending item is missing.");
            @SuppressWarnings("unchecked")
            List<String> dayKeys = (List<String>) request.get("dayKeys");
            validateStoredDays(dayKeys);
            String token = UUID.randomUUID().toString().replace("-", "");
            List<DocumentReference> lockReferences = new ArrayList<>();
            for (String day : dayKeys) {
                DocumentReference lock = firestore.collection(ITEMS).document(itemId)
                        .collection(BOOKED_DAYS).document(day);
                if (transaction.get(lock).exists()) {
                    throw new IllegalStateException(
                            "One or more requested dates were already booked.");
                }
                lockReferences.add(lock);
            }
            transaction.update(requestReference, threeFieldUpdate(
                    "status", "approved",
                    "lockToken", token,
                    "updatedAt", FieldValue.serverTimestamp()));
            for (int index = 0; index < lockReferences.size(); index++) {
                Map<String, Object> lock = new HashMap<>();
                lock.put("requestId", requestId);
                lock.put("lockToken", token);
                lock.put("date", dayKeys.get(index));
                lock.put("updatedAt", FieldValue.serverTimestamp());
                transaction.set(lockReferences.get(index), lock);
            }
            return null;
        });
    }

    @NonNull
    public Task<Void> reject(@NonNull String requestId) {
        return ownerTransition(requestId, "pending", "rejected", false);
    }

    @NonNull
    public Task<Void> activate(@NonNull String requestId) {
        return ownerTransition(requestId, "approved", "active", false);
    }

    @NonNull
    public Task<Void> reportReturn(@NonNull String requestId) {
        FirebaseUser user = currentUser();
        if (firestore == null || user == null) {
            return unavailableTask();
        }
        DocumentReference reference = firestore.collection(REQUESTS).document(requestId);
        return firestore.runTransaction(transaction -> {
            DocumentSnapshot request = transaction.get(reference);
            requireRequestParticipant(request, user.getUid(), false, "active");
            if (Boolean.TRUE.equals(request.getBoolean("returnReported"))) {
                return null;
            }
            transaction.update(reference, twoFieldUpdate(
                    "returnReported", true,
                    "updatedAt", FieldValue.serverTimestamp()));
            return null;
        });
    }

    @NonNull
    public Task<Void> confirmReturn(@NonNull String requestId) {
        FirebaseUser user = currentUser();
        if (firestore == null || user == null) {
            return unavailableTask();
        }
        DocumentReference reference = firestore.collection(REQUESTS).document(requestId);
        return firestore.runTransaction(transaction -> {
            DocumentSnapshot request = transaction.get(reference);
            requireRequestParticipant(request, user.getUid(), true, "active");
            if (!Boolean.TRUE.equals(request.getBoolean("returnReported"))) {
                throw new IllegalStateException("Wait for the borrower to report the return first.");
            }
            transaction.update(reference, twoFieldUpdate(
                    "status", "returned",
                    "updatedAt", FieldValue.serverTimestamp()));
            return null;
        });
    }

    @NonNull
    public Task<Void> cancel(@NonNull String requestId) {
        FirebaseUser user = currentUser();
        if (firestore == null || user == null) {
            return unavailableTask();
        }
        DocumentReference requestReference = firestore.collection(REQUESTS).document(requestId);
        return firestore.runTransaction(transaction -> {
            DocumentSnapshot request = transaction.get(requestReference);
            requireRequestParticipant(request, user.getUid(), false, null);
            String status = request.getString("status");
            if (!("pending".equals(status) || "approved".equals(status))) {
                throw new IllegalStateException("This request can no longer be cancelled.");
            }
            List<DocumentReference> locks = new ArrayList<>();
            if ("approved".equals(status)) {
                String itemId = required(request.getString("itemId"), "Lending item is missing.");
                String token = required(request.getString("lockToken"), "Booking token is missing.");
                @SuppressWarnings("unchecked")
                List<String> dayKeys = (List<String>) request.get("dayKeys");
                validateStoredDays(dayKeys);
                for (String day : dayKeys) {
                    DocumentReference lock = firestore.collection(ITEMS).document(itemId)
                            .collection(BOOKED_DAYS).document(day);
                    DocumentSnapshot lockSnapshot = transaction.get(lock);
                    if (!lockSnapshot.exists()
                            || !requestId.equals(lockSnapshot.getString("requestId"))
                            || !token.equals(lockSnapshot.getString("lockToken"))) {
                        throw new IllegalStateException(
                                "The booking changed. Reopen it before cancelling.");
                    }
                    locks.add(lock);
                }
            }
            transaction.update(requestReference, twoFieldUpdate(
                    "status", "cancelled",
                    "updatedAt", FieldValue.serverTimestamp()));
            for (DocumentReference lock : locks) {
                transaction.delete(lock);
            }
            return null;
        });
    }

    @NonNull
    public Task<Void> rate(
            @NonNull String requestId,
            int score,
            @Nullable String comment) {
        FirebaseUser user = currentUser();
        if (firestore == null || user == null || score < 1 || score > 5) {
            return Tasks.forException(new IllegalArgumentException("Choose a rating from 1 to 5."));
        }
        String cleanComment = comment == null ? "" : comment.trim();
        if (cleanComment.length() > 500) {
            return Tasks.forException(new IllegalArgumentException(
                    "Rating comment must be 500 characters or fewer."));
        }
        DocumentReference requestReference = firestore.collection(REQUESTS).document(requestId);
        String ratingId = requestId + "_" + user.getUid();
        DocumentReference ratingReference = firestore.collection(RATINGS).document(ratingId);
        return firestore.runTransaction(transaction -> {
            DocumentSnapshot request = transaction.get(requestReference);
            requireRequestParticipant(request, user.getUid(), false, "returned");
            if (transaction.get(ratingReference).exists()) {
                throw new IllegalStateException("This lending request is already rated.");
            }
            Map<String, Object> rating = new HashMap<>();
            rating.put("requestId", requestId);
            rating.put("itemId", required(request.getString("itemId"), "Item is missing."));
            rating.put("raterUid", user.getUid());
            rating.put("recipientUid", required(request.getString("ownerUid"), "Owner is missing."));
            rating.put("score", score);
            rating.put("comment", cleanComment);
            rating.put("createdAt", FieldValue.serverTimestamp());
            transaction.set(ratingReference, rating);
            transaction.update(requestReference, twoFieldUpdate(
                    "status", "rated",
                    "updatedAt", FieldValue.serverTimestamp()));
            return null;
        });
    }

    @NonNull
    public Subscription observeRatings(
            @NonNull String recipientUid,
            @NonNull SnapshotCallback<List<LendingRating>> callback) {
        if (firestore == null || currentUserId() == null) {
            callback.onError(new IllegalStateException("Sign in to view trust ratings."));
            return Subscription.NONE;
        }
        ListenerRegistration registration = firestore.collection(RATINGS)
                .whereEqualTo("recipientUid", recipientUid)
                .limit(100)
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
                    if (error != null) {
                        callback.onError(error);
                        return;
                    }
                    List<LendingRating> ratings = new ArrayList<>();
                    if (snapshot != null) {
                        for (QueryDocumentSnapshot document : snapshot) {
                            LendingRating rating = document.toObject(LendingRating.class);
                            rating.setId(document.getId());
                            ratings.add(rating);
                        }
                    }
                    callback.onData(ratings, snapshot != null && snapshot.getMetadata().isFromCache());
                });
        return registration::remove;
    }

    @NonNull
    private Task<Void> ownerTransition(
            @NonNull String requestId,
            @NonNull String currentStatus,
            @NonNull String nextStatus,
            boolean requireReturnReported) {
        FirebaseUser user = currentUser();
        if (firestore == null || user == null) {
            return unavailableTask();
        }
        DocumentReference reference = firestore.collection(REQUESTS).document(requestId);
        return firestore.runTransaction(transaction -> {
            DocumentSnapshot request = transaction.get(reference);
            requireRequestParticipant(request, user.getUid(), true, currentStatus);
            if (requireReturnReported && !Boolean.TRUE.equals(request.getBoolean("returnReported"))) {
                throw new IllegalStateException("The return has not been reported yet.");
            }
            transaction.update(reference, twoFieldUpdate(
                    "status", nextStatus,
                    "updatedAt", FieldValue.serverTimestamp()));
            return null;
        });
    }

    private static void requireRequestParticipant(
            @NonNull DocumentSnapshot request,
            @NonNull String uid,
            boolean requireOwner,
            @Nullable String requiredStatus) {
        if (!request.exists()) {
            throw new IllegalStateException("This lending request no longer exists.");
        }
        String participant = request.getString(requireOwner ? "ownerUid" : "borrowerUid");
        if (!uid.equals(participant)) {
            throw new IllegalStateException(requireOwner
                    ? "Only the owner can perform this action."
                    : "Only the borrower can perform this action.");
        }
        if (requiredStatus != null && !requiredStatus.equals(request.getString("status"))) {
            throw new IllegalStateException("This lending request changed. Reopen it and try again.");
        }
    }

    private static void validateStoredDays(@Nullable List<String> days) {
        if (days == null || days.isEmpty() || days.size() > LendingPolicy.MAX_REQUEST_DAYS) {
            throw new IllegalStateException("The saved borrowing dates are invalid.");
        }
        for (String day : days) {
            if (day == null || !day.matches("\\d{4}-\\d{2}-\\d{2}")) {
                throw new IllegalStateException("The saved borrowing dates are invalid.");
            }
        }
    }

    @NonNull
    private static String required(@Nullable String value, @NonNull String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    @NonNull
    private static Map<String, Object> itemValues(
            @NonNull NewLendingItem input,
            @Nullable String imageUrl) {
        Map<String, Object> values = new HashMap<>();
        values.put("title", input.getTitle());
        values.put("titleNormalized", input.getTitleNormalized());
        values.put("description", input.getDescription());
        values.put("category", input.getCategory());
        values.put("condition", input.getCondition());
        values.put("pickupMethod", input.getPickupMethod());
        values.put("areaLabel", input.getAreaLabel());
        values.put("maxBorrowDays", input.getMaxBorrowDays());
        values.put("depositMinor", input.getDepositMinor());
        values.put("latitude", input.getLatitude());
        values.put("longitude", input.getLongitude());
        values.put("imageUrl", imageUrl);
        values.put("demoImageKey", DemoImagePolicy.normalize(input.getDemoImageKey()));
        return values;
    }

    @NonNull
    private static Map<String, Object> twoFieldUpdate(
            @NonNull String firstKey,
            @NonNull Object firstValue,
            @NonNull String secondKey,
            @NonNull Object secondValue) {
        Map<String, Object> values = new HashMap<>();
        values.put(firstKey, firstValue);
        values.put(secondKey, secondValue);
        return values;
    }

    @NonNull
    private static Map<String, Object> threeFieldUpdate(
            @NonNull String firstKey,
            @NonNull Object firstValue,
            @NonNull String secondKey,
            @NonNull Object secondValue,
            @NonNull String thirdKey,
            @NonNull Object thirdValue) {
        Map<String, Object> values = twoFieldUpdate(
                firstKey, firstValue, secondKey, secondValue);
        values.put(thirdKey, thirdValue);
        return values;
    }

    @Nullable
    private static LendingItem mapItem(@NonNull DocumentSnapshot document) {
        try {
            LendingItem item = document.toObject(LendingItem.class);
            if (item == null || item.getOwnerId() == null || item.getTitle() == null) {
                return null;
            }
            item.setId(document.getId());
            return item;
        } catch (RuntimeException error) {
            return null;
        }
    }

    @Nullable
    private static LendingRequest mapRequest(@NonNull DocumentSnapshot document) {
        try {
            LendingRequest request = document.toObject(LendingRequest.class);
            if (request == null || request.getOwnerUid() == null
                    || request.getBorrowerUid() == null || request.getStatus() == null) {
                return null;
            }
            request.setId(document.getId());
            return request;
        } catch (RuntimeException error) {
            return null;
        }
    }

    @Nullable
    private FirebaseUser currentUser() {
        return auth == null ? null : auth.getCurrentUser();
    }

    @NonNull
    private static Task<Void> unavailableTask() {
        return Tasks.forException(new IllegalStateException(
                "Firebase setup and sign-in are required for this lending action."));
    }

    private static Long timestampMillis(@Nullable Timestamp value) {
        return value == null ? 0L : value.toDate().getTime();
    }
}
