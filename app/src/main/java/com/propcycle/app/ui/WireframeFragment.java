package com.propcycle.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.propcycle.app.R;
import com.propcycle.app.ui.common.ScreenNavigation;

/** Minimal host retained only for the proposal's non-service Welcome screen. */
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
        ScreenNavigation.bindChrome(this, view);
        View primary = view.findViewById(R.id.primary_action);
        if (primary != null) {
            primary.setOnClickListener(ignored ->
                    NavHostFragment.findNavController(this).navigate(R.id.loginFragment));
        }
    }
}
