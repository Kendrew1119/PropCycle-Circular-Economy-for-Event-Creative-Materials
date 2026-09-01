package com.propcycle.app.data.profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Locked built-in avatars that are safe to store in public user profiles. */
public final class ProfileAvatarPolicy {

    public static final String DEFAULT = "default";
    public static final String LEAF = "leaf";
    public static final String RECYCLE = "recycle";
    public static final String HEART = "heart";
    public static final String PACKAGE = "package";
    public static final String SPARKLE = "sparkle";

    private static final List<String> KEYS = Collections.unmodifiableList(Arrays.asList(
            DEFAULT,
            LEAF,
            RECYCLE,
            HEART,
            PACKAGE,
            SPARKLE));
    private static final List<String> LABELS = Collections.unmodifiableList(Arrays.asList(
            "Name initial",
            "Eco leaf",
            "Recycle",
            "Community heart",
            "Reuse package",
            "Sparkle"));

    private ProfileAvatarPolicy() {
    }

    public static boolean isValid(@Nullable String value) {
        return value != null && KEYS.contains(value.trim());
    }

    @NonNull
    public static String normalized(@Nullable String value) {
        return isValid(value) ? value.trim() : DEFAULT;
    }

    @NonNull
    public static List<String> keys() {
        return KEYS;
    }

    @NonNull
    public static List<String> labels() {
        return LABELS;
    }
}
