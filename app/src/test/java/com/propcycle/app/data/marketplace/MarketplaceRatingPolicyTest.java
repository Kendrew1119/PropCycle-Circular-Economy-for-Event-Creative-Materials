package com.propcycle.app.data.marketplace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public final class MarketplaceRatingPolicyTest {

    @Test
    public void summarize_ignoresInvalidValuesAndCalculatesAverage() {
        MarketplaceSellerRating first = rating(5L);
        MarketplaceSellerRating second = rating(3L);
        MarketplaceSellerRating invalid = rating(8L);

        MarketplaceRatingPolicy.Summary summary = MarketplaceRatingPolicy.summarize(
                Arrays.asList(first, second, invalid));

        assertEquals(2, summary.getCount());
        assertEquals(4d, summary.getAverage(), 0d);
        assertEquals("4.0 / 5 from 2 ratings", summary.displayText());
    }

    @Test
    public void summarize_emptyListShowsHonestState() {
        MarketplaceRatingPolicy.Summary summary = MarketplaceRatingPolicy.summarize(
                Collections.emptyList());
        assertEquals(0, summary.getCount());
        assertEquals(0d, summary.getAverage(), 0d);
        assertEquals("No marketplace ratings yet", summary.displayText());
    }

    @Test
    public void validation_acceptsSafeIdsAndOneToFiveStars() {
        assertTrue(MarketplaceRatingPolicy.isSafeSegment("seller-user_123"));
        assertEquals(1, MarketplaceRatingPolicy.requireScore(1));
        assertEquals(5, MarketplaceRatingPolicy.requireScore(5));
        assertThrows(IllegalArgumentException.class,
                () -> MarketplaceRatingPolicy.requireScore(0));
        assertThrows(IllegalArgumentException.class,
                () -> MarketplaceRatingPolicy.requireScore(6));
    }

    private static MarketplaceSellerRating rating(long score) {
        MarketplaceSellerRating rating = new MarketplaceSellerRating();
        rating.setScore(score);
        return rating;
    }
}
