package com.propcycle.app.ui.scanner;

import com.propcycle.app.data.scanner.ScanAnalysis;

/** Keeps AI suggestions from opening actions that conflict with the reviewed category. */
final class ScanActionPolicy {

    private ScanActionPolicy() {
    }

    static boolean canRecycle(ScanAnalysis value) {
        if (value == null) {
            return false;
        }
        if (value.getCategory() == ScanAnalysis.Category.E_WASTE) {
            return true;
        }
        return value.getCategory() == ScanAnalysis.Category.RECYCLABLE
                && value.isRecyclable();
    }

    static boolean canSell(ScanAnalysis value) {
        if (value == null) {
            return false;
        }
        return value.getCategory() == ScanAnalysis.Category.REUSABLE
                || (value.getCategory() == ScanAnalysis.Category.RECYCLABLE
                && value.isRecyclable());
    }

    static boolean canLend(ScanAnalysis value) {
        return value != null && value.getCategory() == ScanAnalysis.Category.REUSABLE;
    }

    static boolean isReviewOnly(ScanAnalysis value) {
        return !canRecycle(value) && !canSell(value) && !canLend(value);
    }

    static String routeLabel(ScanAnalysis value) {
        if (value == null) {
            return "review guidance and verify a safe local option";
        }
        if (value.getCategory() == ScanAnalysis.Category.HAZARDOUS
                || value.getCategory() == ScanAnalysis.Category.E_WASTE) {
            return "specialist drop-off after local check";
        }
        if (canRecycle(value)) {
            return "reuse or recycle after local check";
        }
        if (value.getCategory() == ScanAnalysis.Category.REUSABLE) {
            return "reuse, lend, or list it";
        }
        return "review guidance and verify a safe local option";
    }
}
