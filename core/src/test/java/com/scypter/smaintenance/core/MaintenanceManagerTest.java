package com.scypter.smaintenance.core;

import com.scypter.smaintenance.core.state.MaintenanceState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MaintenanceManagerTest {
    long now;
    List<String> events;
    MaintenanceManager m;

    @BeforeEach void setUp() {
        now = 1_000_000L;
        events = new ArrayList<String>();
        m = new MaintenanceManager(new TimeSource() {
            public long now() { return now; }
        }, new MaintenanceState(), new MaintenanceListener() {
            public void onEnabled(MaintenanceState s) { events.add("on"); }
            public void onDisabled(MaintenanceState s, boolean auto, String by) { events.add(auto ? "auto-off" : "off"); }
            public void onScheduled(MaintenanceState s) { events.add("sched"); }
            public void onScheduleCancelled() { events.add("cancel"); }
            public void onWarning(long left) { events.add("warn" + left / 1000); }
            public void onChanged() { }
        }, new long[]{60, 10, 5});
    }

    @Test void enableDisable() {
        assertTrue(m.enable("admin", 0, "x"));
        assertFalse(m.enable("admin", 0, "x"));
        assertTrue(m.isEnabled());
        assertTrue(m.disable("admin"));
        assertFalse(m.disable("admin"));
        assertEquals(java.util.Arrays.asList("on", "off"), events);
    }

    @Test void autoDisableAtEnd() {
        m.enable("admin", 10_000, "x");
        now += 9_999;
        m.tick();
        assertTrue(m.isEnabled());
        now += 1;
        m.tick();
        assertFalse(m.isEnabled());
        assertEquals("auto-off", events.get(events.size() - 1));
    }

    @Test void expiredEndAtDisablesOnFirstTick() {
        MaintenanceState s = new MaintenanceState();
        s.setEnabled(true);
        s.setEndAt(now - 1);
        MaintenanceManager mm = new MaintenanceManager(new TimeSource() {
            public long now() { return now; }
        }, s, new MaintenanceListener() {
            public void onEnabled(MaintenanceState st) { }
            public void onDisabled(MaintenanceState st, boolean auto, String by) { events.add("auto-off"); }
            public void onScheduled(MaintenanceState st) { }
            public void onScheduleCancelled() { }
            public void onWarning(long left) { }
            public void onChanged() { }
        }, new long[]{});
        mm.tick();
        assertFalse(mm.isEnabled());
        assertEquals(1, events.size());
    }

    @Test void extendAddsTime() {
        m.enable("admin", 10_000, "x");
        assertTrue(m.extend(5_000));
        assertEquals(15_000, m.timeLeft());
    }

    @Test void extendRejectedWhenOffOrNoEnd() {
        assertFalse(m.extend(5_000));
        m.enable("admin", 0, "x");
        assertFalse(m.extend(5_000));
    }

    @Test void scheduleStartsAndWarns() {
        assertFalse(m.schedule("admin", 0, 1000, "x"));
        assertTrue(m.schedule("admin", 61_000, 30_000, "upgrade"));
        m.tick();
        assertFalse(events.contains("warn60"));
        now += 1_000;
        m.tick();
        assertTrue(events.contains("warn60"));
        now += 50_000;
        m.tick();
        assertTrue(events.contains("warn10"));
        now += 10_000;
        m.tick();
        assertTrue(m.isEnabled());
        assertEquals("upgrade", m.snapshot().getReason());
        assertEquals(30_000, m.timeLeft());
        assertEquals(0, m.startsIn());
    }

    @Test void cancelSchedule() {
        m.schedule("admin", 60_000, 0, "x");
        assertTrue(m.cancelSchedule());
        assertFalse(m.cancelSchedule());
        now += 120_000;
        m.tick();
        assertFalse(m.isEnabled());
    }

    @Test void warningsFireOncePerMark() {
        m.schedule("admin", 61_000, 0, "x");
        now += 1_000;
        m.tick();
        m.tick();
        int count = 0;
        for (String e : events) if (e.equals("warn60")) count++;
        assertEquals(1, count);
    }
}
