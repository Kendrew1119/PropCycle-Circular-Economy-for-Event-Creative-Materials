package com.propcycle.app.data.media;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class DemoImagePolicyTest {

    @Test
    public void allowlist_acceptsEveryPublishedDemoKey() {
        assertTrue(DemoImagePolicy.isSelected(DemoImagePolicy.CARDBOARD_BOX));
        assertTrue(DemoImagePolicy.isSelected(DemoImagePolicy.PLASTIC_BOTTLES));
        assertTrue(DemoImagePolicy.isSelected(DemoImagePolicy.METAL_CANS));
        assertTrue(DemoImagePolicy.isSelected(DemoImagePolicy.FABRIC_ROLLS));
        assertTrue(DemoImagePolicy.isSelected(DemoImagePolicy.WOODEN_PALLET));
        assertTrue(DemoImagePolicy.isSelected(DemoImagePolicy.CRAFT_BUNDLE));
        assertTrue(DemoImagePolicy.isSelected(DemoImagePolicy.EVENT_BANNER));
        assertTrue(DemoImagePolicy.isSelected(DemoImagePolicy.FAIRY_LIGHTS));
        assertTrue(DemoImagePolicy.isSelected(DemoImagePolicy.FOLDING_CHAIRS));
        assertTrue(DemoImagePolicy.isSelected(DemoImagePolicy.SPEAKER_SET));
        assertTrue(DemoImagePolicy.isSelected(DemoImagePolicy.DISPLAY_STAND));
        assertTrue(DemoImagePolicy.isSelected(DemoImagePolicy.STORAGE_CRATES));
    }

    @Test
    public void optionalEmptyKeyIsValidButUnknownKeyIsRejected() {
        assertTrue(DemoImagePolicy.isValid(null));
        assertTrue(DemoImagePolicy.isValid(""));
        assertFalse(DemoImagePolicy.isSelected(""));
        assertFalse(DemoImagePolicy.isValid("../../private_photo"));
    }

    @Test
    public void normalization_isStableForFirestore() {
        assertEquals("event_banner", DemoImagePolicy.normalize("  EVENT_BANNER "));
    }
}
