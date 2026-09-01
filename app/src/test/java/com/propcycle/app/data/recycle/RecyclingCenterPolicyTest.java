package com.propcycle.app.data.recycle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public final class RecyclingCenterPolicyTest {

    @Test
    public void normalizeArea_trimsAndCollapsesWhitespace() {
        assertEquals(
                "Petaling Jaya",
                RecyclingCenterPolicy.normalizeArea("  Petaling   Jaya  "));
    }

    @Test
    public void areaValidation_enforcesUsefulBoundaries() {
        assertFalse(RecyclingCenterPolicy.isValidArea("P"));
        assertTrue(RecyclingCenterPolicy.isValidArea("PJ"));
        assertTrue(RecyclingCenterPolicy.isValidArea("A".repeat(80)));
        assertFalse(RecyclingCenterPolicy.isValidArea("A".repeat(81)));
    }

    @Test
    public void manualQuery_addsMalaysiaOnlyWhenNeeded() {
        assertEquals(
                "recycling centre near Petaling Jaya, Malaysia",
                RecyclingCenterPolicy.buildManualQuery("Petaling Jaya"));
        assertEquals(
                "recycling centre near Johor, Malaysia",
                RecyclingCenterPolicy.buildManualQuery("Johor, Malaysia"));
    }

    @Test
    public void distanceKm_returnsReasonableStraightLineEstimate() {
        GeoPoint kualaLumpur = new GeoPoint(3.1390d, 101.6869d);
        GeoPoint petalingJaya = new GeoPoint(3.1073d, 101.6067d);

        double result = RecyclingCenterPolicy.distanceKm(kualaLumpur, petalingJaya);

        assertTrue(result > 5d);
        assertTrue(result < 15d);
    }

    @Test
    public void prepareResults_sortsByDistanceAndCapsAtTen() {
        GeoPoint origin = new GeoPoint(3d, 101d);
        List<RecyclingCenter> values = new ArrayList<>();
        for (int index = 12; index >= 1; index--) {
            values.add(center("id-" + index, 3d, 101d + index * 0.01d, 4.2d));
        }

        List<RecyclingCenter> result = RecyclingCenterPolicy.prepareResults(values, origin);

        assertEquals(RecyclingCenterPolicy.MAX_RESULTS, result.size());
        assertEquals("id-1", result.get(0).getId());
        assertTrue(result.get(0).getDistanceKm() < result.get(9).getDistanceKm());
    }

    @Test
    public void prepareResults_withMeasurementOrigin_addsDistanceToAreaResults() {
        GeoPoint userLocation = new GeoPoint(3.1390d, 101.6869d);
        List<RecyclingCenter> areaResults = List.of(
                center("pj", 3.1073d, 101.6067d, 4.4d));

        List<RecyclingCenter> result = RecyclingCenterPolicy.prepareResults(
                areaResults,
                userLocation);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getDistanceKm() > 5d);
        assertTrue(result.get(0).getDistanceKm() < 15d);
        assertTrue(RecyclingCenterPolicy.formatDistance(result.get(0).getDistanceKm())
                .startsWith("Approx."));
    }

    @Test
    public void invalidRatingAndDistance_areNotDisplayedAsFacts() {
        RecyclingCenter center = center("id", 3d, 101d, 8d);

        assertNull(center.getRating());
        assertEquals("No rating", RecyclingCenterPolicy.formatRating(center.getRating()));
        assertEquals(
                "Use location to estimate distance",
                RecyclingCenterPolicy.formatDistance(null));
    }

    private static RecyclingCenter center(
            String id,
            double latitude,
            double longitude,
            Double rating) {
        return new RecyclingCenter(
                id,
                "Centre " + id,
                "Test address",
                new GeoPoint(latitude, longitude),
                rating,
                null);
    }
}
