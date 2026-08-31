package com.propcycle.app.data.marketplace;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Pure validation and presentation rules for marketplace seller ratings. */
public final class MarketplaceRatingPolicy {

    public static final int MIN_SCORE = 1;
    public static final int MAX_SCORE = 5;
    public static final int MAX_RATINGS = 100;
    private static final Pattern SAFE_SEGMENT = Pattern.compile("[A-Za-z0-9_-]{1,128}");

    private MarketplaceRatingPolicy() {
    }

    public static boolean isSafeSegment(@Nullable String value) {
        return value != null && SAFE_SEGMENT.matcher(value).matches();
    }

    public static int requireScore(int score) {
        if (score < MIN_SCORE || score > MAX_SCORE) {
            throw new IllegalArgumentException("Choose a rating from 1 to 5 stars.");
        }
        return score;
    }

    @NonNull
    public static Summary summarize(@Nullable List<MarketplaceSellerRating> ratings) {
        if (ratings == null || ratings.isEmpty()) {
            return new Summary(0, 0d);
        }
        long total = 0L;
        int count = 0;
        for (MarketplaceSellerRating rating : ratings) {
            Long score = rating == null ? null : rating.getScore();
            if (score != null && score >= MIN_SCORE && score <= MAX_SCORE) {
                total += score;
                count++;
            }
        }
        return new Summary(count, count == 0 ? 0d : total / (double) count);
    }

    public static final class Summary {
        private final int count;
        private final double average;

        private Summary(int count, double average) {
            this.count = count;
            this.average = average;
        }

        public int getCount() { return count; }
        public double getAverage() { return average; }

        @NonNull
        public String displayText() {
            if (count == 0) {
                return "No marketplace ratings yet";
            }
            return String.format(
                    Locale.ROOT,
                    "%.1f / 5 from %d %s",
                    average,
                    count,
                    count == 1 ? "rating" : "ratings");
        }
    }
}
