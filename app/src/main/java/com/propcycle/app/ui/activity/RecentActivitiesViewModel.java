package com.propcycle.app.ui.activity;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.propcycle.app.data.activity.ActivityLogRepository;
import com.propcycle.app.data.activity.ActivityRecord;

import java.util.List;

public final class RecentActivitiesViewModel extends AndroidViewModel {

    private final LiveData<List<ActivityRecord>> activities;

    public RecentActivitiesViewModel(@NonNull Application application) {
        super(application);
        activities = new ActivityLogRepository(application).observeCurrentUser();
    }

    @NonNull
    public LiveData<List<ActivityRecord>> getActivities() {
        return activities;
    }
}
