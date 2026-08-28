package com.propcycle.app.ui.scanner;

import android.animation.ValueAnimator;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
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
import com.propcycle.app.data.scanner.ScannerImageProcessor;
import com.propcycle.app.databinding.DialogAiResultDetailsBinding;
import com.propcycle.app.databinding.FragmentAiResultBinding;
import com.propcycle.app.ui.common.ScreenNavigation;

import java.text.BreakIterator;
import java.io.File;
import java.util.List;
import java.util.Locale;

/** Renders a validated AI result without treating it as authoritative environmental advice. */
public final class AiResultFragment extends Fragment {

    private static final long REVEAL_DURATION_MILLIS = 180L;
    private static final long REVEAL_STAGGER_MILLIS = 100L;

    @Nullable
    private FragmentAiResultBinding binding;
    @Nullable
    private ScanAnalysis analysis;
    private String scanImagePath = "";
    private String handoffTarget = "";

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
        Bundle arguments = getArguments();
        scanImagePath = arguments == null
                ? "" : arguments.getString("scanImagePath", "");
        handoffTarget = arguments == null
                ? "" : arguments.getString("handoffTarget", "");
        render();

        binding.resultDetailsAction.setOnClickListener(ignored -> showDetails());
        binding.recycleAction.setOnClickListener(ignored -> {
            if (analysis != null) {
                deleteUnconsumedImage();
                ScreenNavigation.navigate(this, R.id.recycleCenterFragment, null);
            }
        });
        binding.sellAction.setOnClickListener(ignored -> openEditableListing());
        binding.lendAction.setOnClickListener(ignored -> {
            if (analysis != null) {
                Bundle destination = editableDraftArguments();
                ScreenNavigation.navigateAuthenticated(
                        this, R.id.lendResourceFragment, destination);
            }
        });
        binding.scanAgainAction.setOnClickListener(ignored -> scanAgain());
        presentResultFlow(analysis != null && savedInstanceState == null);
    }

    private void presentResultFlow(boolean animate) {
        if (binding == null) {
            return;
        }
        View[] stages = getRevealStages(binding);
        if (!animate || !areRevealAnimationsEnabled()) {
            for (View stage : stages) {
                stage.animate().cancel();
                stage.setVisibility(View.VISIBLE);
                stage.setAlpha(1f);
                stage.setTranslationY(0f);
            }
            return;
        }

        float entranceOffset = getResources().getDimension(R.dimen.space_sm);
        for (int index = 0; index < stages.length; index++) {
            View stage = stages[index];
            stage.animate().cancel();
            stage.setVisibility(View.INVISIBLE);
            stage.setAlpha(0f);
            stage.setTranslationY(entranceOffset);
            stage.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(REVEAL_DURATION_MILLIS)
                    .setStartDelay(index * REVEAL_STAGGER_MILLIS)
                    .withStartAction(() -> stage.setVisibility(View.VISIBLE))
                    .start();
        }
    }

    @NonNull
    private static View[] getRevealStages(@NonNull FragmentAiResultBinding currentBinding) {
        return new View[]{
                currentBinding.helloBubble,
                currentBinding.identificationBubble,
                currentBinding.resultDisclaimer,
                currentBinding.nextStepSection,
                currentBinding.resultGuidanceCard,
                currentBinding.resultActionsContainer
        };
    }

    private static boolean areRevealAnimationsEnabled() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                || ValueAnimator.areAnimatorsEnabled();
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
        if ("marketplace".equals(handoffTarget)) {
            binding.nextStepBubble.setText(
                    "Your marketplace draft is ready. Choose Create Listing, then review it before publishing.");
        } else if ("lending".equals(handoffTarget)) {
            binding.nextStepBubble.setText(
                    "Your lending draft is ready. Choose Lend Resource, then add availability and pickup details.");
        }
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
        ScanAnalysis currentAnalysis = analysis;
        if (currentAnalysis == null) {
            return;
        }
        DialogAiResultDetailsBinding detailsBinding =
                DialogAiResultDetailsBinding.inflate(getLayoutInflater());
        detailsBinding.detailsMaterialValue.setText(currentAnalysis.getMaterial());
        detailsBinding.detailsCategoryValue.setText(currentAnalysis.getCategory().getDisplayName());
        detailsBinding.detailsRecyclableValue.setText(currentAnalysis.isRecyclable()
                ? R.string.ai_result_details_recyclable_yes
                : R.string.ai_result_details_recyclable_no_or_unsure);
        detailsBinding.detailsModelEstimateValue.setText(getString(
                R.string.ai_result_details_model_estimate_value,
                currentAnalysis.getUncalibratedModelEstimatePercent()));
        detailsBinding.detailsGuidanceValue.setText(formatGuidanceForDisplay(
                currentAnalysis.getRecyclingGuidance()));
        detailsBinding.detailsEnvironmentValue.setText(currentAnalysis.getEnvironmentalNote());
        detailsBinding.detailsSafetyValue.setText(currentAnalysis.getSafetyNote());
        detailsBinding.detailsDisclaimerValue.setText(ScanAnalysis.MALAYSIA_DISCLAIMER);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(currentAnalysis.getItemName())
                .setView(detailsBinding.getRoot())
                .setPositiveButton(R.string.close, null)
                .show();
    }

    @NonNull
    private static String formatGuidanceForDisplay(@NonNull String guidance) {
        String source = guidance.trim();
        if (source.isEmpty()) {
            return guidance;
        }

        StringBuilder formatted = new StringBuilder();
        BreakIterator sentences = BreakIterator.getSentenceInstance(Locale.getDefault());
        for (String paragraph : source.split("[\\r\\n]+")) {
            String trimmedParagraph = paragraph.trim();
            if (trimmedParagraph.isEmpty()) {
                continue;
            }
            sentences.setText(trimmedParagraph);
            int start = sentences.first();
            for (int end = sentences.next();
                    end != BreakIterator.DONE;
                    start = end, end = sentences.next()) {
                String sentence = trimmedParagraph.substring(start, end).trim();
                if (!sentence.isEmpty()) {
                    if (formatted.length() > 0) {
                        formatted.append('\n');
                    }
                    formatted.append("\u2022 ").append(sentence);
                }
            }
        }
        return formatted.length() == 0 ? guidance : formatted.toString();
    }

    private void openEditableListing() {
        if (analysis == null) {
            return;
        }
        ScreenNavigation.navigateAuthenticated(
                this,
                R.id.createListingFragment,
                editableDraftArguments());
    }

    @NonNull
    private Bundle editableDraftArguments() {
        Bundle arguments = new Bundle();
        if (analysis != null) {
            arguments.putString("scanAnalysisJson", analysis.toJson());
        }
        File image = ScannerImageProcessor.resolveTransferredImage(
                requireContext(), scanImagePath);
        if (image != null) {
            arguments.putString("scanImagePath", image.getAbsolutePath());
        }
        return arguments;
    }

    private void scanAgain() {
        deleteUnconsumedImage();
        NavController controller = NavHostFragment.findNavController(this);
        if (!controller.popBackStack(R.id.scannerFragment, false)) {
            controller.navigate(R.id.scannerFragment);
        }
    }

    private void deleteUnconsumedImage() {
        if (scanImagePath.isEmpty()) {
            return;
        }
        ScannerImageProcessor.deleteTransferredImage(requireContext(), scanImagePath);
        scanImagePath = "";
    }

    @NonNull
    private CharSequence buildGuidance(@NonNull ScanAnalysis value) {
        SpannableStringBuilder text = new SpannableStringBuilder();
        appendGuidanceHeading(text, getString(R.string.ai_result_guidance_recommendation_label));
        text.append('\n').append(formatGuidanceForDisplay(value.getRecyclingGuidance()));

        appendGuidanceHeading(text, getString(R.string.ai_result_guidance_upcycling_label));
        List<String> ideas = value.getUpcyclingIdeas();
        for (String idea : ideas) {
            text.append('\n').append("\u2022 ").append(idea);
        }

        appendGuidanceHeading(text, getString(R.string.ai_result_guidance_safety_label));
        text.append('\n').append(formatGuidanceForDisplay(value.getSafetyNote()));
        return text;
    }

    private static void appendGuidanceHeading(
            @NonNull SpannableStringBuilder text,
            @NonNull String heading) {
        if (text.length() > 0) {
            text.append("\n\n");
        }
        int start = text.length();
        text.append(heading);
        text.setSpan(
                new StyleSpan(Typeface.BOLD),
                start,
                text.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private static void setActionEnabled(@NonNull View action, boolean enabled) {
        action.setEnabled(enabled);
        action.setAlpha(enabled ? 1f : 0.5f);
    }

    @Override
    public void onDestroyView() {
        if (binding != null) {
            for (View stage : getRevealStages(binding)) {
                stage.animate().cancel();
            }
        }
        binding = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (getContext() != null) {
            deleteUnconsumedImage();
        }
        super.onDestroy();
    }
}
