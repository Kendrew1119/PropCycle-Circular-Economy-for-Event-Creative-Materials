package com.propcycle.app.ui.scanner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.propcycle.app.data.scanner.ScanAnalysis;

import org.junit.Test;

import java.util.Collections;

public final class ScanActionPolicyTest {

    @Test
    public void recyclableCategory_withPositiveFlag_allowsRecycleAndSellOnly() {
        ScanAnalysis value = analysis(ScanAnalysis.Category.RECYCLABLE, true);

        assertTrue(ScanActionPolicy.canRecycle(value));
        assertTrue(ScanActionPolicy.canSell(value));
        assertFalse(ScanActionPolicy.canLend(value));
        assertFalse(ScanActionPolicy.isReviewOnly(value));
    }

    @Test
    public void recyclableCategory_withNegativeFlag_staysReviewOnly() {
        assertReviewOnly(analysis(ScanAnalysis.Category.RECYCLABLE, false));
    }

    @Test
    public void reusableCategory_allowsSellAndLendOnly() {
        ScanAnalysis value = analysis(ScanAnalysis.Category.REUSABLE, false);

        assertFalse(ScanActionPolicy.canRecycle(value));
        assertTrue(ScanActionPolicy.canSell(value));
        assertTrue(ScanActionPolicy.canLend(value));
        assertFalse(ScanActionPolicy.isReviewOnly(value));
    }

    @Test
    public void eWaste_allowsSpecialistRecycleRouteOnly() {
        ScanAnalysis value = analysis(ScanAnalysis.Category.E_WASTE, false);

        assertTrue(ScanActionPolicy.canRecycle(value));
        assertFalse(ScanActionPolicy.canSell(value));
        assertFalse(ScanActionPolicy.canLend(value));
        assertFalse(ScanActionPolicy.isReviewOnly(value));
    }

    @Test
    public void uncertainOrUnsafeCategories_neverOpenActions() {
        ScanAnalysis unknown = analysis(ScanAnalysis.Category.UNKNOWN, true);
        ScanAnalysis generalWaste = analysis(ScanAnalysis.Category.GENERAL_WASTE, true);
        ScanAnalysis compostable = analysis(ScanAnalysis.Category.COMPOSTABLE, true);
        assertReviewOnly(unknown);
        assertReviewOnly(generalWaste);
        assertReviewOnly(compostable);
        assertReviewOnly(analysis(ScanAnalysis.Category.HAZARDOUS, true));
        assertReviewOnly(null);

        assertEquals(
                "review guidance and verify a safe local option",
                ScanActionPolicy.routeLabel(unknown));
        assertEquals(
                "review guidance and verify a safe local option",
                ScanActionPolicy.routeLabel(generalWaste));
        assertEquals(
                "review guidance and verify a safe local option",
                ScanActionPolicy.routeLabel(compostable));
    }

    @Test
    public void routeLabels_followTheSameReviewedActionPolicy() {
        assertEquals(
                "reuse or recycle after local check",
                ScanActionPolicy.routeLabel(analysis(ScanAnalysis.Category.RECYCLABLE, true)));
        assertEquals(
                "reuse, lend, or list it",
                ScanActionPolicy.routeLabel(analysis(ScanAnalysis.Category.REUSABLE, false)));
        assertEquals(
                "specialist drop-off after local check",
                ScanActionPolicy.routeLabel(analysis(ScanAnalysis.Category.E_WASTE, false)));
        assertEquals(
                "specialist drop-off after local check",
                ScanActionPolicy.routeLabel(analysis(ScanAnalysis.Category.HAZARDOUS, false)));
    }

    private static void assertReviewOnly(ScanAnalysis value) {
        assertFalse(ScanActionPolicy.canRecycle(value));
        assertFalse(ScanActionPolicy.canSell(value));
        assertFalse(ScanActionPolicy.canLend(value));
        assertTrue(ScanActionPolicy.isReviewOnly(value));
    }

    private static ScanAnalysis analysis(
            ScanAnalysis.Category category,
            boolean recyclable) {
        return new ScanAnalysis(
                "Test item",
                "Test material",
                category,
                recyclable,
                70,
                "Verify local acceptance.",
                Collections.singletonList("Use it only when safe."),
                "Reuse may extend useful life.",
                "Handle carefully and inspect it first.");
    }
}
