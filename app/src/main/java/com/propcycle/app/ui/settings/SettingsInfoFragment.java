package com.propcycle.app.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;

import com.propcycle.app.R;
import com.propcycle.app.databinding.FragmentSettingsInfoBinding;
import com.propcycle.app.ui.common.ScreenNavigation;

public final class SettingsInfoFragment extends Fragment {

    private static final String PAGE_HELP = "help";
    private static final String PAGE_PRIVACY = "privacy";
    private static final String PAGE_ABOUT = "about";

    private FragmentSettingsInfoBinding binding;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsInfoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NavDestination destination = NavHostFragment.findNavController(this).getCurrentDestination();
        int destinationId = destination == null ? R.id.settingsFragment : destination.getId();
        if (!ScreenNavigation.navigateAuthenticated(this, destinationId, null)) {
            return;
        }
        ScreenNavigation.bindChrome(this, view);

        String page = getArguments() == null
                ? PAGE_HELP : getArguments().getString("pageType", PAGE_HELP);
        PageSpec spec = pageSpec(page);
        binding.settingsInfoTitle.setText(spec.titleResId);
        binding.settingsInfoIntro.setText(spec.introResId);
        renderSections(spec.sections);
    }

    private void renderSections(@NonNull int[][] sections) {
        LinearLayout content = binding.settingsInfoContent;
        content.removeAllViews();
        int spacing = getResources().getDimensionPixelSize(R.dimen.space_md);
        int dividerHeight = getResources().getDimensionPixelSize(R.dimen.stroke_thin);
        for (int index = 0; index < sections.length; index++) {
            if (index > 0) {
                View divider = new View(requireContext());
                LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dividerHeight);
                dividerParams.setMargins(0, spacing, 0, spacing);
                divider.setLayoutParams(dividerParams);
                divider.setBackgroundResource(R.color.pc_brand_outline);
                content.addView(divider);
            }

            TextView title = new TextView(requireContext());
            title.setText(sections[index][0]);
            title.setTextAppearance(R.style.TextAppearance_PropCycle_SettingsInfoTitle);
            content.addView(title, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView body = new TextView(requireContext());
            body.setText(sections[index][1]);
            body.setTextAppearance(R.style.TextAppearance_PropCycle_SettingsInfoBody);
            body.setLineSpacing(0f, 1.12f);
            LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            bodyParams.topMargin = getResources().getDimensionPixelSize(R.dimen.space_xs);
            content.addView(body, bodyParams);
        }
    }

    @NonNull
    private static PageSpec pageSpec(@Nullable String page) {
        if (PAGE_PRIVACY.equals(page)) {
            return new PageSpec(
                    R.string.settings_privacy_data_title,
                    R.string.settings_privacy_intro,
                    new int[][]{
                            {R.string.settings_privacy_account_title,
                                    R.string.settings_privacy_account_body},
                            {R.string.settings_privacy_location_title,
                                    R.string.settings_privacy_location_body},
                            {R.string.settings_privacy_search_title,
                                    R.string.settings_privacy_search_body},
                            {R.string.settings_privacy_messages_title,
                                    R.string.settings_privacy_messages_body},
                            {R.string.settings_privacy_scanner_title,
                                    R.string.settings_privacy_scanner_body},
                            {R.string.settings_privacy_local_title,
                                    R.string.settings_privacy_local_body}
                    });
        }
        if (PAGE_ABOUT.equals(page)) {
            return new PageSpec(
                    R.string.settings_about_title,
                    R.string.settings_about_intro,
                    new int[][]{
                            {R.string.settings_about_sharing_title,
                                    R.string.settings_about_sharing_body},
                            {R.string.settings_about_decisions_title,
                                    R.string.settings_about_decisions_body},
                            {R.string.settings_about_community_title,
                                    R.string.settings_about_community_body},
                            {R.string.settings_about_scope_title,
                                    R.string.settings_about_scope_body}
                    });
        }
        return new PageSpec(
                R.string.settings_help_faq_title,
                R.string.settings_help_intro,
                new int[][]{
                        {R.string.settings_help_marketplace_question,
                                R.string.settings_help_marketplace_answer},
                        {R.string.settings_help_lending_question,
                                R.string.settings_help_lending_answer},
                        {R.string.settings_help_scanner_question,
                                R.string.settings_help_scanner_answer},
                        {R.string.settings_help_recycle_question,
                                R.string.settings_help_recycle_answer},
                        {R.string.settings_help_map_question,
                                R.string.settings_help_map_answer},
                        {R.string.settings_help_messages_question,
                                R.string.settings_help_messages_answer},
                        {R.string.settings_help_location_question,
                                R.string.settings_help_location_answer}
                });
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    private static final class PageSpec {
        private final int titleResId;
        private final int introResId;
        private final int[][] sections;

        private PageSpec(int titleResId, int introResId, @NonNull int[][] sections) {
            this.titleResId = titleResId;
            this.introResId = introResId;
            this.sections = sections;
        }
    }
}
