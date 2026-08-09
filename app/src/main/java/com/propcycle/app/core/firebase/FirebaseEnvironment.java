package com.propcycle.app.core.firebase;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.MemoryCacheSettings;
import com.propcycle.app.BuildConfig;

/**
 * Owns safe Firebase startup so a developer can build the app before receiving
 * the ignored {@code app/google-services.json} file.
 */
public final class FirebaseEnvironment {

    public static final String SETUP_MESSAGE =
            "Firebase setup is required. Add app/google-services.json, then enable "
                    + "Email/Password Authentication and Cloud Firestore.";

    private static final Object LOCK = new Object();
    private static boolean servicesConfigured;

    private FirebaseEnvironment() {
    }

    @Nullable
    public static FirebaseApp initialize(Context context) {
        FirebaseApp app;
        try {
            app = FirebaseApp.getInstance();
        } catch (IllegalStateException missingDefaultApp) {
            app = FirebaseApp.initializeApp(context.getApplicationContext());
        }

        if (app != null) {
            configureServices(app);
        }
        return app;
    }

    public static boolean isConfigured(Context context) {
        return initialize(context) != null;
    }

    @Nullable
    public static FirebaseAuth auth(Context context) {
        FirebaseApp app = initialize(context);
        return app == null ? null : FirebaseAuth.getInstance(app);
    }

    @Nullable
    public static FirebaseFirestore firestore(Context context) {
        FirebaseApp app = initialize(context);
        return app == null ? null : FirebaseFirestore.getInstance(app);
    }

    private static void configureServices(FirebaseApp app) {
        synchronized (LOCK) {
            if (servicesConfigured) {
                return;
            }

            FirebaseFirestore firestore = FirebaseFirestore.getInstance(app);
            if (BuildConfig.DEBUG && BuildConfig.USE_FIREBASE_EMULATORS) {
                FirebaseAuth.getInstance(app).useEmulator("10.0.2.2", 9099);
                firestore.useEmulator("10.0.2.2", 8080);
            }

            // Firestore's SDK cache is process-wide rather than Firebase-user scoped.
            // Memory-only data prevents one account's private chat cache surviving logout.
            firestore.setFirestoreSettings(new FirebaseFirestoreSettings.Builder(
                    firestore.getFirestoreSettings())
                    .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                    .build());
            servicesConfigured = true;
        }
    }
}
