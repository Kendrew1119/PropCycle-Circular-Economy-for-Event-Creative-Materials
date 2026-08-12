package com.propcycle.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.Toast;
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
            bindNavigation(view, R.id.recent_action, controller, R.id.recentActivitiesFragment);
            bindNavigation(view, R.id.notification_button, controller, R.id.notificationsFragment);
            bind(view, R.id.profile_button,
                    clicked -> ScreenNavigation.navigateAuthenticated(
                            this, R.id.profileFragment, null));
            bindHomeQuickMenu(view, controller);
        } else if (destinationId == R.id.lendResourceFragment) {
            bindNavigation(view, R.id.primary_action, controller, R.id.lendingListFragment);
        } else if (destinationId == R.id.lendingMapFragment) {
            bindNavigation(view, R.id.item_card, controller, R.id.lendingDetailFragment);
            bindNavigation(view, R.id.item_card_secondary, controller, R.id.lendingDetailFragment);
        } else if (destinationId == R.id.lendingListFragment) {
            bindNavigation(view, R.id.item_card, controller, R.id.lendingDetailFragment);
            bindNavigation(view, R.id.item_card_secondary, controller, R.id.lendingDetailFragment);
        } else if (destinationId == R.id.lendingDetailFragment) {
            bind(view, R.id.chat_action,
                    clicked -> Toast.makeText(
                            requireContext(),
                            R.string.lending_chat_deferred,
                            Toast.LENGTH_SHORT).show());
        } else if (destinationId == R.id.profileFragment) {
            bindAccountLabels(view);
            bind(view, R.id.item_card,
                    clicked -> ScreenNavigation.navigateAuthenticated(
                            this, R.id.marketplaceFragment, null));
        }
    }

    private void bindHomeQuickMenu(
            @NonNull View root,
            @NonNull NavController controller) {
        View menuButton = root.findViewById(R.id.menu_button);
        View fanContainer = root.findViewById(R.id.home_menu_fan_container);
        View closeButton = root.findViewById(R.id.home_menu_close);
        View marketAction = root.findViewById(R.id.market_action);
        View shareAction = root.findViewById(R.id.lend_action);
        View mapAction = root.findViewById(R.id.map_action);
        if (menuButton == null
                || fanContainer == null
                || closeButton == null
                || marketAction == null
                || shareAction == null
                || mapAction == null) {
            return;
        }

        float fanSize = 252f * getResources().getDisplayMetrics().density;
        fanContainer.setPivotX(fanSize);
        fanContainer.setPivotY(fanSize);
        fanContainer.setTag(Boolean.FALSE);
        fanContainer.setVisibility(View.GONE);
        fanContainer.setImportantForAccessibility(
                View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        menuButton.setContentDescription("Open quick navigation");

        menuButton.setOnClickListener(clicked -> setHomeMenuExpanded(
                menuButton,
                fanContainer,
                !Boolean.TRUE.equals(fanContainer.getTag())));
        closeButton.setOnClickListener(clicked ->
                setHomeMenuExpanded(menuButton, fanContainer, false));
        marketAction.setOnClickListener(clicked -> {
            setHomeMenuExpanded(menuButton, fanContainer, false);
            ScreenNavigation.navigateAuthenticated(this, R.id.marketplaceFragment, null);
        });
        shareAction.setOnClickListener(clicked -> {
            setHomeMenuExpanded(menuButton, fanContainer, false);
            controller.navigate(R.id.lendResourceFragment);
        });
        mapAction.setOnClickListener(clicked -> {
            setHomeMenuExpanded(menuButton, fanContainer, false);
            controller.navigate(R.id.lendingMapFragment);
        });
    }

    private static void setHomeMenuExpanded(
            @NonNull View menuButton,
            @NonNull View fanContainer,
            boolean expanded) {
        fanContainer.animate().cancel();
        fanContainer.setTag(expanded);
        menuButton.setContentDescription(
                expanded ? "Close quick navigation" : "Open quick navigation");

        if (expanded) {
            fanContainer.setImportantForAccessibility(
                    View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            fanContainer.setAlpha(0f);
            fanContainer.setScaleX(0.76f);
            fanContainer.setScaleY(0.76f);
            fanContainer.setVisibility(View.VISIBLE);
            fanContainer.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(220L)
                    .setInterpolator(new OvershootInterpolator(0.7f))
                    .start();
            return;
        }

        fanContainer.setImportantForAccessibility(
                View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        if (fanContainer.getVisibility() != View.VISIBLE) {
            return;
        }
        fanContainer.animate()
                .alpha(0f)
                .scaleX(0.82f)
                .scaleY(0.82f)
                .setDuration(150L)
                .withEndAction(() -> {
                    if (!Boolean.TRUE.equals(fanContainer.getTag())) {
                        fanContainer.setVisibility(View.GONE);
                    }
                })
                .start();
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
            avatar.setText(displayName.substring(0, 1).toUpperCase(java.util.Locale.ROOT));
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
