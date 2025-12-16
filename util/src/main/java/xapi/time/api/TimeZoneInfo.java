package xapi.time.api;

import java.io.Serializable;

import static xapi.time.api.TimeComponents.*;

///
/// TimeZoneInfo:
///
/// A cross-platform representation of timezone information.
/// Implementations can wrap java.util.TimeZone (JRE) or use browser APIs (GWT).
///
/// Created by James X. Nelson (James@WeTheInter.net) on 02/10/2025 @ 01:21
public class TimeZoneInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String displayName;
    private final int offsetMillis; // raw offset in milliseconds
    private final boolean observesDST;

    public TimeZoneInfo(String id, String displayName, int offsetMillis, boolean observesDST) {
        this.id = id;
        this.displayName = displayName;
        this.offsetMillis = offsetMillis;
        this.observesDST = observesDST;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getOffsetMillis() {
        return offsetMillis;
    }

    public boolean observesDST() {
        return observesDST;
    }
    /**
     * Get the actual offset (including DST) for a specific moment in time.
     * This implementation handles US DST rules (2nd Sunday Mar @ 2am - 1st Sunday Nov @ 2am).
     * Override in platform-specific subclasses for other DST rules.
     *
     * @param epochMillis the time to check
     * @return offset in milliseconds
     */
    public int getOffsetAt(double epochMillis) {
        if (!observesDST) {
            return offsetMillis;
        }
        return offsetMillis + (isInDSTPeriod(epochMillis) ? 3600000 : 0);
    }

    private boolean isInDSTPeriod(double epochMillis) {
        long millis = (long) epochMillis;

        // Work in local standard time
        long localMillis = millis + offsetMillis;

        // Extract year, month, day in local standard time
        int year = computeYearFromLocalMillis(localMillis);
        int month = computeMonthFromLocalMillis(localMillis, year);

        // Fast path: not in DST range
        if (month < 3 || month > 10) return false;  // Jan-Feb, Nov-Dec
        if (month > 3 && month < 11) return true;   // Apr-Oct

        // March or November: need to compute DST boundaries
        if (month == 3) {
            // DST starts: 2nd Sunday in March at 2:00 AM
            long dstStart = computeDSTStart(year);
            return localMillis >= dstStart;
        } else {
            // month == 11: DST ends: 1st Sunday in November at 2:00 AM
            long dstEnd = computeDSTEnd(year);
            return localMillis < dstEnd;
        }
    }

    private long computeDSTStart(int year) {
        // March 1st in local standard time
        long marchFirst = millisAtMonthStart(year, 3) - offsetMillis;
        int dowMarch1 = computeDayOfWeek(marchFirst + offsetMillis);

        // Days until first Sunday
        int daysToFirstSunday = (dowMarch1 == 0) ? 0 : (7 - dowMarch1);
        int secondSunday = daysToFirstSunday + 7 + 1; // +1 for 1-based day

        // 2:00 AM on 2nd Sunday
        return marchFirst + ((secondSunday - 1) * 86400000L) + (2 * 3600000L);
    }

    private long computeDSTEnd(int year) {
        // November 1st in local daylight time (standard + 1 hour)
        long novFirst = millisAtMonthStart(year, 11) - offsetMillis;
        int dowNov1 = computeDayOfWeek(novFirst + offsetMillis + 3600000);

        // Days until first Sunday
        int daysToFirstSunday = (dowNov1 == 0) ? 0 : (7 - dowNov1);
        int firstSunday = daysToFirstSunday + 1; // 1-based

        // 2:00 AM on 1st Sunday (in daylight time, so add DST offset)
        return novFirst + ((firstSunday - 1) * 86400000L) + (2 * 3600000L) + 3600000L;
    }

    // Add these helper methods (can reuse from TimeComponents)
    private static int computeYearFromLocalMillis(long localMillis) {
        // Same logic as TimeComponents.computeYear
        long daysSinceEpoch = localMillis / 86400000L;
        int year = 1970 + (int)(daysSinceEpoch / 365);
        while (localMillis < millisAtYearStart(year)) year--;
        while (localMillis >= millisAtYearStart(year + 1)) year++;
        return year;
    }

    private static int computeMonthFromLocalMillis(long localMillis, int year) {
        // Same logic as TimeComponents.computeMonth
        long yearStart = millisAtYearStart(year);
        long millisIntoYear = localMillis - yearStart;
        int daysIntoYear = (int)(millisIntoYear / 86400000L);
        boolean isLeap = ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0));
        int[] daysInMonth = {31, isLeap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        int dayCount = 0;
        for (int m = 0; m < 12; m++) {
            if (daysIntoYear < dayCount + daysInMonth[m]) return m + 1;
            dayCount += daysInMonth[m];
        }
        return 12;
    }

    @Override
    public String toString() {
        return "TimeZoneInfo{" +
                "id='" + id + '\'' +
                ", name='" + displayName + '\'' +
                ", offset=" + (offsetMillis / 3600000.0) + "h" +
                ", DST=" + observesDST +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeZoneInfo)) return false;
        TimeZoneInfo that = (TimeZoneInfo) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
