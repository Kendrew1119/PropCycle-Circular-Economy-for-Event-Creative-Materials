package com.propcycle.app.core.theme;

import android.app.UiModeManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

public final class AppTheme {

    private AppTheme() {
    }

    public static boolean isDark(@NonNull Context context) {
        return context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
                .getBoolean("dark_theme", false);
    }

    public static void applySaved(@NonNull Context context) {
        apply(context, isDark(context));
    }

    public static void setDark(@NonNull Context context, boolean dark) {
        context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("dark_theme", dark)
                .apply();
        apply(context, dark);
    }

    private static void apply(@NonNull Context context, boolean dark) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            UiModeManager manager = context.getApplicationContext()
                    .getSystemService(UiModeManager.class);
            if (manager != null) {
                manager.setApplicationNightMode(
                        dark ? UiModeManager.MODE_NIGHT_YES : UiModeManager.MODE_NIGHT_NO);
            }
        }
        AppCompatDelegate.setDefaultNightMode(
                dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }
}
