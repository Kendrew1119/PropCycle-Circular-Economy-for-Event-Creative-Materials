package com.propcycle.app.ui.scanner;

import androidx.annotation.NonNull;

import com.propcycle.app.data.scanner.ScanAnalysis;

import java.util.Locale;

/** Pure mapping from a reviewed AI result to editable marketplace and lending drafts. */
public final class ScanPrefillPolicy {

    private ScanPrefillPolicy() {
    }

    @NonNull
    public static MarketplaceDraft marketplace(@NonNull ScanAnalysis analysis) {
        String searchable = searchable(analysis);
        String category;
        if (containsAny(searchable, "banner", "backdrop")) {
            category = "Banner";
        } else if (containsAny(searchable, "fabric", "textile", "cloth", "costume")) {
            category = "Fabric";
        } else if (containsAny(searchable, "wood", "timber", "plywood", "pallet")) {
            category = "Wood";
        } else if (containsAny(searchable, "stationery", "pen", "pencil", "marker")) {
            category = "Stationery";
        } else if (containsAny(searchable, "electronic", "cable", "wire", "battery", "device")) {
            category = "Electronic";
        } else if (containsAny(searchable, "box", "cardboard", "paper", "bottle", "packaging")) {
            category = "Packaging";
        } else if (containsAny(searchable, "toy", "figurine", "miniature")) {
            category = "Toys";
        } else if (containsAny(searchable, "craft", "bead", "ribbon", "paint")) {
            category = "Craft";
        } else if (containsAny(searchable, "decor", "ornament", "flower")) {
            category = "Decoration";
        } else {
            category = "Other";
        }
        return new MarketplaceDraft(
                analysis.getItemName(),
                category,
                "Good",
                "Donation",
                "Pickup",
                draftDescription(analysis));
    }

    @NonNull
    public static LendingDraft lending(@NonNull ScanAnalysis analysis) {
        String searchable = searchable(analysis);
        String category;
        if (containsAny(searchable, "drill", "hammer", "saw", "spanner", "wrench", "tool")) {
            category = "Tools";
        } else if (containsAny(searchable, "electronic", "camera", "speaker", "projector", "cable", "device")) {
            category = "Electronics";
        } else if (containsAny(searchable, "light", "tent", "table", "chair", "stand", "backdrop", "event")) {
            category = "Event gear";
        } else if (containsAny(searchable, "craft", "sewing", "paint", "cutting")) {
            category = "Craft";
        } else {
            category = "Equipment";
        }
        return new LendingDraft(
                analysis.getItemName(),
                category,
                "Good",
                draftDescription(analysis));
    }

    @NonNull
    private static String searchable(@NonNull ScanAnalysis analysis) {
        return (analysis.getItemName() + " " + analysis.getMaterial())
                .toLowerCase(Locale.ROOT);
    }

    private static boolean containsAny(@NonNull String value, @NonNull String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private static String draftDescription(@NonNull ScanAnalysis analysis) {
        return "AI-assisted draft. Identified material: " + analysis.getMaterial()
                + ". " + analysis.getEnvironmentalNote()
                + " Review and correct these details before publishing.";
    }

    public record MarketplaceDraft(
            String title,
            String category,
            String condition,
            String transaction,
            String fulfilment,
            String description) {
    }

    public record LendingDraft(
            String title,
            String category,
            String condition,
            String description) {
    }
}
