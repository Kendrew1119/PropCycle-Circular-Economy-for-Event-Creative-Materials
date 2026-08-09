package com.propcycle.app;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.propcycle.app.core.firebase.FirebaseEnvironment;

/** Hosts the proposal UI, restores an authenticated session, and supports debug visual QA. */
public final class MainActivity extends AppCompatActivity {

    public static final String EXTRA_SCREEN = "screen";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.app_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
                            | WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return windowInsets;
        });

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

    private NavController navController() {
        NavHostFragment host = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (host == null) {
            throw new IllegalStateException("Navigation host is unavailable");
        }
        return host.getNavController();
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
