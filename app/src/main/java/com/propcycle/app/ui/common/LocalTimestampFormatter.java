package com.propcycle.app.ui.common;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Formats UTC epoch timestamps against an explicit local timezone and local day boundary. */
public final class LocalTimestampFormatter {

    private LocalTimestampFormatter() {
    }

    @NonNull
    public static String messageLabel(
            long timestampMillis,
            long nowMillis,
            @NonNull TimeZone localTimeZone,
            @NonNull Locale locale,
            boolean use24HourClock) {
        if (timestampMillis <= 0L) {
            return "";
        }
        String time = format(
                timestampMillis,
                use24HourClock ? "HH:mm" : "h:mm a",
                localTimeZone,
                locale);
        if (sameLocalDay(timestampMillis, nowMillis, localTimeZone)) {
            return time;
        }
        if (isPreviousLocalDay(timestampMillis, nowMillis, localTimeZone)) {
            return "Yesterday, " + time;
        }
        String datePattern = sameLocalYear(timestampMillis, nowMillis, localTimeZone)
                ? "d MMM" : "d MMM yyyy";
        return format(timestampMillis, datePattern, localTimeZone, locale) + ", " + time;
    }

    @NonNull
    public static String compactLabel(
            long timestampMillis,
            long nowMillis,
            @NonNull TimeZone localTimeZone,
            @NonNull Locale locale,
            boolean use24HourClock) {
        if (timestampMillis <= 0L) {
            return "";
        }
        if (sameLocalDay(timestampMillis, nowMillis, localTimeZone)) {
            return format(
                    timestampMillis,
                    use24HourClock ? "HH:mm" : "h:mm a",
                    localTimeZone,
                    locale);
        }
        if (isPreviousLocalDay(timestampMillis, nowMillis, localTimeZone)) {
            return "Yesterday";
        }
        return format(
                timestampMillis,
                sameLocalYear(timestampMillis, nowMillis, localTimeZone)
                        ? "d MMM" : "d MMM yyyy",
                localTimeZone,
                locale);
    }

    private static boolean sameLocalDay(long left, long right, @NonNull TimeZone zone) {
        Calendar first = calendar(left, zone);
        Calendar second = calendar(right, zone);
        return first.get(Calendar.ERA) == second.get(Calendar.ERA)
                && first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }

    private static boolean sameLocalYear(long left, long right, @NonNull TimeZone zone) {
        Calendar first = calendar(left, zone);
        Calendar second = calendar(right, zone);
        return first.get(Calendar.ERA) == second.get(Calendar.ERA)
                && first.get(Calendar.YEAR) == second.get(Calendar.YEAR);
    }

    private static boolean isPreviousLocalDay(
            long timestampMillis,
            long nowMillis,
            @NonNull TimeZone zone) {
        Calendar yesterday = calendar(nowMillis, zone);
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        Calendar timestamp = calendar(timestampMillis, zone);
        return yesterday.get(Calendar.ERA) == timestamp.get(Calendar.ERA)
                && yesterday.get(Calendar.YEAR) == timestamp.get(Calendar.YEAR)
                && yesterday.get(Calendar.DAY_OF_YEAR) == timestamp.get(Calendar.DAY_OF_YEAR);
    }

    @NonNull
    private static Calendar calendar(long millis, @NonNull TimeZone zone) {
        Calendar calendar = Calendar.getInstance(zone, Locale.ROOT);
        calendar.setTimeInMillis(millis);
        return calendar;
    }

    @NonNull
    private static String format(
            long millis,
            @NonNull String pattern,
            @NonNull TimeZone zone,
            @NonNull Locale locale) {
        SimpleDateFormat formatter = new SimpleDateFormat(pattern, locale);
        formatter.setTimeZone(zone);
        return formatter.format(new Date(millis));
    }
}
