package com.propcycle.app.ui.common;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.propcycle.app.R;
import com.propcycle.app.core.firebase.FirebaseEnvironment;

/** Shared local back controls and authenticated routing behavior. */
public final class ScreenNavigation {

    private ScreenNavigation() {
    }

    public static void bindChrome(@NonNull Fragment fragment, @NonNull View root) {
        hide(root, R.id.menu_button);
        bind(root, R.id.back_button,
                ignored -> NavHostFragment.findNavController(fragment).popBackStack());
        bind(root, R.id.close_button,
                ignored -> NavHostFragment.findNavController(fragment).popBackStack());
    }

    public static boolean navigateAuthenticated(
            @NonNull Fragment fragment,
            @IdRes int destination,
            @Nullable Bundle arguments) {
        FirebaseAuth auth = FirebaseEnvironment.auth(fragment.requireContext());
        if (auth == null || auth.getCurrentUser() == null) {
            navigateClearingBackStack(fragment, R.id.loginFragment);
            return false;
        }
        navigate(fragment, destination, arguments);
        return true;
    }

    public static void navigate(
            @NonNull Fragment fragment,
            @IdRes int destination,
            @Nullable Bundle arguments) {
        NavController controller = NavHostFragment.findNavController(fragment);
        NavDestination current = controller.getCurrentDestination();
        if (current == null || current.getId() != destination || arguments != null) {
            controller.navigate(destination, arguments);
        }
    }

    public static boolean navigateTopLevel(
            @NonNull Context context,
            @NonNull NavController controller,
            @IdRes int destination) {
        if (requiresAuthentication(destination)) {
            FirebaseAuth auth = FirebaseEnvironment.auth(context);
            if (auth == null || auth.getCurrentUser() == null) {
                navigateClearingBackStack(controller, R.id.loginFragment);
                return false;
            }
        }
        navigateTopLevel(controller, destination);
        return true;
    }

    public static void navigateClearingBackStack(
            @NonNull Fragment fragment,
            @IdRes int destination) {
        navigateClearingBackStack(
                NavHostFragment.findNavController(fragment), destination);
    }

    private static void navigateTopLevel(
            @NonNull NavController controller,
            @IdRes int destination) {
        NavDestination current = controller.getCurrentDestination();
        if (current != null && current.getId() == destination) {
            return;
        }
        if (!controller.popBackStack(destination, false)) {
            NavOptions options = new NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .build();
            controller.navigate(destination, null, options);
        }
    }

    private static void navigateClearingBackStack(
            @NonNull NavController controller,
            @IdRes int destination) {
        NavOptions options = new NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build();
        controller.navigate(destination, null, options);
    }

    private static boolean requiresAuthentication(@IdRes int destination) {
        return destination == R.id.marketplaceFragment
                || destination == R.id.createListingFragment
                || destination == R.id.scannerFragment
                || destination == R.id.messagesFragment
                || destination == R.id.lendResourceFragment
                || destination == R.id.lendingMapFragment
                || destination == R.id.lendingListFragment
                || destination == R.id.lendingDetailFragment
                || destination == R.id.notificationsFragment
                || destination == R.id.profileFragment;
    }

    private static void bind(
            @NonNull View root,
            @IdRes int viewId,
            @NonNull View.OnClickListener listener) {
        View target = root.findViewById(viewId);
        if (target != null) {
            target.setOnClickListener(listener);
        }
    }

    private static void hide(@NonNull View root, @IdRes int viewId) {
        View target = root.findViewById(viewId);
        if (target != null) {
            target.setVisibility(View.GONE);
            target.setOnClickListener(null);
        }
    }
}
