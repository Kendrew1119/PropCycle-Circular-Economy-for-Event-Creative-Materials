package com.propcycle.app.ui.common;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

import com.propcycle.app.R;
import com.propcycle.app.data.profile.ProfileAvatarPolicy;

import java.util.Locale;

/** Renders the same allowlisted avatar on profile, header, and chat surfaces. */
public final class ProfileAvatarRenderer {

    private ProfileAvatarRenderer() {
    }

    public static void render(
            @NonNull TextView target,
            @NonNull String avatarKey,
            @NonNull String displayName) {
        String normalized = ProfileAvatarPolicy.normalized(avatarKey);
        if (ProfileAvatarPolicy.DEFAULT.equals(normalized)) {
            target.setCompoundDrawables(null, null, null, null);
            String cleanName = displayName.trim();
            target.setText(cleanName.isEmpty()
                    ? "P"
                    : cleanName.substring(0, 1).toUpperCase(Locale.ROOT));
            return;
        }
        Drawable icon = AppCompatResources.getDrawable(
                target.getContext(), drawableFor(normalized));
        target.setText("");
        target.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
    }

    public static void render(
            @NonNull ImageButton target,
            @NonNull String avatarKey) {
        target.setImageResource(drawableFor(ProfileAvatarPolicy.normalized(avatarKey)));
    }

    /** Renders a built-in avatar as a centered image without relying on text metrics. */
    public static void render(
            @NonNull ImageView target,
            @NonNull String avatarKey) {
        target.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        target.setImageResource(drawableFor(ProfileAvatarPolicy.normalized(avatarKey)));
    }

    /** Uses separate text and image layers so profile avatars remain optically centered. */
    public static void render(
            @NonNull TextView initialTarget,
            @NonNull ImageView iconTarget,
            @NonNull String avatarKey,
            @NonNull String displayName) {
        String normalized = ProfileAvatarPolicy.normalized(avatarKey);
        if (ProfileAvatarPolicy.DEFAULT.equals(normalized)) {
            iconTarget.setVisibility(View.GONE);
            initialTarget.setVisibility(View.VISIBLE);
            String cleanName = displayName.trim();
            initialTarget.setText(cleanName.isEmpty()
                    ? "P"
                    : cleanName.substring(0, 1).toUpperCase(Locale.ROOT));
            return;
        }
        initialTarget.setText("");
        initialTarget.setVisibility(View.GONE);
        render(iconTarget, normalized);
        iconTarget.setVisibility(View.VISIBLE);
    }

    @DrawableRes
    private static int drawableFor(@NonNull String avatarKey) {
        return switch (avatarKey) {
            case ProfileAvatarPolicy.LEAF -> R.drawable.ic_welcome_leaf;
            case ProfileAvatarPolicy.RECYCLE -> R.drawable.ic_welcome_recycle;
            case ProfileAvatarPolicy.HEART -> R.drawable.ic_welcome_heart;
            case ProfileAvatarPolicy.PACKAGE -> R.drawable.ic_welcome_package;
            case ProfileAvatarPolicy.SPARKLE -> R.drawable.ic_welcome_sparkle;
            default -> R.drawable.ic_person;
        };
    }
}
