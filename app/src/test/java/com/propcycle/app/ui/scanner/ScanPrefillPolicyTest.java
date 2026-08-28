package com.propcycle.app.ui.scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.propcycle.app.data.scanner.ScanAnalysis;

import org.junit.Test;

import java.util.Collections;

public final class ScanPrefillPolicyTest {

    @Test
    public void marketplaceDraft_mapsMaterialAndRemainsReviewable() {
        ScanPrefillPolicy.MarketplaceDraft draft = ScanPrefillPolicy.marketplace(
                analysis("Cardboard display box", "Corrugated cardboard"));

        assertEquals("Cardboard display box", draft.title());
        assertEquals("Packaging", draft.category());
        assertEquals("Donation", draft.transaction());
        assertEquals("Pickup", draft.fulfilment());
        assertTrue(draft.description().contains("AI-assisted draft"));
        assertTrue(draft.description().contains("Review and correct"));
    }

    @Test
    public void lendingDraft_mapsToolsBeforeGenericEquipment() {
        ScanPrefillPolicy.LendingDraft draft = ScanPrefillPolicy.lending(
                analysis("Cordless drill", "Metal and plastic tool"));

        assertEquals("Tools", draft.category());
        assertEquals("Good", draft.condition());
    }

    @Test
    public void unknownMaterial_usesSafeEditableFallbacks() {
        ScanPrefillPolicy.MarketplaceDraft marketplace = ScanPrefillPolicy.marketplace(
                analysis("Reusable object", "Mixed material"));
        ScanPrefillPolicy.LendingDraft lending = ScanPrefillPolicy.lending(
                analysis("Reusable object", "Mixed material"));

        assertEquals("Other", marketplace.category());
        assertEquals("Equipment", lending.category());
    }

    private static ScanAnalysis analysis(String name, String material) {
        return new ScanAnalysis(
                name,
                material,
                ScanAnalysis.Category.REUSABLE,
                false,
                70,
                "Verify the local option.",
                Collections.singletonList("Reuse it safely."),
                "Reuse may extend useful life.",
                "Inspect it before reuse.");
    }
}
