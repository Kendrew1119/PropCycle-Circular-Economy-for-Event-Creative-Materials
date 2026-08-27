package com.propcycle.app.ui.common;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

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

/** Shared back, menu, authenticated-routing, and logout behavior. */
public final class ScreenNavigation {

    private ScreenNavigation() {
    }

    public static void bindChrome(@NonNull Fragment fragment, @NonNull View root) {
        bind(root, R.id.menu_button, anchor -> showMenu(fragment, anchor));
        bind(root, R.id.back_button,
                ignored -> NavHostFragment.findNavController(fragment).popBackStack());
        bind(root, R.id.close_button,
                ignored -> NavHostFragment.findNavController(fragment).popBackStack());
    }

    public static void showMenu(@NonNull Fragment fragment, @NonNull View anchor) {
        PopupMenu popup = new PopupMenu(fragment.requireContext(), anchor);
        popup.inflate(R.menu.menu_ui_review);
        FirebaseAuth auth = FirebaseEnvironment.auth(fragment.requireContext());
        MenuItem logout = popup.getMenu().findItem(R.id.menu_logout);
        if (logout != null) {
            logout.setVisible(auth != null && auth.getCurrentUser() != null);
        }
        popup.setOnMenuItemClickListener(item -> navigateFromMenu(fragment, item));
        popup.show();
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

    private static boolean navigateFromMenu(
            @NonNull Fragment fragment,
            @NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_logout) {
            FirebaseAuth auth = FirebaseEnvironment.auth(fragment.requireContext());
            if (auth != null) {
                auth.signOut();
            }
            navigateClearingBackStack(fragment, R.id.loginFragment);
            return true;
        }

        @IdRes int destination;
        if (itemId == R.id.menu_home) {
            destination = R.id.homeFragment;
        } else if (itemId == R.id.menu_marketplace) {
            destination = R.id.marketplaceFragment;
        } else if (itemId == R.id.menu_create_listing) {
            destination = R.id.createListingFragment;
        } else if (itemId == R.id.menu_lending) {
            destination = R.id.lendingListFragment;
        } else if (itemId == R.id.menu_lending_map) {
            destination = R.id.lendingMapFragment;
        } else if (itemId == R.id.menu_recycle) {
            destination = R.id.recycleCenterFragment;
        } else if (itemId == R.id.menu_messages) {
            destination = R.id.messagesFragment;
        } else if (itemId == R.id.menu_notifications) {
            destination = R.id.notificationsFragment;
        } else if (itemId == R.id.menu_profile) {
            destination = R.id.profileFragment;
        } else if (itemId == R.id.menu_settings) {
            destination = R.id.settingsFragment;
        } else {
            return false;
        }

        if (requiresAuthentication(destination)) {
            FirebaseAuth auth = FirebaseEnvironment.auth(fragment.requireContext());
            if (auth == null || auth.getCurrentUser() == null) {
                navigateClearingBackStack(fragment, R.id.loginFragment);
            } else {
                navigateTopLevel(NavHostFragment.findNavController(fragment), destination);
            }
        } else {
            navigateTopLevel(NavHostFragment.findNavController(fragment), destination);
        }
        return true;
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
}
