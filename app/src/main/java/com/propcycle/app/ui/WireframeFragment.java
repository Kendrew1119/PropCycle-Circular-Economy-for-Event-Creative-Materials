package com.propcycle.app.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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
    private static final int[] LENDING_ITEM_IDS = {
            R.id.item_card,
            R.id.lending_item_wooden_crates,
            R.id.lending_item_fabric_panels,
            R.id.item_card_secondary,
            R.id.lending_item_led_lights,
            R.id.lending_item_signage_frames
    };

    private String lendingFilter = "all";
    private String lendingQuery = "";

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
        } else if (destinationId == R.id.lendResourceFragment) {
            bindNavigation(view, R.id.primary_action, controller, R.id.lendingListFragment);
        } else if (destinationId == R.id.lendingMapFragment) {
            bindNavigation(view, R.id.item_card, controller, R.id.lendingDetailFragment);
            bindNavigation(view, R.id.item_card_secondary, controller, R.id.lendingDetailFragment);
        } else if (destinationId == R.id.lendingListFragment) {
            bindNavigation(view, R.id.item_card, controller, R.id.lendingDetailFragment);
            bindNavigation(view, R.id.item_card_secondary, controller, R.id.lendingDetailFragment);
            bindLendingListFilters(view);
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
            bind(view, R.id.logout_action, clicked -> {
                FirebaseAuth auth = FirebaseEnvironment.auth(requireContext());
                if (auth != null) {
                    auth.signOut();
                }
                ScreenNavigation.navigateClearingBackStack(this, R.id.loginFragment);
            });
        }
    }

    private void bindLendingListFilters(@NonNull View root) {
        lendingFilter = "all";
        lendingQuery = "";
        bindLendingFilter(root, R.id.lending_filter_all, "all");
        bindLendingFilter(root, R.id.lending_filter_equipment, "equipment");
        bindLendingFilter(root, R.id.lending_filter_materials, "materials");
        bindLendingFilter(root, R.id.lending_filter_nearby, "nearby");

        EditText searchInput = root.findViewById(R.id.lending_search_input);
        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(
                        CharSequence value, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(
                        CharSequence value, int start, int before, int count) {
                    lendingQuery = value == null
                            ? ""
                            : value.toString().trim().toLowerCase(Locale.ROOT);
                    applyLendingFilters(root);
                }

                @Override
                public void afterTextChanged(Editable value) {
                }
            });
        }
        applyLendingFilters(root);
    }

    private void bindLendingFilter(
            @NonNull View root,
            @IdRes int viewId,
            @NonNull String filter) {
        View filterView = root.findViewById(viewId);
        if (filterView != null) {
            filterView.setOnClickListener(ignored -> {
                lendingFilter = filter;
                applyLendingFilters(root);
            });
        }
    }

    private void applyLendingFilters(@NonNull View root) {
        int visibleCount = 0;
        for (@IdRes int itemId : LENDING_ITEM_IDS) {
            View item = root.findViewById(itemId);
            if (item == null) {
                continue;
            }
            String tags = item.getTag() == null
                    ? ""
                    : item.getTag().toString().toLowerCase(Locale.ROOT);
            boolean matchesFilter = "all".equals(lendingFilter)
                    || tags.contains(lendingFilter);
            boolean matchesSearch = lendingQuery.isEmpty()
                    || collectText(item).toLowerCase(Locale.ROOT).contains(lendingQuery);
            boolean visible = matchesFilter && matchesSearch;
            item.setVisibility(visible ? View.VISIBLE : View.GONE);
            if (visible) {
                visibleCount++;
            }
        }

        updateLendingFilter(root, R.id.lending_filter_all, "all".equals(lendingFilter));
        updateLendingFilter(
                root, R.id.lending_filter_equipment, "equipment".equals(lendingFilter));
        updateLendingFilter(
                root, R.id.lending_filter_materials, "materials".equals(lendingFilter));
        updateLendingFilter(
                root, R.id.lending_filter_nearby, "nearby".equals(lendingFilter));

        View emptyState = root.findViewById(R.id.lending_empty_state);
        if (emptyState != null) {
            emptyState.setVisibility(visibleCount == 0 ? View.VISIBLE : View.GONE);
        }
    }

    private void updateLendingFilter(
            @NonNull View root,
            @IdRes int viewId,
            boolean selected) {
        TextView filterView = root.findViewById(viewId);
        if (filterView == null) {
            return;
        }
        filterView.setBackgroundResource(selected ? R.drawable.bg_pill_dark : R.drawable.bg_pill);
        filterView.setTextColor(ContextCompat.getColor(
                requireContext(), selected ? R.color.pc_white : R.color.pc_ink));
        filterView.setSelected(selected);
    }

    @NonNull
    private static String collectText(@NonNull View view) {
        StringBuilder text = new StringBuilder();
        if (view instanceof TextView textView) {
            text.append(textView.getText()).append(' ');
        }
        if (view instanceof ViewGroup group) {
            for (int index = 0; index < group.getChildCount(); index++) {
                text.append(collectText(group.getChildAt(index)));
            }
        }
        return text.toString();
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
