package xapi.time.api;

import xapi.fu.Lazy;

import java.io.Serializable;
import java.util.Objects;

///
/// TimeComponents:
///
/// A breakdown of a moment in time into human-readable components.
/// Replaces java.time.LocalDateTime/ZonedDateTime for cross-platform use.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 02/10/2025 @ 01:22
public class TimeComponents implements Serializable {
    private static final long serialVersionUID = 1L;

    private final double epochMillis;
    private final TimeZoneInfo zone;

    // Lazily computed fields
    private final Lazy<Integer> year;
    private final Lazy<Integer> month;        // 1-12
    private final Lazy<Integer> dayOfMonth;   // 1-31
    private final Lazy<Integer> hour;         // 0-23
    private final Lazy<Integer> minute;       // 0-59
    private final Lazy<Integer> second;       // 0-59
    private final Lazy<Integer> millisecond;  // 0-999
    private final Lazy<Integer> dayOfWeek;    // 0=Sunday, 1=Monday, ..., 6=Saturday
    private final Lazy<Integer> dayOfYear;    // 1-366

    public TimeComponents(double epochMillis, TimeZoneInfo zone) {
        this.epochMillis = epochMillis;
        this.zone = zone;

        // Compute local millis once
        final Lazy<Long> localMillis = Lazy.deferred1(() ->
                (long) epochMillis + zone.getOffsetAt(epochMillis)
        );

        // All fields derive from localMillis
        this.year = Lazy.deferred1(() -> computeYear(localMillis.out1()));
        this.month = Lazy.deferred1(() -> computeMonth(localMillis.out1()));
        this.dayOfMonth = Lazy.deferred1(() -> computeDayOfMonth(localMillis.out1()));
        this.hour = Lazy.deferred1(() -> computeHour(localMillis.out1()));
        this.minute = Lazy.deferred1(() -> computeMinute(localMillis.out1()));
        this.second = Lazy.deferred1(() -> computeSecond(localMillis.out1()));
        this.millisecond = Lazy.deferred1(() -> computeMillisecond(localMillis.out1()));
        this.dayOfWeek = Lazy.deferred1(() -> computeDayOfWeek(localMillis.out1()));
        this.dayOfYear = Lazy.deferred1(() -> computeDayOfYear(localMillis.out1(), year.out1(), month.out1(), dayOfMonth.out1()));
    }

    // Getters that trigger lazy evaluation
    public double getEpochMillis() { return epochMillis; }
    public TimeZoneInfo getZone() { return zone; }
    public int getYear() { return year.out1(); }
    public int getMonth() { return month.out1(); }
    public int getDayOfMonth() { return dayOfMonth.out1(); }
    public int getHour() { return hour.out1(); }
    public int getMinute() { return minute.out1(); }
    public int getSecond() { return second.out1(); }
    public int getMillisecond() { return millisecond.out1(); }
    public int getDayOfWeek() { return dayOfWeek.out1(); }
    public int getDayOfYear() { return dayOfYear.out1(); }

    // Computation helpers using standard epoch math
    private static int computeYear(long localMillis) {
        // Days since Unix epoch (1970-01-01)
        long daysSinceEpoch = localMillis / 86400000L;

        // Approximate year (will refine)
        int year = 1970 + (int)(daysSinceEpoch / 365);

        // Adjust for actual year start
        while (localMillis < millisAtYearStart(year)) {
            year--;
        }
        while (localMillis >= millisAtYearStart(year + 1)) {
            year++;
        }
        return year;
    }

    private static int computeMonth(long localMillis) {
        int year = computeYear(localMillis);
        long yearStart = millisAtYearStart(year);
        long millisIntoYear = localMillis - yearStart;
        int daysIntoYear = (int)(millisIntoYear / 86400000L);

        boolean isLeap = isLeapYear(year);
        int[] daysInMonth = {31, isLeap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

        int dayCount = 0;
        for (int m = 0; m < 12; m++) {
            if (daysIntoYear < dayCount + daysInMonth[m]) {
                return m + 1; // 1-based
            }
            dayCount += daysInMonth[m];
        }
        return 12;
    }

    private static int computeDayOfMonth(long localMillis) {
        int year = computeYear(localMillis);
        int month = computeMonth(localMillis);
        long monthStart = millisAtMonthStart(year, month);
        long millisIntoMonth = localMillis - monthStart;
        return (int)(millisIntoMonth / 86400000L) + 1; // 1-based
    }

    private static int computeHour(long localMillis) {
        return (int)((localMillis / 3600000L) % 24);
    }

    private static int computeMinute(long localMillis) {
        return (int)((localMillis / 60000L) % 60);
    }

    private static int computeSecond(long localMillis) {
        return (int)((localMillis / 1000L) % 60);
    }

    private static int computeMillisecond(long localMillis) {
        return (int)(localMillis % 1000L);
    }

    public static int computeDayOfWeek(long localMillis) {
        // 1970-01-01 was a Thursday (4)
        long daysSinceEpoch = localMillis / 86400000L;
        return (int)((daysSinceEpoch + 4) % 7); // 0=Sunday
    }

    private static int computeDayOfYear(long localMillis, int year, int month, int dayOfMonth) {
        boolean isLeap = isLeapYear(year);
        int[] daysInMonth = {31, isLeap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

        int dayOfYear = dayOfMonth;
        for (int m = 1; m < month; m++) {
            dayOfYear += daysInMonth[m - 1];
        }
        return dayOfYear; // 1-366
    }

    // Helper: millis at start of year
    public static long millisAtYearStart(int year) {
        // Days from 1970 to year start
        long days = 0;
        for (int y = 1970; y < year; y++) {
            days += isLeapYear(y) ? 366 : 365;
        }
        return days * 86400000L;
    }

    // Helper: millis at start of month
    public static long millisAtMonthStart(int year, int month) {
        long yearStart = millisAtYearStart(year);
        boolean isLeap = isLeapYear(year);
        int[] daysInMonth = {31, isLeap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

        int days = 0;
        for (int m = 1; m < month; m++) {
            days += daysInMonth[m - 1];
        }
        return yearStart + (days * 86400000L);
    }

    private static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }

    @Override
    public String toString() {
        return String.format("%04d-%02d-%02d %02d:%02d:%02d.%03d [dow=%d, doy=%d] (%s)",
                getYear(), getMonth(), getDayOfMonth(), getHour(), getMinute(),
                getSecond(), getMillisecond(), getDayOfWeek(), getDayOfYear(), zone.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeComponents)) return false;
        TimeComponents that = (TimeComponents) o;
        return Double.compare(epochMillis, that.epochMillis) == 0
                && Objects.equals(zone.getId(), that.zone.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(epochMillis, zone.getId());
    }
}
