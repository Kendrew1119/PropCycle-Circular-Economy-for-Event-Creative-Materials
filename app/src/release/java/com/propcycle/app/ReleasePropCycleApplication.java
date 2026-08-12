package com.propcycle.app;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

/** Uses Play Integrity in production. Firebase Console registration is still required. */
public final class ReleasePropCycleApplication extends PropCycleApplication {

    @Override
    protected void installAppCheckProvider(FirebaseApp app) {
        FirebaseAppCheck.getInstance(app).installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance());
    }
}
