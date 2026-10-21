package com.scypter.smaintenance.core.time;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TimeParserTest {
    @Test void singleUnits() {
        assertEquals(30_000L, TimeParser.parseMillis("30s"));
        assertEquals(600_000L, TimeParser.parseMillis("10m"));
        assertEquals(7_200_000L, TimeParser.parseMillis("2h"));
        assertEquals(86_400_000L, TimeParser.parseMillis("1d"));
    }
    @Test void combined() {
        assertEquals(5_400_000L, TimeParser.parseMillis("1h30m"));
    }
    @Test void caseInsensitive() {
        assertEquals(60_000L, TimeParser.parseMillis("1M"));
    }
    @Test void rejectsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> TimeParser.parseMillis("abc"));
        assertThrows(IllegalArgumentException.class, () -> TimeParser.parseMillis("0s"));
        assertThrows(IllegalArgumentException.class, () -> TimeParser.parseMillis("-5m"));
        assertThrows(IllegalArgumentException.class, () -> TimeParser.parseMillis(""));
        assertThrows(IllegalArgumentException.class, () -> TimeParser.parseMillis("10"));
    }
}
