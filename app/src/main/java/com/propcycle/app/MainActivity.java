package com.propcycle.app;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.badge.BadgeDrawable;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.propcycle.app.core.firebase.FirebaseEnvironment;
import com.propcycle.app.data.profile.ProfileAvatarPolicy;
import com.propcycle.app.ui.common.ScreenNavigation;
import com.propcycle.app.ui.common.ProfileAvatarRenderer;
import com.propcycle.app.ui.common.UnreadBadgeManager;

/** Hosts the proposal UI, restores an authenticated session, and supports debug visual QA. */
public final class MainActivity extends AppCompatActivity {

    public static final String EXTRA_SCREEN = "screen";
    @Nullable private ListenerRegistration headerProfileRegistration;
    private String headerProfileUserId = "";
    private UnreadBadgeManager unreadBadgeManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.app_root);
        View appContent = findViewById(R.id.app_content);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        View statusBarBackground = findViewById(R.id.status_bar_background);
        View systemNavigationBackground = findViewById(R.id.system_navigation_background);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            int safeBottomInset = Math.max(systemBars.bottom, ime.bottom);
            boolean navigationVisible = bottomNavigation.getVisibility() == View.VISIBLE;
            appContent.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    navigationVisible ? 0 : safeBottomInset);

            ViewGroup.MarginLayoutParams bottomNavigationLayoutParams =
                    (ViewGroup.MarginLayoutParams) bottomNavigation.getLayoutParams();
            int bottomNavigationMargin = navigationVisible ? safeBottomInset : 0;
            if (bottomNavigationLayoutParams.bottomMargin != bottomNavigationMargin) {
                bottomNavigationLayoutParams.bottomMargin = bottomNavigationMargin;
                bottomNavigation.setLayoutParams(bottomNavigationLayoutParams);
            }

            ViewGroup.LayoutParams statusBarLayoutParams = statusBarBackground.getLayoutParams();
            if (statusBarLayoutParams.height != systemBars.top) {
                statusBarLayoutParams.height = systemBars.top;
                statusBarBackground.setLayoutParams(statusBarLayoutParams);
            }
            ViewGroup.LayoutParams navigationLayoutParams =
                    systemNavigationBackground.getLayoutParams();
            if (navigationLayoutParams.height != systemBars.bottom) {
                navigationLayoutParams.height = systemBars.bottom;
                systemNavigationBackground.setLayoutParams(navigationLayoutParams);
            }
            return windowInsets;
        });
        unreadBadgeManager = new UnreadBadgeManager(this, this::renderUnreadBadges);
        bindNavigationChrome();

        if (savedInstanceState == null) {
            String requestedScreen = BuildConfig.DEBUG
                    ? getIntent().getStringExtra(EXTRA_SCREEN)
                    : null;
            @IdRes int destination = destinationFor(requestedScreen);
            boolean restoredSession = false;
            if (requestedScreen == null) {
                FirebaseAuth auth = FirebaseEnvironment.auth(this);
                if (auth != null && auth.getCurrentUser() != null) {
                    destination = R.id.homeFragment;
                    restoredSession = true;
                }
            }
            if (destination != R.id.welcomeFragment) {
                @IdRes int initialDestination = destination;
                boolean clearWelcome = restoredSession;
                root.post(() -> {
                    if (clearWelcome) {
                        NavOptions options = new NavOptions.Builder()
                                .setPopUpTo(R.id.welcomeFragment, true)
                                .build();
                        navController().navigate(initialDestination, null, options);
                    } else {
                        navController().navigate(initialDestination);
                    }
                });
            }
        }
    }

    private void bindNavigationChrome() {
        View root = findViewById(R.id.app_root);
        View appHeader = findViewById(R.id.app_header);
        TextView appHeaderTitle = findViewById(R.id.app_header_title);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        NavController controller = navController();
        findViewById(R.id.app_header_settings).setOnClickListener(ignored -> {
            NavDestination current = controller.getCurrentDestination();
            @IdRes int destination = current != null
                    && current.getId() == R.id.settingsFragment
                    ? R.id.homeFragment
                    : R.id.settingsFragment;
            ScreenNavigation.navigateTopLevel(this, controller, destination);
        });
        findViewById(R.id.app_header_notifications).setOnClickListener(
                ignored -> ScreenNavigation.navigateTopLevel(
                        this, controller, R.id.notificationsFragment));
        findViewById(R.id.app_header_profile).setOnClickListener(
                ignored -> ScreenNavigation.navigateOwnProfile(this, controller));
        bottomNavigation.setOnItemSelectedListener(item -> {
            @IdRes int destination = destinationForBottomNavigationItem(item.getItemId());
            if (destination == View.NO_ID) {
                return false;
            }
            NavDestination current = controller.getCurrentDestination();
            if (current == null || current.getId() != destination) {
                ScreenNavigation.navigateTopLevel(this, controller, destination);
            }
            return true;
        });
        controller.addOnDestinationChangedListener((ignored, destination, arguments) -> {
            refreshHeaderAvatar();
            unreadBadgeManager.refreshUser();
            unreadBadgeManager.setOpenScreens(
                    destination.getId() == R.id.notificationsFragment,
                    destination.getId() == R.id.messagesFragment);
            @IdRes int itemId = bottomNavigationItemForDestination(destination.getId());
            boolean showAppChrome = isAppDestination(destination.getId());
            appHeader.setVisibility(showAppChrome ? View.VISIBLE : View.GONE);
            bottomNavigation.setVisibility(showAppChrome ? View.VISIBLE : View.GONE);
            ViewCompat.requestApplyInsets(root);
            boolean hideHeaderTitle = destination.getId() == R.id.recentActivitiesFragment
                    || destination.getId() == R.id.scannerFragment;
            appHeaderTitle.setText(hideHeaderTitle ? "" : destination.getLabel());
            if (itemId != View.NO_ID) {
                bottomNavigation.getMenu().findItem(itemId).setChecked(true);
            }
        });
    }

    private void renderUnreadBadges(int notificationCount, int messageCount) {
        TextView notificationBadge = findViewById(R.id.app_header_notifications_badge);
        ImageButton notificationButton = findViewById(R.id.app_header_notifications);
        boolean hasNotifications = notificationCount > 0;
        notificationBadge.setVisibility(hasNotifications ? View.VISIBLE : View.GONE);
        notificationBadge.setText(badgeLabel(notificationCount));
        notificationButton.setContentDescription(hasNotifications
                ? "Notifications, " + notificationCount + " unread"
                : getString(R.string.notifications));

        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        BadgeDrawable messageBadge = bottomNavigation.getOrCreateBadge(R.id.bottom_nav_messages);
        messageBadge.setBackgroundColor(ContextCompat.getColor(
                this, R.color.pc_brand_accent_red));
        messageBadge.setBadgeTextColor(ContextCompat.getColor(
                this, R.color.pc_brand_surface));
        messageBadge.setMaxCharacterCount(3);
        messageBadge.setNumber(messageCount);
        messageBadge.setVisible(messageCount > 0);
    }

    private static String badgeLabel(int count) {
        return count > 99 ? "99+" : Integer.toString(Math.max(0, count));
    }

    private void refreshHeaderAvatar() {
        ImageButton avatar = findViewById(R.id.app_header_profile);
        FirebaseAuth auth = FirebaseEnvironment.auth(this);
        String userId = auth == null || auth.getCurrentUser() == null
                ? "" : auth.getCurrentUser().getUid();
        if (userId.equals(headerProfileUserId)) {
            return;
        }
        if (headerProfileRegistration != null) {
            headerProfileRegistration.remove();
            headerProfileRegistration = null;
        }
        headerProfileUserId = userId;
        ProfileAvatarRenderer.render(avatar, ProfileAvatarPolicy.DEFAULT);
        avatar.setContentDescription("Open your profile");
        if (userId.isEmpty()) {
            return;
        }
        FirebaseFirestore firestore = FirebaseEnvironment.firestore(this);
        if (firestore == null) {
            return;
        }
        headerProfileRegistration = firestore.collection("users").document(userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (!userId.equals(headerProfileUserId)
                            || error != null
                            || snapshot == null
                            || !snapshot.exists()) {
                        return;
                    }
                    ProfileAvatarRenderer.render(
                            avatar,
                            ProfileAvatarPolicy.normalized(snapshot.getString("avatarKey")));
                });
    }

    @Override
    protected void onDestroy() {
        if (unreadBadgeManager != null) {
            unreadBadgeManager.stop();
        }
        if (headerProfileRegistration != null) {
            headerProfileRegistration.remove();
            headerProfileRegistration = null;
        }
        super.onDestroy();
    }

    private NavController navController() {
        NavHostFragment host = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (host == null) {
            throw new IllegalStateException("Navigation host is unavailable");
        }
        return host.getNavController();
    }

    @IdRes
    private static int destinationForBottomNavigationItem(@IdRes int itemId) {
        if (itemId == R.id.bottom_nav_home) {
            return R.id.homeFragment;
        } else if (itemId == R.id.bottom_nav_marketplace) {
            return R.id.marketplaceFragment;
        } else if (itemId == R.id.bottom_nav_lend_out) {
            return R.id.lendingListFragment;
        } else if (itemId == R.id.bottom_nav_recycle_center) {
            return R.id.recycleCenterFragment;
        } else if (itemId == R.id.bottom_nav_messages) {
            return R.id.messagesFragment;
        }
        return View.NO_ID;
    }

    @IdRes
    private static int bottomNavigationItemForDestination(@IdRes int destination) {
        if (destination == R.id.homeFragment) {
            return R.id.bottom_nav_home;
        } else if (destination == R.id.marketplaceFragment) {
            return R.id.bottom_nav_marketplace;
        } else if (destination == R.id.lendingListFragment) {
            return R.id.bottom_nav_lend_out;
        } else if (destination == R.id.recycleCenterFragment) {
            return R.id.bottom_nav_recycle_center;
        } else if (destination == R.id.messagesFragment) {
            return R.id.bottom_nav_messages;
        }
        return View.NO_ID;
    }

    private static boolean isAppDestination(@IdRes int destination) {
        return destination != R.id.welcomeFragment
                && destination != R.id.loginFragment
                && destination != R.id.registerFragment;
    }

    @IdRes
    private static int destinationFor(@Nullable String screen) {
        if (screen == null) {
            return R.id.welcomeFragment;
        }
        return switch (screen) {
            case "login" -> R.id.loginFragment;
            case "register" -> R.id.registerFragment;
            case "home" -> R.id.homeFragment;
            case "recent" -> R.id.recentActivitiesFragment;
            case "scanner" -> R.id.scannerFragment;
            case "ai-result" -> R.id.aiResultFragment;
            case "create-listing" -> R.id.createListingFragment;
            case "recycle" -> R.id.recycleCenterFragment;
            case "lend-resource" -> R.id.lendResourceFragment;
            case "market" -> R.id.marketplaceFragment;
            case "market-detail" -> R.id.marketDetailFragment;
            case "chat" -> R.id.conversationFragment;
            case "lending-map" -> R.id.lendingMapFragment;
            case "lending-list" -> R.id.lendingListFragment;
            case "lending-detail" -> R.id.lendingDetailFragment;
            case "notifications" -> R.id.notificationsFragment;
            case "messages" -> R.id.messagesFragment;
            case "settings" -> R.id.settingsFragment;
            case "profile" -> R.id.profileFragment;
            default -> R.id.welcomeFragment;
        };
    }
}
