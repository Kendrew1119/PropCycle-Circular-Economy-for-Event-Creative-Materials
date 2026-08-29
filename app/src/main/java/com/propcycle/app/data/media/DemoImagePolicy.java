package com.propcycle.app.data.media;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Stable allowlist for images bundled with the app for local demonstrations. */
public final class DemoImagePolicy {

    public static final String CARDBOARD_BOX = "cardboard_box";
    public static final String PLASTIC_BOTTLES = "plastic_bottles";
    public static final String METAL_CANS = "metal_cans";
    public static final String FABRIC_ROLLS = "fabric_rolls";
    public static final String WOODEN_PALLET = "wooden_pallet";
    public static final String CRAFT_BUNDLE = "craft_bundle";
    public static final String EVENT_BANNER = "event_banner";
    public static final String FAIRY_LIGHTS = "fairy_lights";
    public static final String FOLDING_CHAIRS = "folding_chairs";
    public static final String SPEAKER_SET = "speaker_set";
    public static final String DISPLAY_STAND = "display_stand";
    public static final String STORAGE_CRATES = "storage_crates";

    private static final Set<String> ALLOWED_KEYS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    CARDBOARD_BOX,
                    PLASTIC_BOTTLES,
                    METAL_CANS,
                    FABRIC_ROLLS,
                    WOODEN_PALLET,
                    CRAFT_BUNDLE,
                    EVENT_BANNER,
                    FAIRY_LIGHTS,
                    FOLDING_CHAIRS,
                    SPEAKER_SET,
                    DISPLAY_STAND,
                    STORAGE_CRATES)));

    private DemoImagePolicy() {
    }

    /** Empty means that a listing does not use a bundled demo image. */
    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isValid(String value) {
        String normalized = normalize(value);
        return normalized.isEmpty() || ALLOWED_KEYS.contains(normalized);
    }

    public static boolean isSelected(String value) {
        return ALLOWED_KEYS.contains(normalize(value));
    }
}
