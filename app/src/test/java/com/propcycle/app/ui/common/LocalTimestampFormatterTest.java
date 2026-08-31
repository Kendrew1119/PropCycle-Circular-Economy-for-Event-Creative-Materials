package com.propcycle.app.ui.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public final class LocalTimestampFormatterTest {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final TimeZone MALAYSIA = TimeZone.getTimeZone("Asia/Kuala_Lumpur");

    @Test
    public void utcInstant_isDisplayedOnTheCorrectMalaysiaLocalDay() {
        long message = utc(2026, Calendar.AUGUST, 31, 16, 30);
        long now = utc(2026, Calendar.AUGUST, 31, 17, 0);

        assertEquals("00:30", LocalTimestampFormatter.messageLabel(
                message, now, MALAYSIA, Locale.ENGLISH, true));
        assertEquals("00:30", LocalTimestampFormatter.compactLabel(
                message, now, MALAYSIA, Locale.ENGLISH, true));
    }

    @Test
    public void previousUtcDay_canStillBeYesterdayInMalaysia() {
        long message = utc(2026, Calendar.AUGUST, 31, 15, 30);
        long now = utc(2026, Calendar.AUGUST, 31, 17, 0);

        assertEquals("Yesterday, 23:30", LocalTimestampFormatter.messageLabel(
                message, now, MALAYSIA, Locale.ENGLISH, true));
        assertEquals("Yesterday", LocalTimestampFormatter.compactLabel(
                message, now, MALAYSIA, Locale.ENGLISH, true));
    }

    @Test
    public void formatterHonours12HourClockAndOlderDates() {
        long message = utc(2025, Calendar.DECEMBER, 30, 4, 5);
        long now = utc(2026, Calendar.JANUARY, 2, 4, 5);

        assertEquals("30 Dec 2025, 12:05 PM", LocalTimestampFormatter.messageLabel(
                message, now, MALAYSIA, Locale.ENGLISH, false));
        assertEquals("30 Dec 2025", LocalTimestampFormatter.compactLabel(
                message, now, MALAYSIA, Locale.ENGLISH, false));
    }

    private static long utc(int year, int month, int day, int hour, int minute) {
        Calendar calendar = Calendar.getInstance(UTC, Locale.ROOT);
        calendar.clear();
        calendar.set(year, month, day, hour, minute, 0);
        return calendar.getTimeInMillis();
    }
}
