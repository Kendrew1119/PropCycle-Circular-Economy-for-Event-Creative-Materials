package com.propcycle.app.core.firebase;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.MemoryCacheSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.propcycle.app.BuildConfig;

/**
 * Owns safe Firebase startup so a developer can build the app before receiving
 * the ignored {@code app/google-services.json} file.
 */
public final class FirebaseEnvironment {

    public static final String SETUP_MESSAGE =
            "Firebase setup is required. Add app/google-services.json, then enable "
                    + "Email/Password Authentication and Cloud Firestore.";
    public static final String STORAGE_SETUP_MESSAGE =
            "Marketplace photo setup is required. Enable the default Firebase Storage "
                    + "bucket, download the latest app/google-services.json, and deploy "
                    + "the reviewed Storage Rules.";

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

    @Nullable
    public static FirebaseStorage storage(Context context) {
        FirebaseApp app = initialize(context);
        if (app == null || app.getOptions().getStorageBucket() == null
                || app.getOptions().getStorageBucket().trim().isEmpty()) {
            return null;
        }
        try {
            return FirebaseStorage.getInstance(app);
        } catch (IllegalArgumentException | IllegalStateException unavailable) {
            return null;
        }
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
                if (app.getOptions().getStorageBucket() != null
                        && !app.getOptions().getStorageBucket().trim().isEmpty()) {
                    FirebaseStorage.getInstance(app).useEmulator("10.0.2.2", 9199);
                }
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
