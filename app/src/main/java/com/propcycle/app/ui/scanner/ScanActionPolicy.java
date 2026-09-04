package com.propcycle.app.ui.scanner;

import com.propcycle.app.data.scanner.ScanAnalysis;

/** Builds advisory route copy without controlling the user's destination choices. */
final class ScanActionPolicy {

    private ScanActionPolicy() {
    }

    static boolean isReviewOnly(ScanAnalysis value) {
        if (value == null) {
            return true;
        }
        return value.getCategory() != ScanAnalysis.Category.E_WASTE
                && value.getCategory() != ScanAnalysis.Category.REUSABLE
                && (value.getCategory() != ScanAnalysis.Category.RECYCLABLE
                || !value.isRecyclable());
    }

    static String routeLabel(ScanAnalysis value) {
        if (value == null) {
            return "review guidance and verify a safe local option";
        }
        if (value.getCategory() == ScanAnalysis.Category.HAZARDOUS
                || value.getCategory() == ScanAnalysis.Category.E_WASTE) {
            return "specialist drop-off after local check";
        }
        if (value.getCategory() == ScanAnalysis.Category.RECYCLABLE
                && value.isRecyclable()) {
            return "reuse or recycle after local check";
        }
        if (value.getCategory() == ScanAnalysis.Category.REUSABLE) {
            return "reuse, lend, or list it";
        }
        return "review guidance and verify a safe local option";
    }
}
