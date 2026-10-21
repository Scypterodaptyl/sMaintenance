package com.scypter.smaintenance.core.schedule;

import com.scypter.smaintenance.core.MaintenanceManager;
import com.scypter.smaintenance.core.TimeSource;
import com.scypter.smaintenance.core.history.HistoryEntry;
import com.scypter.smaintenance.core.history.HistoryLog;
import com.scypter.smaintenance.core.proxy.ServerQueue;
import com.scypter.smaintenance.core.state.MaintenanceState;
import com.scypter.smaintenance.core.update.UpdateChecker;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RecurringTest {
    static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    static long at(int y, int mo, int d, int h, int mi) {
        Calendar c = Calendar.getInstance(UTC);
        c.clear();
        c.set(y, mo - 1, d, h, mi, 0);
        return c.getTimeInMillis();
    }

    @Test void parsesWithAndWithoutScope() {
        RecurringSchedule a = RecurringSchedule.parse("sun 04:00 30m Weekly upgrade", "server", false);
        assertEquals("server", a.scope);
        assertEquals(Calendar.SUNDAY, a.dayOfWeek);
        assertEquals(30 * 60_000L, a.durationMillis);
        assertEquals("Weekly upgrade", a.reason);
        RecurringSchedule b = RecurringSchedule.parse("Survival daily 23:15 1h", "x", true);
        assertEquals("survival", b.scope);
        assertEquals(0, b.dayOfWeek);
        assertEquals(15, b.minute);
    }

    @Test void rejectsBadLines() {
        assertThrows(IllegalArgumentException.class, () -> RecurringSchedule.parse("funday 04:00 30m", "s", false));
        assertThrows(IllegalArgumentException.class, () -> RecurringSchedule.parse("sun 25:00 30m", "s", false));
        assertThrows(IllegalArgumentException.class, () -> RecurringSchedule.parse("sun 04:00 0s", "s", false));
        assertThrows(IllegalArgumentException.class, () -> RecurringSchedule.parse("sun", "s", false));
    }

    @Test void nextStartFindsFollowingSunday() {
        RecurringSchedule s = RecurringSchedule.parse("sun 04:00 30m", "s", false);
        long wed = at(2026, 9, 30, 12, 0);
        assertEquals(at(2026, 10, 4, 4, 0), s.nextStart(wed, UTC));
        long sunEarly = at(2026, 10, 4, 3, 0);
        assertEquals(at(2026, 10, 4, 4, 0), s.nextStart(sunEarly, UTC));
        long sunLate = at(2026, 10, 4, 5, 0);
        assertEquals(at(2026, 10, 11, 4, 0), s.nextStart(sunLate, UTC));
    }

    @Test void dailyRollsToTomorrow() {
        RecurringSchedule s = RecurringSchedule.parse("daily 04:00 10m", "s", false);
        assertEquals(at(2026, 10, 1, 4, 0), s.nextStart(at(2026, 9, 30, 4, 0), UTC));
    }

    @Test void runnerSchedulesWithinLeadOnly() {
        final long[] now = {at(2026, 10, 4, 3, 40)};
        MaintenanceManager m = new MaintenanceManager(new TimeSource() {
            public long now() { return now[0]; }
        }, new MaintenanceState(), new com.scypter.smaintenance.core.MaintenanceListener() {
            public void onEnabled(MaintenanceState s) { }
            public void onDisabled(MaintenanceState s, boolean a, String b) { }
            public void onScheduled(MaintenanceState s) { }
            public void onScheduleCancelled() { }
            public void onWarning(long l) { }
            public void onChanged() { }
        }, new long[]{});
        RecurringRunner r = new RecurringRunner(Arrays.asList(RecurringSchedule.parse("sun 04:00 30m weekly", "server", false)), UTC, 5 * 60_000L);
        RecurringRunner.Managers ms = new RecurringRunner.Managers() {
            public MaintenanceManager get(String scope) { return m; }
        };
        r.tick(now[0], ms);
        assertEquals(0, m.startsIn());
        now[0] = at(2026, 10, 4, 3, 56);
        r.tick(now[0], ms);
        assertEquals(4 * 60_000L, m.startsIn());
        assertEquals("weekly", m.snapshot().getScheduledReason());
    }

    @Test void historyKeepsNewestAndCaps() {
        HistoryLog h = new HistoryLog(new ArrayList<HistoryEntry>(), 3);
        for (int i = 0; i < 5; i++) {
            h.add(new HistoryEntry(i, "s", "a", "r" + i, 0));
        }
        assertEquals(3, h.snapshot().size());
        assertEquals("r4", h.last(2).get(0).reason);
        assertEquals("r3", h.last(2).get(1).reason);
    }

    @Test void versionComparison() {
        assertTrue(UpdateChecker.isNewer("0.1.0", "0.2.0"));
        assertTrue(UpdateChecker.isNewer("0.1.9", "0.1.10"));
        assertFalse(UpdateChecker.isNewer("1.0.0", "1.0.0"));
        assertFalse(UpdateChecker.isNewer("1.2", "1.1.9"));
        assertFalse(UpdateChecker.isNewer(null, "1.0"));
        assertTrue(UpdateChecker.isNewer("1.0", "1.0.1"));
    }

    @Test void queueDrainsOnce() {
        ServerQueue q = new ServerQueue();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        q.add("Survival", a);
        q.add("survival", b);
        q.add("survival", a);
        assertEquals(2, q.drain("SURVIVAL").size());
        assertTrue(q.drain("survival").isEmpty());
        q.add("lobby", a);
        q.remove(a);
        assertTrue(q.drain("lobby").isEmpty());
    }
}
