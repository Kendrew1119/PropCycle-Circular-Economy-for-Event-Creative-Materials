package com.propcycle.app.data.lending;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public final class LendingPolicyTest {

    @Test
    public void itemValidation_normalisesChoicesMoneyAndCoarseLocation() {
        NewLendingItem item = LendingPolicy.validateItem(
                "  LED Light Set  ",
                "Useful for small events.",
                "Event gear",
                "Like new",
                "Meet-up",
                "Petaling Jaya",
                "7",
                "50.25",
                3.12345d,
                101.67891d);

        assertEquals("LED Light Set", item.getTitle());
        assertEquals("event_gear", item.getCategory());
        assertEquals("like_new", item.getCondition());
        assertEquals("meetup", item.getPickupMethod());
        assertEquals(5025L, item.getDepositMinor());
        assertEquals(3.12d, item.getLatitude(), 0d);
        assertEquals(101.68d, item.getLongitude(), 0d);
    }

    @Test
    public void itemValidation_rejectsUnsupportedAndOutOfRangeValues() {
        assertThrows(IllegalArgumentException.class, () -> LendingPolicy.validateItem(
                "AB", "Description", "Tools", "Good", "Pickup", "PJ",
                "7", "0", null, null));
        assertThrows(IllegalArgumentException.class, () -> LendingPolicy.validateItem(
                "Valid title", "Description", "Vehicle", "Good", "Pickup", "PJ",
                "7", "0", null, null));
        assertThrows(IllegalArgumentException.class, () -> LendingPolicy.validateItem(
                "Valid title", "Description", "Tools", "Good", "Pickup", "PJ",
                "32", "0", null, null));
        assertThrows(IllegalArgumentException.class, () -> LendingPolicy.validateItem(
                "Valid title", "Description", "Tools", "Good", "Pickup", "PJ",
                "7", "10.999", null, null));
    }

    @Test
    public void dateKeys_areInclusiveAndBoundedByItemMaximum() {
        List<String> keys = LendingPolicy.dateKeys(
                "2099-05-01", "2099-05-03", "2099-01-01", 7);
        assertEquals(List.of("2099-05-01", "2099-05-02", "2099-05-03"), keys);
        assertThrows(IllegalArgumentException.class, () -> LendingPolicy.dateKeys(
                "2099-05-01", "2099-05-04", "2099-01-01", 3));
        assertThrows(IllegalArgumentException.class, () -> LendingPolicy.dateKeys(
                "2098-12-31", "2099-01-01", "2099-01-01", 7));
    }

    @Test
    public void listFiltering_sortsByApproximateDistanceAndCapsResults() {
        List<LendingItem> source = new ArrayList<>();
        for (int index = 55; index >= 1; index--) {
            LendingItem item = new LendingItem();
            item.setId("item-" + index);
            item.setOwnerId("owner");
            item.setTitle("Tool " + index);
            item.setAreaLabel("Petaling Jaya");
            item.setCategory("tools");
            item.setStatus("available");
            item.setLatitude(3d);
            item.setLongitude(101d + index * 0.01d);
            source.add(item);
        }
        List<LendingItem> result = LendingPolicy.filterAndSort(
                source, "tool", "tools", 3d, 101d);
        assertEquals(LendingPolicy.MAX_RESULTS, result.size());
        assertEquals("item-6", result.get(0).getId());
        assertTrue(LendingPolicy.distanceKm(3d, 101d, 3d, 101.01d) > 1d);
    }

    @Test
    public void listFiltering_searchesTitleAreaAndReadableCategory() {
        LendingItem craft = new LendingItem();
        craft.setId("craft-1");
        craft.setOwnerId("owner");
        craft.setTitle("Cutting mat");
        craft.setAreaLabel("Kampar");
        craft.setCategory("event_gear");
        craft.setStatus("available");

        List<LendingItem> source = List.of(craft);
        assertEquals(1, LendingPolicy.filterAndSort(
                source, "cutting", "all", null, null).size());
        assertEquals(1, LendingPolicy.filterAndSort(
                source, "kampar", "all", null, null).size());
        assertEquals(1, LendingPolicy.filterAndSort(
                source, "event gear", "all", null, null).size());
        assertTrue(LendingPolicy.filterAndSort(
                source, "electronics", "all", null, null).isEmpty());
    }

    @Test
    public void itemValidation_acceptsAllowlistedDemoImageAndRejectsUnknownKey() {
        NewLendingItem item = LendingPolicy.validateItem(
                "Folding chairs", "Six chairs for a small event.", "Equipment", "Good",
                "Pickup", "Kampar", "7", "0", null, null, "folding_chairs");
        assertEquals("folding_chairs", item.getDemoImageKey());

        assertThrows(IllegalArgumentException.class, () -> LendingPolicy.validateItem(
                "Folding chairs", "Six chairs for a small event.", "Equipment", "Good",
                "Pickup", "Kampar", "7", "0", null, null, "unknown_sample"));
    }
}
