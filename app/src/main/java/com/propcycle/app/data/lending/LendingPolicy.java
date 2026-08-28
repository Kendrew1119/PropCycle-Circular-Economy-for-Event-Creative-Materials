package com.propcycle.app.data.lending;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

/** Pure validation, date, location, and presentation rules for lending. */
public final class LendingPolicy {

    public static final int MAX_REQUEST_DAYS = 31;
    public static final int MAX_RESULTS = 50;
    public static final double LOCATION_ROUNDING_FACTOR = 100d;
    private static final TimeZone MALAYSIA = TimeZone.getTimeZone("Asia/Kuala_Lumpur");
    private static final Pattern SAFE_SEGMENT = Pattern.compile("[A-Za-z0-9_-]{1,128}");
    private static final Pattern ISO_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private LendingPolicy() {
    }

    @NonNull
    public static NewLendingItem validateItem(
            @Nullable String title,
            @Nullable String description,
            @Nullable String categoryLabel,
            @Nullable String conditionLabel,
            @Nullable String pickupLabel,
            @Nullable String areaLabel,
            @Nullable String maxDaysText,
            @Nullable String depositText,
            @Nullable Double latitude,
            @Nullable Double longitude) {
        String cleanTitle = clean(title);
        String cleanDescription = clean(description);
        String cleanArea = clean(areaLabel);
        if (cleanTitle.length() < 3 || cleanTitle.length() > 100) {
            throw new IllegalArgumentException("Title must contain 3 to 100 characters.");
        }
        if (cleanDescription.isEmpty() || cleanDescription.length() > 1000) {
            throw new IllegalArgumentException("Description must contain 1 to 1000 characters.");
        }
        if (cleanArea.length() < 2 || cleanArea.length() > 100) {
            throw new IllegalArgumentException("Area must contain 2 to 100 characters.");
        }
        String category = stableId(categoryLabel);
        if (!Arrays.asList("equipment", "tools", "electronics", "event_gear", "craft", "other")
                .contains(category)) {
            throw new IllegalArgumentException("Choose a supported category.");
        }
        String condition = stableId(conditionLabel);
        if (!Arrays.asList("new", "like_new", "good", "fair").contains(condition)) {
            throw new IllegalArgumentException("Choose a supported condition.");
        }
        String pickup = stableId(pickupLabel);
        if ("meet_up".equals(pickup)) {
            pickup = "meetup";
        }
        if (!Arrays.asList("pickup", "meetup").contains(pickup)) {
            throw new IllegalArgumentException("Choose Pickup or Meet-up.");
        }
        int maxDays;
        try {
            maxDays = Integer.parseInt(clean(maxDaysText));
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Maximum borrow days must be a whole number.");
        }
        if (maxDays < 1 || maxDays > MAX_REQUEST_DAYS) {
            throw new IllegalArgumentException("Maximum borrow days must be between 1 and 31.");
        }
        long depositMinor = parseMoneyMinor(depositText);
        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException("Approximate location is incomplete.");
        }
        Double safeLatitude = latitude == null ? null : roundLatitude(latitude);
        Double safeLongitude = longitude == null ? null : roundLongitude(longitude);
        return new NewLendingItem(
                cleanTitle,
                cleanTitle.toLowerCase(Locale.ROOT),
                cleanDescription,
                category,
                condition,
                pickup,
                cleanArea,
                maxDays,
                depositMinor,
                safeLatitude,
                safeLongitude);
    }

    public static long parseMoneyMinor(@Nullable String value) {
        String clean = clean(value);
        if (clean.isEmpty()) {
            return 0L;
        }
        try {
            BigDecimal amount = new BigDecimal(clean).setScale(2, RoundingMode.UNNECESSARY);
            long minor = amount.movePointRight(2).longValueExact();
            if (minor < 0 || minor > 10_000_000L) {
                throw new ArithmeticException();
            }
            return minor;
        } catch (ArithmeticException | NumberFormatException error) {
            throw new IllegalArgumentException("Deposit must be RM 0.00 to RM 100,000.00.");
        }
    }

    @NonNull
    public static List<String> dateKeys(
            @NonNull String startDate,
            @NonNull String endDate,
            @NonNull String today,
            int itemMaximumDays) {
        Date start = parseDate(startDate);
        Date end = parseDate(endDate);
        Date minimum = parseDate(today);
        if (start.before(minimum)) {
            throw new IllegalArgumentException("The borrowing date cannot be in the past.");
        }
        if (end.before(start)) {
            throw new IllegalArgumentException("The end date cannot be before the start date.");
        }
        int allowedDays = Math.max(1, Math.min(MAX_REQUEST_DAYS, itemMaximumDays));
        List<String> keys = new ArrayList<>();
        java.util.Calendar cursor = java.util.Calendar.getInstance(MALAYSIA, Locale.ROOT);
        cursor.setTime(start);
        java.util.Calendar last = java.util.Calendar.getInstance(MALAYSIA, Locale.ROOT);
        last.setTime(end);
        while (!cursor.after(last)) {
            if (keys.size() >= allowedDays) {
                throw new IllegalArgumentException(
                        "Choose no more than " + allowedDays + " inclusive days.");
            }
            keys.add(formatDate(cursor.getTime()));
            cursor.add(java.util.Calendar.DAY_OF_MONTH, 1);
        }
        return Collections.unmodifiableList(keys);
    }

    @NonNull
    public static String todayMalaysia() {
        return formatDate(new Date());
    }

    public static double roundLatitude(double value) {
        if (!Double.isFinite(value) || value < -90d || value > 90d) {
            throw new IllegalArgumentException("Latitude is invalid.");
        }
        return Math.round(value * LOCATION_ROUNDING_FACTOR) / LOCATION_ROUNDING_FACTOR;
    }

    public static double roundLongitude(double value) {
        if (!Double.isFinite(value) || value < -180d || value > 180d) {
            throw new IllegalArgumentException("Longitude is invalid.");
        }
        return Math.round(value * LOCATION_ROUNDING_FACTOR) / LOCATION_ROUNDING_FACTOR;
    }

    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0088d;
        double latitudeDelta = Math.toRadians(lat2 - lat1);
        double longitudeDelta = Math.toRadians(lon2 - lon1);
        double first = Math.sin(latitudeDelta / 2d);
        double second = Math.sin(longitudeDelta / 2d);
        double a = first * first
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * second * second;
        return earthRadiusKm * 2d * Math.atan2(Math.sqrt(a), Math.sqrt(1d - a));
    }

    @NonNull
    public static List<LendingItem> filterAndSort(
            @NonNull List<LendingItem> source,
            @Nullable String query,
            @Nullable String category,
            @Nullable Double userLatitude,
            @Nullable Double userLongitude) {
        String cleanQuery = clean(query).toLowerCase(Locale.ROOT);
        String cleanCategory = stableId(category);
        List<LendingItem> result = new ArrayList<>();
        for (LendingItem item : source) {
            if (item == null || !"available".equals(item.getStatus())) {
                continue;
            }
            String title = clean(item.getTitle()).toLowerCase(Locale.ROOT);
            String area = clean(item.getAreaLabel()).toLowerCase(Locale.ROOT);
            if (!cleanQuery.isEmpty() && !title.contains(cleanQuery) && !area.contains(cleanQuery)) {
                continue;
            }
            if (!cleanCategory.isEmpty() && !"all".equals(cleanCategory)
                    && !cleanCategory.equals(item.getCategory())) {
                continue;
            }
            result.add(item);
            if (result.size() >= MAX_RESULTS) {
                break;
            }
        }
        if (userLatitude != null && userLongitude != null) {
            result.sort(Comparator.comparingDouble(item -> item.hasApproximateLocation()
                    ? distanceKm(userLatitude, userLongitude,
                            item.getLatitude(), item.getLongitude())
                    : Double.MAX_VALUE));
        } else {
            result.sort((left, right) -> clean(left.getTitle())
                    .compareToIgnoreCase(clean(right.getTitle())));
        }
        return result;
    }

    public static boolean isSafeSegment(@Nullable String value) {
        return value != null && SAFE_SEGMENT.matcher(value).matches();
    }

    @NonNull
    public static String stableId(@Nullable String label) {
        return clean(label).toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replaceAll("\\s+", "_");
    }

    @NonNull
    public static String displayLabel(@Nullable String id) {
        String clean = clean(id).replace('_', ' ');
        if (clean.isEmpty()) {
            return "";
        }
        return clean.substring(0, 1).toUpperCase(Locale.ROOT) + clean.substring(1);
    }

    @NonNull
    private static Date parseDate(@NonNull String value) {
        if (!ISO_DATE.matcher(value).matches()) {
            throw new IllegalArgumentException("Choose a valid date.");
        }
        SimpleDateFormat format = dateFormat();
        try {
            Date parsed = format.parse(value);
            if (parsed == null || !value.equals(format.format(parsed))) {
                throw new IllegalArgumentException("Choose a valid date.");
            }
            return parsed;
        } catch (ParseException error) {
            throw new IllegalArgumentException("Choose a valid date.");
        }
    }

    @NonNull
    private static String formatDate(@NonNull Date date) {
        return dateFormat().format(date);
    }

    @NonNull
    private static SimpleDateFormat dateFormat() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        format.setLenient(false);
        format.setTimeZone(MALAYSIA);
        return format;
    }

    @NonNull
    private static String clean(@Nullable String value) {
        return value == null ? "" : value.trim();
    }
}
