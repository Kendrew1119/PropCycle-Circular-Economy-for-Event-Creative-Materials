package com.propcycle.app;

import android.app.Application;

import androidx.annotation.Nullable;

import com.google.firebase.FirebaseApp;
import com.propcycle.app.core.firebase.FirebaseEnvironment;

/** Initializes build-specific App Check before the app requests Firebase services. */
public class PropCycleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp app = existingOrInitializeFirebase();
        if (app != null) {
            installAppCheckProvider(app);
        }
        FirebaseEnvironment.initialize(this);
        
        android.content.SharedPreferences prefs = getSharedPreferences("theme_prefs", android.content.Context.MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_theme", false);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                isDark ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES 
                     : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
    }

    /** Debug and release source sets install different attestation providers. */
    protected void installAppCheckProvider(FirebaseApp app) {
        // No provider in the base source set. Debug and release variants override this method.
    }

    @Nullable
    private FirebaseApp existingOrInitializeFirebase() {
        try {
            return FirebaseApp.getInstance();
        } catch (IllegalStateException missingDefaultApp) {
            return FirebaseApp.initializeApp(this);
        }
    }
}
