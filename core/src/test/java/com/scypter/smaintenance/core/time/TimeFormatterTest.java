package com.scypter.smaintenance.core.time;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TimeFormatterTest {
    @Test void formats() {
        assertEquals("1h 30m", TimeFormatter.format(5_400_000L));
        assertEquals("45s", TimeFormatter.format(45_000L));
        assertEquals("1d 2h", TimeFormatter.format(93_600_000L));
        assertEquals("0s", TimeFormatter.format(0L));
        assertEquals("0s", TimeFormatter.format(-5L));
    }
}
