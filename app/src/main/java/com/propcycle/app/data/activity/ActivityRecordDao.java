package com.propcycle.app.data.activity;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ActivityRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(@NonNull ActivityRecord record);

    @Query("SELECT * FROM activity_records WHERE ownerUid = :ownerUid "
            + "ORDER BY occurredAt DESC LIMIT :limit")
    LiveData<List<ActivityRecord>> observeForOwner(@NonNull String ownerUid, int limit);

    @Query("DELETE FROM activity_records WHERE ownerUid = :ownerUid")
    void clearOwner(@NonNull String ownerUid);
}
