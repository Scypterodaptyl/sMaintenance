package com.scypter.smaintenance.paper;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VersionUtilTest {
    @Test void parsesBukkitVersions() {
        assertArrayEquals(new int[]{1, 8}, VersionUtil.parse("1.8.8-R0.1-SNAPSHOT"));
        assertArrayEquals(new int[]{1, 21}, VersionUtil.parse("1.21.11-R0.1-SNAPSHOT"));
        assertArrayEquals(new int[]{1, 16}, VersionUtil.parse("1.16.5-R0.1-SNAPSHOT"));
    }

    @Test void garbageFallsBackTo18() {
        assertArrayEquals(new int[]{1, 8}, VersionUtil.parse("weird"));
    }
}
