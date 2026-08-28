package com.propcycle.app.data.activity;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Device-local database. Rows are always queried by the signed-in account UID. */
@Database(entities = {ActivityRecord.class}, version = 1, exportSchema = true)
public abstract class PropCycleDatabase extends RoomDatabase {

    private static volatile PropCycleDatabase instance;
    static final ExecutorService WRITE_EXECUTOR = Executors.newSingleThreadExecutor();

    public abstract ActivityRecordDao activityRecordDao();

    @NonNull
    public static PropCycleDatabase get(@NonNull Context context) {
        PropCycleDatabase current = instance;
        if (current != null) {
            return current;
        }
        synchronized (PropCycleDatabase.class) {
            if (instance == null) {
                instance = Room.databaseBuilder(
                                context.getApplicationContext(),
                                PropCycleDatabase.class,
                                "propcycle-local.db")
                        .build();
            }
            return instance;
        }
    }
}
