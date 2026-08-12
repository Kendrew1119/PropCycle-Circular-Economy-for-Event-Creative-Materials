package com.propcycle.app.ui.scanner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.propcycle.app.R;
import com.propcycle.app.data.scanner.ScanAnalysis;
import com.propcycle.app.databinding.FragmentAiResultBinding;
import com.propcycle.app.ui.common.ScreenNavigation;

import java.util.List;

/** Renders a validated AI result without treating it as authoritative environmental advice. */
public final class AiResultFragment extends Fragment {

    @Nullable
    private FragmentAiResultBinding binding;
    @Nullable
    private ScanAnalysis analysis;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentAiResultBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ScreenNavigation.bindChrome(this, view);
        analysis = parseAnalysis();
        render();

        binding.resultDetailsAction.setOnClickListener(ignored -> showDetails());
        binding.recycleAction.setOnClickListener(ignored -> {
            if (analysis != null) {
                ScreenNavigation.navigate(this, R.id.recycleCenterFragment, null);
            }
        });
        binding.sellAction.setOnClickListener(ignored -> openEditableListing());
        binding.lendAction.setOnClickListener(ignored -> {
            if (analysis != null) {
                ScreenNavigation.navigate(this, R.id.lendResourceFragment, null);
            }
        });
        binding.scanAgainAction.setOnClickListener(ignored -> scanAgain());
    }

    @Nullable
    private ScanAnalysis parseAnalysis() {
        Bundle arguments = getArguments();
        String json = arguments == null ? "" : arguments.getString("analysisJson", "");
        if (json.trim().isEmpty()) {
            return null;
        }
        try {
            return ScanAnalysis.fromJson(json);
        } catch (ScanAnalysis.ValidationException invalidResult) {
            return null;
        }
    }

    private void render() {
        if (binding == null) {
            return;
        }
        boolean valid = analysis != null;
        boolean hazardous = valid && analysis.getCategory() == ScanAnalysis.Category.HAZARDOUS;
        boolean electronicWaste = valid && analysis.getCategory() == ScanAnalysis.Category.E_WASTE;
        boolean recycleAllowed = ScanActionPolicy.canRecycle(analysis);
        boolean sellAllowed = ScanActionPolicy.canSell(analysis);
        boolean lendAllowed = ScanActionPolicy.canLend(analysis);
        setActionEnabled(binding.recycleAction, recycleAllowed);
        setActionEnabled(binding.sellAction, sellAllowed);
        setActionEnabled(binding.lendAction, lendAllowed);
        binding.resultDetailsAction.setEnabled(valid);
        binding.resultDetailsAction.setAlpha(valid ? 1f : 0.5f);
        if (!valid) {
            binding.resultStatus.setText(R.string.ai_result_invalid_status);
            binding.resultGuidance.setText(R.string.ai_result_invalid_guidance);
            return;
        }

        binding.resultItemName.setText(analysis.getItemName());
        binding.resultMaterial.setText(getString(
                R.string.ai_result_material_format,
                analysis.getMaterial()));
        binding.resultCategory.setText(getString(
                R.string.ai_result_category_format,
                analysis.getCategory().getDisplayName()));
        binding.resultRoute.setText(getString(
                R.string.ai_result_route_format,
                ScanActionPolicy.routeLabel(analysis)));
        binding.resultConfidence.setText(getString(
                R.string.ai_result_confidence_format,
                analysis.getUncalibratedModelEstimatePercent()));
        binding.resultStatus.setText(R.string.ai_result_review_status);
        binding.resultGuidance.setText(buildGuidance(analysis));
        if (hazardous) {
            binding.nextStepBubble.setText(R.string.ai_result_hazardous_next_step);
        } else if (electronicWaste) {
            binding.nextStepBubble.setText(R.string.ai_result_ewaste_next_step);
        } else if (ScanActionPolicy.isReviewOnly(analysis)) {
            binding.nextStepBubble.setText(R.string.ai_result_review_only_next_step);
        }
    }

    private void showDetails() {
        if (analysis == null) {
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(analysis.getItemName())
                .setMessage(buildDetails(analysis))
                .setPositiveButton("Close", null)
                .show();
    }

    private void openEditableListing() {
        if (analysis == null) {
            return;
        }
        ScreenNavigation.navigateAuthenticated(
                this,
                R.id.createListingFragment,
                null);
    }

    private void scanAgain() {
        NavController controller = NavHostFragment.findNavController(this);
        if (!controller.popBackStack(R.id.scannerFragment, false)) {
            controller.navigate(R.id.scannerFragment);
        }
    }

    @NonNull
    private static String buildGuidance(@NonNull ScanAnalysis value) {
        StringBuilder text = new StringBuilder(value.getRecyclingGuidance());
        text.append("\n\nUpcycling ideas:");
        List<String> ideas = value.getUpcyclingIdeas();
        for (String idea : ideas) {
            text.append("\n- ").append(idea);
        }
        text.append("\n\nSafety note:\n").append(value.getSafetyNote());
        return text.toString();
    }

    @NonNull
    private static String buildDetails(@NonNull ScanAnalysis value) {
        return "Material: " + value.getMaterial()
                + "\nCategory: " + value.getCategory().getDisplayName()
                + "\nRecyclable estimate: " + (value.isRecyclable() ? "Yes" : "No or unsure")
                + "\nUncalibrated model estimate: "
                + value.getUncalibratedModelEstimatePercent() + "%"
                + "\n\nGuidance:\n" + value.getRecyclingGuidance()
                + "\n\nEnvironmental note:\n" + value.getEnvironmentalNote()
                + "\n\nSafety note:\n" + value.getSafetyNote()
                + "\n\n" + ScanAnalysis.MALAYSIA_DISCLAIMER;
    }

    private static void setActionEnabled(@NonNull View action, boolean enabled) {
        action.setEnabled(enabled);
        action.setAlpha(enabled ? 1f : 0.5f);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
