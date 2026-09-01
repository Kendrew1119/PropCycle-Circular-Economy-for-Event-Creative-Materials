package com.propcycle.app.data.profile;

import androidx.annotation.NonNull;

/** Small public identity used by profile links and user avatars. */
public final class PublicProfile {

    private final String userId;
    private final String displayName;
    private final String avatarKey;

    public PublicProfile(
            @NonNull String userId,
            @NonNull String displayName,
            @NonNull String avatarKey) {
        this.userId = userId;
        this.displayName = displayName.trim().isEmpty()
                ? "PropCycle Member" : displayName.trim();
        this.avatarKey = ProfileAvatarPolicy.normalized(avatarKey);
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    @NonNull
    public String getAvatarKey() {
        return avatarKey;
    }
}
