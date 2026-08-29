package com.propcycle.app.ui.common;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.propcycle.app.R;
import com.propcycle.app.data.media.DemoImagePolicy;

/** User-facing catalogue for sample illustrations packaged inside the APK. */
public final class DemoImageCatalog {

    private static final Entry[] ENTRIES = {
            new Entry(DemoImagePolicy.CARDBOARD_BOX, "Cardboard box", R.drawable.demo_cardboard_box),
            new Entry(DemoImagePolicy.PLASTIC_BOTTLES, "Plastic bottles", R.drawable.demo_plastic_bottles),
            new Entry(DemoImagePolicy.METAL_CANS, "Metal cans", R.drawable.demo_metal_cans),
            new Entry(DemoImagePolicy.FABRIC_ROLLS, "Fabric rolls", R.drawable.demo_fabric_rolls),
            new Entry(DemoImagePolicy.WOODEN_PALLET, "Wooden pallet", R.drawable.demo_wooden_pallet),
            new Entry(DemoImagePolicy.CRAFT_BUNDLE, "Craft material bundle", R.drawable.demo_craft_bundle),
            new Entry(DemoImagePolicy.EVENT_BANNER, "Event banner", R.drawable.demo_event_banner),
            new Entry(DemoImagePolicy.FAIRY_LIGHTS, "Fairy lights", R.drawable.demo_fairy_lights),
            new Entry(DemoImagePolicy.FOLDING_CHAIRS, "Folding chairs", R.drawable.demo_folding_chairs),
            new Entry(DemoImagePolicy.SPEAKER_SET, "Speaker set", R.drawable.demo_speaker_set),
            new Entry(DemoImagePolicy.DISPLAY_STAND, "Display stand", R.drawable.demo_display_stand),
            new Entry(DemoImagePolicy.STORAGE_CRATES, "Storage crates", R.drawable.demo_storage_crates)
    };

    private DemoImageCatalog() {
    }

    @NonNull
    public static String[] labels() {
        String[] labels = new String[ENTRIES.length];
        for (int i = 0; i < ENTRIES.length; i++) {
            labels[i] = ENTRIES[i].label;
        }
        return labels;
    }

    @Nullable
    public static String keyAt(int index) {
        if (index < 0 || index >= ENTRIES.length) {
            return null;
        }
        return ENTRIES[index].key;
    }

    @Nullable
    public static String labelFor(@Nullable String key) {
        Entry entry = find(key);
        return entry == null ? null : entry.label;
    }

    @DrawableRes
    public static int drawableFor(@Nullable String key) {
        Entry entry = find(key);
        return entry == null ? 0 : entry.drawable;
    }

    @Nullable
    private static Entry find(@Nullable String key) {
        String normalized = DemoImagePolicy.normalize(key);
        for (Entry entry : ENTRIES) {
            if (entry.key.equals(normalized)) {
                return entry;
            }
        }
        return null;
    }

    private static final class Entry {
        private final String key;
        private final String label;
        @DrawableRes private final int drawable;

        private Entry(String key, String label, @DrawableRes int drawable) {
            this.key = key;
            this.label = label;
            this.drawable = drawable;
        }
    }
}
