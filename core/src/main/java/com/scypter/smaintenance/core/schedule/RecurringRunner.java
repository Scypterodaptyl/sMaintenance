package com.scypter.smaintenance.core.schedule;

import com.scypter.smaintenance.core.MaintenanceManager;

import java.util.List;
import java.util.TimeZone;

public final class RecurringRunner {
    public interface Managers {
        MaintenanceManager get(String scope);
    }

    private final List<RecurringSchedule> schedules;
    private final TimeZone tz;
    private final long leadMillis;

    public RecurringRunner(List<RecurringSchedule> schedules, TimeZone tz, long leadMillis) {
        this.schedules = schedules;
        this.tz = tz;
        this.leadMillis = leadMillis;
    }

    public void tick(long now, Managers managers) {
        for (RecurringSchedule s : schedules) {
            long next = s.nextStart(now, tz);
            if (next - now > leadMillis) {
                continue;
            }
            MaintenanceManager m = managers.get(s.scope);
            if (!m.isEnabled() && m.startsIn() == 0) {
                m.schedule("recurring", next - now, s.durationMillis, s.reason);
            }
        }
    }
}
