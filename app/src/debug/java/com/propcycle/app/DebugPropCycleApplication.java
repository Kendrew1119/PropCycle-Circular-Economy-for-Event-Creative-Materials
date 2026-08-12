package com.propcycle.app;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

/** Uses a per-developer App Check token. This class is absent from release builds. */
public final class DebugPropCycleApplication extends PropCycleApplication {

    @Override
    protected void installAppCheckProvider(FirebaseApp app) {
        FirebaseAppCheck.getInstance(app).installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance());
    }
}
