package com.propcycle.app.ui.common;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.propcycle.app.R;

/** Starts a resource journey with AI assistance while retaining a reliable manual fallback. */
public final class ResourceCreationFlow {

    public static final String TARGET_MARKETPLACE = "marketplace";
    public static final String TARGET_LENDING = "lending";

    private ResourceCreationFlow() {
    }

    public static void show(@NonNull Fragment fragment, @NonNull String target) {
        boolean lending = TARGET_LENDING.equals(target);
        String title = lending ? "Share a lending item" : "Create a marketplace listing";
        String message = "For the quickest setup, take or choose a photo and let the AI prepare "
                + "an editable draft. You will review every detail before publishing.";
        new MaterialAlertDialogBuilder(fragment.requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Use photo and AI", (dialog, which) -> {
                    Bundle arguments = new Bundle();
                    arguments.putString("handoffTarget", target);
                    ScreenNavigation.navigateAuthenticated(
                            fragment, R.id.scannerFragment, arguments);
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Enter manually", (dialog, which) ->
                        ScreenNavigation.navigateAuthenticated(
                                fragment,
                                lending ? R.id.lendResourceFragment : R.id.createListingFragment,
                                null))
                .show();
    }
}
