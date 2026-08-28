package com.propcycle.app.data.activity;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.propcycle.app.core.firebase.FirebaseEnvironment;
import com.propcycle.app.data.scanner.ScanAnalysis;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Records and observes honest, account-scoped recent activity on the current device. */
public final class ActivityLogRepository {

    public static final String TYPE_SCAN = "scan";
    public static final String TYPE_MARKETPLACE_LISTED = "marketplace_listed";
    public static final String TYPE_MARKETPLACE_UPDATED = "marketplace_updated";
    public static final String TYPE_MARKETPLACE_STATUS = "marketplace_status";
    public static final String TYPE_LENDING_LISTED = "lending_listed";
    public static final String TYPE_LENDING_UPDATED = "lending_updated";
    public static final String TYPE_LENDING_REQUEST = "lending_request";
    public static final String TYPE_LENDING_STATUS = "lending_status";
    public static final String TYPE_RECYCLE_SEARCH = "recycle_search";

    public static final String DESTINATION_AI_RESULT = "ai_result";
    public static final String DESTINATION_MARKETPLACE = "marketplace";
    public static final String DESTINATION_LENDING_ITEM = "lending_item";
    public static final String DESTINATION_LENDING_REQUESTS = "lending_requests";
    public static final String DESTINATION_RECYCLE = "recycle";

    private static final int MAX_HISTORY = 100;

    private final Context applicationContext;
    private final ActivityRecordDao dao;

    public ActivityLogRepository(@NonNull Context context) {
        applicationContext = context.getApplicationContext();
        dao = PropCycleDatabase.get(applicationContext).activityRecordDao();
    }

    @NonNull
    public LiveData<List<ActivityRecord>> observeCurrentUser() {
        String uid = currentUid();
        if (uid == null) {
            MutableLiveData<List<ActivityRecord>> empty = new MutableLiveData<>();
            empty.setValue(Collections.emptyList());
            return empty;
        }
        return dao.observeForOwner(uid, MAX_HISTORY);
    }

    public void recordScan(@NonNull ScanAnalysis analysis) {
        record(
                TYPE_SCAN,
                analysis.getItemName() + " scanned",
                "Identified as " + analysis.getMaterial() + ". Review the AI result before acting.",
                DESTINATION_AI_RESULT,
                analysis.toJson());
    }

    public void record(
            @NonNull String type,
            @NonNull String title,
            @NonNull String detail,
            @NonNull String destination,
            @Nullable String payload) {
        String uid = currentUid();
        if (uid == null) {
            return;
        }
        ActivityRecord record = new ActivityRecord(
                UUID.randomUUID().toString(),
                uid,
                bounded(type, 40),
                bounded(title, 120),
                bounded(detail, 500),
                bounded(destination, 40),
                bounded(payload, 12_000),
                System.currentTimeMillis());
        PropCycleDatabase.WRITE_EXECUTOR.execute(() -> dao.insert(record));
    }

    public void clearCurrentUser() {
        String uid = currentUid();
        if (uid != null) {
            PropCycleDatabase.WRITE_EXECUTOR.execute(() -> dao.clearOwner(uid));
        }
    }

    @Nullable
    private String currentUid() {
        FirebaseAuth auth = FirebaseEnvironment.auth(applicationContext);
        FirebaseUser user = auth == null ? null : auth.getCurrentUser();
        return user == null ? null : user.getUid();
    }

    @NonNull
    private static String bounded(@Nullable String value, int maximum) {
        String clean = value == null ? "" : value.trim();
        return clean.length() <= maximum ? clean : clean.substring(0, maximum);
    }
}
