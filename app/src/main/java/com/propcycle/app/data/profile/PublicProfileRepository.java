package com.propcycle.app.data.profile;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.propcycle.app.core.firebase.FirebaseEnvironment;

/** Authenticated reads of one public profile document; Firestore list access stays disabled. */
public final class PublicProfileRepository {

    private final FirebaseFirestore firestore;

    public PublicProfileRepository(@NonNull Context context) {
        firestore = FirebaseEnvironment.firestore(context.getApplicationContext());
    }

    @NonNull
    public Task<PublicProfile> get(@NonNull String userId) {
        String cleanUserId = userId.trim();
        if (firestore == null) {
            return Tasks.forException(new IllegalStateException(FirebaseEnvironment.SETUP_MESSAGE));
        }
        if (!isSafeSegment(cleanUserId)) {
            return Tasks.forException(new IllegalArgumentException("This user profile is invalid."));
        }
        return firestore.collection("users").document(cleanUserId).get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        Exception error = task.getException();
                        throw error == null
                                ? new IllegalStateException("This public profile could not be loaded.")
                                : error;
                    }
                    if (task.getResult() == null || !task.getResult().exists()) {
                        throw new IllegalStateException("This public profile is unavailable.");
                    }
                    String name = task.getResult().getString("displayName");
                    String avatarKey = task.getResult().getString("avatarKey");
                    return new PublicProfile(
                            cleanUserId,
                            name == null ? "PropCycle Member" : name,
                            ProfileAvatarPolicy.normalized(avatarKey));
                });
    }

    private static boolean isSafeSegment(@NonNull String value) {
        return value.length() >= 1
                && value.length() <= 128
                && value.matches("^[A-Za-z0-9_-]+$");
    }
}
