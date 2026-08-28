package com.propcycle.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.propcycle.app.R;
import com.propcycle.app.core.firebase.FirebaseEnvironment;
import com.propcycle.app.ui.common.ScreenNavigation;

import java.util.Locale;

/**
 * Reusable host for proposal screens that remain static. Firebase-backed account,
 * marketplace, chat, and the Phase 2B scanner use dedicated feature Fragments.
 */
public final class WireframeFragment extends Fragment {

    private static final String ARG_LAYOUT = "layoutResId";
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        Bundle arguments = requireArguments();
        @LayoutRes int layoutResId = arguments.getInt(ARG_LAYOUT);
        if (layoutResId == 0) {
            throw new IllegalStateException("A proposal layout resource is required");
        }
        return inflater.inflate(layoutResId, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NavController controller = NavHostFragment.findNavController(this);
        if (controller.getCurrentDestination() == null) {
            return;
        }

        ScreenNavigation.bindChrome(this, view);

        int destinationId = controller.getCurrentDestination().getId();
        if (destinationId == R.id.welcomeFragment) {
            bindNavigation(view, R.id.primary_action, controller, R.id.loginFragment);
        } else if (destinationId == R.id.homeFragment) {
            bindAccountLabels(view);
            bindNavigation(view, R.id.scanner_card, controller, R.id.scannerFragment);
            bindNavigation(
                    view, R.id.home_recycle_action, controller, R.id.recycleCenterFragment);
            bind(view, R.id.home_create_listing_action,
                    clicked -> ScreenNavigation.navigateAuthenticated(
                            this, R.id.createListingFragment, null));
            bindNavigation(
                    view, R.id.home_lend_resource_action, controller, R.id.lendResourceFragment);
            bindNavigation(view, R.id.recent_action, controller, R.id.recentActivitiesFragment);
        } else if (destinationId == R.id.profileFragment) {
            bindAccountLabels(view);
            bind(view, R.id.item_card,
                    clicked -> ScreenNavigation.navigateAuthenticated(
                            this, R.id.marketplaceFragment, null));
            bind(view, R.id.logout_action, clicked -> {
                FirebaseAuth auth = FirebaseEnvironment.auth(requireContext());
                if (auth != null) {
                    auth.signOut();
                }
                ScreenNavigation.navigateClearingBackStack(this, R.id.loginFragment);
            });
        }
    }

    private void bindAccountLabels(@NonNull View root) {
        FirebaseAuth auth = FirebaseEnvironment.auth(requireContext());
        FirebaseUser user = auth == null ? null : auth.getCurrentUser();
        if (user == null) {
            return;
        }

        String displayName = user.getDisplayName();
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = user.getEmail();
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = "PropCycle Member";
        }
        displayName = displayName.trim();

        TextView greeting = root.findViewById(R.id.home_greeting);
        if (greeting != null) {
            greeting.setText(getString(R.string.home_greeting, displayName));
        }
        TextView profileName = root.findViewById(R.id.profile_name);
        if (profileName != null) {
            profileName.setText(displayName);
        }
        TextView avatar = root.findViewById(R.id.profile_avatar_initial);
        if (avatar != null) {
            avatar.setText(displayName.substring(0, 1).toUpperCase(Locale.ROOT));
        }
    }

    private static void bindNavigation(
            @NonNull View root,
            @IdRes int viewId,
            @NonNull NavController controller,
            @IdRes int destination) {
        bind(root, viewId, clicked -> controller.navigate(destination));
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
